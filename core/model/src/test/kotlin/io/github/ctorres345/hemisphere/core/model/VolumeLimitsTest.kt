package io.github.ctorres345.hemisphere.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VolumeLimitsTest {
    @Test
    fun `FAT32 rejects 5 GB and accepts small files`() {
        assertEquals(FsError.FileTooLarge, VolumeLimits.FAT32.check("a.bin", 5L * 1024 * 1024 * 1024))
        assertNull(VolumeLimits.FAT32.check("a.bin", 1024))
    }

    @Test
    fun `FAT32 limit is 4 GiB minus one byte`() {
        val fourGiB = 4L * 1024 * 1024 * 1024
        assertNull(VolumeLimits.FAT32.check("a.bin", fourGiB - 1))
        assertEquals(FsError.FileTooLarge, VolumeLimits.FAT32.check("a.bin", fourGiB))
    }

    @Test
    fun `FAT32 rejects empty names, control characters and a trailing dot or space`() {
        for (name in listOf("", "a\u0001b", "a/b", "name.", "name ")) {
            assertEquals(name, FsError.InvalidName, VolumeLimits.FAT32.check(name))
        }
        assertNull(VolumeLimits.FAT32.check(".hidden"))
    }

    @Test
    fun `FAT32 rejects each forbidden character`() {
        for (c in listOf(':', '?', '"', '*', '|', '<', '>', '\\')) {
            assertEquals("$c", FsError.InvalidName, VolumeLimits.FAT32.check("a${c}b"))
        }
    }

    @Test
    fun `NONE accepts any size and most characters`() {
        assertNull(VolumeLimits.NONE.check("a:b*c", Long.MAX_VALUE))
        assertNull(VolumeLimits.NONE.check("x".repeat(300)))
    }

    @Test
    fun `every volume rejects separators, NUL, dot and dot-dot`() {
        for (name in listOf("a/b", "a\u0000b", ".", "..", "")) {
            assertEquals(name, FsError.InvalidName, VolumeLimits.NONE.check(name))
        }
    }

    @Test
    fun `FAT32 names are at most 255 units`() {
        assertNull(VolumeLimits.FAT32.check("x".repeat(255)))
        assertEquals(FsError.InvalidName, VolumeLimits.FAT32.check("x".repeat(256)))
    }
}
