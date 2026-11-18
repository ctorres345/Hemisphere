package io.github.ctorres345.hemisphere.core.fs.testing

import io.github.ctorres345.hemisphere.core.fs.Sink
import io.github.ctorres345.hemisphere.core.fs.Source
import io.github.ctorres345.hemisphere.core.fs.WriteMode
import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.EntryKey
import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.ProviderRef
import io.github.ctorres345.hemisphere.core.model.VolumeId
import io.github.ctorres345.hemisphere.core.model.VolumeLimits
import io.github.ctorres345.hemisphere.core.model.andThen
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FilterInputStream
import java.io.IOException
import java.time.Instant

/** One entry of a [MemoryTree]. Only touched while the provider holds its lock. */
internal class MemoryNode(
    var name: String,
    val kind: Entry.Kind,
    var parent: MemoryNode?,
    var ref: ProviderRef?,
) {
    var data = ByteArray(0)
    var declaredSize: Long? = null
    var modified: Instant? = null
    var attached = true
    val children = LinkedHashMap<String, MemoryNode>()
    val isFolder get() = kind == Entry.Kind.Folder
    val size: Long get() = declaredSize ?: data.size.toLong()

    fun descendants(): Sequence<MemoryNode> =
        sequenceOf(this) + children.values.asSequence().flatMap { it.descendants() }
}

/**
 * What a [MemoryTree] needs from its provider: the one [lock] for every volume, [changed] to record
 * a changed folder while holding it, and [flushChanges] to send what was recorded once it is free.
 */
internal interface TreeHost {
    val lock: Any

    fun changed(folder: MemoryNode)

    fun flushChanges()
}

/** A parent folder and the new name to give inside it. */
private class Slot(
    val parent: MemoryNode,
    val name: String,
)

/**
 * The folders and files of one volume of an [InMemoryProvider]. Names are folded with the same
 * `EntryKey` rules `FsPath` uses; a [ProviderRef] is a lookup key only when [idAddressed].
 * Not thread-safe: the provider calls it under [TreeHost.lock], and so does a sink when it closes.
 */
