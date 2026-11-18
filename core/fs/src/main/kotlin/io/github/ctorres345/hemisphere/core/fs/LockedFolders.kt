package io.github.ctorres345.hemisphere.core.fs

import android.os.Build
import io.github.ctorres345.hemisphere.core.model.FsPath
import java.util.Locale

/**
 * Folders Android keeps from every app (architecture §3, ADR 0002): on Android 11 (API 30) and up,
 * `Android/data` and `Android/obb` at a volume root cannot be read, even with all-files access. The
 * browser shows them as locked instead of an empty list or an error.
 *
 * Names are compared ignoring case, because shared storage is case-insensitive. The rule looks at the
 * path from the volume root, so a folder the user granted by hand that happens to hold `Android/data`
 * also shows as locked; SAF cannot grant those folders on Android 11+ either.
 *
 * [sdkInt] is `Build.VERSION.SDK_INT` in the app ([FsModule]); tests pass the level they need.
 */
class LockedFolders(
    private val sdkInt: Int,
) {
    /** True when [path] is one of the locked folders on this Android version. */
    fun isLocked(path: FsPath): Boolean {
        val segments = path.segments
        return segments.size == LOCKED_DEPTH && lockedChildren(segments.first()).contains(fold(segments.last()))
    }

    /**
     * The names (lower case) of the locked children of [dir], so a listing can mark those rows. Empty
     * for every folder but `Android` at a volume root on Android 11+.
     */
    fun lockedChildren(dir: FsPath): Set<String> =
        if (dir.segments.size == LOCKED_DEPTH - 1) lockedChildren(dir.segments.first()) else emptySet()

    private fun lockedChildren(parentName: String): Set<String> =
        if (sdkInt >= API_R && fold(parentName) == ANDROID) LOCKED_NAMES else emptySet()

    companion object {
        /** Android 11, the first version that blocks `Android/data` and `Android/obb`. */
        const val API_R = Build.VERSION_CODES.R

        private const val LOCKED_DEPTH = 2
        private const val ANDROID = "android"
        private val LOCKED_NAMES = setOf("data", "obb")

        /** Lower-cases a name the way [lockedChildren] reports it. */
        fun fold(name: String): String = name.lowercase(Locale.ROOT)
    }
}
