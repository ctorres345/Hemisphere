package io.github.ctorres345.hemisphere.core.jobs

import io.github.ctorres345.hemisphere.core.model.JobStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A queue rebuilt from a saved `state.db`, as after process death (HEM-43). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JobQueueRestartTest {
    private val kit = JobsTestKit()

    @After
    fun tearDown() = kit.close()

    @Test
    fun aRunningJobComesBackPausedAndDoesNotRun() =
        runBlocking<Unit> {
            val id = kit.saved(copy(path("int", "a.iso"), to = path("sd", "ROMs")), JobStatus.Running)
            val queue = kit.queue()
            queue.awaitStatus(id, JobStatus.Paused)
            assertEquals(JobStatus.Paused.name, kit.row(id).status)
            assertTrue(kit.runner.noStart())
            assertEquals(JobStatus.Paused, queue.statusOf(id))
        }

    @Test
    fun aPausedJobRunsAgainOnlyWhenResumed() =
        runBlocking<Unit> {
            val id = kit.saved(delete(path("sd", "ROMs")), JobStatus.Running)
            val queue = kit.queue()
            queue.awaitStatus(id, JobStatus.Paused)
            queue.resume(id)
            assertEquals(id, kit.runner.awaitStart())
            queue.awaitStatus(id, JobStatus.Running)
        }

    @Test
    fun finishedAndWaitingJobsKeepTheirStatus() =
        runBlocking<Unit> {
            val done = kit.saved(delete(path("sd", "a")), JobStatus.Done)
            val waiting = kit.saved(delete(path("sd", "b")), JobStatus.WaitingForConflict)
            val queue = kit.queue()
            queue.awaitStatus(done, JobStatus.Done)
            assertEquals(JobStatus.WaitingForConflict, queue.statusOf(waiting))
        }

    /** A queued job never started, so it is scheduled again; that is not a silent resume. */
    @Test
    fun aQueuedJobIsScheduledAgain() =
        runBlocking<Unit> {
            val id = kit.saved(delete(path("sd", "a")), JobStatus.Queued)
            kit.queue()
            assertEquals(id, kit.runner.awaitStart())
        }

    @Test
    fun aDamagedRowFailsInsteadOfCrashing() =
        runBlocking<Unit> {
            val id = kit.saved(delete(path("sd", "a")), JobStatus.Paused)
            kit.database.openHelper.writableDatabase
                .execSQL("UPDATE jobs SET roots = 'junk' WHERE id = ${id.value}")
            val queue = kit.queue()
            queue.awaitStatus(id, JobStatus.Failed)
            assertEquals(JobStatus.Failed.name, kit.row(id).status)
        }

    /** A job left waiting on a conflict has no runner after a restart, but can still be cancelled. */
    @Test
    fun aJobLeftWaitingOnAConflictCanBeCancelled() =
        runBlocking<Unit> {
            val id = kit.saved(delete(path("sd", "b")), JobStatus.WaitingForConflict)
            val queue = kit.queue()
            queue.awaitStatus(id, JobStatus.WaitingForConflict)
            queue.cancel(id)
            queue.awaitStatus(id, JobStatus.Cancelled)
            queue.enqueue(delete(path("sd", "b"))).id()
        }
}
