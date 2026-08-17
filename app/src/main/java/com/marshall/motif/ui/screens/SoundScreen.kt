package com.marshall.motif.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.marshall.motif.ble.BleManager
import com.marshall.motif.ble.Protocol
import com.marshall.motif.ui.components.AncModeRow
import com.marshall.motif.ui.components.AppTopBar
import com.marshall.motif.ui.components.EqSparkline
import com.marshall.motif.ui.components.EqVisual
import com.marshall.motif.ui.components.GraphicEqSliders
import com.marshall.motif.ui.components.GroupedList
import com.marshall.motif.ui.components.HintText
import com.marshall.motif.ui.components.ListDivider
import com.marshall.motif.ui.components.MarshallCard
import com.marshall.motif.ui.components.ScreenScaffold
import com.marshall.motif.ui.components.SectionBlock
import com.marshall.motif.ui.components.StrengthSlider
import com.marshall.motif.ui.theme.Radius
import com.marshall.motif.ui.theme.Space
import com.marshall.motif.ui.theme.marshallSegmentedButtonColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundScreen(
    ble: BleManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = ble.state
    LaunchedEffect(state.connected) {
        if (state.connected) ble.refreshEq()
    }
    val activePreset = Protocol.EqPreset.fromIdOrNull(state.eqPreset)
    var advanced by remember {
        mutableStateOf(activePreset == Protocol.EqPreset.CUSTOM)
    }
    LaunchedEffect(activePreset) {
        if (activePreset == Protocol.EqPreset.CUSTOM) advanced = true
    }
    val visualShape = if (activePreset == Protocol.EqPreset.CUSTOM) {
        state.customEq.map { (it / 6f).coerceIn(-1f, 1f) }.toFloatArray()
    } else {
        activePreset?.shape ?: Protocol.EqPreset.MARSHALL.shape
    }
    val basicPresets = Protocol.EqPreset.entries.filter { it != Protocol.EqPreset.CUSTOM }

    ScreenScaffold(
        modifier = modifier,
        topBar = { AppTopBar(title = "Sound", onBack = onBack) },
    ) {
        SectionBlock("Active noise control") {
            AncModeRow(
                selectedMode = state.ancMode,
                onSelect = ble::setAncMode,
                enabled = state.connected,
            )
            HintText(
                when (state.ancMode) {
                    Protocol.ANC_ON -> "Active noise cancelling"
                    Protocol.ANC_TRANSPARENCY -> "Transparency / ambient"
                    else -> "Playback only (ANC off)"
                },
            )
            if (state.connected && state.ancMode != Protocol.ANC_OFF) {
                MarshallCard {
                    StrengthSlider(
                        title = if (state.ancMode == Protocol.ANC_TRANSPARENCY) {
                            "Transparency"
                        } else {
                            "Noise cancelling"
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

        SectionBlock("Equaliser") {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !advanced,
                    onClick = { advanced = false },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    colors = marshallSegmentedButtonColors(),
                ) { Text("Simple") }
                SegmentedButton(
                    selected = advanced,
                    onClick = {
                        advanced = true
                        if (state.connected && state.eqPreset != Protocol.EqPreset.CUSTOM.id) {
                            ble.setEqPreset(Protocol.EqPreset.CUSTOM.id)
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    colors = marshallSegmentedButtonColors(),
                ) { Text("Advanced") }
            }
            Spacer(Modifier.height(Space.Md))

            if (!advanced) {
                MarshallCard(contentPadding = 0.dp) {
                    EqVisual(
                        shape = visualShape,
                        accent = MaterialTheme.colorScheme.primary,
                        track = MaterialTheme.colorScheme.surfaceContainerHighest,
                        animate = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp),
                    )
                    Column(Modifier.padding(horizontal = Space.Md, vertical = Space.Md)) {
                        Text(
                            text = "Preset",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(Space.Xxs))
                        Text(
                            text = if (activePreset == Protocol.EqPreset.CUSTOM) {
                                "Custom — switch to Advanced"
                            } else {
                                activePreset?.label ?: if (state.connected) "Reading…" else "—"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.height(Space.Sm))
                BasicPresetList(
                    presets = basicPresets,
                    selectedId = state.eqPreset,
                    enabled = state.connected,
                    onSelect = ble::setEqPreset,
                )
            } else {
                MarshallCard(contentPadding = 0.dp) {
                    GraphicEqSliders(
                        values = state.customEq,
                        onCommit = ble::setCustomEq,
                        enabled = state.connected,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Space.Md, vertical = Space.Md),
                        horizontalArrangement = Arrangement.spacedBy(Space.Sm),
                    ) {
                        OutlinedButton(
                            onClick = { ble.setCustomEq(listOf(0, 0, 0, 0, 0)) },
                            enabled = state.connected,
                            shape = Radius.Shape,
                            modifier = Modifier.weight(1f),
                        ) { Text("Reset") }
                    }
                }
                Spacer(Modifier.height(Space.Md))
                EqSnapshotRow(ble)
                HintText("Five Motif bands (160 Hz–6.2 kHz, ±6 dB) over the official Airoha SPP link.")
            }
        }
    }
}

@Composable
private fun BasicPresetList(
    presets: List<Protocol.EqPreset>,
    selectedId: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    GroupedList {
        presets.forEachIndexed { index, preset ->
            val selected = preset.id == selectedId
            Surface(
                onClick = { if (enabled) onSelect(preset.id) },
                enabled = enabled,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Space.Md, vertical = Space.Md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.Md),
                ) {
                    EqSparkline(
                        shape = preset.shape,
                        accent = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .size(width = 72.dp, height = 36.dp),
                    )
                    Text(
                        text = preset.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                    if (selected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            if (index < presets.lastIndex) ListDivider()
        }
    }
}

@Composable
private fun EqSnapshotRow(ble: BleManager) {
    var tick by remember { mutableIntStateOf(0) }
    var showSave by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    val snaps = remember(tick, ble.state.customEqSnapshot) { ble.eqSnapshots() }
    val active = ble.state.customEqSnapshot

    Column(verticalArrangement = Arrangement.spacedBy(Space.Sm)) {
        Text(
            text = "Your curves",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (snaps.isEmpty()) {
            HintText("Save the current sliders. The checkmark shows which saved curve is playing.")
        } else {
            GroupedList {
                snaps.forEachIndexed { index, snap ->
                    val selected = snap.name == active
                    Surface(
                        onClick = { ble.applyEqSnapshot(snap.name) },
                        enabled = ble.state.connected,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = Space.Md, end = Space.Xs, top = Space.Sm, bottom = Space.Sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Space.Sm),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = snap.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    maxLines = 1,
                                )
                                Text(
                                    text = if (selected) "Playing now" else snap.bands.joinToString("  ") {
                                        if (it > 0) "+$it" else "$it"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Active",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            IconButton(
                                onClick = {
                                    ble.deleteEqSnapshot(snap.name)
                                    tick++
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Delete ${snap.name}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (index < snaps.lastIndex) ListDivider()
                }
            }
        }
        OutlinedButton(
            onClick = { showSave = true },
            enabled = ble.state.connected,
            shape = Radius.Shape,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save current sliders") }
    }

    if (showSave) {
        AlertDialog(
            onDismissRequest = { showSave = false },
            title = { Text("Save curve") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 24) name = it },
                    singleLine = true,
                    label = { Text("Name") },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        ble.saveEqSnapshot(name)
                        name = ""
                        tick++
                        showSave = false
                    },
                    enabled = name.isNotBlank(),
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSave = false }) { Text("Cancel") }
            },
        )
    }
}
