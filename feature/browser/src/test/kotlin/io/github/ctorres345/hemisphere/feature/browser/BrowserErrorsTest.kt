package io.github.ctorres345.hemisphere.feature.browser

import io.github.ctorres345.hemisphere.core.model.FsError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the error state shows: a message for every error, and Retry only where a second try can help. */
class BrowserErrorsTest {
    @Test
    fun `listing errors map to their own message and offer Retry`() {
        assertEquals(R.string.browser_error_not_found, FsError.NotFound.messageRes())
        assertEquals(R.string.browser_error_access_denied, FsError.AccessDenied.messageRes())
        assertEquals(R.string.browser_error_volume_unavailable, FsError.VolumeUnavailable.messageRes())
        listOf(FsError.NotFound, FsError.AccessDenied, FsError.VolumeUnavailable, FsError.Io("disk")).forEach {
            assertTrue("$it should offer Retry", it.canRetry())
        }
    }

    @Test
    fun `raw I O text never reaches the screen`() {
        assertEquals(R.string.browser_error_generic, FsError.Io("EIO: /data/secret").messageRes())
    }

    @Test
    fun `errors a retry cannot fix offer no Retry`() {
        assertFalse(FsError.InvalidName.canRetry())
        assertFalse(FsError.Unsupported.canRetry())
    }
}
