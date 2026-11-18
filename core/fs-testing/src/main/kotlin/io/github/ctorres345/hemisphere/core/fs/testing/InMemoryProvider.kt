package io.github.ctorres345.hemisphere.core.fs.testing

import io.github.ctorres345.hemisphere.core.fs.FileSystemProvider
import io.github.ctorres345.hemisphere.core.fs.Sink
import io.github.ctorres345.hemisphere.core.fs.Source
import io.github.ctorres345.hemisphere.core.fs.WriteMode
import io.github.ctorres345.hemisphere.core.model.Capabilities
import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.ProviderRef
import io.github.ctorres345.hemisphere.core.model.VolumeId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant

/**
 * A [FileSystemProvider] held in memory, for use-case, ViewModel and job tests (architecture §10).
 * It follows the whole contract, and `FileSystemProviderContract` runs against it. It serves any
 * volume: each one starts as an empty root folder.
 *
 * - **Identity**: name-addressed by default; with [idAddressed] every entry gets a [ProviderRef]
 *   (or keeps the one it was seeded with), paths with a ref are found by it, and a rename or move
 *   keeps it. Names stay unique in both modes, so one contract covers both.
 * - **Case**: with [caseInsensitive] two names that differ only by case collide, using the
 *   `EntryKey` folding rules from `core:model`. Build paths with
 *   `FsPath.root(volume, caseInsensitive, idAddressed)` so they key the same way.
 * - **Capabilities**: everything on; assign [capabilities] to turn one off and test its fallback.
 * - **Faults**: [failNext] makes the next call of a kind return an error value, [stallNext] holds
 *   it until released. Neither throws. [failNextStream] makes the next opened stream throw
 *   `IOException` on read, write or close, as a real stream does mid-copy.
 * - **Space**: [setCapacity] gives a volume a size; [freeSpace] reports what is left, and
 *   `openWrite` or `copy` returns `NoSpace` when a size hint or a copy does not fit.
 *
 * Seed with [addFolder], [putFile] and read back with [bytesOf]. [batchSize] splits a listing into
 * batches, and [failNext] with a path makes a listing fail after some batches, to test errors part-way.
 */
