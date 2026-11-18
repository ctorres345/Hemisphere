package io.github.ctorres345.hemisphere.core.domain.files

import io.github.ctorres345.hemisphere.core.data.user.FolderViewRepository
import io.github.ctorres345.hemisphere.core.model.FolderSort
import io.github.ctorres345.hemisphere.core.model.FsPath
import javax.inject.Inject

/** Saves how [ObserveFolderUseCase] sorts one folder; open listings of it re-sort at once. */
class SetFolderSortUseCase
    @Inject
    constructor(
        private val folderViews: FolderViewRepository,
    ) {
        suspend operator fun invoke(
            folder: FsPath,
            sort: FolderSort,
        ) = folderViews.setSort(folder, sort)
    }
