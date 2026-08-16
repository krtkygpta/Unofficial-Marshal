package com.marshall.motif.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single source of truth for layout, radius, and motion tokens.
 * Every surface in the app must use these values — no one-off radii.
 */
object Radius {
    /** Universal corner radius for cards, lists, controls, sheets. */
    val Standard: Dp = 20.dp
    val Small: Dp = 12.dp
    val Full: RoundedCornerShape = RoundedCornerShape(percent = 50)

    val Shape = RoundedCornerShape(Standard)
    val ShapeSmall = RoundedCornerShape(Small)
    val ShapeTop = RoundedCornerShape(topStart = Standard, topEnd = Standard)
}

object Space {
    val Xxs: Dp = 4.dp
    val Xs: Dp = 8.dp
    val Sm: Dp = 12.dp
    val Md: Dp = 16.dp
    val Lg: Dp = 20.dp
    val Xl: Dp = 24.dp
    val Xxl: Dp = 32.dp
}

object Dimens {
    val ScreenHorizontal: Dp = Space.Lg
    val ScreenTop: Dp = Space.Xs
    val SectionGap: Dp = Space.Xl
    val ItemGap: Dp = Space.Sm
    val CardPadding: Dp = Space.Md
    val BottomInset: Dp = 40.dp
    val TopBarHeight: Dp = 56.dp
    val IconButton: Dp = 40.dp
    val ActionCircle: Dp = 56.dp
    val BatteryRing: Dp = 88.dp
    val AncControlHeight: Dp = 84.dp
    val ProductHeroHeight: Dp = 220.dp
    val DividerInset: Dp = 56.dp
}
