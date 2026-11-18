package io.github.ctorres345.hemisphere.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.ctorres345.hemisphere.core.model.Accent
import io.github.ctorres345.hemisphere.core.model.DeviceClass
import io.github.ctorres345.hemisphere.core.model.FaceButtons
import io.github.ctorres345.hemisphere.core.model.LayoutPin
import io.github.ctorres345.hemisphere.core.model.Orientation
import io.github.ctorres345.hemisphere.core.model.Settings
import io.github.ctorres345.hemisphere.core.model.ThemeSetting
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import java.io.IOException

/**
 * Settings in a Preferences DataStore. The layout pin is keyed by [deviceClass] and orientation, so
 * a backup restored on another kind of device keeps each device's own pin. Keys hold enum names and
 * numbers only, never paths (ADR 0008).
 */
internal class DataStoreSettingsRepository(
    private val store: DataStore<Preferences>,
    private val deviceClass: DeviceClass,
) : SettingsRepository {
    // A read error shows defaults if nothing was shown yet, then retries with a growing pause, so the
    // flow never ends and later writes still reach every collector. A corrupt file never gets here:
    // the store's corruption handler replaces it (SettingsModule).
    override val settings: Flow<Settings> =
        flow {
            var shown = false
            store.data
                .retryWhen { cause, attempt ->
                    if (cause !is IOException) return@retryWhen false
                    if (!shown) emit(emptyPreferences())
                    delay(RETRY_BASE_MS shl attempt.coerceAtMost(RETRY_MAX_SHIFT).toInt())
                    true
                }.collect {
                    shown = true
                    emit(it.toSettings())
                }
        }

    override suspend fun setTheme(theme: ThemeSetting) = write { it[THEME] = theme.name }

    override suspend fun setAccent(accent: Accent) = write { it[ACCENT] = accent.name }

    override suspend fun setFaceButtons(faceButtons: FaceButtons) = write { it[FACE_BUTTONS] = faceButtons.name }

    override suspend fun setTrashRetentionDays(days: Int) = write { it[TRASH_RETENTION_DAYS] = days.coerceAtLeast(1) }

    override suspend fun setLayoutPin(
        orientation: Orientation,
        pin: LayoutPin,
    ) = write { it[pinKey(orientation)] = pin.name }

    private suspend fun write(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        store.edit { block(it) }
    }

    private fun pinKey(orientation: Orientation) = pinKey(deviceClass, orientation)

    private fun Preferences.toSettings() =
        Settings(
            theme = enumOrDefault(this[THEME], Settings.DEFAULT.theme),
            accent = enumOrDefault(this[ACCENT], Settings.DEFAULT.accent),
            faceButtons = enumOrDefault(this[FACE_BUTTONS], Settings.DEFAULT.faceButtons),
            trashRetentionDays =
                (this[TRASH_RETENTION_DAYS] ?: Settings.DEFAULT.trashRetentionDays).coerceAtLeast(1),
            layoutPinPortrait = enumOrDefault(this[pinKey(Orientation.Portrait)], LayoutPin.Auto),
            layoutPinLandscape = enumOrDefault(this[pinKey(Orientation.Landscape)], LayoutPin.Auto),
        )

    private inline fun <reified E : Enum<E>> enumOrDefault(
        stored: String?,
        default: E,
    ): E = enumValues<E>().firstOrNull { it.name == stored } ?: default

    internal companion object {
        val THEME = stringPreferencesKey("theme")
        val ACCENT = stringPreferencesKey("accent")
        val FACE_BUTTONS = stringPreferencesKey("face_buttons")
        val TRASH_RETENTION_DAYS = intPreferencesKey("trash_retention_days")
        private const val RETRY_BASE_MS = 500L
        private const val RETRY_MAX_SHIFT = 6L

        /** `layout_pin_<phone|handheld|tablet>_<portrait|landscape>` */
        fun pinKey(
            deviceClass: DeviceClass,
            orientation: Orientation,
        ) = stringPreferencesKey(
            "layout_pin_${deviceClass.name.lowercase()}_${orientation.name.lowercase()}",
        )
    }
}
