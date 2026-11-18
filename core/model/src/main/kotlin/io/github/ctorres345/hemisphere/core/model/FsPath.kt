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
 * What equality, selection and lookups use (architecture §3). Name-addressed volumes use [ByPath]:
 * the volume and the path segments, upper-cased per UTF-16 unit when the volume is
 * case-insensitive. Volumes that address by id use [ById]: the volume and the provider ref.
 * The stable text form is in `PathCodec.kt` (version tag `v1`).
 */
sealed interface EntryKey {
    val volume: VolumeId

    data class ByPath(
        override val volume: VolumeId,
        val segments: List<String>,
    ) : EntryKey

    data class ById(
        override val volume: VolumeId,
        val ref: ProviderRef,
    ) : EntryKey

    companion object {
        /**
         * Builds a name-based key, folding each segment when [caseInsensitive]. Like FAT and exFAT,
         * each UTF-16 unit is upper-cased on its own: `ΑΣ`, `ασ` and `ας` fold the same, while `ß`
         * stays apart from `ss` and `İ` from `i`. The default locale never matters.
         */
        fun of(
            volume: VolumeId,
            segments: List<String>,
            caseInsensitive: Boolean,
        ): EntryKey = ByPath(volume, if (caseInsensitive) segments.map(::fold) else segments)

        private fun fold(name: String): String =
            String(
                CharArray(name.length) { i -> name[i].let { if (it.isSurrogate()) it else Character.toUpperCase(it) } },
            )
    }
}

/**
 * A place on a volume. Equality is by [key], not by every field: on a name-addressed volume a stale
 * [ref] never makes two paths to the same entry unequal. Build paths from [root] with the volume's
 * flags so one volume never mixes them.
 */
class FsPath(
    val volume: VolumeId,
    val segments: List<String>,
    val ref: ProviderRef? = null,
    /** Set from the volume root and inherited by [child]. Part of the v1 string form. */
    val caseInsensitive: Boolean = false,
    /** The volume's [Capabilities.idAddressed], set from the root and inherited by [child]. Part of the v1 form. */
    val idAddressed: Boolean = false,
) {
    /**
     * What equality uses: `(volume, ref)` when [idAddressed] and this path has a ref, else the
     * name-based key, folded when [caseInsensitive]. A path on an id-addressed volume without a ref
     * (the root, or a path typed by the user) keys by name until `core:fs` resolves its ref.
     */
    val key: EntryKey
        get() {
            val r = ref
            return if (idAddressed &&
                r != null
            ) {
                EntryKey.ById(volume, r)
            } else {
                EntryKey.of(volume, segments, caseInsensitive)
            }
        }

    /** The last segment, or `null` for the volume root. */
    val name: String? get() = segments.lastOrNull()

    fun child(
        name: String,
        ref: ProviderRef? = null,
    ): FsPath = FsPath(volume, segments + name, ref, caseInsensitive, idAddressed)

    /** The folder that holds this entry; the volume root is its own parent. */
    fun parent(): FsPath = FsPath(volume, segments.dropLast(1), null, caseInsensitive, idAddressed)

    override fun equals(other: Any?): Boolean = other is FsPath && other.key == key

    override fun hashCode(): Int = key.hashCode()

    override fun toString(): String = "FsPath(${volume.value}:/${segments.joinToString("/")})"

    companion object {
        fun root(
            volume: VolumeId,
            caseInsensitive: Boolean = false,
            idAddressed: Boolean = false,
        ): FsPath = FsPath(volume, emptyList(), null, caseInsensitive, idAddressed)
    }
}
