package com.marshall.motif.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marshall.motif.SettingsStore
import com.marshall.motif.ble.BleManager
import com.marshall.motif.ble.MarshallGatt
import com.marshall.motif.ble.Protocol
import com.marshall.motif.ble.friendlyName
import com.marshall.motif.ui.components.AppTopBar
import com.marshall.motif.ui.components.HintText
import com.marshall.motif.ui.components.InfoRow
import com.marshall.motif.ui.components.MarshallCard
import com.marshall.motif.ui.components.ScreenScaffold
import com.marshall.motif.ui.components.SectionBlock
import com.marshall.motif.ui.components.ToggleRow
import com.marshall.motif.ui.theme.Radius
import com.marshall.motif.ui.theme.Space
import com.marshall.motif.ui.theme.CustomThemeMode
import com.marshall.motif.ui.theme.ThemeMode
import com.marshall.motif.ui.theme.marshallSegmentedButtonColors

private data class AccentOption(val label: String, val color: Color)

private fun CustomThemeMode.label(): String = when (this) {
    CustomThemeMode.SYSTEM -> "System"
    CustomThemeMode.LIGHT -> "Light"
    CustomThemeMode.DARK -> "Dark"
}

private val ACCENT_OPTIONS = listOf(
    AccentOption("Gold", Color(0xFFD4AF5A)),
    AccentOption("Blue", Color(0xFF4F7DFF)),
    AccentOption("Purple", Color(0xFF9B6DFF)),
    AccentOption("Green", Color(0xFF35A866)),
    AccentOption("Red", Color(0xFFE45B5B)),
    AccentOption("Teal", Color(0xFF2EAFA3)),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    ble: BleManager,
    settings: SettingsStore,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = ble.state
    var showRename by remember { mutableStateOf(false) }

    ScreenScaffold(
        modifier = modifier,
        topBar = { AppTopBar(title = "Device settings", onBack = onBack) },
    ) {
        SectionBlock("Device") {
            MarshallCard {
                InfoRow("Name", state.deviceName.ifEmpty { "Not connected" })
                if (state.model.isNotEmpty()) InfoRow("Model", state.model)
                if (state.firmware.isNotEmpty()) InfoRow("Firmware", state.firmware)
                if (state.serial.isNotEmpty()) InfoRow("Serial", state.serial)
                if (state.manufacturer.isNotEmpty()) InfoRow("Manufacturer", state.manufacturer)
                InfoRow(
                    "Battery",
                    buildString {
                        append("L ${state.leftBattery?.let { "$it%" } ?: "—"}")
                        append("  ·  R ${state.rightBattery?.let { "$it%" } ?: "—"}")
                        append("  ·  Case ${state.caseBattery?.let { "$it%" } ?: "—"}")
                    },
                )

                Spacer(Modifier.height(Space.Sm))
                when {
                    state.connected -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Space.Sm),
                        ) {
                            if (state.has(MarshallGatt.RENAME)) {
                                OutlinedButton(
                                    onClick = { showRename = true },
                                    modifier = Modifier.weight(1f),
                                    shape = Radius.Shape,
                                ) { Text("Rename") }
                            }
                            FilledTonalButton(
                                onClick = ble::disconnect,
                                modifier = Modifier.weight(1f),
                                shape = Radius.Shape,
                            ) { Text("Disconnect") }
                        }
                        Spacer(Modifier.height(Space.Xs))
                        OutlinedButton(
                            onClick = ble::forgetDevice,
                            modifier = Modifier.fillMaxWidth(),
                            shape = Radius.Shape,
                        ) { Text("Forget device") }
                    }

                    state.connecting -> HintText("Connecting…")
                    else -> HintText("Connect earbuds from My Marshall to manage them here.")
                }
            }
        }

        if (state.connected && state.multipointAvailable) {
            SectionBlock("Multipoint") {
                MarshallCard {
                    if (state.multipointHosts.isEmpty()) {
                        HintText("No linked phones reported. Pull the list again after both devices have connected once.")
                    } else {
                        state.multipointHosts.forEach { host ->
                            InfoRow(
                                host.name.ifBlank { host.mac },
                                buildString {
                                    append(if (host.connected) "Connected" else "Saved")
                                    append("  ·  ")
                                    append(host.mac)
                                },
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Space.Sm),
                            ) {
                                if (host.connected) {
                                    OutlinedButton(
                                        onClick = { ble.disconnectMultipointHost(host.id) },
                                        modifier = Modifier.weight(1f),
                                        shape = Radius.Shape,
                                    ) { Text("Disconnect") }
                                }
                                OutlinedButton(
                                    onClick = { ble.removeMultipointHost(host.id) },
                                    modifier = Modifier.weight(1f),
                                    shape = Radius.Shape,
                                ) { Text("Forget") }
                            }
                            Spacer(Modifier.height(Space.Sm))
                        }
                    }
                    OutlinedButton(
                        onClick = ble::refreshMultipoint,
                        modifier = Modifier.fillMaxWidth(),
                        shape = Radius.Shape,
                    ) { Text("Refresh list") }
                }
            }
        }

        SectionBlock("Sound behaviour") {
            ToggleRow(
                title = "In-ear detection",
                subtitle = "Pause when an earbud is removed",
                checked = state.wearDetectEnabled,
                onCheckedChange = ble::setWearDetect,
                icon = Icons.Rounded.Hearing,
                enabled = state.connected,
            )
            ToggleRow(
                title = "Touch controls",
                subtitle = "Enable the touch surfaces",
                checked = state.touchEnabled,
                onCheckedChange = ble::setTouchEnabled,
                icon = Icons.Rounded.TouchApp,
                enabled = state.connected,
            )
            ToggleRow(
                title = "Interaction sounds",
                subtitle = "Play a tone on touch",
                checked = state.soundsEnabled,
                onCheckedChange = ble::setSounds,
                icon = Icons.Rounded.MusicNote,
                enabled = state.connected,
            )
            if (state.connected && !state.has(MarshallGatt.UI_SOUNDS)) {
                HintText("UI-sounds characteristic not advertised; write is still attempted.")
            }
        }

        if (state.connected) {
            SectionBlock("LE Audio / LC3 (experiment)") {
                ToggleRow(
                    title = "Enable LE Audio flag",
                    subtitle = if (state.leAudioConfigAvailable) {
                        buildString {
                            append(if (state.leAudioPresent) "Firmware reports LE Audio present" else "Present bit off")
                            append(" · ")
                            append(if (state.leAudioEnabled) "enabled" else "disabled")
                            if (state.leAudioRaw.isNotEmpty()) append(" · ${state.leAudioRaw}")
                        }
                    } else {
                        "Characteristic 003d not advertised — Motif firmware may have no toggle"
                    },
                    checked = state.leAudioEnabled,
                    onCheckedChange = ble::setLeAudioEnabled,
                    icon = Icons.Rounded.Bluetooth,
                    enabled = state.connected && state.leAudioConfigAvailable,
                )
                HintText(
                    "This writes Marshall's official LE_AUDIO_CONFIG bit. It cannot invent LC3 if the firmware never shipped the LE Audio stack. After enabling: disconnect, forget the buds in Android Bluetooth, pair again, then check Developer options → Bluetooth Audio HAL / codec, or play music and look for LC3 / LE Audio on the device details screen.",
                )
            }
        }

        if (state.has(MarshallGatt.ECO_CHARGING) || state.connected) {
            SectionBlock("Battery preservation") {
                MarshallCard {
                    val options = listOf("none", "standard", "medium", "max")
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Space.Xs),
                        verticalArrangement = Arrangement.spacedBy(Space.Xs),
                    ) {
                        options.forEach { option ->
                            val selected = state.batterySaverPreset == option
                            FilterChip(
                                selected = selected,
                                onClick = { ble.setBatterySaver(option) },
                                enabled = state.connected,
                                label = { Text(Protocol.batterySaverLabel(option)) },
                                shape = Radius.Shape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.height(Space.Sm))
                    HintText(
                        when (state.batterySaverPreset) {
                            "standard" -> "Tops out near 90% to slow battery aging."
                            "medium" -> "Limits charge speed and tops out near 90%."
                            "max" -> "Slowest charge for longest battery life."
                            else -> "Full battery, normal charging speed."
                        },
                    )
                }
            }
        }

        SectionBlock("Appearance") {
            MarshallCard {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(Space.Xs))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = settings.themeMode == ThemeMode.DYNAMIC,
                        onClick = { settings.setTheme(ThemeMode.DYNAMIC) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        colors = marshallSegmentedButtonColors(),
                    ) { Text("Dynamic") }
                    SegmentedButton(
                        selected = settings.themeMode == ThemeMode.MONOCHROMATIC,
                        onClick = { settings.setTheme(ThemeMode.MONOCHROMATIC) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        colors = marshallSegmentedButtonColors(),
                    ) { Text("Mono") }
                    SegmentedButton(
                        selected = settings.themeMode == ThemeMode.CUSTOM,
                        onClick = { settings.setTheme(ThemeMode.CUSTOM) },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        colors = marshallSegmentedButtonColors(),
                    ) { Text("Custom") }
                }
                Spacer(Modifier.height(Space.Sm))
                HintText(
                    when (settings.themeMode) {
                        ThemeMode.DYNAMIC -> "Material You follows your system wallpaper and colors."
                        ThemeMode.MONOCHROMATIC -> "Black and white, following your system light or dark mode."
                        ThemeMode.CUSTOM -> "Choose the appearance and accent color below."
                    },
                )

                if (settings.themeMode == ThemeMode.CUSTOM) {
                    Spacer(Modifier.height(Space.Md))
                    Text(
                        text = "Mode",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Space.Xs))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        CustomThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = settings.customThemeMode == mode,
                                onClick = { settings.updateCustomThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = CustomThemeMode.entries.size,
                                ),
                                colors = marshallSegmentedButtonColors(),
                            ) { Text(mode.label()) }
                        }
                    }
                    Spacer(Modifier.height(Space.Md))
                    Text(
                        text = "Accent color",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Space.Xs))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Space.Xs),
                        verticalArrangement = Arrangement.spacedBy(Space.Xs),
                    ) {
                        ACCENT_OPTIONS.forEach { option ->
                            val selected = settings.accentColor == option.color.value.toInt()
                            FilterChip(
                                selected = selected,
                                onClick = { settings.updateAccentColor(option.color.value.toInt()) },
                                label = { Text(option.label) },
                                leadingIcon = {
                                    Box(
                                        Modifier
                                            .size(16.dp)
                                            .background(option.color, CircleShape),
                                    )
                                },
                                shape = Radius.Shape,
                            )
                        }
                    }
                }
            }
        }

        SectionBlock("Diagnostics") {
            MarshallCard {
                if (state.connected && state.gattMap.isNotEmpty()) {
                    Text(
                        text = "GATT map",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(Space.Xs))
                    Text(
                        text = state.gattMap.joinToString("\n"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(Modifier.height(Space.Md))
                }
                Text(
                    text = "Recent activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(Space.Xs))
                val recent = ble.logs.takeLast(24)
                if (recent.isEmpty()) {
                    HintText("Nothing logged yet.")
                } else {
                    Text(
                        text = recent.joinToString("\n"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }

    if (showRename) {
        var name by remember { mutableStateOf(state.deviceName) }
        AlertDialog(
            onDismissRequest = { showRename = false },
            shape = Radius.Shape,
            title = { Text("Rename earbuds") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 40) name = it },
                    singleLine = true,
                    label = { Text("New name") },
                    shape = Radius.Shape,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        ble.renameDevice(name.trim())
                        showRename = false
                    },
                    enabled = name.isNotBlank(),
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) { Text("Cancel") }
            },
        )
    }
}
