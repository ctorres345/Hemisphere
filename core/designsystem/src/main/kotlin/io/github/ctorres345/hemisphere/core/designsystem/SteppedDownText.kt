package io.github.ctorres345.hemisphere.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

/**
 * While [enabled], shows [content] with each text color one level down: primary as secondary, secondary
 * as tertiary. The inactive pane in Dual uses it (prototype `.pane:not(.active)`), so it reads as quieter
 * without opacity, which would break text contrast. Toggling [enabled] keeps [content]'s state.
 */
@Composable
fun SteppedDownText(
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    val colors = HemisphereTheme.colors
    val stepped =
        remember(colors, enabled) {
            if (enabled) {
                colors.copy(
                    textPrimary = colors.textSecondary,
                    textSecondary = colors.textTertiary,
                )
            } else {
                colors
            }
        }
    CompositionLocalProvider(LocalHemisphereColors provides stepped, content = content)
}
