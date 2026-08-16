package com.marshall.motif.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.marshall.motif.ble.BleManager
import com.marshall.motif.ble.MarshallGatt
import com.marshall.motif.ui.components.AppTopBar
import com.marshall.motif.ui.components.HintText
import com.marshall.motif.ui.components.MarshallCard
import com.marshall.motif.ui.components.ScreenScaffold
import com.marshall.motif.ui.components.SectionBlock
import com.marshall.motif.ui.components.ToggleRow
import com.marshall.motif.ui.theme.Space

/**
 * Dedicated in-ear detection screen (not the full device settings dump).
 */
@Composable
fun WearScreen(
    ble: BleManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = ble.state
    val supported = !state.connected || state.has(MarshallGatt.WEAR_SENSOR_ACTION)

    ScreenScaffold(
        modifier = modifier,
        topBar = { AppTopBar(title = "In-ear detection", onBack = onBack) },
    ) {
        SectionBlock("Auto play & pause") {
            ToggleRow(
                title = "In-ear detection",
                subtitle = "Pause when an earbud is removed, resume when worn",
                checked = state.wearDetectEnabled,
                onCheckedChange = ble::setWearDetect,
                icon = Icons.Rounded.Hearing,
                enabled = state.connected,
            )
            if (!state.connected) {
                HintText("Connect your Motif II to change this setting.")
            } else if (!supported) {
                HintText("This firmware did not expose a wear-sensor characteristic. The toggle may still work if the device accepts the write.")
            }
        }

        MarshallCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Space.Sm),
            ) {
                Text(
                    text = "How it works",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "When enabled, playback pauses if you take an earbud out and resumes when you put it back in. Useful for conversations without stopping music on your phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
