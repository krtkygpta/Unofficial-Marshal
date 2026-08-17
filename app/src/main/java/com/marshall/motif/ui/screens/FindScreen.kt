package com.marshall.motif.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marshall.motif.ble.BleManager
import com.marshall.motif.ui.components.AppTopBar
import com.marshall.motif.ui.components.BatteryEarbudGlyph
import com.marshall.motif.ui.components.HintText
import com.marshall.motif.ui.components.ScreenScaffold
import com.marshall.motif.ui.theme.Radius
import com.marshall.motif.ui.theme.Space

@Composable
fun FindScreen(
    ble: BleManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = ble.state
    val ringing = state.findRinging

    ScreenScaffold(
        modifier = modifier,
        topBar = { AppTopBar(title = "Find buds", onBack = onBack) },
    ) {
        HintText("Tap a bud to play the find-me tone. Music can keep playing.")
        Spacer(Modifier.height(Space.Lg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.Md),
        ) {
            FindBudCard(
                isLeft = true,
                ringing = ringing == 1 || ringing == 0,
                enabled = state.connected,
                onClick = { ble.ringBuds(1, true) },
                modifier = Modifier.weight(1f),
            )
            FindBudCard(
                isLeft = false,
                ringing = ringing == 2 || ringing == 0,
                enabled = state.connected,
                onClick = { ble.ringBuds(2, true) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(Space.Xl))
        FilledTonalButton(
            onClick = { ble.ringBuds(0, false) },
            enabled = state.connected && ringing != null,
            modifier = Modifier.fillMaxWidth(),
            shape = Radius.Shape,
        ) { Text("Stop ringing") }
    }
}

@Composable
private fun FindBudCard(
    isLeft: Boolean,
    ringing: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pulse = rememberInfiniteTransition(label = "findPulse")
    val dim by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (ringing) 0.45f else 1f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "findAlpha",
    )
    val container by animateColorAsState(
        targetValue = if (ringing) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "findBg",
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = Radius.Shape,
        color = container,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.Md, vertical = Space.Xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BatteryEarbudGlyph(
                isLeft = isLeft,
                modifier = Modifier
                    .size(120.dp)
                    .alpha(if (enabled) dim else 0.4f),
            )
            Spacer(Modifier.height(Space.Md))
            Text(
                text = if (isLeft) "Left" else "Right",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = when {
                    !enabled -> "Not connected"
                    ringing -> "Ringing…"
                    else -> "Tap to ring"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
