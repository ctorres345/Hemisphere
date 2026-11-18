package io.github.ctorres345.hemisphere.core.fs

import io.github.ctorres345.hemisphere.core.model.Capabilities
import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.VolumeId
import io.github.ctorres345.hemisphere.core.model.andThen
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.DirectoryStream
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

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
 * Direct file access by path, for volumes the app can read and write by path (architecture §3).
 * Each volume maps to a root folder; paths never leave it. Names are as the file system has them, so
 * a case-insensitive volume is one whose file system folds case.
 *
 * A folder is read lazily: entries are emitted in batches of [batchSize] as they are read, so the
 * first rows of a big folder show before the rest is read. Each entry's attributes are read once
 * (a link twice: itself, then its target). An entry deleted while the folder is read is skipped;
 * one whose attributes cannot be read is listed without size or date.
 *
 * Blocking work runs on [ioDispatcher]. Nothing is overwritten by accident: `Create` opens with
 * `CREATE_NEW` and moves never pass `REPLACE_EXISTING`, so the check and the act are one step where
 * the platform allows. After a write, rename, move or delete, [mediaScanner] gets the changed paths.
 * There is no server-side copy: [copy] is `Unsupported`.
 */
internal class LocalProvider(
    private val roots: Map<VolumeId, File>,
    private val ioDispatcher: CoroutineDispatcher,
    private val batchSize: Int = DEFAULT_BATCH_SIZE,
    private val attributes: AttributeReader = DefaultAttributeReader,
    private val watcher: DirectoryWatcher = FileObserverWatcher,
    private val mediaScanner: MediaScanner = MediaScanner.None,
) : FileSystemProvider {
    init {
        require(batchSize > 0) { "batchSize must be positive" }
    }

    override val capabilities: Capabilities = Capabilities(move = true, resume = true, setTimes = true, watch = true)

    override suspend fun stat(path: FsPath): FsResult<Entry> =
        io {
            resolve(path).andThen { file -> entryAt(file, path.name.orEmpty()) }
        }

    override suspend fun openRead(
        path: FsPath,
        offset: Long,
    ): FsResult<Source> =
        opening { job ->
            resolve(path).andThen { file ->
                val target = Files.readAttributes(file, BasicFileAttributes::class.java)
                when {
                    target.isDirectory -> {
                        FsResult.Err(FsError.Unsupported)
                    }

                    offset !in 0..target.size() -> {
                        FsResult.Err(FsError.Io("offset $offset is outside 0..${target.size()}"))
                    }

                    else -> {
                        val channel = FileChannel.open(file, StandardOpenOption.READ)
                        FsResult.Ok(LocalSource(channel.position(offset), job))
                    }
                }
            }
        }

    override suspend fun openWrite(
        path: FsPath,
        mode: WriteMode,
        sizeHint: Long?,
    ): FsResult<Sink> =
        opening { job ->
            resolve(path).andThen { file -> openSink(file, mode, sizeHint, job) }
        }

    private fun openSink(
        file: Path,
        mode: WriteMode,
        sizeHint: Long?,
        job: Job?,
    ): FsResult<Sink> {
        val parent = file.parent
        val existing =
            try {
                Files.readAttributes(file, BasicFileAttributes::class.java)
            } catch (
                @Suppress("SwallowedException") e: NoSuchFileException,
            ) {
                null
            }
        val error =
            when {
                parent == null || !Files.isDirectory(parent) -> {
                    FsError.NotFound
                }

                mode == WriteMode.Create && existing != null -> {
                    FsError.AlreadyExists // before the space check: the conflict is what the caller can act on
                }

                existing?.isDirectory == true -> {
                    FsError.Unsupported
                }

                !fits(parent, sizeHint, freed = if (mode == WriteMode.Overwrite) existing?.size() ?: 0 else 0) -> {
                    FsError.NoSpace
                }

                else -> {
                    null
                }
            }
        if (error != null) return FsResult.Err(error)
        val channel = FileChannel.open(file, mode.options)
        return FsResult.Ok(LocalSink(channel, job) { mediaScanner.scan(listOf(file.toString())) })
    }

    override suspend fun createDirectory(path: FsPath): FsResult<Entry> =
        io {
            resolve(path).andThen { file ->
                Files.createDirectory(file)
                FsResult.Ok(Entry(path.name.orEmpty(), Entry.Kind.Folder))
            }
        }

    override suspend fun rename(
        path: FsPath,
        newName: String,
    ): FsResult<Entry> =
        when {
            isUnsafeSegment(newName) -> FsResult.Err(FsError.InvalidName)

            path.segments.isEmpty() -> FsResult.Err(FsError.Unsupported)

            // the volume root keeps its name
            else -> relocate(path, path.parent().child(newName))
        }

    override suspend fun move(
        from: FsPath,
        to: FsPath,
    ): FsResult<Entry> =
        when {
            from.volume != to.volume -> FsResult.Err(FsError.Unsupported)
            from.segments.isEmpty() || to.segments.isEmpty() -> FsResult.Err(FsError.InvalidName)
            else -> relocate(from, to)
        }

    /** Moves [from] to [to] without replacing anything, then reports both paths for a rescan. */
    private suspend fun relocate(
        from: FsPath,
        to: FsPath,
    ): FsResult<Entry> =
        io {
            resolve(from).andThen { source ->
                resolve(to).andThen { target ->
                    relocatePaths(source, target, to.name.orEmpty())
                        .also { if (it is FsResult.Ok) mediaScanner.scan(listOf(source.toString(), target.toString())) }
                }
            }
        }

    private fun relocatePaths(
        source: Path,
        target: Path,
        name: String,
    ): FsResult<Entry> =
        when {
            !Files.exists(source, LinkOption.NOFOLLOW_LINKS) -> {
                notFound()
            }

            isInside(target, source) -> {
                FsResult.Err(FsError.InvalidName)
            }

            else -> {
                moveWithoutReplacing(source, target, attributes)
                entryAt(target, name)
            }
        }

    override suspend fun copy(
        from: FsPath,
        to: FsPath,
    ): FsResult<Entry> = FsResult.Err(FsError.Unsupported)

    override suspend fun setTimes(
        path: FsPath,
        modified: Instant,
    ): FsResult<Unit> =
        io {
            resolve(path).andThen { file ->
                Files.setLastModifiedTime(file, FileTime.from(modified))
                FsResult.Ok(Unit)
            }
        }

    override suspend fun freeSpace(volume: VolumeId): FsResult<Long?> =
        io {
            val root = roots[volume] ?: return@io FsResult.Err(FsError.VolumeUnavailable)
            FsResult.Ok(usableBytes(root))
        }

    override suspend fun delete(path: FsPath): FsResult<Unit> =
        if (path.segments.isEmpty()) {
            FsResult.Err(FsError.Unsupported) // the volume root is not the provider's to delete
        } else {
            val job = currentCoroutineContext()[Job]
            io {
                resolve(path).andThen { file ->
                    try {
                        Files.walkFileTree(file, DeleteVisitor(job))
                    } finally {
                        // Also after a failure part-way: what was deleted must leave MediaStore.
                        mediaScanner.scan(listOf(file.toString()))
                    }
                    FsResult.Ok(Unit)
                }
            }
        }

    /**
     * Deletes children before their folder; never follows a link out of the tree. Stops at the next
     * entry once [job] is cancelled, by throwing `CancellationException`.
     */
    private class DeleteVisitor(
        private val job: Job?,
    ) : SimpleFileVisitor<Path>() {
        override fun visitFile(
            file: Path,
            attrs: BasicFileAttributes,
        ): FileVisitResult {
            job?.ensureActive()
            Files.delete(file)
            return FileVisitResult.CONTINUE
        }

        override fun postVisitDirectory(
            dir: Path,
            exc: IOException?,
        ): FileVisitResult {
            if (exc != null) throw exc
            Files.delete(dir)
            return FileVisitResult.CONTINUE
        }
    }

    override fun observe(dir: FsPath): Flow<Unit> =
        when (val folder = resolve(dir)) {
            is FsResult.Err -> {
                emptyFlow()
            }

            is FsResult.Ok -> {
                watcher
                    .watch(folder.value.toFile())
                    .catch { if (it !is IOException) throw it }
                    .flowOn(ioDispatcher)
            }
        }

    override fun list(dir: FsPath): Flow<FsResult<List<Entry>>> =
        flow {
            when (val folder = resolve(dir).andThen { folder -> catching { Files.newDirectoryStream(folder) } }) {
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
            listedEntry(path)?.let { batch += it }
            if (batch.size == batchSize) {
                emit(FsResult.Ok(batch))
                batch = ArrayList(batchSize)
            }
            next = children.nextChild()
        }
        if (batch.isNotEmpty()) emit(FsResult.Ok(batch))
        if (next is FsResult.Err) emit(next)
    }

    /** Runs [block] on the IO dispatcher; whatever it throws becomes an [FsError]. */
    private suspend fun <T> io(block: () -> FsResult<T>): FsResult<T> =
        withContext(ioDispatcher) { catching(block).flatten() }

    /**
     * Like [io] for a call that opens a stream, bound to the caller's job. If the caller is cancelled
     * while the stream opens, the stream is closed before the cancellation goes on.
     */
    private suspend fun <T : Closeable> opening(block: (Job?) -> FsResult<T>): FsResult<T> {
        val job = currentCoroutineContext()[Job]
        var opened: T? = null
        return try {
            io { block(job).also { opened = (it as? FsResult.Ok)?.value } }
        } catch (e: CancellationException) {
            opened?.close()
            throw e
        }
    }

    private fun resolve(path: FsPath): FsResult<Path> {
        val root = roots[path.volume]
        return when {
            root == null -> FsResult.Err(FsError.VolumeUnavailable)
            path.segments.any(::isUnsafeSegment) -> FsResult.Err(FsError.InvalidName)
            else -> FsResult.Ok(path.segments.fold(root) { parent, name -> File(parent, name) }.toPath())
        }
    }

    /** The next child, null at the end of the folder, or the error that stopped the read. */
    private fun Iterator<Path>.nextChild(): FsResult<Path?> = catching { if (hasNext()) next() else null }

    /** The entry at [path] under [name], or `NotFound`. Throws what reading throws. */
    private fun entryAt(
        path: Path,
        name: String,
    ): FsResult<Entry> = entryOf(path, name)?.let { FsResult.Ok(it) } ?: notFound()

    /** A listed child; null when it was deleted after the folder listed it. */
    private fun listedEntry(path: Path): Entry? =
        when (val read = catching { entryOf(path, path.fileName.toString()) }) {
            is FsResult.Ok -> {
                read.value
            }

            // Unreadable but there: listed with what is known.
            is FsResult.Err -> {
                if (read.error ==
                    FsError.NotFound
                ) {
                    null
                } else {
                    Entry(path.fileName.toString(), Entry.Kind.File)
                }
            }
        }

    /** The entry at [path] under [name], null when nothing is there. Throws what reading throws. */
    private fun entryOf(
        path: Path,
        name: String,
    ): Entry? {
        val attributes =
            try {
                readFollowingLinks(path)
            } catch (
                @Suppress("SwallowedException") e: NoSuchFileException,
            ) {
                return null
            }
        val isFolder = attributes.isDirectory
        return Entry(
            name = name,
            kind = if (isFolder) Entry.Kind.Folder else Entry.Kind.File,
            size = if (isFolder) null else attributes.size(),
            modified =
                attributes
                    .lastModifiedTime()
                    .toMillis()
                    .takeIf { it > 0 }
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
    }
}

private val WriteMode.options: Set<StandardOpenOption>
    get() =
        when (this) {
            WriteMode.Create -> {
                setOf(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
            }

            WriteMode.Overwrite -> {
                setOf(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
            }

            WriteMode.Append -> {
                setOf(StandardOpenOption.WRITE, StandardOpenOption.APPEND)
            }
        }

private fun notFound(): FsResult.Err = FsResult.Err(FsError.NotFound)

private fun <T> FsResult<FsResult<T>>.flatten(): FsResult<T> =
    when (this) {
        is FsResult.Err -> this
        is FsResult.Ok -> value
    }
