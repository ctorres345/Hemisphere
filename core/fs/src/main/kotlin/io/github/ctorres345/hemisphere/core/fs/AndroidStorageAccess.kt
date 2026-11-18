package io.github.ctorres345.hemisphere.core.fs

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.github.ctorres345.hemisphere.core.model.StorageAccessState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Reads the system's storage access and re-reads it when the app comes to the foreground, so a
 * grant or a revoke made on the system settings page shows as soon as the user comes back.
 */
internal class AndroidStorageAccess(
    private val context: Context,
) : StorageAccess {
    private val mutableState = MutableStateFlow(read(sticky = StorageAccessState.Denied))
    override val state: StateFlow<StorageAccessState> = mutableState.asStateFlow()

    init {
        // Observers must be added on the main thread; the process owner replays the current state.
        Handler(Looper.getMainLooper()).post {
            ProcessLifecycleOwner.get().lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStart(owner: LifecycleOwner) = refresh()
                },
            )
        }
    }

    override fun refresh() {
        mutableState.update { read(sticky = it) }
    }

    override fun onPermissionResult(
        granted: Boolean,
        canAskAgain: Boolean,
    ) {
        // The system is the source of truth: a cancelled request reports no results, which must not read as a grant.
        mutableState.value =
            when {
                isGranted() -> StorageAccessState.Granted
                canAskAgain -> StorageAccessState.Denied
                else -> StorageAccessState.NeedsSettings
            }
    }

    /** [sticky] is kept while access is still missing, so NeedsSettings survives a refresh. */
    private fun read(sticky: StorageAccessState): StorageAccessState =
        when {
            isGranted() -> StorageAccessState.Granted
            sticky == StorageAccessState.NeedsSettings -> StorageAccessState.NeedsSettings
            else -> StorageAccessState.Denied
        }

    private fun isGranted(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            runtimePermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        }

    private companion object {
        val runtimePermissions =
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}
