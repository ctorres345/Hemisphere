package io.github.ctorres345.hemisphere.core.jobs

import io.github.ctorres345.hemisphere.core.model.JobKind
import io.github.ctorres345.hemisphere.core.model.JobRequest
import io.github.ctorres345.hemisphere.core.model.JobStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** One long job per destination volume, at most two at once; short jobs never wait (HEM-43). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JobSchedulingTest {
    private val kit = JobsTestKit()

    @After
    fun tearDown() = kit.close()

    @Test
    fun twoJobsToOneVolumeRunOneAtATime() =
        runBlocking<Unit> {
            val queue = kit.queue()
            val first = queue.enqueue(copy(path("int", "a"), to = path("sd", "x"))).id()
            val second = queue.enqueue(copy(path("int", "b"), to = path("sd", "y"))).id()
            assertEquals(first, kit.runner.awaitStart())
            assertTrue(kit.runner.noStart())
            assertEquals(JobStatus.Queued, queue.statusOf(second))

            kit.runner.release(first)
            assertEquals(second, kit.runner.awaitStart())
            queue.awaitStatus(first, JobStatus.Done)
        }

    @Test
    fun jobsToTwoVolumesRunTogetherAndAThirdWaits() =
        runBlocking<Unit> {
            val queue = kit.queue()
            val toSd = queue.enqueue(copy(path("int", "a"), to = path("sd", "x"))).id()
            val toUsb = queue.enqueue(copy(path("int", "b"), to = path("usb", "x"))).id()
            val third = queue.enqueue(copy(path("int", "c"), to = path("otg", "x"))).id()
            assertEquals(setOf(toSd, toUsb), setOf(kit.runner.awaitStart(), kit.runner.awaitStart()))
            assertTrue(kit.runner.noStart())
            assertEquals(JobStatus.Queued, queue.statusOf(third))

            kit.runner.release(toUsb)
            assertEquals(third, kit.runner.awaitStart())
        }

    @Test
    fun aShortJobDoesNotWaitBehindLongOnes() =
        runBlocking<Unit> {
            val queue = kit.queue()
            queue.enqueue(copy(path("int", "a"), to = path("sd", "x"))).id()
            queue.enqueue(copy(path("int", "b"), to = path("usb", "x"))).id()
            kit.runner.awaitStart()
            kit.runner.awaitStart()
            val rename = queue.enqueue(JobRequest(JobKind.Rename, listOf(path("sd", "y")), newName = "z")).id()
            assertEquals(rename, kit.runner.awaitStart())
        }

    @Test
    fun aPausedJobFreesItsSlot() =
        runBlocking<Unit> {
            val queue = kit.queue()
            val first = queue.enqueue(copy(path("int", "a"), to = path("sd", "x"))).id()
            val second = queue.enqueue(copy(path("int", "b"), to = path("sd", "y"))).id()
            kit.runner.awaitStart()
            queue.pause(first)
            assertEquals(second, kit.runner.awaitStart())
            queue.awaitStatus(first, JobStatus.Paused)
        }

    @Test
    fun aCancelledJobEndsCancelled() =
        runBlocking<Unit> {
            val queue = kit.queue()
            val id = queue.enqueue(delete(path("sd", "x"))).id()
            kit.runner.awaitStart()
            queue.cancel(id)
            queue.awaitStatus(id, JobStatus.Cancelled)
            assertEquals(JobStatus.Cancelled.name, kit.row(id).status)
        }

    @Test
    fun aPauseAfterACancelKeepsItCancelled() =
        runBlocking<Unit> {
            val queue = kit.queue()
            val id = queue.enqueue(delete(path("sd", "x"))).id()
            kit.runner.awaitStart()
            queue.cancel(id)
            queue.pause(id)
            queue.awaitStatus(id, JobStatus.Cancelled)
            assertEquals(JobStatus.Cancelled.name, kit.row(id).status)
        }

    /** A pick that fails to start frees its slot for a job the same pass skipped. */
    @Test
    fun aJobThatCannotStartDoesNotHoldItsVolume() =
        runBlocking<Unit> {
            val failing = kit.saved(copy(path("int", "a"), to = path("sd", "x")), JobStatus.Queued)
            val waiting = kit.saved(delete(path("sd", "y")), JobStatus.Queued)
            val queue = kit.queue(mapOf(JobKind.Delete to kit.runner))
            assertEquals(waiting, kit.runner.awaitStart())
            queue.awaitStatus(failing, JobStatus.Failed)
        }

    @Test
    fun aRunnerThatThrowsFailsItsJob() =
        runBlocking<Unit> {
            val queue = kit.queue(mapOf(JobKind.Delete to JobRunner { throw JobFailedException("Read-only volume") }))
            val id = queue.enqueue(delete(path("sd", "x"))).id()
            queue.awaitStatus(id, JobStatus.Failed)
            assertEquals(
                "Read-only volume",
                queue
                    .observe()
                    .value
                    .single()
                    .error,
            )
        }

    /** A pause that lands before the runner is dispatched still frees the slot. */
    @Test
    fun aJobPausedBeforeItsRunnerStartsFreesItsSlot() =
        runBlocking<Unit> {
            repeat(REPEATS) { i ->
                val queue = kit.queue()
                val first = queue.enqueue(copy(path("int", "a$i"), to = path("v$i", "x"))).id()
                queue.pause(first)
                queue.awaitStatus(first, JobStatus.Paused)
                val second = queue.enqueue(copy(path("int", "b$i"), to = path("v$i", "y"))).id()
                queue.awaitStatus(second, JobStatus.Running)
                while (kit.runner.started
                        .tryReceive()
                        .isSuccess
                ) {
                    Unit
                }
            }
        }

    @Test
    fun aRunnerTimeoutFailsTheJobInsteadOfPausingIt() =
        runBlocking<Unit> {
            val queue =
                kit.queue(
                    mapOf(JobKind.Delete to JobRunner { withTimeout(1) { CompletableDeferred<Unit>().await() } }),
                )
            val id = queue.enqueue(delete(path("sd", "x"))).id()
            queue.awaitStatus(id, JobStatus.Failed)
        }

    private companion object {
        const val REPEATS = 20
    }
}
