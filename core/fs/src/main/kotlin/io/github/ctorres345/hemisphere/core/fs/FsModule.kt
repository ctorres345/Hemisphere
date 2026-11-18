package io.github.ctorres345.hemisphere.core.fs

import android.content.Context
import android.os.Environment
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

/**
 * Binds the file-system layer (architecture §9). One [VolumeRepository] for the whole process, so
 * there is one provider per volume and one seeding lock.
 */
@Module
@InstallIn(SingletonComponent::class)
object FsModule {
    @Provides
    @Singleton
    fun storageAccess(
        @ApplicationContext context: Context,
    ): StorageAccess = AndroidStorageAccess(context)

    @Provides
    @Singleton
    fun volumeRepository(
        @ApplicationContext context: Context,
        storageAccess: StorageAccess,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): VolumeRepository =
        appHomeVolumes(
            appFilesDir = context.filesDir,
            primaryRoot = Environment.getExternalStorageDirectory(),
            storageAccess = storageAccess,
            ioDispatcher = ioDispatcher,
        )
}
