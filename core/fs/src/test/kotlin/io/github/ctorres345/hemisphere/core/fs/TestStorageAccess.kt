package io.github.ctorres345.hemisphere.core.fs

import io.github.ctorres345.hemisphere.core.model.StorageAccessState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A [StorageAccess] the test sets by hand. `core:fs` tests cannot use `core:testing`, which depends on `core:fs`. */
internal class TestStorageAccess(
    initial: StorageAccessState,
) : StorageAccess {
    private val mutable = MutableStateFlow(initial)
    override val state: StateFlow<StorageAccessState> = mutable

    fun set(next: StorageAccessState) {
        mutable.value = next
    }

    override fun refresh() = Unit

    override fun onPermissionResult(
        granted: Boolean,
        canAskAgain: Boolean,
    ) = Unit
}

internal val GrantedAccess: StorageAccess = TestStorageAccess(StorageAccessState.Granted)
