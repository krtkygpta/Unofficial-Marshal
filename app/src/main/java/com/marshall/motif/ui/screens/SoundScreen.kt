package com.marshall.motif.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.marshall.motif.ble.BleManager
import com.marshall.motif.ble.Protocol
import com.marshall.motif.ui.components.AncModeRow
import com.marshall.motif.ui.components.AppTopBar
import com.marshall.motif.ui.components.CustomEqSliders
import com.marshall.motif.ui.components.EqPresetPicker
import com.marshall.motif.ui.components.EqVisual
import com.marshall.motif.ui.components.HintText
import com.marshall.motif.ui.components.MarshallCard
import com.marshall.motif.ui.components.ScreenScaffold
import com.marshall.motif.ui.components.SectionBlock
import com.marshall.motif.ui.components.StrengthSlider
import com.marshall.motif.ui.theme.Space

@Composable
fun SoundScreen(
    ble: BleManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = ble.state
    val activePreset = Protocol.EqPreset.fromId(state.eqPreset)
    val visualShape = if (activePreset == Protocol.EqPreset.CUSTOM) {
        state.customEq.map { (it / 12f).coerceIn(-1f, 1f) }.toFloatArray()
    } else {
        activePreset.shape
    }

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
            MarshallCard(contentPadding = 0.dp) {
                EqVisual(
                    shape = visualShape,
                    accent = MaterialTheme.colorScheme.primary,
                    track = MaterialTheme.colorScheme.surfaceContainerHighest,
                    animate = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp),
                )
                Column(Modifier.padding(horizontal = Space.Md, vertical = Space.Md)) {
                    Text(
                        text = "Active preset",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(Space.Xxs))
                    Text(
                        text = activePreset.label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            EqPresetPicker(
                selectedId = state.eqPreset,
                onSelect = ble::setEqPreset,
                enabled = state.connected,
            )

            if (activePreset == Protocol.EqPreset.CUSTOM) {
                MarshallCard {
                    CustomEqSliders(
                        values = state.customEq,
                        onValueChange = { _, _ -> },
                        onCommit = ble::setCustomEq,
                        enabled = state.connected,
                    )
                }
            }

            HintText("Custom EQ uses the official Airoha SPP link (Classic), same as the Marshall app.")
        }
    }
}
