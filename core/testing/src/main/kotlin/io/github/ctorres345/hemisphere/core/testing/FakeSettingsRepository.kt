package io.github.ctorres345.hemisphere.core.testing

import io.github.ctorres345.hemisphere.core.data.settings.SettingsRepository
import io.github.ctorres345.hemisphere.core.model.Accent
import io.github.ctorres345.hemisphere.core.model.FaceButtons
import io.github.ctorres345.hemisphere.core.model.LayoutPin
import io.github.ctorres345.hemisphere.core.model.Orientation
import io.github.ctorres345.hemisphere.core.model.Settings
import io.github.ctorres345.hemisphere.core.model.ThemeSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Settings held in memory. */
class FakeSettingsRepository(
    initial: Settings = Settings.DEFAULT,
) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: Flow<Settings> = state

    override suspend fun setTheme(theme: ThemeSetting) = state.update { it.copy(theme = theme) }

    override suspend fun setAccent(accent: Accent) = state.update { it.copy(accent = accent) }

    override suspend fun setFaceButtons(faceButtons: FaceButtons) = state.update { it.copy(faceButtons = faceButtons) }

    override suspend fun setTrashRetentionDays(days: Int) =
        state.update {
            it.copy(trashRetentionDays = days.coerceAtLeast(1))
        }

    override suspend fun setLayoutPin(
        orientation: Orientation,
        pin: LayoutPin,
    ) = state.update {
        when (orientation) {
            Orientation.Portrait -> it.copy(layoutPinPortrait = pin)
            Orientation.Landscape -> it.copy(layoutPinLandscape = pin)
        }
    }
}
