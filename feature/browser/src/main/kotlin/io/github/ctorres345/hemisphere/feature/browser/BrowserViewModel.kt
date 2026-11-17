package io.github.ctorres345.hemisphere.feature.browser

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ctorres345.hemisphere.core.domain.files.GetHomeFolderUseCase
import io.github.ctorres345.hemisphere.core.domain.files.ObserveFolderUseCase
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.mvi.MviViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Lists the home folder. Turns intents into use-case calls and their output into [BrowserResult]s. */
@HiltViewModel
class BrowserViewModel
    @Inject
    constructor(
        getHomeFolder: GetHomeFolderUseCase,
        private val observeFolder: ObserveFolderUseCase,
    ) : MviViewModel<BrowserUiState, BrowserIntent, BrowserResult>(BrowserUiState(), ::reduce) {
        private var listing: Job? = null

        init {
            open(getHomeFolder())
        }

        override fun onIntent(intent: BrowserIntent) {
            when (intent) {
                BrowserIntent.Retry -> state.value.folder?.let(::open)
            }
        }

        private fun open(folder: FsPath) {
            listing?.cancel()
            emit(BrowserResult.FolderOpened(folder))
            listing =
                viewModelScope.launch {
                    observeFolder(folder).collect { emit(BrowserResult.ListingChanged(it)) }
                }
        }
    }
