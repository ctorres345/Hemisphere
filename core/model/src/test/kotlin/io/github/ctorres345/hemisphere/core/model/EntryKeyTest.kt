package io.github.ctorres345.hemisphere.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.Locale

class EntryKeyTest {
    private val volume = VolumeId("sd")

    @Test
    fun `case-insensitive paths fold, others do not`() {
        assertEquals(FsPath.root(volume, true).child("a.txt").key, FsPath.root(volume, true).child("A.txt").key)
        assertNotEquals(FsPath.root(volume).child("a.txt").key, FsPath.root(volume).child("A.txt").key)
    }

    @Test
    fun `folding uses Locale ROOT under a Turkish default`() {
        val old = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            val key = FsPath.root(volume, true).child("i").key
            assertEquals(EntryKey.ByPath(volume, listOf("I")), key)
        } finally {
            Locale.setDefault(old)
        }
    }

    @Test
    fun `id-addressed volumes key by volume and ref`() {
        val root = FsPath.root(volume, idAddressed = true)
        val first = root.child("a", ProviderRef("doc:1"))
        val second = root.child("a", ProviderRef("doc:2"))
        assertEquals(EntryKey.ById(volume, ProviderRef("doc:1")), first.key)
        // Two entries with one name are different entries on an id-addressed volume.
        assertNotEquals(first, second)
        assertEquals(setOf(first, second).size, 2)
        // Without a ref, or on a name-addressed volume, the key is the name.
        assertEquals(EntryKey.ByPath(volume, listOf("a")), root.child("a").key)
        assertEquals(
            FsPath.root(volume).child("a", ProviderRef("doc:1")),
            FsPath.root(volume).child("a", ProviderRef("doc:2")),
        )
    }

    @Test
    fun `folding keeps the length, so sharp s stays apart from ss`() {
        assertNotEquals(FsPath.root(volume, true).child("straße").key, FsPath.root(volume, true).child("strasse").key)
        assertNotEquals(FsPath.root(volume, true).child("ﬁle").key, FsPath.root(volume, true).child("file").key)
    }

    @Test
    fun `signs and dotted I stay apart from their letters, like on FAT`() {
        val root = FsPath.root(volume, true)
        assertNotEquals(root.child("İ.txt").key, root.child("i.txt").key)
        assertNotEquals(root.child("\u212A").key, root.child("k").key)
    }

    @Test
    fun `final sigma folds like sigma`() {
        assertEquals(FsPath.root(volume, true).child("ΑΣ").key, FsPath.root(volume, true).child("ας").key)
        assertEquals(FsPath.root(volume, true).child("ΑΣ").key, FsPath.root(volume, true).child("ασ").key)
    }
}
