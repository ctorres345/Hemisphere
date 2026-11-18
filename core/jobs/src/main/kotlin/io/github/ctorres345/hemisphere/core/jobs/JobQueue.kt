package io.github.ctorres345.hemisphere.core.jobs

import io.github.ctorres345.hemisphere.core.model.EnqueueResult
import io.github.ctorres345.hemisphere.core.model.JobId
import io.github.ctorres345.hemisphere.core.model.JobRequest
import io.github.ctorres345.hemisphere.core.model.JobState
import kotlinx.coroutines.flow.StateFlow

/**
 * The job queue (architecture §4, ADR 0005). Jobs are saved in `state.db`, so they survive the app
 * being killed: on start a `Running` job becomes `Paused`, and nothing resumes on its own.
 */
interface JobQueue {
    /**
     * Saves and schedules [request], unless a job that is not finished claims an overlapping path
     * (one path is the other or an ancestor of it, compared by `EntryKey`). Then nothing is stored
     * and the answer is [EnqueueResult.InUse] ("In use by a job").
     *
     * Claims: each root is exclusive, and so is each entry a copy or move creates in the
     * destination (`destination/<root name>`). The destination folder itself is shared: two jobs
     * may write into one folder, but no job may change that folder or an ancestor while another
     * writes into it.
     */
    suspend fun enqueue(request: JobRequest): EnqueueResult

    /** Every saved job, oldest first, with live progress. */
    fun observe(): StateFlow<List<JobState>>

    /** Stops a running job at its next chunk boundary, or holds a queued one. */
    suspend fun pause(id: JobId)

    /** Puts a paused job back in the queue. */
    suspend fun resume(id: JobId)

    /** Stops a job for good. A finished job is left as it is. */
    suspend fun cancel(id: JobId)

    /** Removes Done, Failed and Cancelled jobs and their records. Undo rows are kept. */
    suspend fun clearFinished()
}
