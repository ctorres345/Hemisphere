package io.github.ctorres345.hemisphere.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * The app theme. A placeholder over Material 3's default light and dark schemes, following the
 * system setting, until the design tokens ticket generates the colors, type and shapes from the
 * token files in `tokens/` (docs/design-system.md), including AMOLED.
 */
@Composable
fun HemisphereTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}
