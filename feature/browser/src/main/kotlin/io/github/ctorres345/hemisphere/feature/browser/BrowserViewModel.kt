package io.github.ctorres345.hemisphere.feature.browser

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ctorres345.hemisphere.core.domain.files.GetFolderLockUseCase
import io.github.ctorres345.hemisphere.core.domain.files.GetHomeFolderUseCase
import io.github.ctorres345.hemisphere.core.domain.files.ObserveFolderUseCase
import io.github.ctorres345.hemisphere.core.domain.settings.ObserveSettingsUseCase
import io.github.ctorres345.hemisphere.core.domain.settings.SetLayoutPinUseCase
import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.LayoutPin
import io.github.ctorres345.hemisphere.core.model.Settings
import io.github.ctorres345.hemisphere.core.mvi.MviViewModel
import io.github.ctorres345.hemisphere.feature.browser.layout.BrowserWindow
import io.github.ctorres345.hemisphere.feature.browser.layout.PaneLayout
import io.github.ctorres345.hemisphere.feature.browser.layout.SavedShell
import io.github.ctorres345.hemisphere.feature.browser.layout.layoutFor
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneId
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneIntent
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneResult
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneState
import io.github.ctorres345.hemisphere.feature.browser.pane.SavedPanes
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * The Browser screen. It only routes: intents become use-case calls and their output becomes
 * [BrowserResult]s; the pane logic is in `pane/`. Back stacks, scroll anchors, the active pane and the
 * layout are saved in [SavedStateHandle] when Android asks, so they survive process death.
 *
 * The layout follows the user's pin for the window's orientation, or the window width under Auto
 * (design-system §2). Only the shown panes are listed: a pane Single hides keeps its history but reads
 * nothing until it shows again.
 */
@HiltViewModel
class BrowserViewModel
    @Inject
    constructor(
        savedState: SavedStateHandle,
        getHomeFolder: GetHomeFolderUseCase,
        observeSettings: ObserveSettingsUseCase,
        private val observeFolder: ObserveFolderUseCase,
        private val getFolderLock: GetFolderLockUseCase,
        private val setLayoutPin: SetLayoutPinUseCase,
    ) : MviViewModel<BrowserUiState, BrowserIntent, BrowserResult>(
            initialState(savedState, getHomeFolder),
            ::reduce,
        ) {
        // Declared above init: Kotlin runs them in order.
        private val listings = mutableMapOf<PaneId, Job>()
        private var window: BrowserWindow? = null
        private var settings: Settings? = null

        init {
            savedState.setSavedStateProvider(SavedPanes.KEY) { SavedPanes.save(state.value.panes) }
            savedState.setSavedStateProvider(SavedShell.KEY) { SavedShell.save(state.value.shell) }
            state.value.shownPanes.forEach(::list)
            observeSettings()
                .onEach {
                    settings = it
                    relayout()
                }.launchIn(viewModelScope)
        }

        override fun onIntent(intent: BrowserIntent) {
            when (intent) {
                is BrowserIntent.Pane -> {
                    onPaneIntent(intent.pane, intent.intent)
                }

                is BrowserIntent.WindowChanged -> {
                    window = intent.window
                    relayout()
                }

                is BrowserIntent.Activate -> {
                    emit(BrowserResult.Activated(intent.pane))
                }

                BrowserIntent.ToggleLayout -> {
                    toggleLayout()
                }
            }
        }

        /** Shows the layout the pin and window call for; lists a pane that shows, stops one that hides. */
        private fun relayout() {
            val shown = window
            val pin = shown?.let { settings?.pinFor(it.orientation) } ?: return
            val next = layoutFor(pin, shown.width)
            if (next == state.value.shell.layout) return
            val before = state.value.shownPanes
            emit(BrowserResult.LayoutChanged(next))
            val after = state.value.shownPanes
            (before - after.toSet()).forEach { listings.remove(it)?.cancel() }
            (after - before.toSet()).forEach(::list)
        }

        /** Pins the layout not shown for this orientation; the settings flow then switches to it. */
        private fun toggleLayout() {
            val orientation = window?.orientation ?: return
            val pin = if (state.value.shell.layout == PaneLayout.Dual) LayoutPin.Single else LayoutPin.Dual
            viewModelScope.launch {
                // A failed write keeps the layout shown; the user can tap again.
                try {
                    setLayoutPin(orientation, pin)
                } catch (ignored: IOException) {
                }
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
            /** The saved shell and panes when Android restored them, else both panes on the home folder. */
            private fun initialState(
                savedState: SavedStateHandle,
                getHomeFolder: GetHomeFolderUseCase,
            ): BrowserUiState {
                val saved = SavedPanes.restore(savedState[SavedPanes.KEY])
                val home by lazy { getHomeFolder() }
                val panes = PaneId.entries.associateWith { saved[it] ?: PaneState.on(home) }
                return BrowserUiState(shell = SavedShell.restore(savedState[SavedShell.KEY]), panes = panes)
            }
        }
    }
