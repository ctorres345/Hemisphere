package io.github.ctorres345.hemisphere.core.jobs

import androidx.room.Room
import io.github.ctorres345.hemisphere.core.jobs.db.JobEntity
import io.github.ctorres345.hemisphere.core.jobs.db.StateDatabase
import io.github.ctorres345.hemisphere.core.jobs.db.toEntity
import io.github.ctorres345.hemisphere.core.model.EnqueueResult
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.JobId
import io.github.ctorres345.hemisphere.core.model.JobKind
import io.github.ctorres345.hemisphere.core.model.JobRequest
import io.github.ctorres345.hemisphere.core.model.JobStatus
import io.github.ctorres345.hemisphere.core.model.VolumeId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.ConcurrentHashMap

/** Builds queues over one in-memory `state.db`, with runners that wait for the test to release them. */
internal class JobsTestKit {
    val database: StateDatabase =
        Room
            .inMemoryDatabaseBuilder(
                RuntimeEnvironment.getApplication(),
                StateDatabase::class.java,
            ).build()
    val runner = GateRunner()
    private val scopes = mutableListOf<CoroutineScope>()

    fun queue(
        runners: Map<JobKind, JobRunner> = JobKind.entries.associateWith { runner },
        clock: () -> Long = { 0L },
    ): RoomJobQueue {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also(scopes::add)
        return RoomJobQueue(database, runners, scope, clock)
    }

    /** Saves a row as an earlier run of the app left it. */
    suspend fun saved(
        request: JobRequest,
        status: JobStatus,
    ): JobId = JobId(database.jobs().insert(request.toEntity().copy(status = status.name)))

    suspend fun row(id: JobId): JobEntity = checkNotNull(database.jobs().byId(id.value))

    fun close() {
        scopes.forEach { it.cancel() }
    }
}

/** A runner that reports each start and then waits until [release] (or forever). */
internal class GateRunner : JobRunner {
    val started = Channel<JobId>(Channel.UNLIMITED)
    private val gates = ConcurrentHashMap<JobId, CompletableDeferred<Unit>>()

    override suspend fun run(job: JobContext) {
        started.send(job.id)
        gate(job.id).await()
    }

    fun release(id: JobId) {
        gate(id).complete(Unit)
    }

    suspend fun awaitStart(): JobId = withTimeout(TIMEOUT_MS) { started.receive() }

    /** Whether no job starts within a short wait. */
    suspend fun noStart(): Boolean = withTimeoutOrNull(QUIET_MS) { started.receive() } == null

    private fun gate(id: JobId) = gates.getOrPut(id) { CompletableDeferred() }
}

internal suspend fun JobQueue.awaitStatus(
    id: JobId,
    status: JobStatus,
) = withTimeout(TIMEOUT_MS) { observe().first { list -> list.any { it.id == id && it.status == status } } }

internal fun JobQueue.statusOf(id: JobId): JobStatus? = observe().value.firstOrNull { it.id == id }?.status

internal fun EnqueueResult.id(): JobId =
    when (this) {
        is EnqueueResult.Accepted -> id
        EnqueueResult.InUse -> throw AssertionError("expected Accepted, was InUse")
    }

internal fun path(
    volume: String,
    vararg segments: String,
    caseInsensitive: Boolean = false,
) = FsPath(VolumeId(volume), segments.toList(), caseInsensitive = caseInsensitive)

internal fun copy(
    vararg roots: FsPath,
    to: FsPath,
) = JobRequest(JobKind.Copy, roots.toList(), to)

internal fun delete(vararg roots: FsPath) = JobRequest(JobKind.Delete, roots.toList())

private const val TIMEOUT_MS = 5_000L
private const val QUIET_MS = 300L

/** Polls [check] until it holds, for state that has no flow to wait on. */
internal suspend fun JobsTestKit.waitFor(check: suspend () -> Boolean) =
    withTimeout(TIMEOUT_MS) {
        while (!check()) delay(POLL_MS)
    }

private const val POLL_MS = 20L
