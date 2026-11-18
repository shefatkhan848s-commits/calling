package com.example.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CallAcceptGreen
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.StatusRegisteringAmber

private data class DialKey(val digit: String, val letters: String)

private val dialPadRows = listOf(
    listOf(DialKey("1", ""), DialKey("2", "ABC"), DialKey("3", "DEF")),
    listOf(DialKey("4", "GHI"), DialKey("5", "JKL"), DialKey("6", "MNO")),
    listOf(DialKey("7", "PQRS"), DialKey("8", "TUV"), DialKey("9", "WXYZ")),
    listOf(DialKey("*", ""), DialKey("0", "+"), DialKey("#", ""))
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialerScreen(
    dialedNumber: String,
    hasMicPermission: Boolean,
    onRequestMicPermission: () -> Unit,
    onDigitPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onClearAll: () -> Unit,
    onCallClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (!hasMicPermission) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = StatusRegisteringAmber.copy(alpha = 0.14f),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MicOff,
                            contentDescription = null,
                            tint = StatusRegisteringAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.permission_mic_required),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    TextButton(
                        onClick = onRequestMicPermission,
                        colors = ButtonDefaults.textButtonColors(contentColor = StatusRegisteringAmber),
                        modifier = Modifier.testTag("grant_mic_permission_button")
                    ) {
                        Text(
                            text = stringResource(R.string.permission_grant),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Dialed Number Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dialedNumber.ifEmpty { stringResource(R.string.dialpad_placeholder) },
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = if (dialedNumber.isNotEmpty()) JetBrainsMono else MaterialTheme.typography.titleLarge.fontFamily,
                    fontSize = when {
                        dialedNumber.isEmpty() -> 18.sp
                        dialedNumber.length > 14 -> 24.sp
                        else -> 34.sp
                    },
                    fontWeight = if (dialedNumber.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal,
                    letterSpacing = if (dialedNumber.isNotEmpty()) 1.5.sp else 0.sp
                ),
                color = if (dialedNumber.isNotEmpty()) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .testTag("dialed_number_display")
            )
        }

        // Numeric Dialpad Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            dialPadRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { key ->
                        DialPadKeyButton(
                            digit = key.digit,
                            letters = key.letters,
                            onClick = { onDigitPress(key.digit) },
                            onLongClick = if (key.digit == "0") {
                                { onDigitPress("+") }
                            } else {
                                null
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom Action Row: Spacer | Green Call Button | Backspace
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Balance spacer
                Box(modifier = Modifier.size(64.dp))

                // Green Call Button
                Surface(
                    onClick = onCallClick,
                    shape = CircleShape,
                    color = CallAcceptGreen,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("dialer_call_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = stringResource(R.string.action_call),
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Backspace (tap deletes 1 char, long-press clears all)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            enabled = dialedNumber.isNotEmpty(),
                            onClick = onBackspace,
                            onLongClick = onClearAll
                        )
                        .testTag("dialer_backspace_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = stringResource(R.string.action_backspace),
                        tint = if (dialedNumber.isNotEmpty()) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        },
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialPadKeyButton(
    digit: String,
    letters: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        tonalElevation = 1.dp,
        modifier = Modifier
            .size(74.dp)
            .clip(CircleShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("dial_key_$digit")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = digit,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = JetBrainsMono,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
