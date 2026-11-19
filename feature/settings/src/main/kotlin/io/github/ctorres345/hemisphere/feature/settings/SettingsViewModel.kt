package io.github.ctorres345.hemisphere.feature.settings

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ctorres345.hemisphere.core.domain.settings.ObserveSettingsUseCase
import io.github.ctorres345.hemisphere.core.domain.settings.SetAccentUseCase
import io.github.ctorres345.hemisphere.core.domain.settings.SetFaceButtonsUseCase
import io.github.ctorres345.hemisphere.core.domain.settings.SetLayoutPinUseCase
import io.github.ctorres345.hemisphere.core.domain.settings.SetThemeUseCase
import io.github.ctorres345.hemisphere.core.domain.settings.SetTrashRetentionUseCase
import io.github.ctorres345.hemisphere.core.mvi.MviViewModel
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * Shows the saved settings and saves each choice. The state follows the saved settings, so a choice
 * shows once it is saved, and the app theme (which observes the same settings) changes with it.
 */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        observeSettings: ObserveSettingsUseCase,
        private val setTheme: SetThemeUseCase,
        private val setAccent: SetAccentUseCase,
        private val setLayoutPin: SetLayoutPinUseCase,
        private val setFaceButtons: SetFaceButtonsUseCase,
        private val setTrashRetention: SetTrashRetentionUseCase,
    ) : MviViewModel<SettingsUiState, SettingsIntent, SettingsResult>(SettingsUiState(), ::reduce) {
        init {
            viewModelScope.launch {
                observeSettings().collect { emit(SettingsResult.SettingsLoaded(it)) }
            }
        }

        override fun onIntent(intent: SettingsIntent) {
            viewModelScope.launch {
                try {
                    save(intent)
                } catch (_: IOException) {
                    // The write failed (a full disk, say). The screen shows the saved values, so the
                    // old choice stays selected and the user can try again.
                }
            }
        }

        private suspend fun save(intent: SettingsIntent) {
            when (intent) {
                is SettingsIntent.ThemeChosen -> setTheme(intent.theme)
                is SettingsIntent.AccentChosen -> setAccent(intent.accent)
                is SettingsIntent.LayoutPinChosen -> setLayoutPin(intent.orientation, intent.pin)
                is SettingsIntent.FaceButtonsChosen -> setFaceButtons(intent.faceButtons)
                is SettingsIntent.TrashRetentionChosen -> setTrashRetention(intent.days)
            }
        }
    }
