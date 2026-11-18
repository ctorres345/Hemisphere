package io.github.ctorres345.hemisphere.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import io.github.ctorres345.hemisphere.core.model.Accent
import io.github.ctorres345.hemisphere.core.model.DeviceClass
import io.github.ctorres345.hemisphere.core.model.FaceButtons
import io.github.ctorres345.hemisphere.core.model.LayoutPin
import io.github.ctorres345.hemisphere.core.model.Orientation
import io.github.ctorres345.hemisphere.core.model.Settings
import io.github.ctorres345.hemisphere.core.model.ThemeSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

private const val TIMEOUT_MS = 5_000L

class SettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After
    fun tearDown() = scope.cancel()

    // The shipped corruption handler, so the corrupt-file test covers it.
    private fun store(name: String = "settings"): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = settingsCorruptionHandler(),
            scope = scope,
            produceFile = { folder.root.resolve("$name.preferences_pb") },
        )

    private fun repo(
        deviceClass: DeviceClass = DeviceClass.Phone,
        name: String = "settings",
    ) = DataStoreSettingsRepository(store(name), deviceClass)

    @Test
    fun `defaults`() =
        runBlocking {
            val s = repo().settings.first()
            assertEquals(Settings.DEFAULT, s)
            assertEquals(30, s.trashRetentionDays)
        }

    @Test
    fun `a live collector gets each write`() =
        runBlocking {
            val r = repo()
            val seen = Channel<Settings>(Channel.UNLIMITED)
            val job = launch(Dispatchers.IO) { r.settings.collect { seen.send(it) } }
            assertEquals(Settings.DEFAULT, withTimeout(TIMEOUT_MS) { seen.receive() })
            r.setTheme(ThemeSetting.AMOLED)
            assertEquals(ThemeSetting.AMOLED, withTimeout(TIMEOUT_MS) { seen.receive() }.theme)
            r.setAccent(Accent.Violet)
            assertEquals(Accent.Violet, withTimeout(TIMEOUT_MS) { seen.receive() }.accent)
            job.cancel()
        }

    @Test
    fun `each setting saves and is emitted again`() =
        runBlocking {
            val r = repo()
            r.setTheme(ThemeSetting.AMOLED)
            assertEquals(ThemeSetting.AMOLED, r.settings.first().theme)
            r.setAccent(Accent.Violet)
            assertEquals(Accent.Violet, r.settings.first().accent)
            r.setFaceButtons(FaceButtons.Xbox)
            assertEquals(FaceButtons.Xbox, r.settings.first().faceButtons)
            r.setTrashRetentionDays(7)
            assertEquals(7, r.settings.first().trashRetentionDays)
            r.setTrashRetentionDays(0)
            assertEquals(1, r.settings.first().trashRetentionDays)
        }

    @Test
    fun `portrait and landscape pins are separate`() =
        runBlocking {
            val r = repo()
            r.setLayoutPin(Orientation.Portrait, LayoutPin.Single)
            r.setLayoutPin(Orientation.Landscape, LayoutPin.Dual)
            val s = r.settings.first()
            assertEquals(LayoutPin.Single, s.layoutPinPortrait)
            assertEquals(LayoutPin.Dual, s.layoutPinLandscape)
        }

    @Test
    fun `another device class does not read the pin`() =
        runBlocking {
            val shared = store("shared")
            DataStoreSettingsRepository(shared, DeviceClass.Phone).setLayoutPin(Orientation.Portrait, LayoutPin.Dual)
            val handheld = DataStoreSettingsRepository(shared, DeviceClass.Handheld)
            assertEquals(LayoutPin.Auto, handheld.settings.first().layoutPinPortrait)
            assertEquals(
                LayoutPin.Dual,
                DataStoreSettingsRepository(shared, DeviceClass.Phone).settings.first().layoutPinPortrait,
            )
        }

    @Test
    fun `an unknown stored value falls back to the default`() =
        runBlocking {
            val s = store("x")
            s.edit { it[DataStoreSettingsRepository.THEME] = "Sepia" }
            assertEquals(
                ThemeSetting.FollowSystem,
                DataStoreSettingsRepository(s, DeviceClass.Phone).settings.first().theme,
            )
        }

    @Test
    fun `a corrupt file gives defaults and still saves`() =
        runBlocking {
            folder.root.resolve("bad.preferences_pb").writeBytes(ByteArray(64) { (it * 7 + 3).toByte() })
            val r = repo(name = "bad")
            assertEquals(Settings.DEFAULT, r.settings.first())
            // Without the corruption handler this write throws CorruptionException.
            r.setTheme(ThemeSetting.Dark)
            assertEquals(ThemeSetting.Dark, r.settings.first().theme)
        }

    @Test
    fun `the key list holds no paths`() {
        val keys =
            DeviceClass.entries.flatMap { d ->
                Orientation.entries.map { DataStoreSettingsRepository.pinKey(d, it).name }
            }
        assertEquals(
            listOf(
                "layout_pin_phone_portrait",
                "layout_pin_phone_landscape",
                "layout_pin_handheld_portrait",
                "layout_pin_handheld_landscape",
                "layout_pin_tablet_portrait",
                "layout_pin_tablet_landscape",
            ),
            keys,
        )
    }
}
