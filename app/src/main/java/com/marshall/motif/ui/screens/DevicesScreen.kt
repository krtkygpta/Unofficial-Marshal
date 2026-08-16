package com.marshall.motif.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marshall.motif.R
import com.marshall.motif.ble.BleManager
import com.marshall.motif.ui.components.AppTopBar
import com.marshall.motif.ui.components.EarbudArt
import com.marshall.motif.ui.components.ScreenScaffold
import com.marshall.motif.ui.theme.Dimens
import com.marshall.motif.ui.theme.Radius
import com.marshall.motif.ui.theme.Space

@Composable
fun DevicesScreen(
    ble: BleManager,
    onOpenDevice: () -> Unit,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = ble.state
    val hasSavedDevice = state.deviceAddress.isNotEmpty()
    val dark = isSystemInDarkTheme()

    ScreenScaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = "My Marshall",
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.ic_marshall_logo),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                },
            )
        },
    ) {
        // One primary card: the Motif (saved or generic)
        DeviceProductCard(
            name = state.deviceName.ifEmpty { "Motif II A.N.C." },
            status = when {
                state.connected -> "Active"
                state.connecting -> "Connecting…"
                hasSavedDevice -> "Disconnected"
                else -> "Not paired"
            },
            connected = state.connected,
            leftBattery = state.leftBattery.takeIf { state.connected },
            rightBattery = state.rightBattery.takeIf { state.connected },
            onClick = {
                when {
                    state.connected -> onOpenDevice()
                    else -> onConnect()
                }
            },
            trailingIcon = when {
                state.connected -> TrailingIcon.Chevron
                hasSavedDevice || !state.connecting -> TrailingIcon.BtOff
                else -> TrailingIcon.None
            },
            dark = dark,
        )

        Surface(
            shape = Radius.Shape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(Space.Md),
                horizontalArrangement = Arrangement.spacedBy(Space.Sm),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "To pair new earbuds, open the charging case and keep the earbuds inside. Press and hold the pairing button until the light starts pulsing, then tap Connect.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private enum class TrailingIcon { None, Chevron, BtOff }

@Composable
private fun DeviceProductCard(
    name: String,
    status: String,
    connected: Boolean,
    leftBattery: Int?,
    rightBattery: Int?,
    onClick: () -> Unit,
    trailingIcon: TrailingIcon,
    dark: Boolean,
) {
    // Soft neutral backdrop so black Motif product art reads clearly.
    val heroBrush = if (dark) {
        Brush.verticalGradient(listOf(Color(0xFF2E2E34), Color(0xFF1A1A1E)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFF3F3F5), Color(0xFFE0E0E4)))
    }

    Surface(
        onClick = onClick,
        shape = Radius.Shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Space.Lg, end = Space.Md, top = Space.Lg),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(Space.Xxs))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                when (trailingIcon) {
                    TrailingIcon.Chevron -> Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Open",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TrailingIcon.BtOff -> Icon(
                        imageVector = Icons.Rounded.BluetoothDisabled,
                        contentDescription = "Disconnected",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TrailingIcon.None -> Unit
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.Md, vertical = Space.Sm)
                    .height(Dimens.ProductHeroHeight)
                    .clip(Radius.Shape)
                    .background(heroBrush),
                contentAlignment = Alignment.Center,
            ) {
                // Square-ish canvas so the balanced composition stays centered
                // (wide-only layout stretched the buds off the optical mid).
                EarbudArt(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .height(200.dp),
                    leftBattery = leftBattery,
                    rightBattery = rightBattery,
                    active = connected,
                    lightTheme = !dark,
                )
            }
        }
    }
}
