package io.github.ctorres345.hemisphere.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * The app theme. A placeholder over Material 3 defaults until the design tokens ticket generates
 * the colors, type and shapes from the token files in `tokens/` (docs/design-system.md).
 */
@Composable
fun HemisphereTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
