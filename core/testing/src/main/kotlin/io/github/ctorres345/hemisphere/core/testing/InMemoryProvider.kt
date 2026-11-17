package io.github.ctorres345.hemisphere.core.testing

import io.github.ctorres345.hemisphere.core.fs.FileSystemProvider
import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.EntryKey
import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A [FileSystemProvider] held in memory, for use-case and ViewModel tests (architecture §10).
 * Folders are added with [addFolder]; [failNext] makes the next listing of a folder fail after
 * [failAfterBatches] good batches, to test errors part-way.
 */
class InMemoryProvider(
    private val batchSize: Int = Int.MAX_VALUE,
) : FileSystemProvider {
    private val folders = mutableMapOf<EntryKey, List<Entry>>()
    private val failures = mutableMapOf<EntryKey, Failure>()

    /** How many times [list] was called, to check retries. */
    var listCalls: Int = 0
        private set

    fun addFolder(
        path: FsPath,
        vararg entries: Entry,
    ) {
        folders[path.key] = entries.toList()
    }

    fun failNext(
        path: FsPath,
        error: FsError,
        failAfterBatches: Int = 0,
    ) {
        failures[path.key] = Failure(error, failAfterBatches)
    }

    override fun list(dir: FsPath): Flow<FsResult<List<Entry>>> =
        flow {
            listCalls++
            val failure = failures.remove(dir.key)
            val entries = folders[dir.key]
            if (entries == null && failure == null) return@flow emit(FsResult.Err(FsError.NotFound))
            val batches = entries.orEmpty().chunked(batchSize)
            batches.take(failure?.afterBatches ?: batches.size).forEach { emit(FsResult.Ok(it)) }
            if (failure != null) emit(FsResult.Err(failure.error))
        }

    private data class Failure(
        val error: FsError,
        val afterBatches: Int,
    )
}

/** A folder entry, for building test listings. */
fun folder(name: String): Entry = Entry(name, Entry.Kind.Folder)

/** A file entry, for building test listings. */
fun file(
    name: String,
    size: Long = 0,
): Entry = Entry(name, Entry.Kind.File, size = size)
