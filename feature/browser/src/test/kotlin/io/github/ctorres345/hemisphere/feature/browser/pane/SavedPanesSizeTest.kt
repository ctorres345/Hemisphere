package io.github.ctorres345.hemisphere.feature.browser.pane

import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.ProviderRef
import io.github.ctorres345.hemisphere.core.model.VolumeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The saved panes stay under the binder budget, and trimming keeps the open folder. */
@RunWith(RobolectricTestRunner::class)
// Robolectric needs Java 21 for API 35 and up; the build runs on JDK 17 (ADR 0003).
@Config(sdk = [34])
class SavedPanesSizeTest {
    private fun roundTrip(panes: Map<PaneId, PaneState>) = SavedPanes.restore(SavedPanes.save(panes))

    @Test
    fun `the worst case fits the budget without a trim`() {
        val panes = worstCasePanes()

        val size = SavedPanes.encodedSize(panes)

        assertTrue("$size bytes", size < SavedPanes.BUDGET_BYTES)
        assertEquals(panes, SavedPanes.trimmed(panes))
    }

    @Test
    fun `open folders that alone are over budget save nothing, so the panes start over`() {
        val top = idRoot.child("a", ProviderRef("z".repeat(SavedPanes.BUDGET_BYTES)))
        val panes = mapOf(PaneId.Left to PaneState.on(top))

        assertEquals(emptyMap<PaneId, PaneState>(), SavedPanes.trimmed(panes))
        assertEquals(emptyMap<PaneId, PaneState>(), roundTrip(panes))
    }

    @Test
    fun `an oversized state drops the oldest anchors first, then the oldest levels, and keeps the open folder`() {
        // SAF document ids that hold the whole path make every level's ref long.
        val huge = "y".repeat(SavedPanes.BUDGET_BYTES / 2 / BACK_STACK_LIMIT)
        val stack =
            (1 until BACK_STACK_LIMIT).runningFold(idRoot) { below, level ->
                below.child(longName(level), ProviderRef("$huge-$level"))
            }
        val panes =
            mapOf(
                PaneId.Left to PaneState(listOf(tabWithAnchors(stack))),
                PaneId.Right to PaneState(listOf(tabWithAnchors(stack))),
            )
        assertTrue(SavedPanes.encodedSize(panes) > SavedPanes.BUDGET_BYTES)

        val trimmed = SavedPanes.trimmed(panes)
        val restored = roundTrip(panes)

        assertTrue(SavedPanes.encodedSize(trimmed) <= SavedPanes.BUDGET_BYTES)
        PaneId.entries.forEach { id ->
            val tab = trimmed.getValue(id).tab
            assertTrue("anchors go before levels", tab.anchors.isEmpty())
            assertTrue("some levels were dropped", tab.backStack.size < BACK_STACK_LIMIT)
            assertEquals(stack.takeLast(tab.backStack.size), tab.backStack)
            assertEquals(
                stack.last().key,
                restored
                    .getValue(id)
                    .tab.folder.key,
            )
            assertEquals(
                tab.backStack.map { it.key },
                restored
                    .getValue(id)
                    .tab.backStack
                    .map { it.key },
            )
        }
    }

    @Test
    fun `a state just over the budget loses only its oldest anchors`() {
        val panes = worstCasePanes()
        // Characters, two bytes each: one more than fills the budget.
        val extra = (SavedPanes.BUDGET_BYTES - SavedPanes.encodedSize(panes)) / 2 + 1
        // The newest anchor of the left pane grows until the state is just over.
        val left = panes.getValue(PaneId.Left).tab
        val top = left.backStack.dropLast(1).last()
        val grown = left.anchors.getValue(top.key).let { it.copy(focusedRow = it.focusedRow + "z".repeat(extra)) }
        val padded = panes + (PaneId.Left to PaneState(listOf(left.copy(anchors = left.anchors + (top.key to grown)))))
        assertEquals(SavedPanes.BUDGET_BYTES + 2, SavedPanes.encodedSize(padded))

        val trimmed = SavedPanes.trimmed(padded)

        assertTrue(SavedPanes.encodedSize(trimmed) <= SavedPanes.BUDGET_BYTES)
        PaneId.entries.forEach { id ->
            val before = padded.getValue(id).tab
            val after = trimmed.getValue(id).tab
            assertEquals(before.backStack, after.backStack)
            assertTrue(after.anchors.isNotEmpty())
            val oldestKept = before.backStack.indexOfFirst { it.key in after.anchors }
            val dropped =
                before.backStack.withIndex().filter { (_, path) ->
                    path.key in before.anchors &&
                        path.key !in after.anchors
                }
            assertTrue(dropped.all { it.index < oldestKept })
        }
    }

    @Test
    fun `a stack that jumps instead of stepping down still round-trips`() {
        val other = FsPath.root(VolumeId("primary"))
        val music = other.child("Music")
        val stack =
            listOf(
                idRoot,
                idRoot.child("Games", ProviderRef("g")),
                music,
                music.child("Rock"),
                // A jump within one volume: not a child of the level below.
                idRoot.child("Games", ProviderRef("g")).child("Saves", ProviderRef("s")),
            )
        val panes = mapOf(PaneId.Left to PaneState(listOf(tabWithAnchors(stack))))

        val restored = roundTrip(panes).getValue(PaneId.Left).tab

        assertEquals(stack.map { it.key }, restored.backStack.map { it.key })
        assertEquals(stack.map { it.ref }, restored.backStack.map { it.ref })
        assertEquals(panes.getValue(PaneId.Left).tab.anchors, restored.anchors)
    }

    @Test
    fun `a level whose name cannot be rebuilt starts the pane over`() {
        val bundle =
            SavedPanes.save(
                mapOf(PaneId.Left to PaneState(listOf(TabState(listOf(idRoot, idRoot.child("a")))))),
            )
        bundle.putStringArrayList("Left.0.names", arrayListOf("a/b"))

        assertEquals(emptyMap<PaneId, PaneState>(), SavedPanes.restore(bundle))
    }
}
