package io.github.ctorres345.hemisphere.core.testing

import io.github.ctorres345.hemisphere.core.data.user.FolderViewRepository
import io.github.ctorres345.hemisphere.core.model.EntryKey
import io.github.ctorres345.hemisphere.core.model.FolderSort
import io.github.ctorres345.hemisphere.core.model.FsPath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Folder sorts held in memory, by key. Its flows never end, like the real one. */
class FakeFolderViewRepository : FolderViewRepository {
    private val sorts = MutableStateFlow<Map<EntryKey, FolderSort>>(emptyMap())

    override fun observe(folder: FsPath): Flow<FolderSort> =
        sorts.map { it[folder.key] ?: FolderSort.DEFAULT }.distinctUntilChanged()

    override suspend fun setSort(
        folder: FsPath,
        sort: FolderSort,
    ) = sorts.update { if (sort == FolderSort.DEFAULT) it - folder.key else it + (folder.key to sort) }
}
