package io.github.ctorres345.hemisphere.core.fs.testing

import io.github.ctorres345.hemisphere.core.fs.FileSystemProvider
import io.github.ctorres345.hemisphere.core.fs.WriteMode
import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.VolumeId
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.IOException
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

/**
 * The contract suite against [InMemoryProvider], name-addressed and id-addressed, each with
 * case-sensitive and case-insensitive names. The tests after the inherited ones cover what only
 * the fake offers: injected errors and stalls, and space.
 */
@RunWith(Parameterized::class)
@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryProviderContractTest(
    private val idAddressed: Boolean,
    private val caseInsensitive: Boolean,
) : FileSystemProviderContract() {
    private val volume = VolumeId("mem")
    private val memory by lazy { provider as InMemoryProvider }

    override fun newProvider(): FileSystemProvider =
        InMemoryProvider(idAddressed = idAddressed, caseInsensitive = caseInsensitive)

    override fun newRoot(): FsPath = FsPath.root(volume, caseInsensitive, idAddressed)

    // --- injected errors: values, never thrown -------------------------------------------------

    @Test
    fun `an injected error comes back as Err for every call`() =
        runTest {
            val path = write(root.child("a.txt"), "x")
            val error = FsError.Io("injected")
            val calls: Map<FsCall, suspend () -> FsResult<*>> =
                mapOf(
                    FsCall.Stat to { provider.stat(path) },
                    FsCall.OpenRead to { provider.openRead(path) },
                    FsCall.OpenWrite to { provider.openWrite(root.child("b.txt"), WriteMode.Create) },
                    FsCall.CreateDirectory to { provider.createDirectory(root.child("d")) },
                    FsCall.Rename to { provider.rename(path, "c.txt") },
                    FsCall.Move to { provider.move(path, root.child("m.txt")) },
                    FsCall.Copy to { provider.copy(path, root.child("k.txt")) },
                    FsCall.SetTimes to { provider.setTimes(path, Instant.EPOCH) },
                    FsCall.FreeSpace to { provider.freeSpace(volume) },
                    FsCall.Delete to { provider.delete(path) },
                )

            calls.forEach { (call, invoke) ->
                memory.failNext(call, error)
                assertEquals("$call", FsResult.Err(error), invoke())
            }

            // Each error was used once: the entry is untouched and the next calls work.
            assertEquals("x", read(path))
            assertTrue(provider.rename(path, "c.txt") is FsResult.Ok)
        }

    @Test
    fun `an injected error on list is emitted and ends the flow`() =
        runTest {
            memory.failNext(FsCall.List, FsError.AccessDenied)

            assertEquals(listOf(FsResult.Err(FsError.AccessDenied)), provider.list(root).toList())
            assertTrue(provider.list(root).toList().none { it is FsResult.Err })
        }

    @Test
    fun `an injected error fails the call only the given number of times, and leaves state alone`() =
        runTest {
            memory.failNext(FsCall.CreateDirectory, FsError.ReadOnly, times = 2)

            assertEquals(FsResult.Err(FsError.ReadOnly), provider.createDirectory(root.child("d")))
            assertEquals(FsResult.Err(FsError.ReadOnly), provider.createDirectory(root.child("d")))
            assertTrue(provider.createDirectory(root.child("d")) is FsResult.Ok)
        }

    @Test
    fun `a listing can fail part-way after some batches`() =
        runTest {
            val batched = InMemoryProvider(batchSize = 1, idAddressed = idAddressed, caseInsensitive = caseInsensitive)
            batched.addFolder(root, file("a"), file("b"), file("c"))
            batched.failNext(root, FsError.Io("disk"), failAfterBatches = 2)

            val emitted = batched.list(root).toList()

            assertEquals(2, emitted.count { it is FsResult.Ok })
            assertEquals(FsResult.Err(FsError.Io("disk")), emitted.last())
        }

    // --- stalls --------------------------------------------------------------------------------

    @Test
    fun `a stalled call waits until released and then completes`() =
        runTest {
            val path = write(root.child("a.txt"), "x")
            val stall = memory.stallNext(FsCall.Stat)

            val result = async(start = CoroutineStart.UNDISPATCHED) { provider.stat(path) }
            stall.awaitReached()
            assertFalse(result.isCompleted)

            stall.release()
            assertTrue(result.await() is FsResult.Ok)
        }

    @Test
    fun `a stalled call can be cancelled`() =
        runTest {
            val path = write(root.child("a.txt"), "x")
            val stall = memory.stallNext(FsCall.OpenRead)

            val call = async { provider.openRead(path) }
            stall.awaitReached()
            call.cancel()
            advanceUntilIdle()

            assertTrue(runCatching { call.await() }.exceptionOrNull() is CancellationException)
            assertEquals("x", read(path)) // the next call is not stalled
        }

    @Test
    fun `a stalled listing holds back its first batch`() =
        runTest {
            val stall = memory.stallNext(FsCall.List)
            val batches = mutableListOf<FsResult<*>>()

            val job = launch { provider.list(root).collect { batches += it } }
            stall.awaitReached()
            assertTrue(batches.isEmpty())
            stall.release()
            job.join()

            assertEquals(0, batches.count { it is FsResult.Err })
        }

    // --- space ---------------------------------------------------------------------------------

    @Test
    fun `free space is null until a capacity is set, then shrinks with the files`() =
        runTest {
            assertEquals(FsResult.Ok(null), provider.freeSpace(volume))
            memory.setCapacity(volume, 10)

            write(root.child("a.txt"), "1234")

            assertEquals(FsResult.Ok(6L), provider.freeSpace(volume))
        }

    @Test
    fun `a size hint or a copy that does not fit is NoSpace and changes nothing`() =
        runTest {
            val path = write(root.child("a.txt"), "12345678")
            memory.setCapacity(volume, 10)

            assertEquals(
                FsResult.Err(FsError.NoSpace),
                provider.openWrite(root.child("b.txt"), WriteMode.Create, sizeHint = 3),
            )
            assertEquals(FsResult.Err(FsError.NoSpace), provider.copy(path, root.child("c.txt")))

            assertEquals(
                1,
                provider
                    .list(root)
                    .toList()
                    .flatMap { (it as FsResult.Ok).value }
                    .size,
            )
            assertTrue(provider.openWrite(path, WriteMode.Overwrite, sizeHint = 10) is FsResult.Ok)
        }

    @Test
    fun `append space counts only the added bytes, and a close past the capacity throws and keeps the file`() =
        runTest {
            val path = write(root.child("a.txt"), "12345678")
            memory.setCapacity(volume, 10)

            assertEquals(FsResult.Err(FsError.NoSpace), provider.openWrite(path, WriteMode.Append, sizeHint = 3))
            val sink = provider.openWrite(path, WriteMode.Append).value()
            sink.write("123".toByteArray())

            assertThrows(IOException::class.java) { sink.close() }
            assertEquals("12345678", read(path))
        }

    @Test
    fun `Overwrite empties the file at open, before the sink is closed`() =
        runTest {
            val path = write(root.child("a.txt"), "old")

            val sink = provider.openWrite(path, WriteMode.Overwrite).value()

            assertEquals("", read(path))
            sink.use { it.write("new".toByteArray()) }
            assertEquals("new", read(path))
        }

    // --- stream faults -------------------------------------------------------------------------

    @Test
    fun `an injected read fault throws from the next source only`() =
        runTest {
            val path = write(root.child("a.txt"), "x")
            memory.failNextStream(StreamFault.Read)

            val source = provider.openRead(path).value()

            assertThrows(IOException::class.java) { source.use { it.read() } }
            assertEquals("x", read(path))
        }

    @Test
    fun `injected write and close faults throw and leave the old content`() =
        runTest {
            val path = write(root.child("a.txt"), "old")
            memory.failNextStream(StreamFault.Write)
            memory.failNextStream(StreamFault.Close)

            val failingWrite = provider.openWrite(root.child("b.txt"), WriteMode.Create).value()
            assertThrows(IOException::class.java) { failingWrite.write(1) }
            failingWrite.close()
            val failingClose = provider.openWrite(path, WriteMode.Append).value()
            failingClose.write("new".toByteArray())
            assertThrows(IOException::class.java) { failingClose.close() }

            assertEquals("", read(root.child("b.txt")))
            assertEquals("old", read(path))
        }

    @Test
    fun `a stream fault waits for an open that succeeds`() =
        runTest {
            val path = write(root.child("a.txt"), "x")
            memory.failNextStream(StreamFault.Read)

            assertEquals(FsResult.Err(FsError.NotFound), provider.openRead(root.child("missing")))

            val source = provider.openRead(path).value()
            assertThrows(IOException::class.java) { source.use { it.read() } }
        }

    @Test
    fun `an injected call error leaves a pending listing failure for the next listing`() =
        runTest {
            memory.failNext(root, FsError.Io("disk"))
            memory.failNext(FsCall.List, FsError.AccessDenied)

            assertEquals(listOf(FsResult.Err(FsError.AccessDenied)), provider.list(root).toList())
            assertEquals(listOf(FsResult.Err(FsError.Io("disk"))), provider.list(root).toList())
        }

    @Test
    fun `on an id-addressed volume openWrite with a ref writes the entry the ref names`() =
        runTest {
            assumeTrue(idAddressed)
            val path = write(root.child("a.txt"), "old")
            provider.rename(path, "b.txt").value()

            provider.openWrite(path, WriteMode.Overwrite).value().use { it.write("new".toByteArray()) }

            assertEquals("new", read(root.child("b.txt")))
            assertEquals(listOf("b.txt"), names(root))
        }

    // --- capabilities --------------------------------------------------------------------------

    @Test
    fun `turning a capability off makes its calls Unsupported`() =
        runTest {
            val path = write(root.child("a.txt"), "x")
            memory.capabilities =
                memory.capabilities.copy(move = false, serverCopy = false, setTimes = false, resume = false)

            assertEquals(FsResult.Err(FsError.Unsupported), provider.move(path, root.child("b.txt")))
            assertEquals(FsResult.Err(FsError.Unsupported), provider.copy(path, root.child("b.txt")))
            assertEquals(FsResult.Err(FsError.Unsupported), provider.setTimes(path, Instant.EPOCH))
            assertEquals(FsResult.Err(FsError.Unsupported), provider.openWrite(path, WriteMode.Append))
            assertEquals(FsResult.Err(FsError.Unsupported), provider.openRead(path, offset = 1))
        }

    @Test
    fun `the provider reports the identity mode it was built with`() {
        assertEquals(idAddressed, provider.capabilities.idAddressed)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "idAddressed={0}, caseInsensitive={1}")
        fun modes(): List<Array<Boolean>> =
            listOf(false, true).flatMap { id -> listOf(false, true).map { ci -> arrayOf(id, ci) } }
    }
}
