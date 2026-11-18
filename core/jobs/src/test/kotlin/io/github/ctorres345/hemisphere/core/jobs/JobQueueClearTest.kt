package io.github.ctorres345.hemisphere.core.jobs

import io.github.ctorres345.hemisphere.core.jobs.db.RecordEntity
import io.github.ctorres345.hemisphere.core.model.JobStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** clearFinished removes finished jobs and their records, and keeps Undo (HEM-43). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JobQueueClearTest {
    private val kit = JobsTestKit()

    @After
    fun tearDown() = kit.close()

    @Test
    fun onlyFinishedJobsAndTheirRecordsAreRemoved() =
        runBlocking<Unit> {
            val ids =
                listOf(
                    JobStatus.Done,
                    JobStatus.Failed,
                    JobStatus.Cancelled,
                    JobStatus.Paused,
                    JobStatus.WaitingForConflict,
                ).mapIndexed { i, status -> status to kit.saved(delete(path("sd", "f$i")), status) }
                    .toMap()
            val db = kit.database.openHelper.writableDatabase
            ids.values.forEach { id ->
                kit.database.jobs().insertRecords(
                    listOf(RecordEntity(jobId = id.value, type = "Failure", path = "p", detail = null)),
                )
                db.execSQL(
                    "INSERT INTO conflicts (job_id, source, target, choice) VALUES (${id.value}, 's', 't', NULL)",
                )
                db.execSQL(
                    "INSERT INTO undo (job_id, kind, payload, created_at) VALUES (${id.value}, 'Delete', 'x', 0)",
                )
            }
            val queue = kit.queue()
            queue.awaitStatus(ids.getValue(JobStatus.Done), JobStatus.Done)

            queue.clearFinished()

            val kept = setOf(ids.getValue(JobStatus.Paused), ids.getValue(JobStatus.WaitingForConflict))
            assertEquals(
                kept,
                queue
                    .observe()
                    .value
                    .map { it.id }
                    .toSet(),
            )
            assertEquals(
                kept.map { it.value }.toSet(),
                kit.database
                    .jobs()
                    .all()
                    .map { it.id }
                    .toSet(),
            )
            assertEquals(kept.size.toLong(), count("records"))
            assertEquals(kept.size.toLong(), count("conflicts"))
            assertEquals(ids.size.toLong(), count("undo"))
        }

    private fun count(table: String): Long =
        kit.database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use {
            it.moveToFirst()
            it.getLong(0)
        }
}
