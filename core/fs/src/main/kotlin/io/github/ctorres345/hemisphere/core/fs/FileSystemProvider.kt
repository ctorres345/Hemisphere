package io.github.ctorres345.hemisphere.core.fs

import io.github.ctorres345.hemisphere.core.model.Capabilities
import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.VolumeId
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * One storage backend (architecture §3, ADR 0006). No Android types appear here: a Konsist test
 * checks it. The shared contract suite (`FileSystemProviderContract` in `core:fs-testing`) pins
 * what every method below promises; the in-memory provider runs it, real ones will (architecture §2).
 *
 * **Errors are values.** Every call returns [FsResult] (or emits it, for [list]); the only
 * exception that escapes is `CancellationException`, which is always rethrown so Pause and Cancel
 * work. The one exception to the exception rule is the I/O on a [Source] or [Sink] once opened,
 * which throws `IOException` (see [Source]).
 *
 * **Names.** A path names an entry by its [FsPath.segments]; on an `idAddressed` provider the
 * [FsPath.ref] finds it first. A call that gets a name the volume refuses returns `InvalidName`.
 * Two names that differ only by case collide on a case-insensitive volume, by the same
 * `EntryKey` rules `FsPath` uses.
 *
 * **Nothing is overwritten by accident.** [rename], [move], [copy] and `openWrite(Create)` onto a
 * name that is taken return `AlreadyExists` and change nothing: both entries keep their bytes.
 * Only `openWrite(Overwrite)` replaces content, and only the entry it names.
 */
interface FileSystemProvider {
    /** What this provider can do. Callers check this, never the provider's type. */
    val capabilities: Capabilities

    /**
     * Raw batches of a folder's children, in no set order. The flow ends when the folder is read.
     * A failure is emitted as [FsResult.Err] and ends the flow; it never throws, except
     * `CancellationException`. A missing folder is `NotFound`; a file is `Unsupported`.
     */
    fun list(dir: FsPath): Flow<FsResult<List<Entry>>>

    /** The entry at [path], or `NotFound`. The volume root is a folder with an empty name. */
    suspend fun stat(path: FsPath): FsResult<Entry>

    /**
     * The content from byte [offset]. `offset > 0` only when `capabilities.resume`, else
     * `Unsupported`; an offset past the end is an error, not an empty stream. `NotFound` for a
     * missing entry, `Unsupported` for a folder. The caller closes the [Source].
     */
    suspend fun openRead(
        path: FsPath,
        offset: Long = 0,
    ): FsResult<Source>

    /**
     * Opens [path] for writing under [mode]. The parent folder must exist (`NotFound`).
     * [sizeHint] is the number of bytes the caller is about to write, when it knows; a provider
     * that can tell it will not fit returns `NoSpace` now instead of failing half way.
     * `Create` on a taken name is `AlreadyExists`. The caller closes the [Sink]; see its contract
     * for the sync on `close()`.
     */
    suspend fun openWrite(
        path: FsPath,
        mode: WriteMode,
        sizeHint: Long? = null,
    ): FsResult<Sink>

    /** Makes one folder; the parent must exist. `AlreadyExists` if the name is taken by anything. */
    suspend fun createDirectory(path: FsPath): FsResult<Entry>

    /**
     * Gives the entry a new name in the same folder, keeping its content (and, on `idAddressed`
     * volumes, its ref). `AlreadyExists` when another entry has that name, or one that folds to it
     * on a case-insensitive volume; the entry itself never collides, so a case-only rename is allowed.
     */
    suspend fun rename(
        path: FsPath,
        newName: String,
    ): FsResult<Entry>

    /**
     * Moves an entry to [to] (the full new path, new name included) on the same volume, only when
     * `capabilities.move`, else `Unsupported`. A folder moves with everything in it. `AlreadyExists`
     * when [to] is taken; moving a folder into itself is `InvalidName`. Across volumes: `Unsupported`.
     *
     * SAF note: `DocumentsContract.moveDocument` takes a target folder and keeps the name, so a SAF
     * provider moves into `to`'s parent and then renames, and a provider may pick another name if a
     * clash appears between the two steps. The returned [Entry] carries the name it really got.
     */
    suspend fun move(
        from: FsPath,
        to: FsPath,
    ): FsResult<Entry>

    /**
     * Copies one file to [to] on the same volume without passing its bytes through the app, only
     * when `capabilities.serverCopy`, else `Unsupported`. A folder is `Unsupported` (the job walks
     * folders). `AlreadyExists` when [to] is taken. Across volumes: `Unsupported`.
     *
     * SAF note: `DocumentsContract.copyDocument` also takes a target folder, and a document provider
     * may add a suffix such as `" (1)"` instead of failing. A SAF provider checks the name first and
     * renames the copy after; callers read the name from the returned [Entry], never from [to].
     */
    suspend fun copy(
        from: FsPath,
        to: FsPath,
    ): FsResult<Entry>

    /** Sets the modified time, only when `capabilities.setTimes`, else `Unsupported`. */
    suspend fun setTimes(
        path: FsPath,
        modified: Instant,
    ): FsResult<Unit>

    /** Free bytes on [volume], or `Ok(null)` when the backend cannot tell. */
    suspend fun freeSpace(volume: VolumeId): FsResult<Long?>

    /**
     * Deletes the entry for good (Trash is `core:jobs`, not here). A folder goes with everything in
     * it, so a caller that wants per-item progress or errors walks the folder itself first.
     * `NotFound` when it is already gone.
     */
    suspend fun delete(path: FsPath): FsResult<Unit>

    /**
     * A signal that something directly in [dir] changed; the caller relists. May emit nothing on a
     * backend that cannot watch (`capabilities.watch` is false). It cannot carry an error: a
     * backend that fails to watch just ends or stays silent.
     */
    fun observe(dir: FsPath): Flow<Unit>
}
