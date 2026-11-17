package io.github.ctorres345.hemisphere.core.fs

import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.VolumeId
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LocalProviderTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val volume = VolumeId("local")
    private val dispatcher = StandardTestDispatcher()

    private fun provider(batchSize: Int = 500) = LocalProvider(mapOf(volume to temp.root), dispatcher, batchSize)

    // The provider's dispatcher must share runTest's scheduler, or its work never runs.
    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) { block() }

    @Test
    fun `lists folders and files with their kind and size`() =
        test {
            temp.newFolder("Music")
            File(temp.root, "a.txt").writeText("hello")

            val entries = provider().list(FsPath.root(volume)).toList().flatMap { (it as FsResult.Ok).value }

            assertEquals(setOf("Music", "a.txt"), entries.map { it.name }.toSet())
            assertEquals(Entry.Kind.Folder, entries.single { it.name == "Music" }.kind)
            assertEquals(5L, entries.single { it.name == "a.txt" }.size)
        }

    @Test
    fun `emits large folders in batches`() =
        test {
            repeat(5) { File(temp.root, "f$it").writeText("") }

            val batches = provider(batchSize = 2).list(FsPath.root(volume)).toList()

            assertEquals(listOf(2, 2, 1), batches.map { (it as FsResult.Ok).value.size })
        }

    @Test
    fun `a missing folder is NotFound`() =
        test {
            val result = provider().list(FsPath.root(volume).child("gone")).toList()
            assertEquals(listOf(FsResult.Err(FsError.NotFound)), result)
        }

    @Test
    fun `an unknown volume is VolumeUnavailable`() =
        test {
            val result = provider().list(FsPath.root(VolumeId("other"))).toList()
            assertEquals(listOf(FsResult.Err(FsError.VolumeUnavailable)), result)
        }

    @Test
    fun `a dot-dot segment cannot leave the root`() =
        test {
            val result = provider().list(FsPath(volume, listOf(".."))).toList()
            assertEquals(listOf(FsResult.Err(FsError.InvalidName)), result)
        }

    @Test
    fun `sample content fills only an empty home`() {
        val home = File(temp.root, "home")
        SampleContent.seedIfEmpty(home)
        assertTrue(File(home, "Music").isDirectory)

        val other = temp.newFolder("other")
        File(other, "mine.txt").writeText("")
        SampleContent.seedIfEmpty(other)
        assertEquals(listOf("mine.txt"), other.list()!!.toList())
    }
}
