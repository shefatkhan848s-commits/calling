package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.SipAccountEntity
import com.example.sip.SipForegroundService
import com.example.ui.AccountFormDialog
import com.example.ui.AccountSelectorTopBar
import com.example.ui.AccountsScreen
import com.example.ui.ActiveCallScreen
import com.example.ui.DialerScreen
import com.example.ui.RecentsScreen
import com.example.ui.SplashScreen
import com.example.ui.theme.ShCallingTheme
import com.example.util.ApkShareHelper
import com.example.viewmodel.SoftphoneViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class MainTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DIALER(R.string.tab_dialer, Icons.Filled.Dialpad, Icons.Outlined.Dialpad),
    RECENTS(R.string.tab_recents, Icons.Filled.History, Icons.Outlined.History),
    ACCOUNTS(R.string.tab_accounts, Icons.Filled.ManageAccounts, Icons.Outlined.ManageAccounts)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SipForegroundService.start(this)

        setContent {
            ShCallingTheme {
                ShCallingAppRoot()
            }
        }
    }
}

@Composable
fun ShCallingAppRoot(
    viewModel: SoftphoneViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showSplash by rememberSaveable { mutableStateOf(true) }
    var currentTab by rememberSaveable { mutableStateOf(MainTab.DIALER) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<SipAccountEntity?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasMicPermission = grants[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(showSplash) {
        if (!showSplash) {
            val permissionsToRequest = buildList {
                if (!hasMicPermission) add(Manifest.permission.RECORD_AUDIO)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notifGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!notifGranted) add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            if (permissionsToRequest.isNotEmpty()) {
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Back navigation from secondary tabs returns to Dialer
    BackHandler(enabled = !showSplash && !uiState.activeCall.isCallActive && currentTab != MainTab.DIALER) {
        currentTab = MainTab.DIALER
    }

    when {
        showSplash -> {
            SplashScreen(onFinished = { showSplash = false })
        }

        uiState.activeCall.isCallActive -> {
            ActiveCallScreen(
                callState = uiState.activeCall,
                onToggleMute = viewModel::toggleMute,
                onToggleSpeaker = viewModel::toggleSpeaker,
                onToggleHold = viewModel::toggleHold,
                onSendDtmf = viewModel::sendInCallDtmf,
                onAnswerCall = viewModel::answerCall,
                onDeclineCall = viewModel::declineCall,
                onEndCall = viewModel::endCall
            )
        }

        else -> {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    Column(modifier = Modifier.statusBarsPadding()) {
                        AccountSelectorTopBar(
                            accounts = uiState.accounts,
                            activeAccount = uiState.activeAccount,
                            onSelectAccount = viewModel::selectActiveAccount,
                            onOpenAccounts = { currentTab = MainTab.ACCOUNTS }
                        )
                    }
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Crossfade(
                    targetState = currentTab,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    label = "main_tab_crossfade"
                ) { tab ->
                    when (tab) {
                        MainTab.DIALER -> {
                            DialerScreen(
                                dialedNumber = uiState.dialedNumber,
                                hasMicPermission = hasMicPermission,
                                onRequestMicPermission = {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                                },
                                onDigitPress = viewModel::appendDigit,
                                onBackspace = viewModel::deleteLastDigit,
                                onClearAll = viewModel::clearDialedNumber,
                                onCallClick = {
                                    if (!hasMicPermission) {
                                        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                                    } else {
                                        viewModel.placeCall()
                                    }
                                }
                            )
                        }

                        MainTab.RECENTS -> {
                            RecentsScreen(
                                recentCalls = uiState.recentCalls,
                                onRedial = { number, accountId ->
                                    viewModel.setDialedNumber(number)
                                    if (!hasMicPermission) {
                                        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                                    } else {
                                        viewModel.placeCall(
                                            destinationOverride = number,
                                            accountIdOverride = accountId
                                        )
                                    }
                                },
                                onClearHistory = viewModel::clearCallHistory
                            )
                        }

                        MainTab.ACCOUNTS -> {
                            AccountsScreen(
                                accounts = uiState.accounts,
                                activeAccountId = uiState.activeAccount?.entity?.id,
                                onAddAccountClick = {
                                    editingAccount = null
                                    showAccountDialog = true
                                },
                                onEditAccountClick = { account ->
                                    editingAccount = account
                                    showAccountDialog = true
                                },
                                onDeleteAccountClick = viewModel::deleteSipAccount,
                                onSelectActiveAccount = viewModel::selectActiveAccount,
                                onReregisterAccount = viewModel::reregisterAccount,
                                onShareApkClick = {
                                    coroutineScope.launch {
                                        val result = ApkShareHelper.shareInstalledApk(context)
                                        result.onFailure { err ->
                                            snackbarHostState.showSnackbar(
                                                err.localizedMessage ?: "Unable to share APK"
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (showAccountDialog) {
                AccountFormDialog(
                    editingAccount = editingAccount,
                    onDismiss = {
                        showAccountDialog = false
                        editingAccount = null
                    },
                    onSave = { id, label, domain, port, username, password, transport ->
                        viewModel.saveSipAccount(
                            existingId = id,
                            label = label,
                            domain = domain,
                            port = port,
                            username = username,
                            password = password,
                            transport = transport
                        )
                        showAccountDialog = false
                        editingAccount = null
                    }
                )
            }
        }
    }
}
