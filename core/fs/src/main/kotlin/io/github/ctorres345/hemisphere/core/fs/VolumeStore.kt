package io.github.ctorres345.hemisphere.core.fs

import io.github.ctorres345.hemisphere.core.model.VolumeId
import kotlinx.coroutines.flow.Flow

/** A SAF tree grant as an opaque URI string. Plain Kotlin: `core:fs` converts it to a `Uri` where needed. */
@JvmInline
value class TreeGrant(
    val uri: String,
)

/** The answer to "do I already know this removable volume without a UUID?" (architecture §3). */
sealed interface RemovableMatch {
    /** No saved id has this label and capacity. */
    data object None : RemovableMatch

    /** Exactly one saved id has this label and capacity. */
    data class One(
        val id: VolumeId,
    ) : RemovableMatch

    /**
     * Two or more saved ids match. The caller asks the user and never guesses: it shows [candidates],
     * keeps the one the user picks, and may retire the others with [VolumeStore.removeRemovableId].
     */
    data class Ambiguous(
        val candidates: List<VolumeId>,
    ) : RemovableMatch
}

/**
 * What must be saved about volumes across reboots (architecture §3 and §8): SAF grants and ids of
 * removable volumes that have no UUID. Implemented in `core:data` (`volumes.db`).
 */
interface VolumeStore {
    /** Saves or replaces the write grant for [volume]. */
    suspend fun saveGrant(
        volume: VolumeId,
        grant: TreeGrant,
    )

    suspend fun grantFor(volume: VolumeId): TreeGrant?

    suspend fun removeGrant(volume: VolumeId)

    /** Every saved grant, by volume. Emits again when one is saved or removed. */
    fun observeGrants(): Flow<Map<VolumeId, TreeGrant>>

    /** Looks up a saved id by [label] and [capacityBytes]. */
    suspend fun findRemovableId(
        label: String,
        capacityBytes: Long,
    ): RemovableMatch

    /**
     * The id for a volume with this [label] and [capacityBytes], made and saved when none matches.
     * Atomic, so two lookups of one card at once (a mount broadcast sent twice) make one id, not two.
     * Never returns [RemovableMatch.None].
     */
    suspend fun findOrCreateRemovableId(
        label: String,
        capacityBytes: Long,
    ): RemovableMatch

    /**
     * Saves a new random id even though others match, for when the user says the card is a new one.
     * Use [findOrCreateRemovableId] otherwise.
     */
    suspend fun createRemovableId(
        label: String,
        capacityBytes: Long,
    ): VolumeId

    /**
     * Forgets a removable id and its saved grant, for a stale match the user rejected. Read
     * [grantFor] first: the caller releases that URI permission itself, and only when no other saved
     * grant holds the same URI (a stale id and the real card can share one).
     */
    suspend fun removeRemovableId(id: VolumeId)
}
