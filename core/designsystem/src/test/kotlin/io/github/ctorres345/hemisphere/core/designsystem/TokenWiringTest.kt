package io.github.ctorres345.hemisphere.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * The public token objects are wired to the generated ThemeTokens by hand. ThemeTokensCodegenTest
 * proves ThemeTokens matches the JSON; this proves each name here reads its own token.
 */
class TokenWiringTest {
    @Test
    fun `spacing reads the space tokens`() {
        assertEquals(ThemeTokens.spaceXs, HemisphereSpacing.xs)
        assertEquals(ThemeTokens.spaceS, HemisphereSpacing.s)
        assertEquals(ThemeTokens.spaceM, HemisphereSpacing.m)
        assertEquals(ThemeTokens.spaceL, HemisphereSpacing.l)
        assertEquals(ThemeTokens.spaceXl, HemisphereSpacing.xl)
        assertEquals(ThemeTokens.spaceXxl, HemisphereSpacing.xxl)
    }

    @Test
    fun `row heights, sizes and shapes read their tokens`() {
        assertEquals(ThemeTokens.rowComfortable, HemisphereRowHeight.comfortable)
        assertEquals(ThemeTokens.rowDefault, HemisphereRowHeight.default)
        assertEquals(ThemeTokens.rowCompact, HemisphereRowHeight.compact)
        assertEquals(ThemeTokens.sizeAppBar, HemisphereSizes.appBar)
        assertEquals(ThemeTokens.sizeTopBar, HemisphereSizes.topBar)
        assertEquals(ThemeTokens.sizeActivePaneBar, HemisphereSizes.activePaneBar)
        assertEquals(ThemeTokens.sizeBorder, HemisphereSizes.border)
        assertEquals(RoundedCornerShape(ThemeTokens.radiusChip), HemisphereShapes.chip)
        assertEquals(RoundedCornerShape(ThemeTokens.radiusRow), HemisphereShapes.row)
        assertEquals(RoundedCornerShape(ThemeTokens.radiusSheet), HemisphereShapes.sheet)
    }

    @Test
    fun `each type style reads its own type token`() {
        val pairs =
            listOf(
                ThemeTokens.typeTitle to hemisphereTypography.title,
                ThemeTokens.typeHeading to hemisphereTypography.heading,
                ThemeTokens.typeBody to hemisphereTypography.body,
                ThemeTokens.typeMeta to hemisphereTypography.meta,
                ThemeTokens.typeCaption to hemisphereTypography.caption,
            )
        pairs.forEach { (token, style) ->
            assertEquals(token.fontSize, style.fontSize)
            assertEquals(token.lineHeight, style.lineHeight)
            assertEquals(token.fontWeight, style.fontWeight)
        }
    }

    @Test
    fun `each theme style uses its own generated palette`() {
        assertSame(ThemeTokens.light, ThemeStyle.Light.palette())
        assertSame(ThemeTokens.dark, ThemeStyle.Dark.palette())
        assertSame(ThemeTokens.amoled, ThemeStyle.Amoled.palette())
    }

    @Test
    fun `Material's type slots read the token styles`() {
        val material = hemisphereTypography.toMaterial()
        assertEquals(hemisphereTypography.title, material.titleLarge)
        assertEquals(hemisphereTypography.heading, material.titleMedium)
        assertEquals(hemisphereTypography.body, material.bodyLarge)
        assertEquals(hemisphereTypography.meta, material.bodySmall)
        assertEquals(hemisphereTypography.caption, material.labelSmall)
    }
}
