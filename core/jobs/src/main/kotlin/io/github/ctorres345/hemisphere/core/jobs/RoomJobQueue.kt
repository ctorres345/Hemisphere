package io.github.ctorres345.hemisphere.core.jobs

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import io.github.ctorres345.hemisphere.core.jobs.db.StateDatabase
import io.github.ctorres345.hemisphere.core.jobs.db.savedRequest
import io.github.ctorres345.hemisphere.core.jobs.db.toEntity
import io.github.ctorres345.hemisphere.core.jobs.db.toState
import io.github.ctorres345.hemisphere.core.model.EnqueueResult
import io.github.ctorres345.hemisphere.core.model.JobId
import io.github.ctorres345.hemisphere.core.model.JobKind
import io.github.ctorres345.hemisphere.core.model.JobRequest
import io.github.ctorres345.hemisphere.core.model.JobState
import io.github.ctorres345.hemisphere.core.model.JobStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * [JobQueue] over `state.db`. One mutex orders every change: the in-memory list is what
 * [observe] shows, and Room is the saved copy. On start, `Running` rows become `Paused`, saved
 * `Queued` jobs are scheduled again (they never started), and every call waits for that.
 *
 * A pause or cancel records what was asked, then cancels the runner's coroutine; when it ends,
 * its progress is flushed and the status written, and its slot frees for the next job. A job
 * waiting on a conflict keeps its slot, since its runner is still running.
 */
