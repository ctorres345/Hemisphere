package io.github.ctorres345.hemisphere.core.fs

import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.VolumeId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import java.io.File
import java.io.IOException

/** The volume of the app's own `home` folder: readable without any storage permission. */
val AppHomeVolume = VolumeId("app-home")

/**
 * The walking skeleton's volumes: only the app's own `home` folder under [appFilesDir]. An empty
 * home gets a few sample folders and files on the first listing, so there is something to see.
 * Real storage volumes replace this with the storage-access ticket.
 */
fun appHomeVolumes(
    appFilesDir: File,
    ioDispatcher: CoroutineDispatcher,
): VolumeRepository = AppHomeVolumeRepository(File(appFilesDir, "home"), ioDispatcher)

internal class AppHomeVolumeRepository(
    private val home: File,
    private val ioDispatcher: CoroutineDispatcher,
) : VolumeRepository {
    private val seedLock = Any()
    private var seeded = false
    private val local = LocalProvider(mapOf(AppHomeVolume to home), ioDispatcher)
    private val provider =
        object : FileSystemProvider {
            override fun list(dir: FsPath) =
                local
                    .list(dir)
                    .onStart { seedOnce() }
                    .flowOn(ioDispatcher)
        }

    /** Runs on the IO dispatcher. A failed seed is retried on the next listing; it never fails the listing. */
    private fun seedOnce() =
        synchronized(seedLock) {
            if (!seeded) seeded = SampleContent.seedIfEmpty(home)
        }

    override fun homeFolder(): FsPath = FsPath.root(AppHomeVolume)

    override fun providerFor(volume: VolumeId): FsResult<FileSystemProvider> =
        if (volume == AppHomeVolume) FsResult.Ok(provider) else FsResult.Err(FsError.VolumeUnavailable)
}

/** A few entries for an empty home folder, so a fresh install shows a list. */
internal object SampleContent {
    private val folders = listOf("Documents", "Music", "Pictures", "ROMs")
    private val files = mapOf("Readme.txt" to "Hemisphere sample file.\n", "notes.md" to "# Notes\n")

    /** True when home needs no seeding or was seeded; false when writing failed. */
    fun seedIfEmpty(home: File): Boolean {
        return try {
            home.mkdirs()
            val children = home.list()
            if (children == null || children.isNotEmpty()) return true
            folders.forEach { File(home, it).mkdir() }
            files.forEach { (name, text) -> File(home, name).writeText(text) }
            true
        } catch (
            @Suppress("SwallowedException") e: IOException,
        ) {
            // Samples are optional: the listing goes on and shows what is there.
            false
        } catch (
            @Suppress("SwallowedException") e: SecurityException,
        ) {
            false
        }
    }
}
