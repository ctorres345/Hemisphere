package io.github.ctorres345.hemisphere.feature.browser

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import io.github.ctorres345.hemisphere.core.domain.files.GetFolderLockUseCase
import io.github.ctorres345.hemisphere.core.domain.files.GetHomeFolderUseCase
import io.github.ctorres345.hemisphere.core.domain.files.ObserveFolderUseCase
import io.github.ctorres345.hemisphere.core.fs.LockedFolders
import io.github.ctorres345.hemisphere.core.fs.testing.FakeVolumeRepository
import io.github.ctorres345.hemisphere.core.fs.testing.InMemoryProvider
import io.github.ctorres345.hemisphere.core.fs.testing.file
import io.github.ctorres345.hemisphere.core.fs.testing.folder
import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.testing.MainDispatcherRule
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneId
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneIntent
import io.github.ctorres345.hemisphere.feature.browser.pane.SavedPanes
import io.github.ctorres345.hemisphere.feature.browser.pane.ScrollAnchor
import io.github.ctorres345.hemisphere.feature.browser.pane.worstCasePanes
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Back stacks and scroll anchors survive process death: save the state, build a new ViewModel on it. */
@RunWith(RobolectricTestRunner::class)
// Robolectric needs Java 21 for API 35 and up; the build runs on JDK 17 (ADR 0003).
@Config(sdk = [34])
class BrowserSavedStateTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val provider = InMemoryProvider()
    private val volumes = FakeVolumeRepository(provider)
    private val home = volumes.homeFolder()
    private val music = home.child("Music")
    private val rock = music.child("Rock")

    private fun viewModel(savedState: SavedStateHandle) =
        BrowserViewModel(
            savedState,
            GetHomeFolderUseCase(volumes),
            ObserveFolderUseCase(volumes, main.dispatcher),
            GetFolderLockUseCase(LockedFolders(LockedFolders.API_R)),
        )

    /**
     * What the framework does on process death: [SavedStateHandle.savedStateProvider] writes the handle,
     * registered providers included, and `createHandle` reads it back in the new process.
     */
    @SuppressLint("RestrictedApi")
    private fun SavedStateHandle.afterProcessDeath(): SavedStateHandle =
        SavedStateHandle.createHandle(savedStateProvider().saveState(), null)

    @Test
    fun `the back stack and anchors come back after process death, and the open folder is listed again`() =
        runTest(main.dispatcher) {
            provider.addFolder(home, folder("Music"))
            provider.addFolder(music, folder("Rock"))
            provider.addFolder(rock, file("song.mp3"))
            val handle = SavedStateHandle()
            val before = viewModel(handle)
            val atHome = ScrollAnchor(focusedRow = "Music", firstVisibleRow = "Music", firstVisibleOffset = 7)
            before.open(folder("Music"), from = home, leaving = atHome)
            before.open(folder("Rock"), from = music)

            val after =
                viewModel(handle.afterProcessDeath())
                    .state.value.activePane!!
                    .tab

            assertEquals(listOf(home, music, rock), after.backStack)
            assertEquals(mapOf(home.key to atHome), after.anchors)
            assertEquals(listOf("song.mp3"), after.listing.entries.map { it.name })
        }

    @Test
    fun `a saved state that no longer reads back starts on the home folder`() =
        runTest(main.dispatcher) {
            val broken = Bundle().apply { putInt("Left.tabCount", 1) }

            val tab =
                viewModel(SavedStateHandle(mapOf(SavedPanes.KEY to broken)))
                    .state.value.activePane!!
                    .tab

            assertEquals(listOf(home), tab.backStack)
        }

    private fun BrowserViewModel.open(
        entry: Entry,
        from: FsPath,
        leaving: ScrollAnchor? = null,
    ) = onIntent(BrowserIntent.Pane(PaneId.Left, PaneIntent.OpenEntry(entry, from, leaving)))

    @Test
    fun `fifty long-named levels with refs in both panes round-trip with equal keys and anchors`() {
        val panes = worstCasePanes()

        val bundle = SavedPanes.save(panes)
        val restored = SavedPanes.restore(bundle)

        // Saved as a root plus one raw name per level above it, not as full paths per level.
        assertEquals(
            panes
                .getValue(PaneId.Left)
                .tab.backStack
                .drop(1)
                .map { it.name },
            bundle.getStringArrayList("Left.0.names"),
        )

        assertEquals(panes.keys, restored.keys)
        panes.forEach { (id, pane) ->
            val before = pane.tab
            val after = restored.getValue(id).tab
            assertEquals(before.backStack.map { it.key }, after.backStack.map { it.key })
            assertEquals(before.backStack.map { it.ref }, after.backStack.map { it.ref })
            assertEquals(before.anchors, after.anchors)
        }
    }
}
