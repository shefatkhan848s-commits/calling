package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.SipAccountEntity
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.StatusFailedRed
import com.example.viewmodel.SipAccountUiModel

@Composable
fun AccountsScreen(
    accounts: List<SipAccountUiModel>,
    activeAccountId: Long?,
    onAddAccountClick: () -> Unit,
    onEditAccountClick: (SipAccountEntity) -> Unit,
    onDeleteAccountClick: (Long) -> Unit,
    onSelectActiveAccount: (Long) -> Unit,
    onReregisterAccount: (Long) -> Unit,
    onShareApkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.accounts_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${accounts.size} configured • Concurrent SIP registration",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = onShareApkClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("share_apk_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.action_share_apk),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_share_apk),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            if (accounts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(78.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ManageAccounts,
                                    contentDescription = null,
                                    tint = BrandCyan,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.empty_accounts_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.empty_accounts_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onAddAccountClick,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandCyan),
                            modifier = Modifier.testTag("empty_add_account_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color(0xFF001F26)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.add_account),
                                color = Color(0xFF001F26),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 6.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.testTag("accounts_list")
                ) {
                    items(accounts, key = { it.entity.id }) { accountUi ->
                        SipAccountCard(
                            accountUi = accountUi,
                            isActive = accountUi.entity.id == activeAccountId,
                            onSelectActive = { onSelectActiveAccount(accountUi.entity.id) },
                            onReregister = { onReregisterAccount(accountUi.entity.id) },
                            onEdit = { onEditAccountClick(accountUi.entity) },
                            onDelete = { onDeleteAccountClick(accountUi.entity.id) }
                        )
                    }
                }
            }
        }

        if (accounts.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = onAddAccountClick,
                containerColor = BrandCyan,
                contentColor = Color(0xFF001F26),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_account)
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.add_account),
                        fontWeight = FontWeight.Bold
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp)
                    .testTag("fab_add_account")
            )
        }
    }
}

@Composable
private fun SipAccountCard(
    accountUi: SipAccountUiModel,
    isActive: Boolean,
    onSelectActive: () -> Unit,
    onReregister: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val entity = accountUi.entity
    val statusColor = registrationStatusColor(accountUi.status)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
        ),
        border = if (isActive) BorderStroke(1.5.dp, BrandCyan.copy(alpha = 0.8f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sip_account_card_${entity.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RegistrationStatusDot(status = accountUi.status, size = 11.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = entity.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isActive) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = BrandCyan.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = stringResource(R.string.active_badge),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandCyan,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${entity.username}@${entity.domain}:${entity.port}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMono),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = entity.transport,
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Live registration status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${registrationStatusLabel(accountUi.status)}${
                        accountUi.statusMessage.takeIf { it.isNotBlank() && it != registrationStatusLabel(accountUi.status) }
                            ?.let { " — $it" } ?: ""
                    }",
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!isActive) {
                    OutlinedButton(
                        onClick = onSelectActive,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("set_active_account_${entity.id}")
                    ) {
                        Text(
                            text = stringResource(R.string.set_as_active),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onReregister,
                        modifier = Modifier.testTag("reregister_account_${entity.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.retry_registration),
                            tint = BrandCyan
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_account_${entity.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_account),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_account_${entity.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_account),
                            tint = StatusFailedRed
                        )
                    }
                }
            }
        }
    }
}
