package io.github.ctorres345.hemisphere

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.FileInputStream
import java.io.FileNotFoundException
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Reads the merged manifest of the debug and release variants (the build merges them before this
 * runs; see `app/build.gradle.kts`) and checks the storage permissions of ADR 0002 and the no-INTERNET
 * rule of ADR 0008, including anything a library merges in.
 */
class MergedManifestTest {
    private val android = "http://schemas.android.com/apk/res/android"

    private fun manifest(variant: String): Element {
        val path =
            checkNotNull(System.getProperty("hemisphere.mergedManifest.$variant")) {
                "The build did not pass the merged $variant manifest to the test."
            }
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val input =
            try {
                FileInputStream(path)
            } catch (e: FileNotFoundException) {
                throw AssertionError("The merged $variant manifest is missing: $path", e)
            }
        return input.use { factory.newDocumentBuilder().parse(it).documentElement }
    }

    private fun Element.permissions(): Map<String, Element> =
        (0 until getElementsByTagName("uses-permission").length)
            .map { getElementsByTagName("uses-permission").item(it) as Element }
            .associateBy { it.getAttributeNS(android, "name") }

    private fun forEachVariant(check: (variant: String, manifest: Element) -> Unit) {
        listOf("debug", "release").forEach { check(it, manifest(it)) }
    }

    @Test
    fun `requests the storage permissions`() =
        forEachVariant { variant, manifest ->
            val permissions = manifest.permissions()
            listOf(
                "android.permission.MANAGE_EXTERNAL_STORAGE",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
            ).forEach { assertTrue("$variant manifest lacks $it", it in permissions) }
        }

    @Test
    fun `write permission stops at Android 10`() =
        forEachVariant { variant, manifest ->
            val write = manifest.permissions().getValue("android.permission.WRITE_EXTERNAL_STORAGE")
            assertEquals("$variant maxSdkVersion", "29", write.getAttributeNS(android, "maxSdkVersion"))
        }

    @Test
    fun `keeps legacy external storage for Android 10`() =
        forEachVariant { variant, manifest ->
            val application = manifest.getElementsByTagName("application").item(0) as Element
            assertEquals(
                "$variant requestLegacyExternalStorage",
                "true",
                application.getAttributeNS(android, "requestLegacyExternalStorage"),
            )
        }

    @Test
    fun `never requests INTERNET`() =
        forEachVariant { variant, manifest ->
            assertFalse(
                "$variant manifest requests INTERNET",
                "android.permission.INTERNET" in manifest.permissions(),
            )
        }
}
