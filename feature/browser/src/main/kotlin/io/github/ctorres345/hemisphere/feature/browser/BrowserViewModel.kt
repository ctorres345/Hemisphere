package io.github.ctorres345.hemisphere.feature.browser

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ctorres345.hemisphere.core.domain.files.GetFolderLockUseCase
import io.github.ctorres345.hemisphere.core.domain.files.GetHomeFolderUseCase
import io.github.ctorres345.hemisphere.core.domain.files.ObserveFolderUseCase
import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.mvi.MviViewModel
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneId
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneIntent
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneResult
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneState
import io.github.ctorres345.hemisphere.feature.browser.pane.SavedPanes
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * The Browser screen. It only routes: intents become use-case calls and their output becomes
 * [BrowserResult]s; the pane logic is in `pane/`. Back stacks and scroll anchors are saved in
 * [SavedStateHandle] when Android asks, so they survive process death.
 *
 * Only the shown panes are listed: Single shows the left pane, so the right pane keeps its history
 * but reads nothing until Dual (HEM-54) shows it.
 */
@HiltViewModel
class BrowserViewModel
    @Inject
    constructor(
        savedState: SavedStateHandle,
        getHomeFolder: GetHomeFolderUseCase,
        private val observeFolder: ObserveFolderUseCase,
        private val getFolderLock: GetFolderLockUseCase,
    ) : MviViewModel<BrowserUiState, BrowserIntent, BrowserResult>(
            initialState(savedState, getHomeFolder),
            ::reduce,
        ) {
        // Declared above init: Kotlin runs them in order.
        private val listings = mutableMapOf<PaneId, Job>()

        init {
            savedState.setSavedStateProvider(SavedPanes.KEY) { SavedPanes.save(state.value.panes) }
            list(PaneId.Left)
        }

        override fun onIntent(intent: BrowserIntent) {
            when (intent) {
                is BrowserIntent.Pane -> onPaneIntent(intent.pane, intent.intent)
            }
        }

        private fun onPaneIntent(
            pane: PaneId,
            intent: PaneIntent,
        ) {
            val tab = state.value.panes[pane]?.tab ?: return
            when {
                // A row of the open folder's listing only: a second tap that lands before the list recomposes
                // still holds the old folder's entry, and would open <new folder>/<old name>. Comparing the
                // folder, not the entry, also holds when a child has the same name and metadata (a/a).
                intent is PaneIntent.OpenEntry &&
                    intent.entry.kind == Entry.Kind.Folder &&
                    intent.from == tab.folder -> {
                    emit(
                        BrowserResult.Pane(
                            pane,
                            PaneResult.Opened(tab.folder.child(intent.entry.name, intent.entry.ref), intent.leaving),
                        ),
                    )
                    list(pane)
                }

                intent is PaneIntent.Back && tab.canGoBack -> {
                    emit(BrowserResult.Pane(pane, PaneResult.WentBack))
                    list(pane)
                }

                intent is PaneIntent.Retry -> {
                    list(pane)
                }

                // Opening files comes with HEM-68; back at the root is left to the system.
                else -> {}
            }
        }

        /** Lists the pane's open folder. The old folder's collector is cancelled first, so it never wins. */
        private fun list(pane: PaneId) {
            listings.remove(pane)?.cancel()
            val folder: FsPath =
                state.value.panes[pane]
                    ?.tab
                    ?.folder ?: return
            val lock = getFolderLock(folder)
            emit(BrowserResult.Pane(pane, PaneResult.ListingStarted(folder, lock)))
            if (lock.locked) return
            val listing =
                observeFolder(folder)
                    .onEach { emit(BrowserResult.Pane(pane, PaneResult.ListingChanged(folder, it))) }
                    .launchIn(viewModelScope)
            listings[pane] = listing
        }

        private companion object {
            /** The saved panes when Android restored them, else both panes on the home folder. */
            private fun initialState(
                savedState: SavedStateHandle,
                getHomeFolder: GetHomeFolderUseCase,
            ): BrowserUiState {
                val saved = SavedPanes.restore(savedState[SavedPanes.KEY])
                val home by lazy { getHomeFolder() }
                val panes = PaneId.entries.associateWith { saved[it] ?: PaneState.on(home) }
                return BrowserUiState(panes = panes)
            }
        }
    }
