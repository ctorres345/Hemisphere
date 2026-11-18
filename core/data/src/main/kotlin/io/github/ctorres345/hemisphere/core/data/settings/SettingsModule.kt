package io.github.ctorres345.hemisphere.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.ctorres345.hemisphere.core.fs.IoDispatcher
import io.github.ctorres345.hemisphere.core.model.DeviceClass
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/** Marks the settings DataStore, so only [SettingsRepository] reads its keys (architecture §8). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class SettingsStore

/**
 * Replaces a corrupt settings file with an empty one, so start never crashes. The repository test
 * uses this same handler.
 */
internal fun settingsCorruptionHandler() = ReplaceFileCorruptionHandler { emptyPreferences() }

/** Binds the settings store. */
@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {
    @Provides
    @Singleton
    @SettingsStore
    internal fun provideSettingsDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = settingsCorruptionHandler(),
            scope = CoroutineScope(ioDispatcher + SupervisorJob()),
            produceFile = { context.preferencesDataStoreFile("settings") },
        )

    @Provides
    @Singleton
    internal fun settingsRepository(
        @SettingsStore store: DataStore<Preferences>,
        deviceClass: DeviceClass,
    ): SettingsRepository = DataStoreSettingsRepository(store, deviceClass)
}
