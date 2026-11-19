package io.github.ctorres345.hemisphere.feature.settings

import io.github.ctorres345.hemisphere.core.designsystem.FaceButtonLayout
import io.github.ctorres345.hemisphere.core.model.Accent
import io.github.ctorres345.hemisphere.core.model.FaceButtons
import io.github.ctorres345.hemisphere.core.model.LayoutPin
import io.github.ctorres345.hemisphere.core.model.Orientation
import io.github.ctorres345.hemisphere.core.model.Settings
import io.github.ctorres345.hemisphere.core.model.ThemeSetting

/** Everything the Settings screen shows: the saved settings, null until they are read. */
data class SettingsUiState(
    val settings: Settings? = null,
)

/**
 * What the screen reads from the device, not from the saved settings.
 *
 * @param version the app version, shown under About.
 * @param autoFaceButtons the layout Auto picks for the connected pad (Xbox when none is known).
 */
data class SettingsDevice(
    val version: String,
    val autoFaceButtons: FaceButtonLayout = FaceButtonLayout.Xbox,
)

/** Everything the user can do on the Settings screen. Each choice is saved at once. */
sealed interface SettingsIntent {
    data class ThemeChosen(
        val theme: ThemeSetting,
    ) : SettingsIntent

    data class AccentChosen(
        val accent: Accent,
    ) : SettingsIntent

    data class LayoutPinChosen(
        val orientation: Orientation,
        val pin: LayoutPin,
    ) : SettingsIntent

    data class FaceButtonsChosen(
        val faceButtons: FaceButtons,
    ) : SettingsIntent

    data class TrashRetentionChosen(
        val days: Int,
    ) : SettingsIntent
}

/** Everything that can change [SettingsUiState]. */
sealed interface SettingsResult {
    /** The saved settings changed, or were read for the first time. */
    data class SettingsLoaded(
        val settings: Settings,
    ) : SettingsResult
}
