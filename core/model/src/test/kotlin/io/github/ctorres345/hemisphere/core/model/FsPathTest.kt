package io.github.ctorres345.hemisphere.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FsPathTest {
    private val volume = VolumeId("primary")

    @Test
    fun `paths are equal by key, whatever their ref`() {
        val a = FsPath(volume, listOf("Download"), ProviderRef("doc:1"))
        val b = FsPath(volume, listOf("Download"), ProviderRef("doc:2"))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `child adds one segment and names it`() {
        val child = FsPath.root(volume).child("Music")
        assertEquals(listOf("Music"), child.segments)
        assertEquals("Music", child.name)
    }

    @Test
    fun `the root has no name`() {
        assertNull(FsPath.root(volume).name)
    }
}
