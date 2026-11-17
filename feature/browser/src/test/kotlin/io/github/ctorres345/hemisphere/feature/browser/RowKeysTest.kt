package io.github.ctorres345.hemisphere.feature.browser

import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.ProviderRef
import io.github.ctorres345.hemisphere.core.model.VolumeId
import io.github.ctorres345.hemisphere.core.testing.InMemoryProvider
import io.github.ctorres345.hemisphere.core.testing.file
import io.github.ctorres345.hemisphere.core.testing.folder
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RowKeysTest {
    private fun assertUnique(keys: List<String>) = assertEquals(keys.size, keys.toSet().size)

    @Test
    fun `two entries with the same name get two keys`() {
        val keys = rowKeys(listOf(file("a.txt"), file("a.txt")))

        assertEquals(2, keys.size)
        assertUnique(keys)
    }

    @Test
    fun `three or more equal names stay unique`() {
        assertUnique(rowKeys(List(4) { file("same") }))
    }

    @Test
    fun `a file and a folder with the same name get different keys`() {
        assertUnique(rowKeys(listOf(folder("a"), file("a"))))
    }

    @Test
    fun `names that differ only in case get different keys`() {
        assertUnique(rowKeys(listOf(file("A"), file("a"))))
    }

    @Test
    fun `a name that looks like a suffixed key does not collide`() {
        assertUnique(rowKeys(listOf(file("a"), file("a"), file("a#1"))))
    }

    @Test
    fun `entries with a provider ref are keyed by it, not by position`() {
        val first = file("a.txt", ref = ProviderRef("doc:1"))
        val second = file("a.txt", ref = ProviderRef("doc:2"))

        // The same entry keeps its key when a later batch lands an equal name before it.
        assertEquals(rowKeys(listOf(second)).single(), rowKeys(listOf(first, second))[1])
        assertNotEquals(rowKeys(listOf(first)).single(), rowKeys(listOf(second)).single())
    }

    @Test
    fun `a unique name keeps its key whatever comes before it`() {
        val before = rowKeys(listOf(file("b"))).single()
        val after = rowKeys(listOf(file("a"), file("a"), file("b")))[2]

        assertEquals(before, after)
    }

    @Test
    fun `rows from every test provider have unique keys`() =
        runTest {
            val dir = FsPath.root(VolumeId("v"))
            val provider = InMemoryProvider(batchSize = 2)
            provider.addFolder(
                dir,
                file("a.txt"),
                file("a.txt"),
                folder("a.txt"),
                file("b", ref = ProviderRef("1")),
                file("b", ref = ProviderRef("2")),
            )

            val entries =
                provider
                    .list(dir)
                    .toList()
                    .flatMap { (it as FsResult.Ok<List<Entry>>).value }

            assertEquals(5, entries.size)
            assertUnique(rowKeys(entries))
        }
}
