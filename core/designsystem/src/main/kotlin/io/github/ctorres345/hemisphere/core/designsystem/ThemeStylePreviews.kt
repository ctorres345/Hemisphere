package io.github.ctorres345.hemisphere.core.designsystem

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

/** Renders a `@Preview` in Light, Dark and AMOLED: `@PreviewParameter(ThemeStylePreviews::class) style: ThemeStyle`. */
class ThemeStylePreviews : PreviewParameterProvider<ThemeStyle> {
    override val values = ThemeStyle.entries.asSequence()
}
