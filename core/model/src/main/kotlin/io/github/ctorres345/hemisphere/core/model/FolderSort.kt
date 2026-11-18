package io.github.ctorres345.hemisphere.core.model

/**
 * How one folder's entries are ordered (architecture §8). Folders always come before files; [field]
 * and [descending] order each group. A folder with no saved sort uses [DEFAULT].
 */
data class FolderSort(
    val field: Field = Field.Name,
    val descending: Boolean = false,
) {
    enum class Field { Name, Size, Date }

    companion object {
        /** By name, A to Z. */
        val DEFAULT = FolderSort()
    }
}
