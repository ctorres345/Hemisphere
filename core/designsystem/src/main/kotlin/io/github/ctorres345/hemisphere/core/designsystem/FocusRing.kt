package io.github.ctorres345.hemisphere.core.designsystem

import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RippleConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo

/**
 * The shared focus ring (docs/design-system.md §3). It draws an `accent.focus` outline while the
 * element below it has focus, and only when the [InputMode] [shows focus][InputMode.showsFocus] (gamepad
 * or keyboard): touch users never see it. Put it before
 * the focus target in the chain, for example `Modifier.focusRing().clickable { }`, so it sees that
 * target's focus.
 *
 * `accent.focus` keeps 3:1 against every surface in every theme and accent (`tools/contrast`).
 *
 * With [fill], a focused element also gets an `accent.focus` background at 8%, as the prototype's
 * focused list row (`.row.gf`).
 *
 * [FocusRingPlacement.Outside] draws past the element's bounds, so its container needs room for
 * `focusRingGap + focusRing` (4dp) on each side, or a clipping parent cuts the ring off.
 */
fun Modifier.focusRing(
    shape: Shape = HemisphereShapes.row,
    placement: FocusRingPlacement = FocusRingPlacement.Inside,
    fill: Boolean = false,
): Modifier = this then FocusRingElement(shape, placement, fill)

/**
 * The ripple for an element drawn with `focusRing(fill = true)`: Material's own press feedback in the
 * content color, but no focus layer, so a focused row shows only the ring and its 8% fill. Provide it
 * with `LocalRippleConfiguration` around that element only; other Material controls keep their focus layer.
 */
@OptIn(ExperimentalMaterial3Api::class)
val FocusFillRipple: RippleConfiguration =
    RippleConfiguration(
        rippleAlpha = RippleAlpha(draggedAlpha = 0.16f, focusedAlpha = 0f, hoveredAlpha = 0.08f, pressedAlpha = 0.1f),
    )

private data class FocusRingElement(
    val shape: Shape,
    val placement: FocusRingPlacement,
    val fill: Boolean,
) : ModifierNodeElement<FocusRingNode>() {
    override fun create() = FocusRingNode(shape, placement, fill)

    override fun update(node: FocusRingNode) {
        node.shape = shape
        node.placement = placement
        node.fill = fill
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "focusRing"
        properties["shape"] = shape
        properties["placement"] = placement
        properties["fill"] = fill
    }
}

private class FocusRingNode(
    var shape: Shape,
    var placement: FocusRingPlacement,
    var fill: Boolean,
) : Modifier.Node(),
    DrawModifierNode,
    FocusEventModifierNode,
    CompositionLocalConsumerModifierNode {
    private var focused = false

    override fun onFocusEvent(focusState: FocusState) {
        if (focused != focusState.isFocused) {
            focused = focusState.isFocused
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        val stroke = HemisphereSizes.focusRing.toPx()
        // An element with no area, or one smaller than the stroke for an inside ring (say, collapsing
        // in an animation), gets no ring: a negative outline size would make the shape throw.
        val minSize = if (placement == FocusRingPlacement.Inside) stroke else 0f
        val show = focused && currentValueOf(LocalInputMode).showsFocus && size.minDimension > minSize
        val color = currentValueOf(LocalHemisphereColors).accent.focus
        if (show && fill) drawOutline(shape.createOutline(size, layoutDirection, this), color.copy(alpha = FILL_ALPHA))
        drawContent()
        if (!show) return
        // How far the ring's center line sits from the element's edge: inward for Inside, outward past the gap.
        val grow =
            when (placement) {
                FocusRingPlacement.Inside -> -stroke / 2
                FocusRingPlacement.Outside -> HemisphereSizes.focusRingGap.toPx() + stroke / 2
            }
        val outline = shape.createOutline(Size(size.width + 2 * grow, size.height + 2 * grow), layoutDirection, this)
        translate(-grow, -grow) {
            drawOutline(outline, color, style = Stroke(stroke))
        }
    }

    private companion object {
        /** The prototype's `color-mix(in srgb, var(--a-focus) 8%, transparent)`. */
        const val FILL_ALPHA = 0.08f
    }
}
