package io.github.ctorres345.hemisphere.core.jobs

import androidx.room.withTransaction
import io.github.ctorres345.hemisphere.core.jobs.db.JobEntity
import io.github.ctorres345.hemisphere.core.jobs.db.StateDatabase
import io.github.ctorres345.hemisphere.core.jobs.db.savedCursor
import io.github.ctorres345.hemisphere.core.jobs.db.savedProgress
import io.github.ctorres345.hemisphere.core.jobs.db.toEntity
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.JobId
import io.github.ctorres345.hemisphere.core.model.JobProgress
import io.github.ctorres345.hemisphere.core.model.JobRequest
import io.github.ctorres345.hemisphere.core.model.encode
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * One run of a job. Progress goes live through [publish] at once and to `state.db` at most once
 * every [SAVE_INTERVAL_MS]; an item's cursor, progress and records are saved in one transaction.
 * Every write is guarded by the `Running` status, so a late write never undoes a pause or cancel.
 * Used from the runner's coroutine only, then [flush] once it ends.
 */
internal class RoomJobContext(
    override val id: JobId,
    override val request: JobRequest,
    row: JobEntity,
    private val database: StateDatabase,
    private val clock: () -> Long,
    private val publish: (JobProgress, FsPath?) -> Unit,
) : JobContext {
    override val cursor: JobCursor = row.savedCursor()
    private var progress: JobProgress = row.savedProgress()
    private val dao = database.jobs()
    private var item: FsPath? = null
    private var lastSave: Long? = null
    private var unsaved = false

    override suspend fun progress(
        progress: JobProgress,
        currentItem: FsPath?,
    ) {
        this.progress = progress
        item = currentItem
        unsaved = true
        publish(progress, currentItem)
        val last = lastSave
        if (last == null || clock() - last >= SAVE_INTERVAL_MS) save()
    }

    override suspend fun itemFinished(
        cursor: JobCursor,
        progress: JobProgress,
        records: List<JobRecord>,
    ) {
        this.progress = progress
        publish(progress, item)
        // The item is already done on disk, so a pause or cancel never rolls its cursor and
        // records back: the commit completes, and the stop lands at the runner's next suspension.
        withContext(NonCancellable) {
            database.withTransaction {
                val saved =
                    dao.saveCursor(
                        id.value,
                        cursor.lastItem,
                        cursor.bytesDone,
                        cursor.tempCounter,
                        cursor.tempPath?.encode(),
                    )
                if (saved > 0) {
                    saveProgress()
                    if (records.isNotEmpty()) dao.insertRecords(records.map { it.toEntity(id) })
                }
            }
        }
        lastSave = clock()
        unsaved = false
    }

    /** Saves progress not yet written. Called once the runner has stopped. */
    suspend fun flush() {
        if (unsaved) save()
    }

    private suspend fun save() {
        saveProgress()
        lastSave = clock()
        unsaved = false
    }

    private suspend fun saveProgress() =
        dao.saveProgress(
            id.value,
            progress.itemsDone,
            progress.itemsTotal,
            progress.bytesDone,
            progress.bytesTotal,
            item?.encode(),
        )

    companion object {
        const val SAVE_INTERVAL_MS = 1_000L
    }
}
