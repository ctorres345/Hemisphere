package io.github.ctorres345.hemisphere.core.designsystem

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
 * The shared gamepad focus ring (docs/design-system.md §3). It draws an `accent.focus` outline while the
 * element below it has focus, and only in [InputMode.Gamepad]: touch users never see it. Put it before
 * the focus target in the chain, for example `Modifier.focusRing().clickable { }`, so it sees that
 * target's focus.
 *
 * `accent.focus` keeps 3:1 against every surface in every theme and accent (`tools/contrast`).
 */
fun Modifier.focusRing(
    shape: Shape = HemisphereShapes.row,
    placement: FocusRingPlacement = FocusRingPlacement.Inside,
): Modifier = this then FocusRingElement(shape, placement)

private data class FocusRingElement(
    val shape: Shape,
    val placement: FocusRingPlacement,
) : ModifierNodeElement<FocusRingNode>() {
    override fun create() = FocusRingNode(shape, placement)

    override fun update(node: FocusRingNode) {
        node.shape = shape
        node.placement = placement
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "focusRing"
        properties["shape"] = shape
        properties["placement"] = placement
    }
}

private class FocusRingNode(
    var shape: Shape,
    var placement: FocusRingPlacement,
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
        drawContent()
        if (!focused || currentValueOf(LocalInputMode) != InputMode.Gamepad) return
        val stroke = HemisphereSizes.focusRing.toPx()
        // How far the ring's center line sits from the element's edge: inward for Inside, outward past the gap.
        val grow =
            when (placement) {
                FocusRingPlacement.Inside -> -stroke / 2
                FocusRingPlacement.Outside -> HemisphereSizes.focusRingGap.toPx() + stroke / 2
            }
        val outline = shape.createOutline(Size(size.width + 2 * grow, size.height + 2 * grow), layoutDirection, this)
        translate(-grow, -grow) {
            drawOutline(outline, currentValueOf(LocalHemisphereColors).accent.focus, style = Stroke(stroke))
        }
    }
}
