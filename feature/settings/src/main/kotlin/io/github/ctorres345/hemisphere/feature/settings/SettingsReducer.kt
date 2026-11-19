package io.github.ctorres345.hemisphere.feature.settings

/** The only place [SettingsUiState] changes. Pure: no I/O, no coroutines, no clock. */
fun reduce(
    state: SettingsUiState,
    result: SettingsResult,
): SettingsUiState =
    when (result) {
        is SettingsResult.SettingsLoaded -> state.copy(settings = result.settings)
    }
