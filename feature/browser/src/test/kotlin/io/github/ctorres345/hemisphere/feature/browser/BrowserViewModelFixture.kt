package io.github.ctorres345.hemisphere.feature.browser

import androidx.lifecycle.SavedStateHandle
import io.github.ctorres345.hemisphere.core.domain.files.GetFolderLockUseCase
import io.github.ctorres345.hemisphere.core.domain.files.GetHomeFolderUseCase
import io.github.ctorres345.hemisphere.core.domain.files.ObserveFolderUseCase
import io.github.ctorres345.hemisphere.core.fs.LockedFolders
import io.github.ctorres345.hemisphere.core.fs.testing.FakeVolumeRepository
import io.github.ctorres345.hemisphere.core.fs.testing.InMemoryProvider
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.testing.MainDispatcherRule
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneId
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneIntent
import io.github.ctorres345.hemisphere.feature.browser.pane.TabState
import org.junit.Rule

/** Real use cases over the in-memory provider, shared by the Browser ViewModel tests. No mocks. */
abstract class BrowserViewModelFixture {
    @get:Rule
    val main = MainDispatcherRule()

    protected val provider = InMemoryProvider()
    protected val volumes = FakeVolumeRepository(provider)
    protected val home: FsPath = volumes.homeFolder()
    protected val music = home.child("Music")

    protected fun viewModel(sdkInt: Int = LockedFolders.API_R) =
        BrowserViewModel(
            SavedStateHandle(),
            GetHomeFolderUseCase(volumes),
            ObserveFolderUseCase(volumes, main.dispatcher),
            GetFolderLockUseCase(LockedFolders(sdkInt)),
        )

    protected val BrowserViewModel.tab: TabState get() = state.value.activePane!!.tab

    protected fun BrowserViewModel.send(intent: PaneIntent) = onIntent(BrowserIntent.Pane(PaneId.Left, intent))

    protected fun BrowserViewModel.open(name: String) =
        send(PaneIntent.OpenEntry(tab.listing.entries.first { it.name == name }, leaving = null))
}
