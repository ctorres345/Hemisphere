package io.github.ctorres345.hemisphere.core.domain.files

import io.github.ctorres345.hemisphere.core.fs.StorageAccess
import io.github.ctorres345.hemisphere.core.model.StorageAccessState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams whether the app may use shared storage. It emits again after the user changes it in the system settings. */
class ObserveStorageAccessUseCase
    @Inject
    constructor(
        private val storageAccess: StorageAccess,
    ) {
        operator fun invoke(): Flow<StorageAccessState> = storageAccess.state
    }
