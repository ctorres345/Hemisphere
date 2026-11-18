package io.github.ctorres345.hemisphere.core.testing

import io.github.ctorres345.hemisphere.core.fs.StorageAccess
import io.github.ctorres345.hemisphere.core.model.StorageAccessState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A [StorageAccess] the test drives by setting [state]; it records the permission results it gets. Like the
 * Android implementation, a result reads [systemGranted] rather than trusting the dialog's `granted`.
 */
class FakeStorageAccess(
    initial: StorageAccessState = StorageAccessState.Granted,
) : StorageAccess {
    private val mutable = MutableStateFlow(initial)
    override val state: StateFlow<StorageAccessState> = mutable

    /** What the system would say if asked again. */
    var systemGranted: Boolean = initial == StorageAccessState.Granted

    var refreshCount = 0
        private set
    val results = mutableListOf<Pair<Boolean, Boolean>>()

    fun set(next: StorageAccessState) {
        mutable.value = next
    }

    override fun refresh() {
        refreshCount++
    }

    override fun onPermissionResult(
        granted: Boolean,
        canAskAgain: Boolean,
    ) {
        results += granted to canAskAgain
        mutable.value =
            when {
                systemGranted -> StorageAccessState.Granted
                canAskAgain -> StorageAccessState.Denied
                else -> StorageAccessState.NeedsSettings
            }
    }
}
