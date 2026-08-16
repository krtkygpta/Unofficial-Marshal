package com.marshall.motif.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marshall.motif.ui.theme.Dimens
import com.marshall.motif.ui.theme.MarshallAmber
import com.marshall.motif.ui.theme.MarshallGreenRing
import com.marshall.motif.ui.theme.MarshallRed
import com.marshall.motif.ui.theme.Space

/** Battery status color: green high, amber mid, red low. */
fun batteryColor(fraction: Float): Color = when {
    fraction > 0.5f -> MarshallGreenRing
    fraction > 0.25f -> MarshallAmber
    else -> MarshallRed
}

fun batteryColorForLevel(value: Int?): Color? =
    value?.let { batteryColor(it.coerceIn(0, 100) / 100f) }

/**
 * One battery column: product glyph, label, %, and a clear level track.
 *
 * UX: percentage alone is slow to scan; a short filled bar gives instant
 * “full / half / empty” comparison across Left · Case · Right.
 */
@Composable
fun BatteryCell(
    value: Int?,
    label: String,
    modifier: Modifier = Modifier,
    glyphSize: Dp = Dimens.BatteryRing * 0.72f,
    content: @Composable (statusColor: Color?) -> Unit,
) {
    val fraction = value?.coerceIn(0, 100)?.div(100f)
    val status = batteryColorForLevel(value)
    val track = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val animated by animateFloatAsState(
        targetValue = fraction ?: 0f,
        animationSpec = tween(420),
        label = "batteryFrac",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(glyphSize + 4.dp),
        ) {
            content(status)
        }

        Spacer(Modifier.height(Space.Xs))

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = value?.let { "$it%" } ?: "—",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )

        Spacer(Modifier.height(Space.Xs))

        // Level bar — the actual visual battery, not a decorative dot
        BatteryLevelBar(
            fraction = if (value == null) null else animated,
            fillColor = status ?: MaterialTheme.colorScheme.onSurfaceVariant,
            trackColor = track,
            modifier = Modifier
                .widthIn(max = 72.dp)
                .fillMaxWidth(0.72f)
                .height(5.dp),
        )
    }
}

/**
 * Rounded capsule track with colored fill to [fraction].
 * When [fraction] is null (unknown), draws an empty track only.
 */
@Composable
fun BatteryLevelBar(
    fraction: Float?,
    fillColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val h = size.height
        val r = CornerRadius(h / 2f, h / 2f)
        // Track
        drawRoundRect(
            color = trackColor,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = r,
        )
        val f = fraction?.coerceIn(0f, 1f)
        if (f != null && f > 0.001f) {
            val fillW = (size.width * f).coerceAtLeast(h) // keep capsule readable at low %
            drawRoundRect(
                color = fillColor,
                topLeft = Offset.Zero,
                size = Size(fillW.coerceAtMost(size.width), h),
                cornerRadius = r,
            )
        }
    }
}

@Composable
fun BatteryRow(
    left: Int?,
    right: Int?,
    case: Int?,
    modifier: Modifier = Modifier,
) {
    val glyphSize = Dimens.BatteryRing * 0.72f
    val ink = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.Sm),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top,
    ) {
        BatteryCell(
            value = left,
            label = "Left",
            modifier = Modifier.weight(1f),
            glyphSize = glyphSize,
        ) { status ->
            BatteryEarbudGlyph(
                isLeft = true,
                modifier = Modifier.size(glyphSize),
                bodyColor = ink,
                statusColor = status,
            )
        }
        BatteryCell(
            value = case,
            label = "Case",
            modifier = Modifier.weight(1f),
            glyphSize = glyphSize,
        ) { status ->
            BatteryCaseGlyph(
                modifier = Modifier.size(glyphSize),
                bodyColor = ink,
                statusColor = status,
            )
        }
        BatteryCell(
            value = right,
            label = "Right",
            modifier = Modifier.weight(1f),
            glyphSize = glyphSize,
        ) { status ->
            BatteryEarbudGlyph(
                isLeft = false,
                modifier = Modifier.size(glyphSize),
                bodyColor = ink,
                statusColor = status,
            )
        }
    }
}
