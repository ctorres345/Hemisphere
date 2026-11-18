package io.github.ctorres345.hemisphere.core.fs

import io.github.ctorres345.hemisphere.core.model.StorageAccessState
import kotlinx.coroutines.flow.StateFlow

/**
 * Whether the app may use shared storage (ADR 0002): all-files access on Android 11+, the runtime
 * storage permission on Android 8 to 10. Lives in `core:fs` because `core:domain` cannot see
 * `core:platform`.
 */
interface StorageAccess {
    /** The current access. The Android implementation re-reads it every time the app comes to the foreground. */
    val state: StateFlow<StorageAccessState>

    /** Reads the system's answer again. */
    fun refresh()

    /**
     * Records the answer to the runtime permission dialog (Android 8 to 10). A denial the system will
     * not ask about again ([canAskAgain] false) turns the state into [StorageAccessState.NeedsSettings].
     * [granted] is the dialog's answer; the Android implementation reads the system again instead of
     * trusting it, so a cancelled request never counts as a grant.
     */
    fun onPermissionResult(
        granted: Boolean,
        canAskAgain: Boolean,
    )
}
