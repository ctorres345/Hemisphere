package io.github.ctorres345.hemisphere.feature.browser.access

import org.junit.Assert.assertEquals
import org.junit.Test

class AccessTargetTest {
    @Test
    fun `Android 11 and later open the all-files page`() {
        assertEquals(AccessTarget.AllFilesSettings, AccessLaunch.Grant.target(sdkInt = 30))
        assertEquals(AccessTarget.AllFilesSettings, AccessLaunch.Grant.target(sdkInt = 36))
    }

    @Test
    fun `Android 8 to 10 show the runtime permission dialog`() {
        assertEquals(AccessTarget.RuntimePermission, AccessLaunch.Grant.target(sdkInt = 26))
        assertEquals(AccessTarget.RuntimePermission, AccessLaunch.Grant.target(sdkInt = 29))
    }

    @Test
    fun `the settings launch opens the app settings on every version`() {
        assertEquals(AccessTarget.AppSettings, AccessLaunch.AppSettings.target(sdkInt = 26))
        assertEquals(AccessTarget.AppSettings, AccessLaunch.AppSettings.target(sdkInt = 35))
    }
}
