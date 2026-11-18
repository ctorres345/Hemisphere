package io.github.ctorres345.hemisphere.core.fs

import io.github.ctorres345.hemisphere.core.fs.testing.FileSystemProviderContract
import io.github.ctorres345.hemisphere.core.model.Capabilities
import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.VolumeId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.cancellation.CancellationException

/**
 * The shared contract suite against [LocalProvider] on a temp folder, with the JVM watcher. The
 * volume's case rule is the temp folder's own: macOS folds case, Linux does not. The tests after the
 * inherited ones cover what only the local provider does: cancellation inside a stream, and MediaStore
 * scans. Real file I/O runs on its own threads, so the provider uses the real IO dispatcher.
 */
@Suppress("InjectDispatcher")
class LocalProviderContractTest : FileSystemProviderContract() {
    @get:Rule
    val temp = TemporaryFolder()

    private val volume = VolumeId("local")
    private val scanned = mutableListOf<String>()
    private val volumeRoot by lazy { temp.newFolder("volume") }

    override fun newProvider(): FileSystemProvider =
        LocalProvider(
            roots = mapOf(volume to volumeRoot),
            ioDispatcher = Dispatchers.IO,
            watcher = WatchServiceWatcher,
            mediaScanner = { paths -> synchronized(scanned) { scanned += paths } },
        )

    override fun newRoot(): FsPath = FsPath.root(volume, caseInsensitive = foldsCase(), idAddressed = false)

    /** True when the temp folder's file system takes `PROBE` for `probe`. */
    private fun foldsCase(): Boolean {
        val probe = File(temp.newFolder("case-probe"), "probe")
        probe.createNewFile()
        return File(probe.parentFile, "PROBE").exists()
    }

    private fun file(name: String) = File(volumeRoot, name).path

    @Test
    fun `capabilities are move, resume, setTimes and watch, without server copy`() {
        assertEquals(Capabilities(move = true, resume = true, setTimes = true, watch = true), provider.capabilities)
    }

    @Test
    fun `cancelling a read mid-stream closes the file and rethrows cancellation`() =
        runTest {
            val path = write(root.child("a.txt"), "hello")
            val opened = CompletableDeferred<InputStream>()
            val reader =
                launch {
                    opened.complete(provider.openRead(path).value())
                    awaitCancellation()
                }
            val source = opened.await()
            assertEquals('h'.code, source.read())

            reader.cancelAndJoin()

            assertThrows(CancellationException::class.java) { source.read(ByteArray(4)) }
            assertFalse((source as LocalSource).isOpen)
        }

    @Test
    fun `cancelling a write mid-stream closes the file and rethrows cancellation`() =
        runTest {
            val opened = CompletableDeferred<OutputStream>()
            val writer =
                launch {
                    opened.complete(provider.openWrite(root.child("a.txt"), WriteMode.Create).value())
                    awaitCancellation()
                }
            val sink = opened.await()
            sink.write("part".toByteArray())

            writer.cancelAndJoin()

            assertThrows(CancellationException::class.java) { sink.write(1) }
            assertFalse((sink as LocalSink).isOpen)
            sink.close() // the caller's use { } still closes it: nothing is thrown, nothing is scanned
            assertEquals(emptyList<String>(), scanned)
        }

    @Test
    fun `closing a sink after its job was cancelled neither syncs nor scans`() =
        runTest {
            val opened = CompletableDeferred<OutputStream>()
            val writer =
                launch {
                    opened.complete(provider.openWrite(root.child("a.txt"), WriteMode.Create).value())
                    awaitCancellation()
                }
            val sink = opened.await()
            sink.write("part".toByteArray())

            writer.cancelAndJoin()
            sink.close() // no write saw the cancellation first

            assertFalse((sink as LocalSink).isOpen)
            assertEquals(emptyList<String>(), scanned)
        }

    @Test
    fun `a stream opened in a block that has ended keeps working`() =
        runTest {
            val path = write(root.child("a.txt"), "hello")

            val source = withContext(Dispatchers.Default) { provider.openRead(path).value() }

            assertEquals("hello", source.use { it.readBytes().decodeToString() })
        }

    @Test
    fun `MediaStore is asked to rescan after a write, a rename, a move and a delete`() =
        runTest {
            val folder = root.child("d")
            provider.createDirectory(folder).value()
            write(root.child("a.txt"), "x")
            provider.rename(root.child("a.txt"), "b.txt").value()
            provider.move(root.child("b.txt"), folder.child("c.txt")).value()
            provider.delete(folder).value()

            assertEquals(
                listOf(
                    file("a.txt"),
                    file("a.txt"),
                    file("b.txt"),
                    file("b.txt"),
                    File(file("d"), "c.txt").path,
                    file("d"),
                ),
                scanned,
            )
        }

    @Test
    fun `Create on a taken name is AlreadyExists even when the size hint would not fit`() =
        runTest {
            write(root.child("a.txt"), "x")

            val result = provider.openWrite(root.child("a.txt"), WriteMode.Create, sizeHint = Long.MAX_VALUE / 2)

            assertEquals(FsResult.Err(FsError.AlreadyExists), result)
        }

    @Test
    fun `free space is a number for a readable volume`() =
        runTest {
            val free = provider.freeSpace(volume).value()

            assertTrue(free != null && free >= 0)
        }

    @Test
    fun `a failed rename scans nothing`() =
        runTest {
            write(root.child("a.txt"), "x")
            write(root.child("b.txt"), "y")
            scanned.clear()

            provider.rename(root.child("a.txt"), "b.txt")

            assertEquals(emptyList<String>(), scanned)
        }
}
