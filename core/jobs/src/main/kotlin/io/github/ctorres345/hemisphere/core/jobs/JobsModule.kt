package io.github.ctorres345.hemisphere.core.jobs

import android.content.Context
import android.os.SystemClock
import androidx.room.Room
import dagger.MapKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import io.github.ctorres345.hemisphere.core.fs.IoDispatcher
import io.github.ctorres345.hemisphere.core.jobs.db.StateDatabase
import io.github.ctorres345.hemisphere.core.model.JobKind
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/** The key of a [JobRunner] in the `Map<JobKind, JobRunner>` multibinding. */
@MapKey
annotation class JobKindKey(
    val value: JobKind,
)

/** Declares the runner map, so it may stay empty until the tickets that fill it. */
@Module
@InstallIn(SingletonComponent::class)
interface JobRunnersModule {
    @Multibinds
    fun runners(): Map<JobKind, JobRunner>
}

/**
 * Binds `state.db` and [JobQueue]. Migrations are added to the builder with each schema version
 * and never fall back to destructive migration (architecture §8).
 */
@Module
@InstallIn(SingletonComponent::class)
object JobsModule {
    @Provides
    @Singleton
    internal fun provideStateDatabase(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): StateDatabase =
        Room
            .databaseBuilder(context, StateDatabase::class.java, StateDatabase.FILE_NAME)
            .setQueryCoroutineContext(ioDispatcher)
            .build()

    @Provides
    @Singleton
    internal fun jobQueue(
        database: StateDatabase,
        runners: Map<JobKind, @JvmSuppressWildcards JobRunner>,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): JobQueue =
        RoomJobQueue(
            database = database,
            runners = runners,
            scope = CoroutineScope(SupervisorJob() + ioDispatcher),
            clock = SystemClock::elapsedRealtime,
        )
}
