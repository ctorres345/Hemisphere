package io.github.ctorres345.hemisphere.core.data.settings

import io.github.ctorres345.hemisphere.core.model.Accent
import io.github.ctorres345.hemisphere.core.model.FaceButtons
import io.github.ctorres345.hemisphere.core.model.LayoutPin
import io.github.ctorres345.hemisphere.core.model.Orientation
import io.github.ctorres345.hemisphere.core.model.Settings
import io.github.ctorres345.hemisphere.core.model.ThemeSetting
import kotlinx.coroutines.flow.Flow

/**
 * Saved settings, observed as a [Flow] (architecture §8). Holds no paths or file names (ADR 0008).
 * A read error shows defaults and keeps retrying. The setters throw [java.io.IOException] when the
 * write fails (a full disk, say), so callers catch it.
 */
interface SettingsRepository {
    val settings: Flow<Settings>

    suspend fun setTheme(theme: ThemeSetting)

    suspend fun setAccent(accent: Accent)

    suspend fun setFaceButtons(faceButtons: FaceButtons)

    /** Days a Trash item is kept. Values below 1 are raised to 1. */
    suspend fun setTrashRetentionDays(days: Int)

    suspend fun setLayoutPin(
        orientation: Orientation,
        pin: LayoutPin,
    )
}
