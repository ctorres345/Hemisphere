package io.github.ctorres345.hemisphere.feature.browser.access

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings

// The system pages the access screen opens. They need a Context, so they live beside the route, not in the ViewModel.

internal tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun Context.packageUri(): Uri = Uri.fromParts("package", packageName, null)

/**
 * The all-files page for this app; the app's settings page when no activity handles it. [target] only
 * picks it on Android 11+, so the constant (a plain string) is never read on an older device.
 */
@SuppressLint("InlinedApi")
internal fun Context.openAllFilesSettings() {
    // Some builds (Android Go, some vendor ROMs) lack the per-app page; the list page still has the toggle.
    val opened =
        tryStart(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, packageUri())) ||
            tryStart(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
    if (!opened) openAppSettings()
}

private fun Context.tryStart(intent: Intent): Boolean =
    try {
        // From an activity the page joins our task, so Back returns here; only a bare context needs a new task.
        startActivity(if (findActivity() == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) else intent)
        true
    } catch (
        @Suppress("SwallowedException") e: ActivityNotFoundException,
    ) {
        false
    }

internal fun Context.openAppSettings() {
    // No settings app to open: the screen stays, and the user can try the other button.
    tryStart(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri()))
}
