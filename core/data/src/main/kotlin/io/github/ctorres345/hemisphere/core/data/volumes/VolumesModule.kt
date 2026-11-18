package io.github.ctorres345.hemisphere.core.data.volumes

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.ctorres345.hemisphere.core.fs.IoDispatcher
import io.github.ctorres345.hemisphere.core.fs.VolumeStore
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

/**
 * Binds `volumes.db` and [VolumeStore]. Migrations are added to the builder with each schema
 * version and never fall back to destructive migration (architecture §8).
 */
@Module
@InstallIn(SingletonComponent::class)
object VolumesModule {
    @Provides
    @Singleton
    internal fun provideVolumesDatabase(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): VolumesDatabase =
        Room
            .databaseBuilder(context, VolumesDatabase::class.java, VolumesDatabase.FILE_NAME)
            .setQueryCoroutineContext(ioDispatcher)
            .build()

    @Provides
    @Singleton
    internal fun volumeStore(database: VolumesDatabase): VolumeStore = RoomVolumeStore(database.dao())
}
