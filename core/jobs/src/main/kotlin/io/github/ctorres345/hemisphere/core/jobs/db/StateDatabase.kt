package io.github.ctorres345.hemisphere.core.jobs.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

// Paths are stored in their v1 text form (`FsPath.encode()`), never as keys (PathCodec.kt).
// Statuses, kinds and record types are stored by enum name.

/** One job: what it asked for, its saved place and its last saved progress (architecture §4). */
@Entity(tableName = "jobs")
internal data class JobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val status: String,
    /** The roots as one text, see [PathList]. */
    val roots: String,
    val destination: String?,
    val options: String,
    @ColumnInfo(name = "new_name") val newName: String? = null,
    @ColumnInfo(name = "cursor_last_item") val cursorLastItem: String? = null,
    @ColumnInfo(name = "cursor_bytes") val cursorBytes: Long = 0,
    @ColumnInfo(name = "temp_counter") val tempCounter: Long = 0,
    @ColumnInfo(name = "temp_path") val tempPath: String? = null,
    @ColumnInfo(name = "items_done") val itemsDone: Long = 0,
    @ColumnInfo(name = "items_total") val itemsTotal: Long? = null,
    @ColumnInfo(name = "bytes_done") val bytesDone: Long = 0,
    @ColumnInfo(name = "bytes_total") val bytesTotal: Long? = null,
    @ColumnInfo(name = "current_item") val currentItem: String? = null,
    val error: String? = null,
)

/** One failure, skip, Replace or Undo step of a job, written with the cursor that passed it. */
@Entity(
    tableName = "records",
    foreignKeys = [ForeignKey(JobEntity::class, ["id"], ["job_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("job_id")],
)
internal data class RecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "job_id") val jobId: Long,
    val type: String,
    val path: String,
    val detail: String?,
)

/** A conflict a job waits on, and the user's answer once given (HEM-48). */
@Entity(
    tableName = "conflicts",
    foreignKeys = [ForeignKey(JobEntity::class, ["id"], ["job_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("job_id")],
)
internal data class ConflictEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "job_id") val jobId: Long,
    val source: String,
    val target: String,
    val choice: String?,
)

/** One entry in a volume's Trash folder (HEM-47). Outlives the job that trashed it. */
@Entity(tableName = "trash_index", indices = [Index("volume_id")])
internal data class TrashIndexEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "volume_id") val volumeId: String,
    @ColumnInfo(name = "trash_name") val trashName: String,
    @ColumnInfo(name = "original_path") val originalPath: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long,
)

/** One Undo action (HEM-52). Not tied to the jobs table, so clearing finished jobs keeps it. */
@Entity(tableName = "undo")
internal data class UndoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "job_id") val jobId: Long,
    val kind: String,
    val payload: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Dao
internal interface JobsDao {
    @Insert
    suspend fun insert(job: JobEntity): Long

    @Query("SELECT * FROM jobs ORDER BY id")
    suspend fun all(): List<JobEntity>

    @Query("SELECT * FROM jobs WHERE id = :id")
    suspend fun byId(id: Long): JobEntity?

    @Query("SELECT * FROM jobs WHERE status NOT IN (:finished) ORDER BY id")
    suspend fun unfinished(finished: List<String>): List<JobEntity>

    /** Process death: whatever was running is now paused (architecture §4). */
    @Query("UPDATE jobs SET status = 'Paused' WHERE status = 'Running'")
    suspend fun pauseRunning()

    @Query("UPDATE jobs SET status = :status, error = :error WHERE id = :id")
    suspend fun setStatus(
        id: Long,
        status: String,
        error: String? = null,
    )

    /** Saves progress only while the job runs, so a late write never undoes a status change. */
    @Query(
        "UPDATE jobs SET items_done = :itemsDone, items_total = :itemsTotal, bytes_done = :bytesDone, " +
            "bytes_total = :bytesTotal, current_item = :currentItem WHERE id = :id AND status = 'Running'",
    )
    @Suppress("LongParameterList") // One parameter per column of a column-targeted UPDATE.
    suspend fun saveProgress(
        id: Long,
        itemsDone: Long,
        itemsTotal: Long?,
        bytesDone: Long,
        bytesTotal: Long?,
        currentItem: String?,
    )

    @Query(
        "UPDATE jobs SET cursor_last_item = :lastItem, cursor_bytes = :bytes, temp_counter = :tempCounter, " +
            "temp_path = :tempPath WHERE id = :id AND status = 'Running'",
    )
    suspend fun saveCursor(
        id: Long,
        lastItem: String?,
        bytes: Long,
        tempCounter: Long,
        tempPath: String?,
    ): Int

    @Insert
    suspend fun insertRecords(records: List<RecordEntity>)

    @Query("SELECT * FROM records WHERE job_id = :jobId ORDER BY id")
    suspend fun records(jobId: Long): List<RecordEntity>

    @Query("DELETE FROM records WHERE job_id IN (:ids)")
    suspend fun deleteRecordsOf(ids: List<Long>)

    @Query("DELETE FROM conflicts WHERE job_id IN (:ids)")
    suspend fun deleteConflictsOf(ids: List<Long>)

    @Query("DELETE FROM jobs WHERE id IN (:ids)")
    suspend fun deleteJobs(ids: List<Long>)
}

/**
 * `state.db` (architecture §8): jobs, their records and conflicts, the Trash index and Undo. It
 * stays at version 1 until the MVP ships; later tickets edit the entities and `1.json` in place.
 */
@Database(
    entities = [
        JobEntity::class,
        RecordEntity::class,
        ConflictEntity::class,
        TrashIndexEntity::class,
        UndoEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
internal abstract class StateDatabase : RoomDatabase() {
    abstract fun jobs(): JobsDao

    companion object {
        const val FILE_NAME = "state.db"
    }
}
