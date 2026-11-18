package io.github.ctorres345.hemisphere.core.designsystem

import android.view.KeyEvent
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent

/**
 * Takes input from windows the Activity does not see: a menu, popup or dialog has its own window, so
 * its keys and touches skip the Activity's tracker. The app provides it as [LocalInputReporter].
 */
interface InputReporter {
    /** A key reached the popup; the reporter classifies it as the Activity does. */
    fun onKey(event: KeyEvent)

    /** A pointer went down in the popup: touch mode. */
    fun onPointer()
}

/**
 * The app's [InputReporter], or null (previews, tests): then nothing is reported. A local like
 * [LocalInputMode], since popups anywhere in the tree need it and it never changes.
 */
@Suppress("ComposeCompositionLocalUsage")
val LocalInputReporter = staticCompositionLocalOf<InputReporter?> { null }

/**
 * Reports this subtree's keys and pointer presses to [LocalInputReporter], so last input wins inside a
 * popup window too (docs/design-system.md §3). Put it on the content of every menu, popup and dialog.
 */
fun Modifier.reportsInput(): Modifier = this then ReportsInputElement

private data object ReportsInputElement : ModifierNodeElement<ReportsInputNode>() {
    override fun create() = ReportsInputNode()

    override fun update(node: ReportsInputNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "reportsInput"
    }
}

private class ReportsInputNode :
    Modifier.Node(),
    KeyInputModifierNode,
    PointerInputModifierNode,
    CompositionLocalConsumerModifierNode {
    override fun onPreKeyEvent(event: ComposeKeyEvent): Boolean {
        currentValueOf(LocalInputReporter)?.onKey(event.nativeKeyEvent)
        return false
    }

    override fun onKeyEvent(event: ComposeKeyEvent): Boolean = false

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        if (pass == PointerEventPass.Initial && pointerEvent.type == PointerEventType.Press) {
            currentValueOf(LocalInputReporter)?.onPointer()
        }
    }

    override fun onCancelPointerInput() = Unit
}
