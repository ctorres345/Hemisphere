package io.github.ctorres345.hemisphere.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import io.github.ctorres345.hemisphere.core.model.DeviceClass
import io.github.ctorres345.hemisphere.core.model.Settings
import io.github.ctorres345.hemisphere.core.model.ThemeSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

/** The read-error path of [DataStoreSettingsRepository.settings], with a store that fails on demand. */
class DataStoreSettingsRetryTest {
    private val saved = mutablePreferencesOf(DataStoreSettingsRepository.THEME to ThemeSetting.Dark.name)

    private class FailingStore(
        private val failures: Int,
        private val error: Throwable,
        private val value: Preferences,
    ) : DataStore<Preferences> {
        var reads = 0

        override val data: Flow<Preferences> =
            flow {
                reads++
                if (reads <= failures) throw error
                emit(value)
            }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences) = error("unused")
    }

    @Test
    fun `a read error shows defaults once, then the saved value after a retry`() =
        runTest {
            val store = FailingStore(failures = 2, error = IOException("disk"), value = saved)
            val seen = DataStoreSettingsRepository(store, DeviceClass.Phone).settings.take(2).toList()
            assertEquals(listOf(Settings.DEFAULT, Settings.DEFAULT.copy(theme = ThemeSetting.Dark)), seen)
            assertEquals(3, store.reads)
        }

    @Test
    fun `other errors are not swallowed`() {
        val store = FailingStore(failures = 1, error = IllegalStateException("bug"), value = saved)
        assertThrows(IllegalStateException::class.java) {
            runTest { DataStoreSettingsRepository(store, DeviceClass.Phone).settings.take(1).toList() }
        }
    }
}
