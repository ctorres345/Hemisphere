package io.github.ctorres345.hemisphere.core.data.user

import io.github.ctorres345.hemisphere.core.model.FolderSort
import io.github.ctorres345.hemisphere.core.model.FsPath
import kotlinx.coroutines.flow.Flow

/**
 * The saved view of each folder in `user.db` (architecture §8); for now its sort. Rows are found by
 * the folder's key. When the key misses (a refreshed ref, or a key whose case fold changed), the row
 * saved for the same path is used, and the next save files it under the new key.
 */
interface FolderViewRepository {
    /** The folder's sort, [FolderSort.DEFAULT] when none is saved, then every change to it. */
    fun observe(folder: FsPath): Flow<FolderSort>

    /** Saves [sort] for [folder]; the default sort removes the row. */
    suspend fun setSort(
        folder: FsPath,
        sort: FolderSort,
    )
}
