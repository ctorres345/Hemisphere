package io.github.ctorres345.hemisphere.core.model

import java.time.Instant

/** One child of a folder. It does not repeat the full path: the caller knows the parent. */
data class Entry(
    val name: String,
    val kind: Kind,
    val size: Long? = null,
    val modified: Instant? = null,
    val ref: ProviderRef? = null,
) {
    enum class Kind { Folder, File }
}

/** One emission of a folder listing: everything read so far, sorted, plus whether reading is done. */
data class Listing(
    val entries: List<Entry>,
    val complete: Boolean,
    val error: FsError? = null,
)
