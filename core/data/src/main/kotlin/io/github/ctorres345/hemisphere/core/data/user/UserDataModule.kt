package io.github.ctorres345.hemisphere.core.data.user

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.ctorres345.hemisphere.core.fs.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

/**
 * Binds `user.db` and [FolderViewRepository]. Migrations are added to the builder with each schema
 * version and never fall back to destructive migration (architecture §8).
 */
@Module
@InstallIn(SingletonComponent::class)
object UserDataModule {
    @Provides
    @Singleton
    internal fun provideUserDatabase(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): UserDatabase =
        Room
            .databaseBuilder(context, UserDatabase::class.java, UserDatabase.FILE_NAME)
            .setQueryCoroutineContext(ioDispatcher)
            .build()

    @Provides
    @Singleton
    internal fun folderViewRepository(database: UserDatabase): FolderViewRepository =
        RoomFolderViewRepository(database.folderViews())
}
