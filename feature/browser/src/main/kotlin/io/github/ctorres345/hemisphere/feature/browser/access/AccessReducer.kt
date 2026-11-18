package io.github.ctorres345.hemisphere.feature.browser.access

import io.github.ctorres345.hemisphere.core.model.StorageAccessState

/** The only place [AccessUiState] changes. Pure: no I/O, no coroutines, no clock. */
fun reduce(
    state: AccessUiState,
    result: AccessResult,
): AccessUiState =
    when (result) {
        is AccessResult.AccessChanged -> {
            state.copy(
                access = result.access,
                // A pending launch means nothing once access is granted.
                launch = if (result.access == StorageAccessState.Granted) null else state.launch,
            )
        }

        AccessResult.GrantRequested -> {
            state.copy(
                launch =
                    if (state.access == StorageAccessState.NeedsSettings) {
                        AccessLaunch.AppSettings
                    } else {
                        AccessLaunch.Grant
                    },
            )
        }

        AccessResult.LaunchCleared -> {
            state.copy(launch = null)
        }
    }
