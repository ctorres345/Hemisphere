package io.github.ctorres345.hemisphere.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/** The three themes (docs/design-system.md §5). AMOLED is the true-black dark theme. */
enum class ThemeStyle {
    Light,
    Dark,
    Amoled,
    ;

    val isDark: Boolean get() = this != Light
}

/**
 * The app theme, built only from the generated token values (the JSON files in `tokens/`). Light or Dark follows
 * the system until Settings lets the user pick, AMOLED included; the accent defaults to blue.
 *
 * Material components read the mapped [MaterialTheme]; Hemisphere UI reads [HemisphereTheme.colors]
 * and [HemisphereTheme.typography] for the token names.
 */
@Composable
fun HemisphereTheme(
    style: ThemeStyle = if (isSystemInDarkTheme()) ThemeStyle.Dark else ThemeStyle.Light,
    accent: Accent = Accent.Blue,
    content: @Composable () -> Unit,
) {
    val colors = remember(style, accent) { style.palette().withAccent(accent) }
    CompositionLocalProvider(LocalHemisphereColors provides colors) {
        MaterialTheme(
            colorScheme = remember(colors, style) { colors.toColorScheme(dark = style.isDark) },
            typography = materialTypography,
            shapes = materialShapes,
            content = content,
        )
    }
}

/** Token values for the current theme, by their token names. */
object HemisphereTheme {
    val colors: HemisphereColors
        @Composable @ReadOnlyComposable
        get() = LocalHemisphereColors.current

    /** The same in every theme, so it needs no composition local. */
    val typography: HemisphereTypography get() = hemisphereTypography
}

/** Corner radii from `tokens/base.json` (docs/design-system.md §7). */
object HemisphereShapes {
    val chip = RoundedCornerShape(ThemeTokens.radiusChip)
    val row = RoundedCornerShape(ThemeTokens.radiusRow)
    val sheet = RoundedCornerShape(ThemeTokens.radiusSheet)
}

private val materialTypography = hemisphereTypography.toMaterial()

private val materialShapes =
    Shapes(
        extraSmall = HemisphereShapes.chip,
        small = HemisphereShapes.row,
        medium = HemisphereShapes.sheet,
        large = HemisphereShapes.sheet,
        extraLarge = HemisphereShapes.sheet,
    )

// The theme's colors change with the theme and accent, so they travel as a composition local, like
// MaterialTheme's own. This is the one local the app defines (compose-lints allows theme values).
@Suppress("ComposeCompositionLocalUsage")
private val LocalHemisphereColors =
    staticCompositionLocalOf { ThemeTokens.light.withAccent(Accent.Blue) }

internal fun ThemeStyle.palette(): ThemePalette =
    when (this) {
        ThemeStyle.Light -> ThemeTokens.light
        ThemeStyle.Dark -> ThemeTokens.dark
        ThemeStyle.Amoled -> ThemeTokens.amoled
    }

/**
 * Material's slots in token terms: the accent drives primary, surfaces are flat (borders, not
 * shadows or tonal tint, design-system §4), and outlines are the two border tokens.
 */
internal fun HemisphereColors.toColorScheme(dark: Boolean): ColorScheme {
    val scheme = if (dark) darkColorScheme() else lightColorScheme()
    return scheme.copy(
        primary = accent.fill,
        onPrimary = accent.onFill,
        primaryContainer = accent.tint,
        onPrimaryContainer = textPrimary,
        inversePrimary = accent.fill,
        secondary = accent.fill,
        onSecondary = accent.onFill,
        secondaryContainer = accent.tint,
        onSecondaryContainer = textPrimary,
        tertiary = accent.fill,
        onTertiary = accent.onFill,
        tertiaryContainer = accent.tint,
        onTertiaryContainer = textPrimary,
        background = surfaceCanvas,
        onBackground = textPrimary,
        surface = surfaceCanvas,
        onSurface = textPrimary,
        surfaceVariant = surfaceRaised,
        onSurfaceVariant = textSecondary,
        surfaceTint = surfaceCanvas,
        inverseSurface = textPrimary,
        inverseOnSurface = surfaceCanvas,
        error = danger,
        onError = surfaceRaised,
        errorContainer = surfaceOverlay,
        onErrorContainer = danger,
        outline = borderStrong,
        outlineVariant = borderDefault,
        surfaceBright = surfaceOverlay,
        surfaceDim = surfaceCanvas,
        surfaceContainerLowest = surfaceCanvas,
        surfaceContainerLow = surfaceRaised,
        surfaceContainer = surfaceRaised,
        surfaceContainerHigh = surfaceOverlay,
        surfaceContainerHighest = surfaceOverlay,
    )
}
