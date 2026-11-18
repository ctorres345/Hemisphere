package io.github.ctorres345.hemisphere.core.jobs

import io.github.ctorres345.hemisphere.core.model.JobKind
import io.github.ctorres345.hemisphere.core.model.JobProgress
import io.github.ctorres345.hemisphere.core.model.JobStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicLong

/** Progress is saved at most once a second; an item's cursor and records are saved together (HEM-43). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JobProgressTest {
    private val kit = JobsTestKit()
    private val now = AtomicLong(0)

    @After
    fun tearDown() = kit.close()

    @Test
    fun progressIsSavedAtMostOnceASecondAndFlushedOnPause() =
        runBlocking<Unit> {
            val reported = CompletableDeferred<Unit>()
            val runner =
                JobRunner { job ->
                    job.progress(JobProgress(bytesDone = 1), null)
                    now.set(500)
                    job.progress(JobProgress(bytesDone = 2), null)
                    reported.complete(Unit)
                    CompletableDeferred<Unit>().await()
                }
            val queue = kit.queue(mapOf(JobKind.Copy to runner)) { now.get() }
            val id = queue.enqueue(copy(path("int", "a"), to = path("sd", "x"))).id()
            reported.await()
            assertEquals(1L, kit.row(id).bytesDone)
            assertEquals(
                2L,
                queue
                    .observe()
                    .value
                    .single()
                    .progress.bytesDone,
            )

            queue.pause(id)
            queue.awaitStatus(id, JobStatus.Paused)
            assertEquals(2L, kit.row(id).bytesDone)
        }

    @Test
    fun aFinishedItemSavesCursorAndRecordsAndResumeReadsTheCursor() =
        runBlocking<Unit> {
            val seen = mutableListOf<JobCursor>()
            val runner =
                JobRunner { job ->
                    seen += job.cursor
                    if (job.cursor.lastItem == null) {
                        job.itemFinished(
                            JobCursor(lastItem = "a/1", tempCounter = 3),
                            JobProgress(itemsDone = 1),
                            listOf(JobRecord(RecordType.Skip, path("int", "a", "1"))),
                        )
                        CompletableDeferred<Unit>().await()
                    }
                }
            val queue = kit.queue(mapOf(JobKind.Copy to runner))
            val id = queue.enqueue(copy(path("int", "a"), to = path("sd", "x"))).id()
            queue.awaitStatus(id, JobStatus.Running)
            kit.waitFor {
                kit.database
                    .jobs()
                    .records(id.value)
                    .isNotEmpty()
            }
            queue.pause(id)
            queue.awaitStatus(id, JobStatus.Paused)
            assertEquals("a/1", kit.row(id).cursorLastItem)
            assertEquals(
                1,
                kit.database
                    .jobs()
                    .records(id.value)
                    .size,
            )

            queue.resume(id)
            queue.awaitStatus(id, JobStatus.Done)
            assertEquals(JobCursor(lastItem = "a/1", tempCounter = 3), seen.last())
        }

    /** A pause that lands while an item's commit is in flight never rolls that item back. */
    @Test
    fun aPauseDuringItemFinishedKeepsTheCursor() =
        runBlocking<Unit> {
            lateinit var queue: JobQueue
            val runner =
                JobRunner { job ->
                    queue.pause(job.id)
                    job.itemFinished(JobCursor(lastItem = "a"), JobProgress(itemsDone = 1))
                    CompletableDeferred<Unit>().await()
                }
            queue = kit.queue(mapOf(JobKind.Copy to runner))
            val id = queue.enqueue(copy(path("int", "a"), to = path("sd", "x"))).id()
            queue.awaitStatus(id, JobStatus.Paused)
            assertEquals("a", kit.row(id).cursorLastItem)
            assertEquals(1L, kit.row(id).itemsDone)
        }

    @Test
    fun clearFinishedHandlesMoreJobsThanSqliteBinds() =
        runBlocking<Unit> {
            val queue = kit.queue(mapOf(JobKind.Delete to JobRunner { }))
            val ids = (0 until 1001).map { queue.enqueue(delete(path("sd", "f$it"))).id() }
            ids.forEach { queue.awaitStatus(it, JobStatus.Done) }
            queue.clearFinished()
            assertEquals(
                0,
                kit.database
                    .jobs()
                    .all()
                    .size,
            )
        }
}
