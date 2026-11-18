package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.SipAccountEntity
import com.example.data.SipTransport
import com.example.sip.RegistrationStatus
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.StatusFailedRed
import com.example.ui.theme.StatusOfflineSlate
import com.example.ui.theme.StatusRegisteredGreen
import com.example.ui.theme.StatusRegisteringAmber
import com.example.viewmodel.SipAccountUiModel

@Composable
fun RegistrationStatusDot(
    status: RegistrationStatus,
    size: Dp = 10.dp,
    modifier: Modifier = Modifier
) {
    val dotColor = when (status) {
        RegistrationStatus.REGISTERED -> StatusRegisteredGreen
        RegistrationStatus.REGISTERING -> StatusRegisteringAmber
        RegistrationStatus.FAILED -> StatusFailedRed
        RegistrationStatus.OFFLINE -> StatusOfflineSlate
    }

    Box(
        modifier = modifier
            .size(size + 6.dp)
            .clip(CircleShape)
            .background(dotColor.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(dotColor)
        )
    }
}

fun registrationStatusLabel(status: RegistrationStatus): String = when (status) {
    RegistrationStatus.REGISTERED -> "Registered"
    RegistrationStatus.REGISTERING -> "Registering…"
    RegistrationStatus.FAILED -> "Failed"
    RegistrationStatus.OFFLINE -> "Offline"
}

@Composable
fun registrationStatusColor(status: RegistrationStatus): Color = when (status) {
    RegistrationStatus.REGISTERED -> StatusRegisteredGreen
    RegistrationStatus.REGISTERING -> StatusRegisteringAmber
    RegistrationStatus.FAILED -> StatusFailedRed
    RegistrationStatus.OFFLINE -> StatusOfflineSlate
}

@Composable
fun AccountSelectorTopBar(
    accounts: List<SipAccountUiModel>,
    activeAccount: SipAccountUiModel?,
    onSelectAccount: (Long) -> Unit,
    onOpenAccounts: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (accounts.isEmpty()) {
                                onOpenAccounts()
                            } else {
                                dropdownExpanded = true
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("active_account_selector"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activeAccount != null) {
                        RegistrationStatusDot(status = activeAccount.status, size = 10.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeAccount?.entity?.label
                                ?: stringResource(R.string.no_account_selected),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (activeAccount != null) {
                                "${registrationStatusLabel(activeAccount.status)} • ${activeAccount.entity.username}@${activeAccount.entity.domain}"
                            } else {
                                stringResource(R.string.tap_to_add_account)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (activeAccount != null) {
                                registrationStatusColor(activeAccount.status)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(R.string.select_outgoing_account),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    accounts.forEach { acc ->
                        val isSelected = acc.entity.id == activeAccount?.entity?.id
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RegistrationStatusDot(status = acc.status, size = 9.dp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = acc.entity.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${acc.entity.username}@${acc.entity.domain}:${acc.entity.port} (${acc.entity.transport})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = BrandCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                dropdownExpanded = false
                                onSelectAccount(acc.entity.id)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            FilledTonalButton(
                onClick = onOpenAccounts,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("top_accounts_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ManageAccounts,
                    contentDescription = stringResource(R.string.tab_accounts),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.tab_accounts),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun AccountFormDialog(
    editingAccount: SipAccountEntity?,
    onDismiss: () -> Unit,
    onSave: (
        existingId: Long,
        label: String,
        domain: String,
        port: Int,
        username: String,
        password: String,
        transport: SipTransport
    ) -> Unit
) {
    var label by remember(editingAccount) { mutableStateOf(editingAccount?.label ?: "") }
    var domain by remember(editingAccount) { mutableStateOf(editingAccount?.domain ?: "") }
    var portText by remember(editingAccount) { mutableStateOf((editingAccount?.port ?: 5060).toString()) }
    var username by remember(editingAccount) { mutableStateOf(editingAccount?.username ?: "") }
    var password by remember(editingAccount) { mutableStateOf(editingAccount?.password ?: "") }
    var transport by remember(editingAccount) {
        mutableStateOf(editingAccount?.transportEnum ?: SipTransport.UDP)
    }
    var passwordVisible by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = if (editingAccount == null) {
                    stringResource(R.string.add_account)
                } else {
                    stringResource(R.string.edit_account)
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.field_account_label)) },
                    placeholder = { Text("e.g., Office Line 1") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_account_label")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = domain,
                        onValueChange = {
                            domain = it
                            validationError = null
                        },
                        label = { Text(stringResource(R.string.field_sip_domain)) },
                        placeholder = { Text("sip.provider.com") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.7f)
                            .testTag("input_sip_domain")
                    )

                    OutlinedTextField(
                        value = portText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 5) {
                                portText = input
                            }
                        },
                        label = { Text(stringResource(R.string.field_port)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("input_sip_port")
                    )
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        validationError = null
                    },
                    label = { Text(stringResource(R.string.field_username)) },
                    placeholder = { Text("1001") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_sip_username")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.field_password)) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_sip_password")
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.field_transport),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SipTransport.entries.forEach { option ->
                            val selected = transport == option
                            FilterChip(
                                selected = selected,
                                onClick = { transport = option },
                                label = {
                                    Text(
                                        text = option.label,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = BrandCyan
                                ),
                                modifier = Modifier.testTag("transport_${option.name.lowercase()}")
                            )
                        }
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StatusFailedRed
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedDomain = domain.trim()
                    val trimmedUser = username.trim()
                    if (trimmedDomain.isEmpty() || trimmedUser.isEmpty()) {
                        validationError = "Please enter both SIP Domain and Username."
                        return@Button
                    }
                    val parsedPort = portText.toIntOrNull()?.takeIf { it in 1..65535 } ?: 5060
                    onSave(
                        editingAccount?.id ?: 0L,
                        label.trim(),
                        trimmedDomain,
                        parsedPort,
                        trimmedUser,
                        password,
                        transport
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandCyan),
                modifier = Modifier.testTag("save_account_button")
            ) {
                Text(
                    text = stringResource(R.string.save_account),
                    color = Color(0xFF001F26),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
