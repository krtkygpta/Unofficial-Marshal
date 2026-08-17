package com.marshall.motif.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.marshall.motif.ble.BleManager
import com.marshall.motif.ble.Protocol
import com.marshall.motif.ui.components.AncModeRow
import com.marshall.motif.ui.components.AppTopBar
import com.marshall.motif.ui.components.BatteryRow
import com.marshall.motif.ui.components.GroupedList
import com.marshall.motif.ui.components.ListDivider
import com.marshall.motif.ui.components.MarshallCard
import com.marshall.motif.ui.components.ScreenScaffold
import com.marshall.motif.ui.components.SectionBlock
import com.marshall.motif.ui.components.SettingsRow
import com.marshall.motif.ui.components.StrengthSlider
import com.marshall.motif.ui.theme.Space

@Composable
fun HomeScreen(
    ble: BleManager,
    onBack: () -> Unit,
    onOpenSound: () -> Unit,
    onOpenControls: () -> Unit,
    onOpenWear: () -> Unit,
    onOpenFind: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = ble.state
    var showRename by remember { mutableStateOf(false) }

    ScreenScaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = "",
                onBack = onBack,
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = state.deviceName.ifEmpty { "Motif II A.N.C." },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                if (state.connected) {
                    IconButton(
                        onClick = { showRename = true },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Rename",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Text(
                text = when {
                    state.connected -> "Active"
                    state.connecting -> "Connecting…"
                    else -> "Not connected"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Space.Xl))

            if (state.connected || state.connecting) {
                BatteryRow(
                    left = state.leftBattery,
                    right = state.rightBattery,
                    case = state.caseBattery,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.connecting) {
                Spacer(Modifier.height(Space.Md))
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Quiet text actions — not primary filled buttons
            if (state.connected) {
                Spacer(Modifier.height(Space.Md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(onClick = {
                        ble.disconnect()
                        onBack()
                    }) {
                        Text(
                            "Disconnect",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = {
                        ble.forgetDevice()
                        onBack()
                    }) {
                        Text(
                            "Forget",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

        }

        SectionBlock("Active noise control") {
            AncModeRow(
                selectedMode = state.ancMode,
                onSelect = ble::setAncMode,
                enabled = state.connected,
            )
            if (state.connected && state.ancMode != Protocol.ANC_OFF) {
                MarshallCard {
                    StrengthSlider(
                        title = if (state.ancMode == Protocol.ANC_TRANSPARENCY) {
                            "Transparency level"
                        } else {
                            "Noise cancelling level"
                        },
                        value = if (state.ancMode == Protocol.ANC_TRANSPARENCY) {
                            state.transparencyStrength
                        } else {
                            state.ancStrength
                        },
                        onValueChange = {
                            if (state.ancMode == Protocol.ANC_TRANSPARENCY) {
                                ble.setTransparencyStrength(it)
                            } else {
                                ble.setAncStrength(it)
                            }
                        },
                        enabled = true,
                    )
                }
            }
        }

        SectionBlock("More controls") {
            GroupedList {
                SettingsRow(
                    title = "Sound",
                    subtitle = "Equaliser and noise control",
                    icon = Icons.Rounded.GraphicEq,
                    onClick = onOpenSound,
                )
                ListDivider()
                SettingsRow(
                    title = "Controls and gestures",
                    subtitle = "Customize taps and holds",
                    icon = Icons.Rounded.TouchApp,
                    onClick = onOpenControls,
                )
                ListDivider()
                SettingsRow(
                    title = "In-ear detection",
                    subtitle = if (state.wearDetectEnabled) "On" else "Off",
                    icon = Icons.Rounded.Hearing,
                    onClick = onOpenWear,
                )
                ListDivider()
                SettingsRow(
                    title = "Find buds",
                    subtitle = "Ring left or right",
                    icon = Icons.Rounded.NotificationsActive,
                    onClick = onOpenFind,
                )
                ListDivider()
                SettingsRow(
                    title = "Device settings",
                    subtitle = "Battery, name and diagnostics",
                    icon = Icons.Rounded.Settings,
                    onClick = onOpenSettings,
                )
                ListDivider()
                SettingsRow(
                    title = "Switch device",
                    subtitle = "Other paired Marshall products",
                    icon = Icons.Rounded.Bluetooth,
                    onClick = onBack,
                )
            }
        }
    }

    if (showRename) {
        RenameDialog(
            initial = state.deviceName,
            onConfirm = {
                ble.renameDevice(it)
                showRename = false
            },
            onDismiss = { showRename = false },
        )
    }
}

@Composable
private fun RenameDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text("Rename earbuds") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 40) name = it },
                singleLine = true,
                label = { Text("New name") },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
