package io.github.ctorres345.hemisphere.core.domain.files

import io.github.ctorres345.hemisphere.core.fs.LockedFolders
import io.github.ctorres345.hemisphere.core.model.FsPath
import javax.inject.Inject

/**
 * Whether Android keeps a folder private, and which of its children it keeps private (architecture §3).
 * On Android 11+ `Android/data` and `Android/obb` cannot be read, so the browser shows them as locked
 * rows and opens them on a short explanation instead of listing them.
 */
class GetFolderLockUseCase
    @Inject
    constructor(
        private val lockedFolders: LockedFolders,
    ) {
        operator fun invoke(dir: FsPath): FolderLock =
            FolderLock(
                locked = lockedFolders.isLocked(dir),
                lockedChildren = lockedFolders.lockedChildren(dir),
            )
    }

/**
 * [locked]: the folder itself cannot be read. [lockedChildren]: the lower-case names of child folders
 * that cannot be read; compare a listed name with [isChildLocked].
 */
data class FolderLock(
    val locked: Boolean = false,
    val lockedChildren: Set<String> = emptySet(),
) {
    /** True when the child folder called [name] is locked. */
    fun isChildLocked(name: String): Boolean = lockedChildren.isNotEmpty() && LockedFolders.fold(name) in lockedChildren

    companion object {
        val NONE = FolderLock()
    }
}
