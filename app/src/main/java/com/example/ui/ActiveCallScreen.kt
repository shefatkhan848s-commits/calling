package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.sip.ActiveCallStatus
import com.example.sip.ActiveCallUiState
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.CallAcceptGreen
import com.example.ui.theme.CallEndRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.StatusRegisteredGreen
import com.example.ui.theme.StatusRegisteringAmber

private val dtmfRows = listOf(
    listOf('1', '2', '3'),
    listOf('4', '5', '6'),
    listOf('7', '8', '9'),
    listOf('*', '0', '#')
)

@Composable
fun ActiveCallScreen(
    callState: ActiveCallUiState,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleHold: () -> Unit,
    onSendDtmf: (Char) -> Unit,
    onAnswerCall: () -> Unit,
    onDeclineCall: () -> Unit,
    onEndCall: () -> Unit
) {
    var showInCallKeypad by remember { mutableStateOf(false) }
    var dtmfBuffer by remember { mutableStateOf("") }

    val pulseTransition = rememberInfiniteTransition(label = "call_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "call_pulse_scale"
    )

    val statusText = when (callState.status) {
        ActiveCallStatus.OUTGOING -> stringResource(R.string.call_status_outgoing)
        ActiveCallStatus.RINGING -> stringResource(R.string.call_status_ringing)
        ActiveCallStatus.INCOMING -> stringResource(R.string.call_status_incoming)
        ActiveCallStatus.CONNECTED -> stringResource(R.string.call_status_connected)
        ActiveCallStatus.PAUSED -> stringResource(R.string.call_status_paused)
        ActiveCallStatus.ENDED, ActiveCallStatus.NONE -> stringResource(R.string.call_status_ended)
    }

    val statusColor = when (callState.status) {
        ActiveCallStatus.CONNECTED -> StatusRegisteredGreen
        ActiveCallStatus.PAUSED -> StatusRegisteringAmber
        ActiveCallStatus.INCOMING, ActiveCallStatus.RINGING, ActiveCallStatus.OUTGOING -> BrandCyan
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1428),
                        DarkSurface,
                        DarkBackground
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("active_call_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Active SIP Account Badge & Caller Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = BrandCyan.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = stringResource(R.string.via_account, callState.accountLabel.ifBlank { "SIP" }),
                        style = MaterialTheme.typography.labelMedium,
                        color = BrandCyan,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("active_call_account_label")
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                if (!showInCallKeypad) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(116.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            BrandCyan.copy(alpha = 0.28f),
                                            BrandBlue.copy(alpha = 0.10f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(88.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = BrandCyan,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }

                Text(
                    text = callState.remoteDisplayName.ifBlank { callState.remoteNumber },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("active_call_remote_number")
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    color = statusColor,
                    modifier = Modifier.testTag("active_call_status")
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = formatDuration(callState.durationSeconds),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = JetBrainsMono,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("active_call_timer")
                )
            }

            // Middle: In-Call DTMF Keypad OR Call Controls Grid
            AnimatedVisibility(visible = showInCallKeypad && callState.status != ActiveCallStatus.INCOMING) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 320.dp)
                ) {
                    if (dtmfBuffer.isNotEmpty()) {
                        Text(
                            text = dtmfBuffer,
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMono),
                            color = BrandCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    dtmfRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { digit ->
                                Surface(
                                    onClick = {
                                        dtmfBuffer += digit
                                        onSendDtmf(digit)
                                    },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .size(62.dp)
                                        .testTag("incall_dtmf_$digit")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = digit.toString(),
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontFamily = JetBrainsMono,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                    TextButton(onClick = { showInCallKeypad = false }) {
                        Text(stringResource(R.string.hide_keypad), color = BrandCyan)
                    }
                }
            }

            // Bottom Controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp)
            ) {
                if (callState.status == ActiveCallStatus.INCOMING) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                onClick = onDeclineCall,
                                shape = CircleShape,
                                color = CallEndRed,
                                modifier = Modifier
                                    .size(72.dp)
                                    .testTag("incoming_decline_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CallEnd,
                                        contentDescription = stringResource(R.string.action_decline),
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.action_decline),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                onClick = onAnswerCall,
                                shape = CircleShape,
                                color = CallAcceptGreen,
                                modifier = Modifier
                                    .size(72.dp)
                                    .testTag("incoming_answer_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = stringResource(R.string.action_answer),
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.action_answer),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    if (!showInCallKeypad) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            CallControlAction(
                                icon = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                label = stringResource(if (callState.isMuted) R.string.action_unmute else R.string.action_mute),
                                isActive = callState.isMuted,
                                onClick = onToggleMute,
                                testTag = "call_mute_button"
                            )
                            CallControlAction(
                                icon = Icons.AutoMirrored.Filled.VolumeUp,
                                label = stringResource(R.string.action_speaker),
                                isActive = callState.isSpeakerOn,
                                onClick = onToggleSpeaker,
                                testTag = "call_speaker_button"
                            )
                            CallControlAction(
                                icon = if (callState.isOnHold) Icons.Default.PlayArrow else Icons.Default.Pause,
                                label = stringResource(if (callState.isOnHold) R.string.action_resume else R.string.action_hold),
                                isActive = callState.isOnHold,
                                onClick = onToggleHold,
                                testTag = "call_hold_button"
                            )
                            CallControlAction(
                                icon = Icons.Default.Dialpad,
                                label = stringResource(R.string.action_keypad),
                                isActive = showInCallKeypad,
                                onClick = { showInCallKeypad = !showInCallKeypad },
                                testTag = "call_keypad_button"
                            )
                        }
                    }

                    // Red End Call Button
                    Surface(
                        onClick = onEndCall,
                        shape = CircleShape,
                        color = CallEndRed,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(74.dp)
                            .testTag("call_end_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = stringResource(R.string.action_end_call),
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallControlAction(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (isActive) BrandCyan else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .testTag(testTag)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) Color(0xFF001F26) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isActive) BrandCyan else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
