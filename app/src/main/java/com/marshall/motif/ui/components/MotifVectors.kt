package com.marshall.motif.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.marshall.motif.R

/**
 * Motif II A.N.C. product art from the official-style illustrations
 * in `assets/` (`earbud_left.svg`, `earbud_right.svg`, `case.svg`).
 */

@Composable
fun EarbudArt(
    modifier: Modifier = Modifier,
    leftBattery: Int? = null,
    rightBattery: Int? = null,
    active: Boolean = true,
    lightTheme: Boolean = true,
    accentColor: Color = Color.Unspecified,
) {
    @Suppress("UNUSED_PARAMETER")
    val unused = Triple(leftBattery, rightBattery, lightTheme) to accentColor

    Row(
        modifier = modifier
            .alpha(if (active) 1f else 0.48f)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        MotifIllustration(
            resId = R.drawable.motif_earbud_left,
            contentDescription = "Left Motif II earbud",
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(0.96f),
        )
        MotifIllustration(
            resId = R.drawable.motif_case,
            contentDescription = "Motif II case",
            modifier = Modifier
                .weight(1.35f)
                .fillMaxHeight(0.58f)
                .padding(bottom = 4.dp),
        )
        MotifIllustration(
            resId = R.drawable.motif_earbud_right,
            contentDescription = "Right Motif II earbud",
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(0.96f),
        )
    }
}

@Composable
fun BatteryEarbudGlyph(
    isLeft: Boolean,
    modifier: Modifier = Modifier,
    bodyColor: Color = Color.Unspecified,
    accentColor: Color = Color.Unspecified,
    statusColor: Color? = null,
) {
    @Suppress("UNUSED_PARAMETER")
    val unused = Triple(bodyColor, accentColor, statusColor)

    MotifIllustration(
        resId = if (isLeft) R.drawable.motif_earbud_left else R.drawable.motif_earbud_right,
        contentDescription = if (isLeft) "Left earbud" else "Right earbud",
        modifier = modifier,
    )
}

@Composable
fun BatteryCaseGlyph(
    modifier: Modifier = Modifier,
    bodyColor: Color = Color.Unspecified,
    accentColor: Color = Color.Unspecified,
    statusColor: Color? = null,
) {
    @Suppress("UNUSED_PARAMETER")
    val unused = Triple(bodyColor, accentColor, statusColor)

    MotifIllustration(
        resId = R.drawable.motif_case,
        contentDescription = "Charging case",
        modifier = modifier,
    )
}

@Composable
fun EarbudGlyph(
    isLeft: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    accentColor: Color = Color.Unspecified,
    statusColor: Color? = null,
) = BatteryEarbudGlyph(isLeft, modifier, color, accentColor, statusColor)

@Composable
fun CaseGlyph(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    accentColor: Color = Color.Unspecified,
    statusColor: Color? = null,
) = BatteryCaseGlyph(modifier, color, accentColor, statusColor)

@Composable
private fun MotifIllustration(
    @DrawableRes resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        Image(
            painter = painterResource(resId),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}
