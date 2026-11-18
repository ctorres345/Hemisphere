package io.github.ctorres345.hemisphere.core.data.volumes

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

/** A saved SAF tree grant for one volume. */
@Entity(tableName = "grants")
internal data class GrantEntity(
    @PrimaryKey @ColumnInfo(name = "volume_id") val volumeId: String,
    @ColumnInfo(name = "tree_uri") val treeUri: String,
)

/** The random id of a removable volume with no UUID, with what it is matched by. */
@Entity(
    tableName = "removable_ids",
    indices = [Index(value = ["label", "capacity_bytes"])],
)
internal data class RemovableIdEntity(
    @PrimaryKey @ColumnInfo(name = "volume_id") val volumeId: String,
    val label: String,
    @ColumnInfo(name = "capacity_bytes") val capacityBytes: Long,
)

@Dao
internal interface VolumesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putGrant(grant: GrantEntity)

    @Query("SELECT * FROM grants WHERE volume_id = :volumeId")
    suspend fun grant(volumeId: String): GrantEntity?

    @Query("DELETE FROM grants WHERE volume_id = :volumeId")
    suspend fun deleteGrant(volumeId: String)

    @Query("SELECT * FROM grants")
    fun observeGrants(): Flow<List<GrantEntity>>

    @Insert
    suspend fun insertRemovable(row: RemovableIdEntity)

    @Query("SELECT * FROM removable_ids WHERE label = :label AND capacity_bytes = :capacityBytes")
    suspend fun removableMatches(
        label: String,
        capacityBytes: Long,
    ): List<RemovableIdEntity>

    @Query("DELETE FROM removable_ids WHERE volume_id = :volumeId")
    suspend fun deleteRemovable(volumeId: String)

    /** Forgets a removable id and its grant together. */
    @Transaction
    suspend fun forgetRemovable(volumeId: String) {
        deleteRemovable(volumeId)
        deleteGrant(volumeId)
    }
}

/** `volumes.db` (architecture §8). Never backed up. Schemas are exported to `core/data/schemas`. */
@Database(
    entities = [GrantEntity::class, RemovableIdEntity::class],
    version = 1,
    exportSchema = true,
)
internal abstract class VolumesDatabase : RoomDatabase() {
    abstract fun dao(): VolumesDao

    companion object {
        const val FILE_NAME = "volumes.db"
    }
}
