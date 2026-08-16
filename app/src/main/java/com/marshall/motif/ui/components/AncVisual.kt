package com.marshall.motif.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.marshall.motif.R
import com.marshall.motif.ble.Protocol
import com.marshall.motif.ui.theme.M3EMotion
import com.marshall.motif.ui.theme.Radius
import com.marshall.motif.ui.theme.Space

/**
 * Shared ANC glyphs used by the in-app Sound screen and the home-screen widget.
 * - ANC on  → ic_noise_control_on
 * - Off     → ic_noise_control_off (Material Icons Rounded noise_control_off)
 * - Aware   → ic_noise_control_transparency (Material Icons Rounded noise_aware)
 */
@DrawableRes
fun ancModeDrawable(mode: Int): Int = when (mode) {
    Protocol.ANC_ON -> R.drawable.ic_noise_control_on
    Protocol.ANC_TRANSPARENCY -> R.drawable.ic_noise_control_transparency
    else -> R.drawable.ic_noise_control_off
}

@Composable
fun AncIcon(
    mode: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(ancModeDrawable(mode)),
        contentDescription = Protocol.ancModeLabel(mode),
        tint = color,
        modifier = modifier,
    )
}

@Composable
fun AncModeRow(
    selectedMode: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val modes = listOf(
        Protocol.ANC_ON to "ANC",
        Protocol.ANC_OFF to "Off",
        Protocol.ANC_TRANSPARENCY to "Aware",
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.Xs),
    ) {
        modes.forEach { (mode, label) ->
            val selected = selectedMode == mode
            val bg by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                animationSpec = M3EMotion.effectsDefault(),
                label = "ancBg$mode",
            )
            val fg by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = M3EMotion.effectsDefault(),
                label = "ancFg$mode",
            )
            Surface(
                onClick = { if (enabled) onSelect(mode) },
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 84.dp),
                shape = Radius.Shape,
                color = bg,
                contentColor = fg,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Space.Xs, vertical = Space.Md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    AncIcon(
                        mode = mode,
                        color = fg,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.size(Space.Xs))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        textAlign = TextAlign.Center,
                        color = fg,
                    )
                }
            }
        }
    }
}
