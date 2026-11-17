package io.github.ctorres345.hemisphere.core.model

/** One storage root: internal storage, an SD card, a USB drive, later a network share (architecture §3). */
@JvmInline
value class VolumeId(
    val value: String,
)

/** An opaque lookup hint from the provider, such as a SAF document id. Never parsed outside `core:fs`. */
@JvmInline
value class ProviderRef(
    val value: String,
)

/**
 * What equality, selection and lookups use (architecture §3). On name-addressed providers it is the
 * volume and the path segments. Case folding for case-insensitive volumes and the `idAddressed`
 * form come with the volumes that need them.
 */
data class EntryKey(
    val volume: VolumeId,
    val segments: List<String>,
)

/**
 * A place on a volume. Equality is by [key], not by every field: a stale [ref] never makes two paths
 * to the same entry unequal.
 */
class FsPath(
    val volume: VolumeId,
    val segments: List<String>,
    val ref: ProviderRef? = null,
) {
    val key: EntryKey get() = EntryKey(volume, segments)

    /** The last segment, or `null` for the volume root. */
    val name: String? get() = segments.lastOrNull()

    fun child(
        name: String,
        ref: ProviderRef? = null,
    ): FsPath = FsPath(volume, segments + name, ref)

    override fun equals(other: Any?): Boolean = other is FsPath && other.key == key

    override fun hashCode(): Int = key.hashCode()

    override fun toString(): String = "FsPath(${volume.value}:/${segments.joinToString("/")})"

    companion object {
        fun root(volume: VolumeId): FsPath = FsPath(volume, emptyList())
    }
}
