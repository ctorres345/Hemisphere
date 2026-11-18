package io.github.ctorres345.hemisphere.core.fs

import android.content.Context
import android.media.MediaScannerConnection
import android.util.Log

/**
 * Asks MediaStore to look at changed files again, so other apps (galleries, music players) see a
 * write, rename, move or delete (architecture §3: MediaStore is not a provider). [scan] returns at
 * once; the scan runs in the background, and a failed scan is not an error of the file operation.
 */
fun interface MediaScanner {
    /** [paths] are absolute file paths: both the old and the new one after a rename or move. */
    fun scan(paths: List<String>)

    companion object {
        /** Scans nothing: for volumes MediaStore does not index (the app's own files) and for tests. */
        val None = MediaScanner { }
    }
}

/** Scans through `MediaScannerConnection`, which also drops paths that no longer exist from MediaStore. */
internal class AndroidMediaScanner(
    private val context: Context,
) : MediaScanner {
    override fun scan(paths: List<String>) {
        if (paths.isEmpty()) return
        try {
            MediaScannerConnection.scanFile(context, paths.toTypedArray(), null, null)
        } catch (
            @Suppress("TooGenericExceptionCaught") e: RuntimeException,
        ) {
            // A failed scan is not an error of the file operation that asked for it.
            Log.w(TAG, "MediaStore scan failed", e)
        }
    }

    private companion object {
        const val TAG = "MediaScanner"
    }
}
