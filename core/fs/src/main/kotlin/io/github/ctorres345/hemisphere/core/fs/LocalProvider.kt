package io.github.ctorres345.hemisphere.core.fs

import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.VolumeId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.IOException
import java.nio.file.AccessDeniedException
import java.nio.file.DirectoryIteratorException
import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.LinkOption
import java.nio.file.NoSuchFileException
import java.nio.file.NotDirectoryException
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant

/** Reads one entry's attributes, following a link to its target or not. A test seam for [LocalProvider]. */
internal fun interface AttributeReader {
    fun read(
        path: Path,
        followLinks: Boolean,
    ): BasicFileAttributes
}

/** Reads attributes from the file system. */
internal object DefaultAttributeReader : AttributeReader {
    override fun read(
        path: Path,
        followLinks: Boolean,
    ): BasicFileAttributes {
        val type = BasicFileAttributes::class.java
        return if (followLinks) {
            Files.readAttributes(
                path,
                type,
            )
        } else {
            Files.readAttributes(path, type, LinkOption.NOFOLLOW_LINKS)
        }
    }
}

/**
 * Direct file access by path, for volumes the app can read by path (architecture §3). Each volume
 * maps to a root folder; paths never leave it.
 *
 * A folder is read lazily: entries are emitted in batches of [batchSize] as they are read, so the
 * first rows of a big folder show before the rest is read. Each entry's attributes are read once
 * (a link twice: itself, then its target). An entry deleted while the folder is read is skipped;
 * one whose attributes cannot be read is listed without size or date.
 */
internal class LocalProvider(
    private val roots: Map<VolumeId, File>,
    private val ioDispatcher: CoroutineDispatcher,
    private val batchSize: Int = DEFAULT_BATCH_SIZE,
    private val attributes: AttributeReader = DefaultAttributeReader,
) : FileSystemProvider {
    init {
        require(batchSize > 0) { "batchSize must be positive" }
    }

    override fun list(dir: FsPath): Flow<FsResult<List<Entry>>> =
        flow {
            when (val folder = resolve(dir).andThen(::open)) {
                is FsResult.Err -> {
                    emit(folder)
                }

                is FsResult.Ok -> {
                    val stream = folder.value
                    try {
                        emitBatches(stream)
                    } finally {
                        // Also when the collector stops early, such as on leaving the folder.
                        catching { stream.close() }
                    }
                }
            }
        }.flowOn(ioDispatcher)

    /** An error part-way emits the entries read so far, then the error, and ends the listing. */
    private suspend fun FlowCollector<FsResult<List<Entry>>>.emitBatches(stream: DirectoryStream<Path>) {
        val children = stream.iterator()
        var batch = ArrayList<Entry>(batchSize)
        var next = children.nextChild()
        while (true) {
            val path = (next as? FsResult.Ok)?.value ?: break
            currentCoroutineContext().ensureActive() // blocking reads do not check for cancellation
            toEntry(path)?.let { batch += it }
            if (batch.size == batchSize) {
                emit(FsResult.Ok(batch))
                batch = ArrayList(batchSize)
            }
            next = children.nextChild()
        }
        if (batch.isNotEmpty()) emit(FsResult.Ok(batch))
        if (next is FsResult.Err) emit(next)
    }

    private fun resolve(path: FsPath): FsResult<Path> {
        val root = roots[path.volume]
        return when {
            root == null -> FsResult.Err(FsError.VolumeUnavailable)
            path.segments.any(::isUnsafeSegment) -> FsResult.Err(FsError.InvalidName)
            else -> FsResult.Ok(path.segments.fold(root) { parent, name -> File(parent, name) }.toPath())
        }
    }

    /** A segment that would name another folder than the one asked for, or escape the volume root. */
    private fun isUnsafeSegment(segment: String): Boolean =
        segment in UNSAFE_SEGMENTS || '/' in segment || '\u0000' in segment

    private fun open(folder: Path): FsResult<DirectoryStream<Path>> = catching { Files.newDirectoryStream(folder) }

    /** The next child, null at the end of the folder, or the error that stopped the read. */
    private fun Iterator<Path>.nextChild(): FsResult<Path?> = catching { if (hasNext()) next() else null }

    /** Null when the entry was deleted after the folder listed it. */
    private fun toEntry(path: Path): Entry? {
        val name = path.fileName.toString()
        val attributes =
            when (val read = catching { readFollowingLinks(path) }) {
                is FsResult.Ok -> {
                    read.value
                }

                is FsResult.Err -> {
                    if (read.error == FsError.NotFound) return null
                    null // unreadable: listed with what is known
                }
            }
        val isFolder = attributes?.isDirectory == true
        return Entry(
            name = name,
            kind = if (isFolder) Entry.Kind.Folder else Entry.Kind.File,
            size = if (isFolder) null else attributes?.size(),
            modified =
                attributes
                    ?.lastModifiedTime()
                    ?.toMillis()
                    ?.takeIf { it > 0 }
                    ?.let { Instant.ofEpochMilli(it) },
        )
    }

    /** The link's target, or the link itself when its target is gone. */
    private fun readFollowingLinks(path: Path): BasicFileAttributes {
        val own = attributes.read(path, followLinks = false)
        if (!own.isSymbolicLink) return own
        return try {
            attributes.read(path, followLinks = true)
        } catch (
            @Suppress("SwallowedException") e: IOException,
        ) {
            own
        }
    }

    private companion object {
        const val DEFAULT_BATCH_SIZE = 500
        val UNSAFE_SEGMENTS = setOf("", ".", "..")
    }
}

private inline fun <T, R> FsResult<T>.andThen(next: (T) -> FsResult<R>): FsResult<R> =
    when (this) {
        is FsResult.Err -> this
        is FsResult.Ok -> next(value)
    }

/** Runs [block] and turns the I/O failures it throws into an [FsError]; no exception leaves `core:fs`. */
@Suppress("SwallowedException") // the type is the whole message
private inline fun <T> catching(block: () -> T): FsResult<T> =
    try {
        FsResult.Ok(block())
    } catch (e: InvalidPathException) {
        FsResult.Err(FsError.InvalidName)
    } catch (e: DirectoryIteratorException) {
        FsResult.Err(e.cause.toFsError())
    } catch (e: IOException) {
        FsResult.Err(e.toFsError())
    } catch (e: SecurityException) {
        FsResult.Err(FsError.AccessDenied)
    }

private fun IOException?.toFsError(): FsError =
    when (this) {
        is NoSuchFileException -> FsError.NotFound
        is NotDirectoryException -> FsError.Unsupported
        is AccessDeniedException -> FsError.AccessDenied
        else -> FsError.Io(this?.message)
    }
