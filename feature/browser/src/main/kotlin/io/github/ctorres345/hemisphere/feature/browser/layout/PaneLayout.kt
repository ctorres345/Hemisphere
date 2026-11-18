package io.github.ctorres345.hemisphere.feature.browser.layout

import androidx.compose.ui.unit.Dp
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereSizes
import io.github.ctorres345.hemisphere.core.model.LayoutPin
import io.github.ctorres345.hemisphere.core.model.Orientation

/** What the Browser shows (design-system §2): the active pane alone, or both panes side by side. */
enum class PaneLayout { Single, Dual }

/** The window the Browser is shown in, as the screen measures it. */
data class BrowserWindow(
    val width: Dp,
    val orientation: Orientation,
)

/** A pin wins; Auto shows Dual from [HemisphereSizes.dualMinWidth] and Single below it. */
fun layoutFor(
    pin: LayoutPin,
    width: Dp,
): PaneLayout =
    when (pin) {
        LayoutPin.Single -> PaneLayout.Single
        LayoutPin.Dual -> PaneLayout.Dual
        LayoutPin.Auto -> if (width >= HemisphereSizes.dualMinWidth) PaneLayout.Dual else PaneLayout.Single
    }
