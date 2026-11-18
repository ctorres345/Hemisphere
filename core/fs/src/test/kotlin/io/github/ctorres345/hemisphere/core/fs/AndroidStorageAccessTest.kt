package io.github.ctorres345.hemisphere.core.fs

import android.Manifest
import android.app.Application
import io.github.ctorres345.hemisphere.core.model.StorageAccessState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The Android 8 to 10 path. Robolectric 4.17 cannot fake `Environment.isExternalStorageManager()`, so the
 * Android 11+ path is proven by the manual API 34 run instead.
 */
@RunWith(RobolectricTestRunner::class)
class AndroidStorageAccessTest {
    private val app: Application = RuntimeEnvironment.getApplication()
    private val permissions =
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @Test
    @Config(sdk = [29])
    fun `on Android 10 the runtime permissions decide access`() {
        val access = AndroidStorageAccess(app)
        assertEquals(StorageAccessState.Denied, access.state.value)

        shadowOf(app).grantPermissions(*permissions)
        access.refresh()

        assertEquals(StorageAccessState.Granted, access.state.value)
    }

    @Test
    @Config(sdk = [29])
    fun `a result that says granted while the system says no is not a grant`() {
        val access = AndroidStorageAccess(app)

        access.onPermissionResult(granted = true, canAskAgain = true)

        assertEquals(StorageAccessState.Denied, access.state.value)
    }

    @Test
    @Config(sdk = [29])
    fun `a final denial needs settings until access is granted there`() {
        val access = AndroidStorageAccess(app)

        access.onPermissionResult(granted = false, canAskAgain = false)
        assertEquals(StorageAccessState.NeedsSettings, access.state.value)
        access.refresh()
        assertEquals(StorageAccessState.NeedsSettings, access.state.value)

        shadowOf(app).grantPermissions(*permissions)
        access.refresh()
        assertEquals(StorageAccessState.Granted, access.state.value)
    }
}
