package io.github.ctorres345.hemisphere.core.testing

import io.github.ctorres345.hemisphere.core.fs.RemovableMatch
import io.github.ctorres345.hemisphere.core.fs.TreeGrant
import io.github.ctorres345.hemisphere.core.fs.VolumeStore
import io.github.ctorres345.hemisphere.core.model.VolumeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Grants and removable ids held in memory. Ids are `fake-1`, `fake-2`, and so on. */
class FakeVolumeStore : VolumeStore {
    private data class Removable(
        val id: VolumeId,
        val label: String,
        val capacityBytes: Long,
    )

    private val grants = MutableStateFlow<Map<VolumeId, TreeGrant>>(emptyMap())
    private val removables = mutableListOf<Removable>()
    private var created = 0
    private val lock = Mutex()

    override suspend fun saveGrant(
        volume: VolumeId,
        grant: TreeGrant,
    ) = grants.update { it + (volume to grant) }

    override suspend fun grantFor(volume: VolumeId): TreeGrant? = grants.value[volume]

    override suspend fun removeGrant(volume: VolumeId) = grants.update { it - volume }

    override fun observeGrants(): Flow<Map<VolumeId, TreeGrant>> = grants

    override suspend fun findRemovableId(
        label: String,
        capacityBytes: Long,
    ): RemovableMatch = lock.withLock { match(label, capacityBytes) }

    override suspend fun findOrCreateRemovableId(
        label: String,
        capacityBytes: Long,
    ): RemovableMatch =
        lock.withLock {
            val match = match(label, capacityBytes)
            if (match == RemovableMatch.None) RemovableMatch.One(insert(label, capacityBytes)) else match
        }

    override suspend fun createRemovableId(
        label: String,
        capacityBytes: Long,
    ): VolumeId = lock.withLock { insert(label, capacityBytes) }

    override suspend fun removeRemovableId(id: VolumeId) {
        lock.withLock { removables.removeAll { it.id == id } }
        grants.update { it - id }
    }

    private fun match(
        label: String,
        capacityBytes: Long,
    ): RemovableMatch {
        val ids = removables.filter { it.label == label && it.capacityBytes == capacityBytes }.map { it.id }
        return when (ids.size) {
            0 -> RemovableMatch.None
            1 -> RemovableMatch.One(ids.single())
            else -> RemovableMatch.Ambiguous(ids)
        }
    }

    private fun insert(
        label: String,
        capacityBytes: Long,
    ): VolumeId {
        val id = VolumeId("fake-${++created}")
        removables += Removable(id, label, capacityBytes)
        return id
    }
}
