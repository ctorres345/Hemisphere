package io.github.ctorres345.hemisphere.core.data.user

import io.github.ctorres345.hemisphere.core.model.FolderSort
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.encode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class RoomFolderViewRepository(
    private val dao: FolderViewDao,
) : FolderViewRepository {
    override fun observe(folder: FsPath): Flow<FolderSort> =
        dao
            .observe(folder.key.encode(), folder.lookup())
            .map { it?.toSort() ?: FolderSort.DEFAULT }
            .distinctUntilChanged()

    override suspend fun setSort(
        folder: FsPath,
        sort: FolderSort,
    ) {
        if (sort == FolderSort.DEFAULT) {
            dao.delete(folder.key.encode(), folder.lookup())
        } else {
            dao.replace(
                FolderViewEntity(
                    entryKey = folder.key.encode(),
                    pathLookup = folder.lookup(),
                    sortField = sort.field.name,
                    descending = sort.descending,
                ),
            )
        }
    }

    private fun FolderViewEntity.toSort(): FolderSort =
        FolderSort(
            field = FolderSort.Field.entries.firstOrNull { it.name == sortField } ?: FolderSort.Field.Name,
            descending = descending,
        )

    /** The path text without its ref, which stays the same when the provider refreshes the ref. */
    private fun FsPath.lookup(): String = FsPath(volume, segments, null, caseInsensitive, idAddressed).encode()
}
