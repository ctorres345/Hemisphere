package io.github.ctorres345.hemisphere.feature.browser.access

import io.github.ctorres345.hemisphere.core.model.StorageAccessState

/**
 * Everything the access screen shows. [launch] is a one-off event kept as state: the screen opens
 * the system page, then clears it with [AccessIntent.LaunchHandled] (architecture §6).
 */
data class AccessUiState(
    val access: StorageAccessState = StorageAccessState.Denied,
    val launch: AccessLaunch? = null,
)

/** Which system page the screen should open next. The screen maps it to an Android intent by API level. */
enum class AccessLaunch {
    /** Ask for access: the all-files page on Android 11+, the runtime permission dialog before. */
    Grant,

    /** The app's own settings page, where the user turns the permission on after refusing twice. */
    AppSettings,
}

/** Everything the user can do on the access screen. */
sealed interface AccessIntent {
    /** The Grant button. */
    data object GrantClicked : AccessIntent

    /** The "Pick a folder instead" button. */
    data object PickFolderClicked : AccessIntent

    /** The screen opened the page asked for by [AccessUiState.launch]. */
    data object LaunchHandled : AccessIntent

    /**
     * The runtime permission dialog closed (Android 8 to 10). [canAskAgain] is the system's rationale
     * flag after a denial.
     */
    data class PermissionResult(
        val granted: Boolean,
        val canAskAgain: Boolean,
    ) : AccessIntent
}

/** Everything that can change [AccessUiState]. */
sealed interface AccessResult {
    /** Storage access changed, or was read again. */
    data class AccessChanged(
        val access: StorageAccessState,
    ) : AccessResult

    /** The user asked to grant access. */
    data object GrantRequested : AccessResult

    /** The screen handled the pending launch. */
    data object LaunchCleared : AccessResult
}
