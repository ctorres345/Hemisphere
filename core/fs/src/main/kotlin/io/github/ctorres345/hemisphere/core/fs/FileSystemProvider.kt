package io.github.ctorres345.hemisphere.core.fs

import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import kotlinx.coroutines.flow.Flow

/**
 * One storage backend (architecture §3, ADR 0006). The walking skeleton needs only [list]; `stat`,
 * `openRead`, `openWrite` and the rest are added by the tickets that use them.
 */
interface FileSystemProvider {
    /**
     * Raw batches of a folder's children, in no set order. The flow ends when the folder is read.
     * A failure is emitted as [FsResult.Err] and ends the flow; it never throws, except
     * `CancellationException`.
     */
    fun list(dir: FsPath): Flow<FsResult<List<Entry>>>
}
