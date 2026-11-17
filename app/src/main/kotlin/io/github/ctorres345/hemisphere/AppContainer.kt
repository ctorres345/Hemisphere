package io.github.ctorres345.hemisphere

import android.content.Context
import io.github.ctorres345.hemisphere.core.domain.files.GetHomeFolderUseCase
import io.github.ctorres345.hemisphere.core.domain.files.ObserveFolderUseCase
import io.github.ctorres345.hemisphere.core.fs.appHomeVolumes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Hand-written wiring until Hilt lands (architecture §9). It builds the data layer once and hands
 * out use cases; ViewModels never see anything else.
 */
class AppContainer(
    context: Context,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val volumes = appHomeVolumes(context.filesDir, ioDispatcher)

    val getHomeFolder = GetHomeFolderUseCase(volumes)
    val observeFolder = ObserveFolderUseCase(volumes, defaultDispatcher)
}