internal class RoomJobQueue(
    private val database: StateDatabase,
    private val runners: Map<JobKind, JobRunner>,
    private val scope: CoroutineScope,
    private val clock: () -> Long,
) : JobQueue {
    private val dao = database.jobs()
    private val lock = Mutex()
    private val states = MutableStateFlow<List<JobState>>(emptyList())
    private val requests = mutableMapOf<JobId, JobRequest>()
    private val running = mutableMapOf<JobId, Job>()
    private val stopAs = mutableMapOf<JobId, JobStatus>()

    @Volatile private var ready: Deferred<Unit> = loadAsync()

    override suspend fun enqueue(request: JobRequest): EnqueueResult =
        locked {
            val saved =
                database.withTransaction {
                    val busy =
                        dao.unfinished(FINISHED).any { row ->
                            row.savedRequest()?.let { PathOverlap.clashes(request, it) } ==
                                true
                        }
                    if (busy) null else dao.insert(request.toEntity())
                }
            if (saved == null) {
                EnqueueResult.InUse
            } else {
                val id = JobId(saved)
                requests[id] = request
                states.update { it + JobState(id, request.kind, JobStatus.Queued) }
                schedule()
                EnqueueResult.Accepted(id)
            }
        }

    override fun observe(): StateFlow<List<JobState>> = states.asStateFlow()

    override suspend fun pause(id: JobId) = stop(id, JobStatus.Paused)

    override suspend fun cancel(id: JobId) = stop(id, JobStatus.Cancelled)

    override suspend fun resume(id: JobId) =
        locked {
            if (states.statusOf(id) == JobStatus.Paused) {
                setStatus(id, JobStatus.Queued)
                schedule()
            }
        }

    override suspend fun clearFinished() =
        locked {
            // By the ids shown as finished, so a job whose final status write failed goes too.
            // Records and conflicts are deleted explicitly, not left to the foreign-key cascade.
            val cleared =
                states.value
                    .filter { it.status.isFinished }
                    .map { it.id }
                    .toSet()
            val ids = cleared.map { it.value }
            // In chunks: SQLite before 3.32 (API 26-29) binds at most 999 variables.
            database.withTransaction {
                ids.chunked(MAX_SQL_VARIABLES).forEach { chunk ->
                    dao.deleteRecordsOf(chunk)
                    dao.deleteConflictsOf(chunk)
                    dao.deleteJobs(chunk)
                }
            }
            states.update { list -> list.filterNot { it.id in cleared } }
            cleared.forEach(requests::remove)
        }

    /**
     * Pauses or cancels: a running job through its coroutine; a queued one, or one left waiting on a
     * conflict by an earlier run, at once; a paused one only to cancel it.
     */
    private suspend fun stop(
        id: JobId,
        status: JobStatus,
    ) = locked {
        val job = running[id]
        val current = states.statusOf(id)
        if (job != null) {
            // A pending cancel is never turned back into a pause.
            if (stopAs[id] != JobStatus.Cancelled) stopAs[id] = status
            job.cancel()
        } else if (current in idleStoppable || (status == JobStatus.Cancelled && current == JobStatus.Paused)) {
            setStatus(id, status)
        }
    }

    /** Runs [block] once the saved jobs are loaded, under the queue's one lock. */
    private suspend fun <T> locked(block: suspend () -> T): T {
        awaitLoaded()
        return lock.withLock { block() }
    }

    private fun loadAsync(): Deferred<Unit> = scope.async { lock.withLock { load() } }

    /** A failed load (a locked or full disk) is rethrown to this caller and tried again by the next. */
    private suspend fun awaitLoaded() {
        val loading = ready
        try {
            loading.await()
        } catch (e: SQLiteException) {
            if (ready === loading) ready = loadAsync()
            throw e
        }
    }

    private suspend fun load() {
        requests.clear()
        dao.pauseRunning()
        states.value =
            dao.all().map { row ->
                val request = row.savedRequest()
                val state = row.toState(request?.kind ?: JobKind.Copy)
                when {
                    request != null -> {
                        state.also { requests[it.id] = request }
                    }

                    state.status.isFinished -> {
                        state
                    }

                    else -> {
                        dao.setStatus(row.id, JobStatus.Failed.name, DAMAGED)
                        state.copy(status = JobStatus.Failed, error = DAMAGED)
                    }
                }
            }
        schedule()
    }

    private suspend fun schedule() {
        fun slot(id: JobId) = requests[id]?.let { Scheduler.slot(id, it) }
        // Again while a pick failed to start: its slot stayed free for a job this pass skipped.
        do {
            val queued = states.value.filter { it.status == JobStatus.Queued }.mapNotNull { slot(it.id) }
            val launched = Scheduler.toStart(queued, running.keys.mapNotNull(::slot)).map { start(it) }
        } while (false in launched)
    }

    /**
     * Starts one job and returns whether its runner launched. A failed write (a full disk) fails
     * that job in memory and never stops the others.
     */
    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun start(id: JobId): Boolean {
        val request = requests.getValue(id)
        val runner = runners[request.kind]
        return try {
            val row = dao.byId(id.value)
            if (runner == null || row == null) {
                setStatus(id, JobStatus.Failed, "No runner for ${request.kind}")
                false
            } else {
                setStatus(id, JobStatus.Running)
                val context =
                    RoomJobContext(id, request, row, database, clock) { progress, item ->
                        states.patch(id) { it.copy(progress = progress, currentItem = item) }
                    }
                // ATOMIC: a pause or cancel that lands before the coroutine is dispatched still runs
                // execute(), so its finish() frees the slot and writes the status.
                running[id] = scope.launch(start = CoroutineStart.ATOMIC) { execute(context, runner) }
                true
            }
        } catch (e: SQLiteException) {
            states.patch(id) { it.copy(status = JobStatus.Failed, error = e.message) }
            false
        }
    }

    /**
     * Runs a job to its end. A runner's crash fails its job instead of the app. A cancellation that
     * no pause or cancel asked for (a runner's own timeout) fails the job too, so it never pauses
     * silently.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun execute(
        context: RoomJobContext,
        runner: JobRunner,
    ) {
        var status: JobStatus? = null
        var error: String? = null
        try {
            runner.run(context)
            status = JobStatus.Done
        } catch (e: CancellationException) {
            // Stopped from outside: finish() reads whether that was a pause or a cancel.
            if (currentCoroutineContext().isActive) {
                status = JobStatus.Failed
                error = e.message ?: STOPPED
            }
        } catch (e: Throwable) {
            // Any Throwable, so a runner's Error (a TODO() stub) fails its job instead of the app.
            status = JobStatus.Failed
            error = e.message ?: e::class.simpleName
        } finally {
            withContext(NonCancellable) { finish(context, status, error) }
        }
    }

    /**
     * Frees the job's slot and writes how it ended: [status], or when it was stopped, what was
     * asked (Paused when nothing was, as when the app shuts down). The in-memory state is updated
     * even when the write fails, so a full disk never leaves a job shown as running.
     */
    private suspend fun finish(
        context: RoomJobContext,
        status: JobStatus?,
        error: String?,
    ) = lock.withLock {
        val id = context.id
        running.remove(id)
        val asked = stopAs.remove(id)
        val ended =
            when {
                status != null -> status
                asked != null -> asked
                else -> JobStatus.Paused
            }
        try {
            context.flush()
            setStatus(id, ended, error)
        } catch (e: SQLiteException) {
            states.patch(id) { it.copy(status = ended, error = error ?: e.message) }
        }
        schedule()
    }

    private suspend fun setStatus(
        id: JobId,
        status: JobStatus,
        error: String? = null,
    ) {
        dao.setStatus(id.value, status.name, error)
        states.patch(id) { it.copy(status = status, error = error) }
    }

    private companion object {
        const val DAMAGED = "This saved job could not be read"
        const val STOPPED = "The job stopped unexpectedly"
        const val MAX_SQL_VARIABLES = 999
        val idleStoppable = setOf(JobStatus.Queued, JobStatus.WaitingForConflict)
        val FINISHED = JobStatus.entries.filter { it.isFinished }.map { it.name }
    }
}

/** Replaces job [id]'s state with [change] of it. */
private fun MutableStateFlow<List<JobState>>.patch(
    id: JobId,
    change: (JobState) -> JobState,
) = update { list -> list.map { if (it.id == id) change(it) else it } }

private fun StateFlow<List<JobState>>.statusOf(id: JobId): JobStatus? = value.firstOrNull { it.id == id }?.status
