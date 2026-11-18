package io.github.ctorres345.hemisphere.core.testing

import io.github.ctorres345.hemisphere.core.jobs.JobQueue
import io.github.ctorres345.hemisphere.core.model.EnqueueResult
import io.github.ctorres345.hemisphere.core.model.JobId
import io.github.ctorres345.hemisphere.core.model.JobRequest
import io.github.ctorres345.hemisphere.core.model.JobState
import io.github.ctorres345.hemisphere.core.model.JobStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * A queue that runs nothing: enqueued jobs stay `Queued` until a test moves them with [setState].
 * Set [refuseNext] to answer the next enqueue with "In use by a job".
 */
class FakeJobQueue : JobQueue {
    private val states = MutableStateFlow<List<JobState>>(emptyList())
    private var nextId = 1L

    /** Every request this queue accepted, oldest first. */
    val enqueued = mutableListOf<JobRequest>()

    var refuseNext = false

    override suspend fun enqueue(request: JobRequest): EnqueueResult {
        if (refuseNext) {
            refuseNext = false
            return EnqueueResult.InUse
        }
        val id = JobId(nextId++)
        enqueued += request
        states.update { it + JobState(id, request.kind, JobStatus.Queued) }
        return EnqueueResult.Accepted(id)
    }

    override fun observe(): StateFlow<List<JobState>> = states.asStateFlow()

    override suspend fun pause(id: JobId) =
        move(id, JobStatus.Paused) {
            it == JobStatus.Queued ||
                it == JobStatus.Running ||
                it == JobStatus.WaitingForConflict
        }

    override suspend fun resume(id: JobId) = move(id, JobStatus.Queued) { it == JobStatus.Paused }

    override suspend fun cancel(id: JobId) = move(id, JobStatus.Cancelled) { !it.isFinished }

    override suspend fun clearFinished() = states.update { list -> list.filterNot { it.status.isFinished } }

    /** Replaces the state of job [state].id, as a runner would. */
    fun setState(state: JobState) = states.update { list -> list.map { if (it.id == state.id) state else it } }

    private fun move(
        id: JobId,
        to: JobStatus,
        allowed: (JobStatus) -> Boolean,
    ) = states.update { list -> list.map { if (it.id == id && allowed(it.status)) it.copy(status = to) else it } }
}
