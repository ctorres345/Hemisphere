package io.github.ctorres345.hemisphere.feature.browser.access

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ctorres345.hemisphere.core.domain.files.ObserveStorageAccessUseCase
import io.github.ctorres345.hemisphere.core.domain.files.RecordStoragePermissionResultUseCase
import io.github.ctorres345.hemisphere.core.mvi.MviViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Follows storage access and turns the Grant button into the system page to open. */
@HiltViewModel
class AccessViewModel
    @Inject
    constructor(
        observeStorageAccess: ObserveStorageAccessUseCase,
        private val recordPermissionResult: RecordStoragePermissionResultUseCase,
    ) : MviViewModel<AccessUiState, AccessIntent, AccessResult>(AccessUiState(), ::reduce) {
        init {
            viewModelScope.launch {
                observeStorageAccess().collect { emit(AccessResult.AccessChanged(it)) }
            }
        }

        override fun onIntent(intent: AccessIntent) {
            when (intent) {
                AccessIntent.GrantClicked -> {
                    emit(AccessResult.GrantRequested)
                }

                AccessIntent.LaunchHandled -> {
                    emit(AccessResult.LaunchCleared)
                }

                is AccessIntent.PermissionResult -> {
                    recordPermissionResult(intent.granted, intent.canAskAgain)
                }

                AccessIntent.PickFolderClicked -> {
                    // TODO(HEM-41): open the folder picker once the SAF fallback exists. A no-op until then.
                }
            }
        }
    }
