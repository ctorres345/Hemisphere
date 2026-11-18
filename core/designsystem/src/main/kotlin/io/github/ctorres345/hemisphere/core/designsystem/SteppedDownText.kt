package io.github.ctorres345.hemisphere.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

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
    CompositionLocalProvider(
        LocalHemisphereColors provides stepped,
        LocalSteppedDown provides enabled,
        content = content,
    )
}

/**
 * True inside an enabled [SteppedDownText]. A selected row reads it to keep its meta at the base
 * secondary color, since tertiary is too faint on the accent tint (prototype
 * `.pane:not(.active) .row[aria-selected="true"] .meta`, design-system §3). It travels with the stepped
 * colors, as a theme value does.
 */
@Suppress("ComposeCompositionLocalUsage")
val LocalSteppedDown = staticCompositionLocalOf { false }
