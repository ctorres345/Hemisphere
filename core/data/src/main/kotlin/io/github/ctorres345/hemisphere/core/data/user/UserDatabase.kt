package io.github.ctorres345.hemisphere.core.data.user

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * One folder's saved view. [entryKey] is the folder's `EntryKey` in its v1 text form and is what
 * lookups use. [pathLookup] is the folder's `FsPath` text without its ref, which finds the row again
 * when the key no longer matches.
 */
@Entity(tableName = "folder_view", indices = [Index(value = ["path_lookup"])])
internal data class FolderViewEntity(
    @PrimaryKey @ColumnInfo(name = "entry_key") val entryKey: String,
    @ColumnInfo(name = "path_lookup") val pathLookup: String,
    @ColumnInfo(name = "sort_field") val sortField: String,
    val descending: Boolean,
)

@Dao
internal interface FolderViewDao {
    /**
     * The row saved under [entryKey], else one saved for the same path under another key (a refreshed
     * ref, or a key whose case fold changed). Reading never moves a row, so panes that hold the same
     * folder under different keys all see it.
     */
    @Query(
        "SELECT * FROM folder_view WHERE entry_key = :entryKey OR path_lookup = :pathLookup " +
            "ORDER BY entry_key = :entryKey DESC LIMIT 1",
    )
    fun observe(
        entryKey: String,
        pathLookup: String,
    ): Flow<FolderViewEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(row: FolderViewEntity)

    @Query("DELETE FROM folder_view WHERE entry_key = :entryKey OR path_lookup = :pathLookup")
    suspend fun delete(
        entryKey: String,
        pathLookup: String,
    )

    /** Keeps one row per folder: drops any row under the same key or path, then writes [row] under its key. */
    @Transaction
    suspend fun replace(row: FolderViewEntity) {
        delete(row.entryKey, row.pathLookup)
        insert(row)
    }
}

/**
 * `user.db` (architecture §8): what the user chose, such as per-folder views. It holds folder paths,
 * so it is never cloud backed up (ADR 0008). Schemas are exported to `core/data/schemas`.
 */
@Database(entities = [FolderViewEntity::class], version = 1, exportSchema = true)
internal abstract class UserDatabase : RoomDatabase() {
    abstract fun folderViews(): FolderViewDao

    companion object {
        const val FILE_NAME = "user.db"
    }
}
