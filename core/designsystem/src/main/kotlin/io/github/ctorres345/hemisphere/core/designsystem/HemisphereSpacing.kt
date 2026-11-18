package io.github.ctorres345.hemisphere.core.designsystem

import androidx.compose.ui.unit.Dp

/**
 * The spacing scale from `tokens/base.json` (design-system §7), a 4dp grid. UI code uses these,
 * never `dp` literals.
 */
object HemisphereSpacing {
    val xs: Dp = ThemeTokens.spaceXs
    val s: Dp = ThemeTokens.spaceS
    val m: Dp = ThemeTokens.spaceM
    val l: Dp = ThemeTokens.spaceL
    val xl: Dp = ThemeTokens.spaceXl
    val xxl: Dp = ThemeTokens.spaceXxl
}

/** Row heights per density, from `tokens/base.json` (docs/design-system.md §7). */
object HemisphereRowHeight {
    val comfortable: Dp = ThemeTokens.rowComfortable
    val default: Dp = ThemeTokens.rowDefault
    val compact: Dp = ThemeTokens.rowCompact
}

/** Component sizes from `tokens/base.json`. */
object HemisphereSizes {
    /** The top app bar in touch mode (prototype `.appbar`). */
    val appBar: Dp = ThemeTokens.sizeAppBar

    /** The outline of buttons and fields (prototype `.btn`). */
    val border: Dp = ThemeTokens.sizeBorder
}
