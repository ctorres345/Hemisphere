package io.github.ctorres345.hemisphere.core.designsystem

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class HemisphereThemeTest {
    @Test
    fun `every theme has every accent`() {
        ThemeStyle.entries.forEach { style ->
            assertEquals(style.name, Accent.entries.toSet(), style.palette().accents.keys)
        }
    }

    @Test
    fun `AMOLED is the true black theme and differs from Dark`() {
        assertEquals(
            0xFF000000.toInt(),
            ThemeStyle.Amoled
                .palette()
                .colors
                .surfaceCanvas
                .toArgb(),
        )
        assertNotEquals(
            ThemeStyle.Dark
                .palette()
                .colors.surfaceCanvas,
            ThemeStyle.Amoled
                .palette()
                .colors.surfaceCanvas,
        )
    }

    @Test
    fun `the chosen accent drives Material's primary slots`() {
        Accent.entries.forEach { accent ->
            val colors = ThemeStyle.Dark.palette().withAccent(accent)
            val scheme = colors.toColorScheme(dark = true)
            assertEquals(accent.name, colors.accent.fill, scheme.primary)
            assertEquals(accent.name, colors.accent.onFill, scheme.onPrimary)
            assertEquals(accent.name, colors.accent.tint, scheme.primaryContainer)
        }
    }

    @Test
    fun `Material surfaces and outlines come from the theme tokens`() {
        ThemeStyle.entries.forEach { style ->
            val colors = style.palette().withAccent(Accent.Blue)
            val scheme = colors.toColorScheme(dark = style.isDark)
            assertEquals(style.name, colors.surfaceCanvas, scheme.surface)
            assertEquals(style.name, colors.surfaceCanvas, scheme.background)
            assertEquals(style.name, colors.textPrimary, scheme.onSurface)
            assertEquals(style.name, colors.textSecondary, scheme.onSurfaceVariant)
            assertEquals(style.name, colors.borderStrong, scheme.outline)
            assertEquals(style.name, colors.borderDefault, scheme.outlineVariant)
            assertEquals(style.name, colors.danger, scheme.error)
        }
    }
}
