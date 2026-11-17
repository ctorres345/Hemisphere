import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManifestRulesTest {
    @Test
    fun `finds the internet permission`() {
        val manifest = """<manifest><uses-permission android:name="android.permission.INTERNET" /></manifest>"""
        assertTrue(ManifestRules.requestsInternet(manifest))
    }

    @Test
    fun `ignores other permissions`() {
        val manifest = """<manifest><uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" /></manifest>"""
        assertFalse(ManifestRules.requestsInternet(manifest))
    }
}
