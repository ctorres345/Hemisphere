package io.github.ctorres345.hemisphere.feature.browser.layout

import androidx.compose.ui.unit.Dp
import io.github.ctorres345.hemisphere.core.model.LayoutPin
import io.github.ctorres345.hemisphere.core.model.Orientation
import io.github.ctorres345.hemisphere.core.model.Settings
import org.junit.Assert.assertEquals
import org.junit.Test

/** Auto picks by width at the 600dp threshold; a pin always wins. */
class LayoutModeTest {
    @Test
    fun `Auto shows Dual from 600dp and Single below it`() {
        assertEquals(PaneLayout.Dual, layoutFor(LayoutPin.Auto, Dp(600f)))
        assertEquals(PaneLayout.Single, layoutFor(LayoutPin.Auto, Dp(599f)))
    }

    @Test
    fun `a pin wins over the width`() {
        assertEquals(PaneLayout.Single, layoutFor(LayoutPin.Single, Dp(960f)))
        assertEquals(PaneLayout.Dual, layoutFor(LayoutPin.Dual, Dp(411f)))
    }

    @Test
    fun `each orientation has its own pin`() {
        val settings = Settings(layoutPinPortrait = LayoutPin.Dual, layoutPinLandscape = LayoutPin.Single)

        assertEquals(LayoutPin.Dual, settings.pinFor(Orientation.Portrait))
        assertEquals(LayoutPin.Single, settings.pinFor(Orientation.Landscape))
    }
}
