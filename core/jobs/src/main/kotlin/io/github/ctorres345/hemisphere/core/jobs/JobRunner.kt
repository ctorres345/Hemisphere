package io.github.ctorres345.hemisphere.core.jobs

import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.JobId
import io.github.ctorres345.hemisphere.core.model.JobProgress
import io.github.ctorres345.hemisphere.core.model.JobRequest

/**
 * Runs one kind of job (copy, move, and so on). Later tickets bind one per `JobKind` into the
 * `Map<JobKind, JobRunner>` multibinding with [JobKindKey].
 *
 * Returning means the job is Done; throwing [JobFailedException] means Failed. Pause and cancel
 * arrive as coroutine cancellation: stop at the next chunk boundary and rethrow it. A job is never
 * resumed on its own after process death.
 */
fun interface JobRunner {
    suspend fun run(job: JobContext)
}

/** What a runner sees of its job, and how it saves its place. */
interface JobContext {
    val id: JobId
    val request: JobRequest

    /** Where the last run stopped; the default cursor for a new job. */
    val cursor: JobCursor

    /** Updates the live progress. Saved to `state.db` at most once a second. */
    suspend fun progress(
        progress: JobProgress,
        currentItem: FsPath?,
    )

    /**
     * An item finished: saves [cursor], [progress] and [records] in one transaction, so a crash
     * never leaves finished items without their records.
     */
    suspend fun itemFinished(
        cursor: JobCursor,
        progress: JobProgress,
        records: List<JobRecord> = emptyList(),
    )
}

/**
 * A job's saved place (architecture §4): the relative path of the last finished item in the
 * ordinal depth-first walk, `bytesDone` of the current file, the temp counter that only goes up,
 * and the current temp file's path as the provider returned it.
 */
data class JobCursor(
    val lastItem: String? = null,
    val bytesDone: Long = 0,
    val tempCounter: Long = 0,
    val tempPath: FsPath? = null,
)

/** One row that outlives the job: a failure, a skip, a Replace, or an Undo step. */
data class JobRecord(
    val type: RecordType,
    val path: FsPath,
    val detail: String? = null,
)

enum class RecordType { Failure, Skip, Replace, UndoStep }

/** A runner's way to fail its job with a message for the user. */
class JobFailedException(
    message: String,
) : Exception(message)