class InMemoryProvider(
    private val batchSize: Int = Int.MAX_VALUE,
    private val idAddressed: Boolean = false,
    private val caseInsensitive: Boolean = false,
    private val clock: () -> Instant = Instant::now,
) : FileSystemProvider {
    override var capabilities: Capabilities =
        Capabilities(
            idAddressed = idAddressed,
            move = true,
            resume = true,
            serverCopy = true,
            setTimes = true,
            watch = true,
        )

    private val lock = Any()
    private val trees = HashMap<VolumeId, MemoryTree>()

    /** One conflated channel per running `observe`, so a slow watcher never loses its own signal. */
    private val watchers = HashSet<Watcher>()

    /** Folders changed under the lock, sent by [flushChanges] once it is released. */
    private val pendingChanges = ArrayList<MemoryNode>()
    private val listFailures = HashMap<FsPath, Failure>()
    private val streamFaults = ArrayDeque<StreamFault>()
    private val failures = HashMap<FsCall, ArrayDeque<FsError>>()
    private val stalls = HashMap<FsCall, ArrayDeque<Stall>>()
    private val counts = HashMap<FsCall, Int>()

    /** How many times [list] was called, to check retries. */
    val listCalls: Int get() = callCount(FsCall.List)

    /** How many times [call] was made, failed or stalled ones included. */
    fun callCount(call: FsCall): Int = synchronized(lock) { counts[call] ?: 0 }

    /** Makes the next [times] calls of kind [call] return [error]. Not for [FsCall.Observe], which cannot fail. */
    fun failNext(
        call: FsCall,
        error: FsError,
        times: Int = 1,
    ) {
        require(call != FsCall.Observe) { "observe returns no FsResult, so it cannot fail" }
        synchronized(lock) { repeat(times) { failures.getOrPut(call) { ArrayDeque() }.addLast(error) } }
    }

    /** Makes the next listing of [path] fail with [error] after [failAfterBatches] good batches. */
    fun failNext(
        path: FsPath,
        error: FsError,
        failAfterBatches: Int = 0,
    ) {
        synchronized(lock) { listFailures[path] = Failure(error, failAfterBatches) }
    }

    /**
     * Makes the next stream opened for [fault] throw `IOException`: the next `openRead` source for
     * [StreamFault.Read], the next `openWrite` sink for [StreamFault.Write] and [StreamFault.Close].
     */
    fun failNextStream(fault: StreamFault) {
        synchronized(lock) { streamFaults.addLast(fault) }
    }

    /** Opens with the first queued fault that [matches], and uses it up only when the open succeeds. */
    private fun <T> withStreamFault(
        matches: (StreamFault) -> Boolean,
        open: (StreamFault?) -> FsResult<T>,
    ): FsResult<T> {
        val fault = streamFaults.firstOrNull(matches)
        return open(fault).also { if (it is FsResult.Ok && fault != null) streamFaults.remove(fault) }
    }

    /** Holds the next call of kind [call] until the returned [Stall] is released. */
    fun stallNext(call: FsCall): Stall =
        Stall().also { stall -> synchronized(lock) { stalls.getOrPut(call) { ArrayDeque() }.addLast(stall) } }

    /** Gives [volume] a size in bytes, or `null` for no limit (the default, which [freeSpace] reports as `null`). */
    fun setCapacity(
        volume: VolumeId,
        bytes: Long?,
    ) = synchronized(lock) { tree(volume).capacity = bytes }

    /**
     * Makes [path] a folder, with its missing parents, holding exactly [entries]. A file's size is only
     * what it reports.
     */
    fun addFolder(
        path: FsPath,
        vararg entries: Entry,
    ) = locked { tree(path.volume).seedFolder(path, entries.toList()) }

    /** Makes [path] a file holding [bytes], with its missing parent folders. */
    fun putFile(
        path: FsPath,
        bytes: ByteArray,
        modified: Instant? = null,
    ) = locked { tree(path.volume).seedFile(path, bytes, modified) }

    /** The bytes of the file at [path], or `null` when there is none, for assertions. */
    fun bytesOf(path: FsPath): ByteArray? =
        synchronized(lock) { tree(path.volume).resolve(path)?.takeIf { !it.isFolder }?.data }

    private fun tree(volume: VolumeId): MemoryTree =
        trees.getOrPut(volume) {
            MemoryTree(volume, caseInsensitive, idAddressed, clock, host)
        }

    /** Runs [block] under the lock, then sends the change signals it recorded, outside the lock. */
    private fun <T> locked(block: () -> T): T = synchronized(lock, block).also { flushChanges() }

    private fun flushChanges() {
        val due =
            synchronized(lock) {
                val changed = pendingChanges.toSet().also { pendingChanges.clear() }
                // Resolved again on each change, so a folder created, renamed or replaced later still counts.
                watchers.filter { tree(it.dir.volume).resolve(it.dir) in changed }
            }
        due.forEach { it.signals.trySend(Unit) }
    }

    private val host =
        object : TreeHost {
            override val lock: Any get() = this@InMemoryProvider.lock

            override fun changed(folder: MemoryNode) {
                pendingChanges += folder
            }

            override fun flushChanges() = this@InMemoryProvider.flushChanges()
        }

    /**
     * Counts the call, waits out a stall, then returns an injected error, if any. The suppression is
     * because `Stall.pass()` suspends, which detekt's typed run misses across the internal call.
     */
    @Suppress("RedundantSuspendModifier")
    private suspend fun gate(call: FsCall): FsError? {
        val stall =
            synchronized(lock) {
                counts[call] = (counts[call] ?: 0) + 1
                stalls[call]?.removeFirstOrNull()
            }
        if (stall != null) stall.pass()
        return synchronized(lock) { failures[call]?.removeFirstOrNull() }
    }

    private suspend fun <T> run(
        call: FsCall,
        block: () -> FsResult<T>,
    ): FsResult<T> = gate(call)?.let { FsResult.Err(it) } ?: locked(block)

    private fun unsupportedUnless(allowed: Boolean): FsResult.Err? =
        if (allowed) null else FsResult.Err(FsError.Unsupported)

    override fun list(dir: FsPath): Flow<FsResult<List<Entry>>> =
        flow {
            val injected = gate(FsCall.List)
            // An injected call error wins, and leaves a pending listing failure for the next listing.
            val (listing, failure) =
                synchronized(lock) {
                    tree(dir.volume).children(dir) to (if (injected == null) listFailures.remove(dir) else null)
                }
            when {
                injected != null -> {
                    emit(FsResult.Err(injected))
                }

                listing is FsResult.Err && failure == null -> {
                    emit(listing)
                }

                else -> {
                    val batches = (listing as? FsResult.Ok)?.value.orEmpty().chunked(batchSize)
                    batches.take(failure?.afterBatches ?: batches.size).forEach { emit(FsResult.Ok(it)) }
                    if (failure != null) emit(FsResult.Err(failure.error))
                }
            }
        }

    override suspend fun stat(path: FsPath): FsResult<Entry> = run(FsCall.Stat) { tree(path.volume).stat(path) }

    override suspend fun openRead(
        path: FsPath,
        offset: Long,
    ): FsResult<Source> =
        run(FsCall.OpenRead) {
            withStreamFault({ it == StreamFault.Read }) { fault ->
                tree(path.volume).openRead(path, offset, capabilities.resume, fault)
            }
        }

    override suspend fun openWrite(
        path: FsPath,
        mode: WriteMode,
        sizeHint: Long?,
    ): FsResult<Sink> =
        run(FsCall.OpenWrite) {
            unsupportedUnless(mode != WriteMode.Append || capabilities.resume)
                ?: withStreamFault({ it != StreamFault.Read }) { fault ->
                    tree(path.volume).openWrite(path, mode, sizeHint, fault)
                }
        }

    override suspend fun createDirectory(path: FsPath): FsResult<Entry> =
        run(FsCall.CreateDirectory) { tree(path.volume).createDirectory(path) }

    override suspend fun rename(
        path: FsPath,
        newName: String,
    ): FsResult<Entry> = run(FsCall.Rename) { tree(path.volume).rename(path, newName) }

    override suspend fun move(
        from: FsPath,
        to: FsPath,
    ): FsResult<Entry> =
        run(FsCall.Move) {
            unsupportedUnless(capabilities.move && from.volume == to.volume) ?: tree(from.volume).move(from, to)
        }

    override suspend fun copy(
        from: FsPath,
        to: FsPath,
    ): FsResult<Entry> =
        run(FsCall.Copy) {
            unsupportedUnless(capabilities.serverCopy && from.volume == to.volume) ?: tree(from.volume).copy(from, to)
        }

    override suspend fun setTimes(
        path: FsPath,
        modified: Instant,
    ): FsResult<Unit> =
        run(FsCall.SetTimes) { unsupportedUnless(capabilities.setTimes) ?: tree(path.volume).setTimes(path, modified) }

    override suspend fun freeSpace(volume: VolumeId): FsResult<Long?> =
        run(FsCall.FreeSpace) {
            FsResult.Ok(tree(volume).freeBytes())
        }

    override suspend fun delete(path: FsPath): FsResult<Unit> = run(FsCall.Delete) { tree(path.volume).delete(path) }

    override fun observe(dir: FsPath): Flow<Unit> =
        flow {
            gate(FsCall.Observe)
            val watcher = Watcher(dir)
            synchronized(lock) { watchers += watcher }
            try {
                for (signal in watcher.signals) emit(signal)
            } finally {
                synchronized(lock) { watchers -= watcher }
            }
        }

    /** Identity-compared on purpose: two watchers of one folder each get a signal. */
    private class Watcher(
        val dir: FsPath,
    ) {
        val signals = Channel<Unit>(Channel.CONFLATED)
    }

    private data class Failure(
        val error: FsError,
        val afterBatches: Int,
    )
}

/** A folder entry, for building test listings. */
fun folder(
    name: String,
    ref: ProviderRef? = null,
): Entry = Entry(name, Entry.Kind.Folder, ref = ref)

/** A file entry, for building test listings. */
fun file(
    name: String,
    size: Long = 0,
    ref: ProviderRef? = null,
): Entry = Entry(name, Entry.Kind.File, size = size, ref = ref)
