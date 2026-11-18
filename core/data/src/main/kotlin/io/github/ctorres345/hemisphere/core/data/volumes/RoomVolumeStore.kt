package io.github.ctorres345.hemisphere.core.data.volumes

import io.github.ctorres345.hemisphere.core.fs.RemovableMatch
import io.github.ctorres345.hemisphere.core.fs.TreeGrant
import io.github.ctorres345.hemisphere.core.fs.VolumeStore
import io.github.ctorres345.hemisphere.core.model.VolumeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * [VolumeStore] over `volumes.db`. Room runs the queries off the main thread. Removable-id writes go
 * through one lock, so a find-then-create is atomic; Hilt keeps one instance per process.
 */
internal class RoomVolumeStore(
    private val dao: VolumesDao,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : VolumeStore {
    private val removables = Mutex()

    override suspend fun saveGrant(
        volume: VolumeId,
        grant: TreeGrant,
    ) = dao.putGrant(GrantEntity(volume.value, grant.uri))

    override suspend fun grantFor(volume: VolumeId): TreeGrant? = dao.grant(volume.value)?.let { TreeGrant(it.treeUri) }

    override suspend fun removeGrant(volume: VolumeId) = dao.deleteGrant(volume.value)

    override fun observeGrants(): Flow<Map<VolumeId, TreeGrant>> =
        dao.observeGrants().map { rows -> rows.associate { VolumeId(it.volumeId) to TreeGrant(it.treeUri) } }

    override suspend fun findRemovableId(
        label: String,
        capacityBytes: Long,
    ): RemovableMatch {
        val ids = dao.removableMatches(label, capacityBytes).map { VolumeId(it.volumeId) }
        return when (ids.size) {
            0 -> RemovableMatch.None
            1 -> RemovableMatch.One(ids.single())
            else -> RemovableMatch.Ambiguous(ids)
        }
    }

    override suspend fun findOrCreateRemovableId(
        label: String,
        capacityBytes: Long,
    ): RemovableMatch =
        removables.withLock {
            val match = findRemovableId(label, capacityBytes)
            if (match == RemovableMatch.None) RemovableMatch.One(insert(label, capacityBytes)) else match
        }

    override suspend fun createRemovableId(
        label: String,
        capacityBytes: Long,
    ): VolumeId = removables.withLock { insert(label, capacityBytes) }

    override suspend fun removeRemovableId(id: VolumeId) = removables.withLock { dao.forgetRemovable(id.value) }

    private suspend fun insert(
        label: String,
        capacityBytes: Long,
    ): VolumeId {
        val id = newId()
        dao.insertRemovable(RemovableIdEntity(id, label, capacityBytes))
        return VolumeId(id)
    }
}
