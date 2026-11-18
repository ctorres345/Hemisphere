package io.github.ctorres345.hemisphere.core.fs

import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.StorageAccessState
import io.github.ctorres345.hemisphere.core.model.VolumeId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** The volume of the app's own `home` folder: readable without any storage permission. */
val AppHomeVolume = VolumeId("app-home")

/** The device's internal storage (the shared storage root), readable once [StorageAccess] is granted (HEM-39). */
val PrimaryVolume = VolumeId("primary")

/**
 * The app's volumes: the internal storage at [primaryRoot], which the app opens on and which is
 * served only while [storageAccess] is granted, and the app's own `home` folder under [appFilesDir].
 * In debug builds a new app home gets a few sample folders and files on its first listing, so
 * there is something to see. Release builds never seed ([BuildConfig.SEED_SAMPLES]) and start with
 * an empty app home. SD cards and picked folders come with later tickets.
 */
fun appHomeVolumes(
    appFilesDir: File,
    primaryRoot: File,
    storageAccess: StorageAccess,
    ioDispatcher: CoroutineDispatcher,
): VolumeRepository =
    AppHomeVolumeRepository(
        home = File(appFilesDir, "home"),
        seedMarker = File(appFilesDir, SampleContent.MARKER_NAME),
        primaryRoot = primaryRoot,
        storageAccess = storageAccess,
        ioDispatcher = ioDispatcher,
        seedSamples = BuildConfig.SEED_SAMPLES,
    )

internal class AppHomeVolumeRepository(
    private val home: File,
    private val seedMarker: File,
    private val primaryRoot: File,
    private val storageAccess: StorageAccess,
    private val ioDispatcher: CoroutineDispatcher,
    private val seedSamples: Boolean,
) : VolumeRepository {
    private val primaryProvider = LocalProvider(mapOf(PrimaryVolume to primaryRoot), ioDispatcher)
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

    /**
     * Runs on the IO dispatcher. Without samples it only makes sure home exists, so a fresh
     * release install lists an empty folder instead of an error. A failed seed is retried on the
     * next listing; it never fails the listing.
     */
    private fun seedOnce() =
        synchronized(seedLock) {
            if (!seeded) {
                seeded =
                    if (seedSamples) {
                        SampleContent.seed(home, seedMarker)
                    } else {
                        home.mkdirs() || home.isDirectory
                    }
            }
        }

    override fun homeFolder(): FsPath = FsPath.root(PrimaryVolume)

    override fun providerFor(volume: VolumeId): FsResult<FileSystemProvider> =
        when (volume) {
            PrimaryVolume -> {
                if (storageAccess.state.value == StorageAccessState.Granted) {
                    FsResult.Ok(primaryProvider)
                } else {
                    FsResult.Err(FsError.AccessDenied)
                }
            }

            AppHomeVolume -> {
                FsResult.Ok(provider)
            }

            else -> {
                FsResult.Err(FsError.VolumeUnavailable)
            }
        }
}

/**
 * A few entries for a new home folder, so a fresh install shows a list.
 *
 * A marker file beside home (never inside it, so it never shows in a listing) records progress:
 * [STARTED] before the first sample is written, [DONE] after the last. While it is not [DONE], each
 * run writes the samples that are missing, so a seed that failed or was killed halfway is finished
 * later. With no marker, a home holding only sample names is treated the same way (a seed cut
 * short before the marker existed); a home with any other file belongs to the user and is left alone.
 */
internal object SampleContent {
    const val MARKER_NAME = ".home-seed"
    const val STARTED = "started"
    const val DONE = "done"

    private val folders = listOf("Documents", "Music", "Pictures", "ROMs")
    private val files = mapOf("Readme.txt" to "Hemisphere sample file.\n", "notes.md" to "# Notes\n")
    private val sampleNames = folders.toSet() + files.keys

    fun isDone(marker: File): Boolean = readMarker(marker) == DONE

    private fun readMarker(marker: File): String? = if (marker.isFile) marker.readText() else null

    /** True when home needs no more seeding; false when a write failed and the next run should retry. */
    fun seed(
        home: File,
        marker: File,
    ): Boolean =
        try {
            home.mkdirs()
            val children = home.list() ?: return false // home could not be made: try again later
            when {
                isDone(marker) -> {}

                // A marker that is empty or unreadable is judged like a missing one, by what home holds.
                readMarker(marker) == STARTED || children.all { it in sampleNames } -> {
                    writeMarker(marker, STARTED)
                    writeMissing(home, marker)
                }

                // Other files and no seed started: the user's own home.
                else -> {
                    writeMarker(marker, DONE)
                }
            }
            isDone(marker)
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

    /** Replaces the marker in one step, so a crash never leaves it empty or half written. */
    private fun writeMarker(
        marker: File,
        text: String,
    ) {
        val temp = File(marker.parentFile, "${marker.name}.tmp")
        FileOutputStream(temp).use { out ->
            out.write(text.toByteArray())
            out.fd.sync() // on disk before the rename, so a power cut cannot leave an empty marker
        }
        Files.move(temp.toPath(), marker.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    /** Never overwrites: an entry that is already there is kept as it is. */
    private fun writeMissing(
        home: File,
        marker: File,
    ) {
        folders
            .map { File(home, it) }
            .filterNot { it.exists() }
            .forEach { if (!it.mkdir()) throw IOException("Could not create ${it.name}") }
        files.forEach { (name, text) ->
            val file = File(home, name)
            if (!file.exists()) file.writeText(text)
        }
        writeMarker(marker, DONE)
    }
}
