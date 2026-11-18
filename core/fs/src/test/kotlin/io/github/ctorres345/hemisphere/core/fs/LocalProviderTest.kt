package io.github.ctorres345.hemisphere.core.fs

import io.github.ctorres345.hemisphere.core.model.Capabilities
import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.VolumeId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.AccessDeniedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant

class LocalProviderTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val volume = VolumeId("local")
    private val dispatcher = StandardTestDispatcher()

    private fun provider(batchSize: Int = 500) = LocalProvider(mapOf(volume to temp.root), dispatcher, batchSize)

    /** Reads attributes like the provider does, and counts each read by file name. */
    private class CountingReader(
        private val deletedOnRead: Set<String> = emptySet(),
        private val denied: Set<String> = emptySet(),
    ) : AttributeReader {
        val reads = mutableListOf<String>()

        override fun read(
            path: Path,
            followLinks: Boolean,
        ): BasicFileAttributes {
            val name = path.fileName.toString()
            reads += name
            if (name in deletedOnRead) Files.delete(path) // deleted between the folder read and this one
            if (name in denied) throw AccessDeniedException(path.toString())
            return DefaultAttributeReader.read(path, followLinks)
        }
    }

    private fun countingProvider(
        reader: CountingReader,
        batchSize: Int,
    ) = LocalProvider(mapOf(volume to temp.root), dispatcher, batchSize, reader)

    // The provider's dispatcher must share runTest's scheduler, or its work never runs.
    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) { block() }

    @Test
    fun `every call beyond list is Unsupported until the local-storage ticket, and capabilities are none`() =
        test {
            val local = provider()
            val path = FsPath.root(volume).child("a.txt")
            val unsupported = FsResult.Err(FsError.Unsupported)

            assertEquals(Capabilities.NONE, local.capabilities)
            assertEquals(unsupported, local.stat(path))
            assertEquals(unsupported, local.openRead(path))
            assertEquals(unsupported, local.openWrite(path, WriteMode.Create))
            assertEquals(unsupported, local.createDirectory(path))
            assertEquals(unsupported, local.rename(path, "b.txt"))
            assertEquals(unsupported, local.move(path, path.child("b")))
            assertEquals(unsupported, local.copy(path, path.child("b")))
            assertEquals(unsupported, local.setTimes(path, Instant.EPOCH))
            assertEquals(unsupported, local.freeSpace(volume))
            assertEquals(unsupported, local.delete(path))
            assertEquals(emptyList<Unit>(), local.observe(path).toList())
        }

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
    fun `a segment with a NUL character is InvalidName`() =
        test {
            val result = provider().list(FsPath(volume, listOf("a\u0000b"))).toList()
            assertEquals(listOf(FsResult.Err(FsError.InvalidName)), result)
        }

    @Test
    fun `a dot-dot segment cannot leave the root`() =
        test {
            val result = provider().list(FsPath(volume, listOf(".."))).toList()
            assertEquals(listOf(FsResult.Err(FsError.InvalidName)), result)
        }

    @Test
    fun `the first batch comes before the rest of the folder is read`() =
        test {
            repeat(5) { File(temp.root, "f$it").writeText("") }
            val reader = CountingReader()

            val first = countingProvider(reader, batchSize = 2).list(FsPath.root(volume)).first()

            assertEquals(2, (first as FsResult.Ok).value.size)
            assertEquals(2, reader.reads.size)
        }

    @Test
    fun `each entry's attributes are read once`() =
        test {
            temp.newFolder("Music")
            repeat(4) { File(temp.root, "f$it").writeText("") }
            val reader = CountingReader()

            countingProvider(reader, batchSize = 2).list(FsPath.root(volume)).toList()

            assertEquals(listOf("Music", "f0", "f1", "f2", "f3"), reader.reads.sorted())
        }

    @Test
    fun `an entry deleted while the folder is read is skipped`() =
        test {
            listOf("a", "b", "c").forEach { File(temp.root, it).writeText("") }
            val reader = CountingReader(deletedOnRead = setOf("b"))

            val results = countingProvider(reader, batchSize = 500).list(FsPath.root(volume)).toList()

            val names = results.flatMap { (it as FsResult.Ok).value }.map { it.name }
            assertEquals(listOf("a", "c"), names.sorted())
        }

    @Test
    fun `a file where a folder is asked for is Unsupported`() =
        test {
            File(temp.root, "a.txt").writeText("")
            val result = provider().list(FsPath.root(volume).child("a.txt")).toList()
            assertEquals(listOf(FsResult.Err(FsError.Unsupported)), result)
        }

    @Test
    fun `an entry whose attributes cannot be read is listed without size or date`() =
        test {
            listOf("a", "b").forEach { File(temp.root, it).writeText("x") }
            val reader = CountingReader(denied = setOf("b"))

            val entries =
                countingProvider(reader, batchSize = 500)
                    .list(FsPath.root(volume))
                    .toList()
                    .flatMap { (it as FsResult.Ok).value }

            assertEquals(Entry("b", Entry.Kind.File), entries.single { it.name == "b" })
            assertEquals(1L, entries.single { it.name == "a" }.size)
        }

    @Test
    fun `a link to a folder is listed as a folder`() =
        test {
            temp.newFolder("Music")
            Files.createSymbolicLink(temp.root.toPath().resolve("link"), temp.root.toPath().resolve("Music"))

            val entries = provider().list(FsPath.root(volume)).toList().flatMap { (it as FsResult.Ok).value }

            assertEquals(Entry.Kind.Folder, entries.single { it.name == "link" }.kind)
        }

    @Test
    fun `a link whose target is gone is still listed`() =
        test {
            Files.createSymbolicLink(temp.root.toPath().resolve("link"), temp.root.toPath().resolve("missing"))

            val entries = provider().list(FsPath.root(volume)).toList().flatMap { (it as FsResult.Ok).value }

            assertEquals(listOf("link"), entries.map { it.name })
            assertEquals(Entry.Kind.File, entries.single().kind)
        }

    @Test
    fun `an exact multiple of the batch size has no empty last batch`() =
        test {
            repeat(4) { File(temp.root, "f$it").writeText("") }
            val batches = provider(batchSize = 2).list(FsPath.root(volume)).toList()
            assertEquals(listOf(2, 2), batches.map { (it as FsResult.Ok).value.size })
        }

    @Test
    fun `an empty folder emits nothing`() =
        test {
            assertEquals(emptyList<FsResult<List<Entry>>>(), provider().list(FsPath.root(volume)).toList())
        }
}
