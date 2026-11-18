package io.github.ctorres345.hemisphere.core.model

/** A job's id in the queue. Ids grow with each enqueue, so they also give the queue's order. */
@JvmInline
value class JobId(
    val value: Long,
)

/**
 * What a job does (architecture §4). Short jobs run at once and never wait behind a long job;
 * long jobs share the per-volume limit.
 */
enum class JobKind(
    val isShort: Boolean,
) {
    Copy(isShort = false),
    Move(isShort = false),
    Trash(isShort = false),
    Delete(isShort = false),
    Restore(isShort = false),
    Rename(isShort = true),
    CreateFolder(isShort = true),
}

/** `Queued → Running → (Paused | WaitingForConflict) → Done | Failed | Cancelled` (architecture §4). */
enum class JobStatus {
    Queued,
    Running,
    Paused,
    WaitingForConflict,
    Done,
    Failed,
    Cancelled,
    ;

    /** Done, Failed and Cancelled: the job no longer claims its paths. */
    val isFinished: Boolean get() = this == Done || this == Failed || this == Cancelled
}

/** Items and bytes done; a total is `null` until the counting pass knows it. */
data class JobProgress(
    val itemsDone: Long = 0,
    val itemsTotal: Long? = null,
    val bytesDone: Long = 0,
    val bytesTotal: Long? = null,
)

/**
 * What a job asks for. [roots] are the entries the job reads or changes; [destination] is the
 * folder it writes into, if any. [options] is the runner's own saved text (conflict choices, a new
 * name), opaque to the queue. [newName] is a rename's new name, which the queue claims next to
 * each root.
 */
data class JobRequest(
    val kind: JobKind,
    val roots: List<FsPath>,
    val destination: FsPath? = null,
    val options: String = "",
    val newName: String? = null,
) {
    init {
        require(roots.isNotEmpty()) { "A job needs at least one root" }
    }
}

/** One job as the progress chip and the queue sheet show it. [error] is the message of a failed or paused job. */
data class JobState(
    val id: JobId,
    val kind: JobKind,
    val status: JobStatus,
    val progress: JobProgress = JobProgress(),
    val currentItem: FsPath? = null,
    val error: String? = null,
)

/** The answer to an enqueue. [InUse] means a job that is not finished claims an overlapping path; nothing is stored. */
sealed interface EnqueueResult {
    data class Accepted(
        val id: JobId,
    ) : EnqueueResult

    data object InUse : EnqueueResult
}
