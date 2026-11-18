package io.github.ctorres345.hemisphere.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FsPathSerializationTest {
    private val volume = VolumeId("primary")

    private fun roundTrip(path: FsPath) {
        val back = (decodeFsPath(path.encode()) as FsResult.Ok).value
        assertEquals(path, back)
        assertEquals(path.caseInsensitive, back.caseInsensitive)
        assertEquals(path.idAddressed, back.idAddressed)
        assertEquals(path.ref, back.ref)
        assertEquals(path.segments, back.segments)
        assertEquals(path.volume, back.volume)
    }

    @Test
    fun `round trips with and without ref, both case modes`() {
        for (ci in listOf(true, false)) {
            roundTrip(FsPath.root(volume, ci))
            roundTrip(FsPath.root(volume, ci).child("Music").child("a.txt"))
            roundTrip(FsPath(volume, listOf("Music"), ProviderRef("doc:1"), ci))
            roundTrip(FsPath.root(volume, ci, idAddressed = true).child("Music", ProviderRef("doc:1")))
        }
    }

    @Test
    fun `awkward characters round trip`() {
        roundTrip(FsPath(VolumeId("v|1/%"), listOf("a/b", "c|d", "100%", "", "naïve 日本\n"), ProviderRef("r|/%~"), true))
    }

    @Test
    fun `v1 text is pinned`() {
        assertEquals("v1|0|primary||", FsPath.root(volume).encode())
        assertEquals(
            "v1|1|primary|~doc%7C1|/Music/A%2FB.txt",
            FsPath(volume, listOf("Music", "A/B.txt"), ProviderRef("doc|1"), true).encode(),
        )
        assertEquals("v1|3|primary|~d|", FsPath(volume, emptyList(), ProviderRef("d"), true, true).encode())
        assertEquals("v1|k|primary|/music", EntryKey.ByPath(volume, listOf("music")).encode())
        assertEquals("v1|i|primary|doc%7C1", EntryKey.ById(volume, ProviderRef("doc|1")).encode())
    }

    @Test
    fun `entry keys round trip`() {
        val keys =
            listOf(
                EntryKey.ByPath(volume, listOf("a", "b|c")),
                EntryKey.ByPath(volume, emptyList()),
                EntryKey.ById(volume, ProviderRef("x/y")),
            )
        for (k in keys) assertEquals(k, (decodeEntryKey(k.encode()) as FsResult.Ok).value)
    }

    @Test
    fun `unknown version and garbage are errors, not crashes`() {
        for (s in listOf(
            "v9|0|primary||",
            "",
            "garbage",
            "v1|4|p||",
            "v1|01|p||",
            "v1|x|p||",
            "v1|0|p|x|",
            "v1|0|p||a",
            "v1|0|p||/%zz",
            "v1|0|p||/%",
            "v1|0|p",
        )) {
            assertTrue(s, decodeFsPath(s) is FsResult.Err)
        }
        for (s in listOf("v9|k|p|", "v1|z|p|", "v1|k|p|a", "")) {
            assertTrue(s, decodeEntryKey(s) is FsResult.Err)
        }
    }
}
