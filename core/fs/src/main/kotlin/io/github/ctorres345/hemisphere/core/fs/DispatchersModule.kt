package io.github.ctorres345.hemisphere.core.fs

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * The real dispatchers behind [IoDispatcher] and [DefaultDispatcher], next to the qualifiers. Tests
 * pass their own. This is the one place that names them, hence the InjectDispatcher suppression.
 */
@Module
@InstallIn(SingletonComponent::class)
@Suppress("InjectDispatcher")
object DispatchersModule {
    @Provides
    @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