internal class MemoryTree(
    private val volume: VolumeId,
    private val caseInsensitive: Boolean,
    private val idAddressed: Boolean,
    private val clock: () -> Instant,
    private val host: TreeHost,
) {
    private val lock get() = host.lock

    private fun changed(node: MemoryNode) = host.changed(node)

    private fun flush() = host.flushChanges()

    val root = MemoryNode("", Entry.Kind.Folder, null, null)
    var capacity: Long? = null
    private val byRef = HashMap<String, MemoryNode>()
    private var lastId = 0

    fun freeBytes(): Long? = capacity?.let { (it - used()).coerceAtLeast(0) }

    private fun used(): Long = root.descendants().filterNot { it.isFolder }.sumOf { it.size }

    fun entryOf(node: MemoryNode): Entry =
        Entry(
            name = node.name,
            kind = node.kind,
            size = if (node.isFolder) null else node.size,
            modified = node.modified,
            ref = node.ref,
        )

    private fun fold(name: String): String =
        (EntryKey.of(volume, listOf(name), caseInsensitive) as EntryKey.ByPath).segments.single()

    private fun walk(segments: List<String>): MemoryNode? =
        segments.fold<String, MemoryNode?>(root) { node, name -> node?.children?.get(fold(name)) }

    /** By ref first on an id-addressed volume (a stale ref is then `NotFound`), else by name. */
    fun resolve(path: FsPath): MemoryNode? {
        val ref = path.ref
        return if (idAddressed && ref != null) byRef[ref.value]?.takeIf { it.attached } else walk(path.segments)
    }

    private fun found(path: FsPath): FsResult<MemoryNode> =
        resolve(path)?.let { FsResult.Ok(it) } ?: FsResult.Err(FsError.NotFound)

    private fun slotFor(path: FsPath): FsResult<Slot> {
        val name = path.name
        val error = if (name == null) FsError.InvalidName else VolumeLimits.NONE.check(name)
        val parent = walk(path.segments.dropLast(1))
        return when {
            error != null -> FsResult.Err(error)
            parent == null -> FsResult.Err(FsError.NotFound)
            !parent.isFolder -> FsResult.Err(FsError.Unsupported)
            else -> FsResult.Ok(Slot(parent, name.orEmpty()))
        }
    }

    private fun attach(
        parent: MemoryNode,
        node: MemoryNode,
        allowDuplicate: Boolean = false,
    ) {
        val key = fold(node.name)
        // A seeded listing may hold two entries with one name, as a cloud drive does. Only the first is found by name.
        parent.children[if (allowDuplicate && key in parent.children) "$key\u0000${parent.children.size}" else key] =
            node
        node.parent = parent
        changed(parent)
    }

    private fun create(
        slot: Slot,
        kind: Entry.Kind,
        given: ProviderRef? = null,
        allowDuplicate: Boolean = false,
    ): MemoryNode {
        val ref = given ?: if (idAddressed) ProviderRef("${volume.value}#${++lastId}") else null
        val node = MemoryNode(slot.name, kind, slot.parent, ref)
        node.modified = clock()
        if (idAddressed) ref?.let { byRef[it.value] = node }
        attach(slot.parent, node, allowDuplicate)
        return node
    }

    private fun detach(node: MemoryNode) {
        val parent = node.parent ?: return
        parent.children.values.remove(node)
        node.parent = null
        changed(parent)
    }

    private fun forget(node: MemoryNode) {
        node.descendants().forEach {
            it.attached = false
            it.ref?.let { ref -> byRef.remove(ref.value) }
        }
    }

    fun children(path: FsPath): FsResult<List<Entry>> =
        found(path).andThen { node ->
            if (node.isFolder) FsResult.Ok(node.children.values.map(::entryOf)) else FsResult.Err(FsError.Unsupported)
        }

    fun stat(path: FsPath): FsResult<Entry> = found(path).map(::entryOf)

    /** [fault] set to [StreamFault.Read] makes every read of the returned stream throw. */
    fun openRead(
        path: FsPath,
        offset: Long,
        canResume: Boolean,
        fault: StreamFault? = null,
    ): FsResult<Source> =
        found(path).andThen { node ->
            when {
                node.isFolder || (offset > 0 && !canResume) -> {
                    FsResult.Err(FsError.Unsupported)
                }

                offset < 0 || offset > node.data.size -> {
                    FsResult.Err(FsError.Io("offset $offset is outside the file"))
                }

                else -> {
                    val bytes = ByteArrayInputStream(node.data, offset.toInt(), node.data.size - offset.toInt())
                    FsResult.Ok(if (fault == StreamFault.Read) FailingSource(bytes) else bytes)
                }
            }
        }

    /**
     * On an id-addressed volume a path with a ref writes to the entry the ref names, wherever it now
     * is; a stale ref is `NotFound` for `Overwrite` and `Append`, so nothing else is overwritten by
     * accident, while `Create` goes by name. Without a ref, or on a name-addressed volume, the name
     * decides. `Overwrite` empties an existing file here, at open, as `O_TRUNC` does on a real
     * volume. For `Append`, [sizeHint] is the number of bytes to add.
     */
    fun openWrite(
        path: FsPath,
        mode: WriteMode,
        sizeHint: Long?,
        fault: StreamFault? = null,
    ): FsResult<Sink> =
        if (idAddressed && path.ref != null && mode != WriteMode.Create) {
            found(path).andThen { node -> openExisting(node, mode, sizeHint, fault) }
        } else {
            slotFor(path).andThen { slot -> openSlot(slot, mode, sizeHint, fault) }
        }

    private fun openExisting(
        node: MemoryNode,
        mode: WriteMode,
        sizeHint: Long?,
        fault: StreamFault?,
    ): FsResult<Sink> =
        when (val error = writeError(node, mode, sizeHint)) {
            null -> {
                if (mode == WriteMode.Overwrite) truncate(node)
                val base = if (mode == WriteMode.Append) node.data else ByteArray(0)
                FsResult.Ok(MemorySink(node, base, fault))
            }

            else -> {
                FsResult.Err(error)
            }
        }

    private fun openSlot(
        slot: Slot,
        mode: WriteMode,
        sizeHint: Long?,
        fault: StreamFault?,
    ): FsResult<Sink> {
        val existing = slot.parent.children[fold(slot.name)]
        return when {
            existing != null -> {
                openExisting(existing, mode, sizeHint, fault)
            }

            else -> {
                writeError(null, mode, sizeHint)?.let { FsResult.Err(it) }
                    ?: FsResult.Ok(MemorySink(create(slot, Entry.Kind.File), ByteArray(0), fault))
            }
        }
    }

    private fun writeError(
        existing: MemoryNode?,
        mode: WriteMode,
        sizeHint: Long?,
    ): FsError? =
        when {
            existing != null && existing.isFolder -> FsError.Unsupported
            mode == WriteMode.Create && existing != null -> FsError.AlreadyExists
            mode == WriteMode.Append && existing == null -> FsError.NotFound
            !fits(sizeHint, existing.takeIf { mode == WriteMode.Overwrite }) -> FsError.NoSpace
            else -> null
        }

    private fun truncate(node: MemoryNode) {
        node.data = ByteArray(0)
        node.declaredSize = null
        node.modified = clock()
        changed(node.parent ?: node)
    }

    /** Whether [sizeHint] more bytes fit, counting [replaced]'s bytes as free since they go away. */
    private fun fits(
        sizeHint: Long?,
        replaced: MemoryNode?,
    ): Boolean {
        val free = freeBytes() ?: return true
        return sizeHint == null || sizeHint <= free + (replaced?.size ?: 0)
    }

    fun createDirectory(path: FsPath): FsResult<Entry> =
        slotFor(path).andThen { slot ->
            if (slot.parent.children.containsKey(fold(slot.name))) {
                FsResult.Err(FsError.AlreadyExists)
            } else {
                FsResult.Ok(entryOf(create(slot, Entry.Kind.Folder)))
            }
        }

    fun rename(
        path: FsPath,
        newName: String,
    ): FsResult<Entry> =
        found(path).andThen { node ->
            val parent = node.parent
            val error = VolumeLimits.NONE.check(newName)
            val clash = parent?.children?.get(fold(newName))
            when {
                parent == null || error != null -> {
                    FsResult.Err(error ?: FsError.InvalidName)
                }

                clash != null && clash !== node -> {
                    FsResult.Err(FsError.AlreadyExists)
                }

                else -> {
                    detach(node)
                    node.name = newName
                    attach(parent, node)
                    FsResult.Ok(entryOf(node))
                }
            }
        }

    fun move(
        from: FsPath,
        to: FsPath,
    ): FsResult<Entry> =
        found(from).andThen { node ->
            slotFor(to).andThen { slot ->
                val clash = slot.parent.children[fold(slot.name)]
                when {
                    node.parent == null || slot.parent in node.descendants() -> {
                        FsResult.Err(FsError.InvalidName)
                    }

                    clash != null && clash !== node -> {
                        FsResult.Err(FsError.AlreadyExists)
                    }

                    else -> {
                        detach(node)
                        node.name = slot.name
                        attach(slot.parent, node)
                        FsResult.Ok(entryOf(node))
                    }
                }
            }
        }

    fun copy(
        from: FsPath,
        to: FsPath,
    ): FsResult<Entry> =
        found(from).andThen { source ->
            slotFor(to).andThen { slot ->
                when {
                    source.isFolder -> {
                        FsResult.Err(FsError.Unsupported)
                    }

                    slot.parent.children.containsKey(fold(slot.name)) -> {
                        FsResult.Err(FsError.AlreadyExists)
                    }

                    !fits(source.size, null) -> {
                        FsResult.Err(FsError.NoSpace)
                    }

                    else -> {
                        FsResult.Ok(
                            entryOf(
                                create(slot, Entry.Kind.File).also {
                                    it.data = source.data
                                    it.declaredSize = source.declaredSize
                                },
                            ),
                        )
                    }
                }
            }
        }

    fun setTimes(
        path: FsPath,
        modified: Instant,
    ): FsResult<Unit> =
        found(path).map {
            it.modified = modified
            changed(it.parent ?: it)
        }

    fun delete(path: FsPath): FsResult<Unit> =
        found(path).andThen { node ->
            if (node.parent == null) {
                FsResult.Err(FsError.InvalidName)
            } else {
                detach(node)
                forget(node)
                FsResult.Ok(Unit)
            }
        }

    /** Makes [path] a folder, with any missing parents, and gives it exactly [entries] as children. */
    fun seedFolder(
        path: FsPath,
        entries: List<Entry>,
    ) {
        val folder = ensureFolder(path.segments)
        folder.children.values.toList().forEach {
            detach(it)
            forget(it)
        }
        entries.forEach { entry ->
            val node = create(Slot(folder, entry.name), entry.kind, entry.ref, allowDuplicate = true)
            node.modified = entry.modified
            if (!node.isFolder) node.declaredSize = entry.size ?: 0L
        }
    }

    fun seedFile(
        path: FsPath,
        bytes: ByteArray,
        modified: Instant?,
    ) {
        val parent = ensureFolder(path.segments.dropLast(1))
        val name = path.name.orEmpty()
        parent.children[fold(name)]?.let {
            detach(it)
            forget(it)
        }
        val node = create(Slot(parent, name), Entry.Kind.File)
        node.data = bytes
        node.modified = modified ?: clock()
    }

    private fun ensureFolder(segments: List<String>): MemoryNode =
        segments.fold(root) { parent, name ->
            parent.children[fold(name)] ?: create(Slot(parent, name), Entry.Kind.Folder)
        }

    /**
     * What [MemorySink.close] does: the whole new content appears at once. Throws, changing nothing,
     * when the file was deleted meanwhile or the new content does not fit the volume's capacity.
     */
    private fun commit(
        node: MemoryNode,
        bytes: ByteArray,
    ) {
        if (!node.attached) throw IOException("${node.name} was deleted while it was written")
        val limit = capacity
        if (limit != null &&
            used() - node.size + bytes.size > limit
        ) {
            throw IOException("no space left for ${node.name}")
        }
        node.data = bytes
        node.declaredSize = null
        node.modified = clock()
        changed(node.parent ?: node)
    }

    /**
     * Collects writes and makes them the file's content on `close()`, which is the "synced" point here.
     * [fault] makes the first write, or the close, throw `IOException` as a full disk or a lost
     * connection would; a failed close commits nothing.
     */
    private inner class MemorySink(
        private val node: MemoryNode,
        private val base: ByteArray,
        private val fault: StreamFault?,
    ) : ByteArrayOutputStream() {
        private var closed = false

        override fun write(b: Int) {
            ensureOpen()
            super.write(b)
        }

        override fun write(
            b: ByteArray,
            off: Int,
            len: Int,
        ) {
            ensureOpen()
            super.write(b, off, len)
        }

        private fun ensureOpen() {
            if (closed) throw IOException("sink is closed")
            if (fault == StreamFault.Write) throw IOException("injected write failure")
        }

        override fun close() {
            if (closed) return
            closed = true
            if (fault == StreamFault.Close) throw IOException("injected close failure")
            if (fault == StreamFault.Write) return
            try {
                synchronized(lock) { commit(node, base + toByteArray()) }
            } finally {
                // Send the change signals only now that the lock is free.
                this@MemoryTree.flush()
            }
        }
    }

    /** A source whose reads all throw, for [StreamFault.Read]. */
    private class FailingSource(
        source: Source,
    ) : FilterInputStream(source) {
        override fun read(): Int = throw IOException("injected read failure")

        override fun read(
            b: ByteArray,
            off: Int,
            len: Int,
        ): Int = throw IOException("injected read failure")
    }
}

private inline fun <T, R> FsResult<T>.map(transform: (T) -> R): FsResult<R> = andThen { FsResult.Ok(transform(it)) }
