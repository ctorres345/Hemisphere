package io.github.ctorres345.hemisphere.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit

/** One typography token from `tokens/base.json`. Built only by ThemeTokens. */
@Immutable
internal data class TokenTextStyle(
    val fontSize: TextUnit,
    val lineHeight: TextUnit,
    val fontWeight: FontWeight,
)

/**
 * The five type tokens (docs/design-system.md §6). Inter is not bundled yet, so the family is the
 * system sans-serif (Roboto), the tokens' fallback.
 */
@Immutable
data class HemisphereTypography(
    val title: TextStyle,
    val heading: TextStyle,
    val body: TextStyle,
    val meta: TextStyle,
    val caption: TextStyle,
)

/** Centred, untrimmed line boxes, as CSS lays out the prototype's text (and as Material's styles do). */
private fun TokenTextStyle.toTextStyle() =
    TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontWeight = fontWeight,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    )

internal val hemisphereTypography =
    HemisphereTypography(
        title = ThemeTokens.typeTitle.toTextStyle(),
        heading = ThemeTokens.typeHeading.toTextStyle(),
        body = ThemeTokens.typeBody.toTextStyle(),
        // Tabular figures line sizes up in a column, as the prototype's .meta does.
        meta = ThemeTokens.typeMeta.toTextStyle().copy(fontFeatureSettings = "tnum"),
        caption = ThemeTokens.typeCaption.toTextStyle(),
    )

/** Material components read these slots, so each maps to the nearest token. */
internal fun HemisphereTypography.toMaterial(): Typography {
    val emphasized = body.copy(fontWeight = heading.fontWeight)
    return Typography(
        displayLarge = title,
        displayMedium = title,
        displaySmall = title,
        headlineLarge = title,
        headlineMedium = title,
        headlineSmall = title,
        titleLarge = title,
        titleMedium = heading,
        titleSmall = emphasized,
        bodyLarge = body,
        bodyMedium = body,
        bodySmall = meta,
        labelLarge = emphasized,
        labelMedium = meta,
        labelSmall = caption,
    )
}
