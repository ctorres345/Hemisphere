package io.github.ctorres345.hemisphere.core.jobs

import io.github.ctorres345.hemisphere.core.model.EnqueueResult
import io.github.ctorres345.hemisphere.core.model.JobKind
import io.github.ctorres345.hemisphere.core.model.JobRequest
import io.github.ctorres345.hemisphere.core.model.JobStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Enqueue refuses a job whose paths overlap an unfinished job's, in both directions (HEM-43). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JobQueueOverlapTest {
    private val kit = JobsTestKit()

    @After
    fun tearDown() = kit.close()

    @Test
    fun theSameRootIsRefused() =
        runBlocking<Unit> {
            val queue = kit.queue()
            queue.enqueue(copy(path("sd", "ROMs", "a.iso"), to = path("int", "Backup"))).id()
            assertEquals(EnqueueResult.InUse, queue.enqueue(delete(path("sd", "ROMs", "a.iso"))))
        }

    @Test
    fun aDescendantOfARunningRootIsRefused() =
        runBlocking<Unit> {
            val queue = kit.queue()
            queue.enqueue(delete(path("sd", "ROMs"))).id()
            assertEquals(EnqueueResult.InUse, queue.enqueue(delete(path("sd", "ROMs", "gba", "a.gba"))))
        }

    @Test
    fun anAncestorOfARunningRootIsRefused() =
        runBlocking<Unit> {
            val queue = kit.queue()
            queue.enqueue(delete(path("sd", "ROMs", "gba", "a.gba"))).id()
            val rename = JobRequest(JobKind.Rename, listOf(path("sd", "ROMs")), newName = "Games")
            assertEquals(EnqueueResult.InUse, queue.enqueue(rename))
        }

    @Test
    fun changingADestinationOrItsAncestorIsRefused() =
        runBlocking<Unit> {
            val queue = kit.queue()
            queue.enqueue(copy(path("int", "a.iso"), to = path("sd", "ROMs"))).id()
            assertEquals(EnqueueResult.InUse, queue.enqueue(delete(path("sd", "ROMs"))))
            assertEquals(EnqueueResult.InUse, queue.enqueue(delete(path("sd"))))
        }

    @Test
    fun theEntryACopyCreatesIsRefused() =
        runBlocking<Unit> {
            val queue = kit.queue()
            queue.enqueue(copy(path("int", "a.iso"), to = path("sd", "ROMs"))).id()
            assertEquals(EnqueueResult.InUse, queue.enqueue(delete(path("sd", "ROMs", "a.iso"))))
        }

    /** Option A: the destination folder is shared, so two copies may write into one folder. */
    @Test
    fun twoCopiesIntoOneFolderAreAccepted() =
        runBlocking<Unit> {
            val queue = kit.queue()
            queue.enqueue(copy(path("int", "a.iso"), to = path("sd", "ROMs"))).id()
            queue.enqueue(copy(path("int", "b.iso"), to = path("sd", "ROMs"))).id()
            queue.enqueue(delete(path("sd", "ROMs", "other.iso"))).id()
        }

    @Test
    fun copiesOfOneNameIntoOneFolderAreRefused() =
        runBlocking<Unit> {
            val queue = kit.queue()
            queue.enqueue(copy(path("int", "a.iso"), to = path("sd", "ROMs"))).id()
            assertEquals(EnqueueResult.InUse, queue.enqueue(copy(path("usb", "a.iso"), to = path("sd", "ROMs"))))
        }

    @Test
    fun aRenameToTheNameACopyCreatesIsRefused() =
        runBlocking<Unit> {
            val queue = kit.queue()
            queue.enqueue(copy(path("int", "a.iso"), to = path("sd", "ROMs"))).id()
            val rename = JobRequest(JobKind.Rename, listOf(path("sd", "ROMs", "b.iso")), newName = "a.iso")
            assertEquals(EnqueueResult.InUse, queue.enqueue(rename))
        }

    @Test
    fun twoRenamesToOneNameAreRefused() =
        runBlocking<Unit> {
            val queue = kit.queue()
            queue.enqueue(JobRequest(JobKind.Rename, listOf(path("sd", "a")), newName = "c")).id()
            val second = JobRequest(JobKind.Rename, listOf(path("sd", "b")), newName = "c")
            assertEquals(EnqueueResult.InUse, queue.enqueue(second))
        }

    @Test
    fun aPausedJobStillClaimsItsPaths() =
        runBlocking<Unit> {
            val queue = kit.queue()
            val id = queue.enqueue(delete(path("sd", "ROMs"))).id()
            kit.runner.awaitStart()
            queue.pause(id)
            queue.awaitStatus(id, JobStatus.Paused)
            assertEquals(EnqueueResult.InUse, queue.enqueue(delete(path("sd", "ROMs", "a.iso"))))
        }

    @Test
    fun aJobWaitingForAConflictStillClaimsItsPaths() =
        runBlocking<Unit> {
            kit.saved(copy(path("int", "a.iso"), to = path("sd", "ROMs")), JobStatus.WaitingForConflict)
            val queue = kit.queue()
            assertEquals(EnqueueResult.InUse, queue.enqueue(delete(path("int", "a.iso"))))
        }

    @Test
    fun aDoneJobDoesNotBlock() =
        runBlocking<Unit> {
            val queue = kit.queue()
            val id = queue.enqueue(delete(path("sd", "ROMs"))).id()
            kit.runner.release(id)
            queue.awaitStatus(id, JobStatus.Done)
            queue.enqueue(delete(path("sd", "ROMs"))).id()
        }

    @Test
    fun aCaseInsensitiveVolumeComparesFoldedNames() =
        runBlocking<Unit> {
            val queue = kit.queue()
            queue.enqueue(delete(path("sd", "ROMs", caseInsensitive = true))).id()
            assertEquals(
                EnqueueResult.InUse,
                queue.enqueue(delete(path("sd", "roms", "A.iso", caseInsensitive = true))),
            )
            queue.enqueue(delete(path("usb", "ROMs", caseInsensitive = true))).id()
        }

    @Test
    fun aRefusedJobIsNotStored() =
        runBlocking<Unit> {
            val queue = kit.queue()
            queue.enqueue(delete(path("sd", "ROMs"))).id()
            queue.enqueue(delete(path("sd", "ROMs")))
            assertEquals(
                1,
                kit.database
                    .jobs()
                    .all()
                    .size,
            )
            assertEquals(1, queue.observe().value.size)
        }
}
