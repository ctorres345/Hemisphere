package io.github.ctorres345.hemisphere.core.domain.files

import io.github.ctorres345.hemisphere.core.fs.StorageAccess
import javax.inject.Inject

/**
 * Passes on the answer to the runtime storage permission dialog (Android 8 to 10). When the system
 * will not ask again, the access becomes `NeedsSettings`.
 */
class RecordStoragePermissionResultUseCase
    @Inject
    constructor(
        private val storageAccess: StorageAccess,
    ) {
        operator fun invoke(
            granted: Boolean,
            canAskAgain: Boolean,
        ) = storageAccess.onPermissionResult(granted, canAskAgain)
    }
