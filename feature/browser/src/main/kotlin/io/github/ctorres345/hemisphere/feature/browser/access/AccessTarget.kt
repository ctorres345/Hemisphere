package io.github.ctorres345.hemisphere.feature.browser.access

/** The system page or dialog behind an [AccessLaunch] on one Android version (ADR 0002). */
enum class AccessTarget {
    /** `ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` for this app (Android 11+). */
    AllFilesSettings,

    /** The runtime storage permission dialog (Android 8 to 10). */
    RuntimePermission,

    /** `ACTION_APPLICATION_DETAILS_SETTINGS` for this app. */
    AppSettings,
}

private const val ALL_FILES_ACCESS_API = 30

/** The page to open for this launch on a device running API level [sdkInt]. */
fun AccessLaunch.target(sdkInt: Int): AccessTarget =
    when {
        this == AccessLaunch.AppSettings -> AccessTarget.AppSettings
        sdkInt >= ALL_FILES_ACCESS_API -> AccessTarget.AllFilesSettings
        else -> AccessTarget.RuntimePermission
    }
