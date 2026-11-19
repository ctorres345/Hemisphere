package io.github.ctorres345.hemisphere.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * One theme's colors from `tokens/color.<theme>.json` with the first accent, plus every accent.
 * Built only by ThemeTokens.
 */
@Immutable
internal data class ThemePalette(
    val colors: HemisphereColors,
    val accents: Map<Accent, AccentColors>,
)

/** The five accent roles (docs/design-system.md §5, Accent). */
@Immutable
data class AccentColors(
    val fill: Color,
    val onFill: Color,
    val tint: Color,
    val focus: Color,
    val text: Color,
)

/**
 * The colors UI code reads through [HemisphereTheme.colors]: the theme's surfaces, text, borders and
 * semantic colors, plus the user's accent. Names follow the tokens.
 */
@Immutable
data class HemisphereColors(
    val surfaceCanvas: Color,
    val surfaceRaised: Color,
    val surfaceOverlay: Color,
    val borderDefault: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val danger: Color,
    val warning: Color,
    val success: Color,
    val accent: AccentColors,
    /** Every accent in this theme, for a picker that shows them all (Settings). */
    val accents: Map<Accent, AccentColors> = emptyMap(),
)

internal fun ThemePalette.withAccent(accent: Accent): HemisphereColors =
    colors.copy(accent = accents.getValue(accent), accents = accents)
