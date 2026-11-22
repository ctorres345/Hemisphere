package io.github.ctorres345.hemisphere.core.fs.testing

import io.github.ctorres345.hemisphere.core.fs.FileSystemProvider
import io.github.ctorres345.hemisphere.core.fs.WriteMode
import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.ProviderRef
import io.github.ctorres345.hemisphere.core.model.VolumeId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.IOException
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

/**
 * What every [FileSystemProvider] promises (architecture §3). A provider's test class extends this
 * and says how to build an empty one. `InMemoryProvider` runs it now; the local and SAF providers
 * will run it once their test has a module that may use this one (architecture §2), so a use case
 * that passes on `InMemoryProvider` meets the same behavior on a real device.
 *
 * Each test gets a new provider from [newProvider]. Tests that need a capability the provider does
 * not have are skipped (JUnit assumption); a provider without `move` must instead return
 * `Unsupported`, which a test here checks.
 *
 * The rules pinned here are the ones where a wrong answer loses a file: `rename`, `move`, `copy`
 * and `openWrite(Create)` onto a taken name return `AlreadyExists` and change nothing.
 */
abstract class FileSystemProviderContract {
    /** A new provider whose volume at [root] is empty. */
    protected abstract fun newProvider(): FileSystemProvider

    /** The root of the empty volume, built with the volume's case and id flags (`FsPath.root(...)`). */
    protected abstract fun newRoot(): FsPath

    protected val provider: FileSystemProvider by lazy { newProvider() }
    protected val root: FsPath by lazy { newRoot() }
    private val caps get() = provider.capabilities

    protected fun <T> FsResult<T>.value(): T =
        when (this) {
            is FsResult.Ok -> value
            is FsResult.Err -> throw AssertionError("expected Ok but got $error")
        }

    protected fun assertError(
        expected: FsError,
        result: FsResult<*>,
    ) = assertEquals(FsResult.Err(expected), result)

    /** The path of [entry] inside [parent], with the ref the provider gave it. */
    protected fun FsPath.at(entry: Entry): FsPath = child(entry.name, entry.ref)

    protected suspend fun write(
        path: FsPath,
        text: String,
        mode: WriteMode = WriteMode.Create,
    ): FsPath {
        provider.openWrite(path, mode, text.length.toLong()).value().use { it.write(text.toByteArray()) }
        return path.withRefOfStat()
    }

    /**
     * After a write the entry has a ref on an id-addressed volume: look it up by name, return the path
     * that carries it.
     */
    private suspend fun FsPath.withRefOfStat(): FsPath =
        provider.stat(this).value().let { entry -> FsPath(volume, segments, entry.ref, caseInsensitive, idAddressed) }

    protected suspend fun read(
        path: FsPath,
        offset: Long = 0,
    ): String = provider.openRead(path, offset).value().use { it.readBytes().decodeToString() }

    private suspend fun makeFolder(name: String): FsPath = root.at(provider.createDirectory(root.child(name)).value())

    /** The names listed in [dir], sorted, so a test sees an entry that appeared or vanished by name. */
    protected suspend fun names(dir: FsPath): List<String> =
        provider
            .list(dir)
            .toList()
            .flatMap { it.value() }
            .map { it.name }
            .sorted()

    /** [path] looked up by its name only, so a provider that answered by ref is checked by name too. */
    private fun FsPath.byName(): FsPath = FsPath(volume, segments, null, caseInsensitive, idAddressed)

    @Test
    fun `the root is a folder`() =
        runTest {
            assertEquals(Entry.Kind.Folder, provider.stat(root).value().kind)
        }

    @Test
    fun `a missing entry is NotFound for stat, openRead and delete`() =
        runTest {
            val missing = root.child("missing")

            assertError(FsError.NotFound, provider.stat(missing))
            assertError(FsError.NotFound, provider.openRead(missing))
            assertError(FsError.NotFound, provider.delete(missing))
        }

    @Test
    fun `createDirectory makes a folder that stat and list report`() =
        runTest {
            val entry = provider.createDirectory(root.child("docs")).value()

            assertEquals("docs", entry.name)
            assertEquals(Entry.Kind.Folder, entry.kind)
            assertEquals(entry.kind, provider.stat(root.at(entry)).value().kind)
            assertEquals(
                listOf("docs"),
                provider
                    .list(root)
                    .toList()
                    .flatMap { it.value() }
                    .map { it.name },
            )
        }

    @Test
    fun `createDirectory on a taken name is AlreadyExists, whatever the taker is`() =
        runTest {
            val taken = makeFolder("taken")
            write(root.child("file"), "x")

            assertError(FsError.AlreadyExists, provider.createDirectory(taken))
            assertError(FsError.AlreadyExists, provider.createDirectory(root.child("file")))
        }

    @Test
    fun `createDirectory needs its parent`() =
        runTest {
            assertError(FsError.NotFound, provider.createDirectory(root.child("no").child("deeper")))
        }

    @Test
    fun `a name the volume refuses is InvalidName`() =
        runTest {
            assertError(FsError.InvalidName, provider.createDirectory(root.child("a/b")))
            assertError(FsError.InvalidName, provider.createDirectory(root.child("..")))
            assertError(FsError.InvalidName, provider.openWrite(root.child(""), WriteMode.Create))
        }

    @Test
    fun `list gives every child over any number of batches and ends`() =
        runTest {
            write(root.child("a.txt"), "1")
            write(root.child("b.txt"), "22")
            makeFolder("c")

            val entries = provider.list(root).toList().flatMap { it.value() }

            assertEquals(listOf("a.txt", "b.txt", "c"), entries.map { it.name }.sorted())
            assertEquals(2L, entries.single { it.name == "b.txt" }.size)
        }

    @Test
    fun `list of a missing folder is NotFound and of a file is Unsupported`() =
        runTest {
            write(root.child("file"), "x")

            assertEquals(listOf(FsResult.Err(FsError.NotFound)), provider.list(root.child("missing")).toList())
            assertEquals(listOf(FsResult.Err(FsError.Unsupported)), provider.list(root.child("file")).toList())
        }

    @Test
    fun `bytes written are read back after close`() =
        runTest {
            val path = write(root.child("a.txt"), "hello")

            assertEquals("hello", read(path))
            assertEquals(5L, provider.stat(path).value().size)
            assertEquals(Entry.Kind.File, provider.stat(path).value().kind)
        }

    @Test
    fun `openRead from an offset skips that many bytes when the provider can resume`() =
        runTest {
            assumeTrue(caps.resume)
            val path = write(root.child("a.txt"), "hello")

            assertEquals("llo", read(path, offset = 2))
            assertEquals("", read(path, offset = 5))
            assertTrue(provider.openRead(path, offset = 6) is FsResult.Err)
        }

    @Test
    fun `openRead from an offset is Unsupported without resume`() =
        runTest {
            assumeTrue(!caps.resume)
            val path = write(root.child("a.txt"), "hello")

            assertError(FsError.Unsupported, provider.openRead(path, offset = 2))
        }

    @Test
    fun `openRead of a folder is Unsupported`() =
        runTest {
            assertError(FsError.Unsupported, provider.openRead(makeFolder("d")))
        }

    @Test
    fun `openWrite Create on an existing name is AlreadyExists and changes nothing`() =
        runTest {
            val path = write(root.child("a.txt"), "original")

            assertError(FsError.AlreadyExists, provider.openWrite(path, WriteMode.Create))
            assertError(FsError.AlreadyExists, provider.openWrite(root.child("a.txt"), WriteMode.Create, sizeHint = 3))

            assertEquals("original", read(path))
            assertEquals("original", read(path.byName()))
            assertEquals(listOf("a.txt"), names(root))
        }

    @Test
    fun `openWrite Overwrite replaces the whole content and creates a missing entry`() =
        runTest {
            val path = write(root.child("a.txt"), "a long first content")

            write(path, "short", WriteMode.Overwrite)
            val created = write(root.child("new.txt"), "fresh", WriteMode.Overwrite)

            assertEquals("short", read(path))
            assertEquals("fresh", read(created))
        }

    @Test
    fun `openWrite Append adds to the end when the provider can resume`() =
        runTest {
            assumeTrue(caps.resume)
            val path = write(root.child("a.txt"), "part1-")

            write(path, "part2", WriteMode.Append)

            assertEquals("part1-part2", read(path))
            assertError(FsError.NotFound, provider.openWrite(root.child("none"), WriteMode.Append))
        }

    @Test
    fun `openWrite Append is Unsupported without resume`() =
        runTest {
            assumeTrue(!caps.resume)
            val path = write(root.child("a.txt"), "x")

            assertError(FsError.Unsupported, provider.openWrite(path, WriteMode.Append))
        }

    @Test
    fun `openWrite needs an existing folder as parent and cannot write over a folder`() =
        runTest {
            val dir = makeFolder("d")

            assertError(FsError.NotFound, provider.openWrite(root.child("no").child("a"), WriteMode.Create))
            assertError(FsError.Unsupported, provider.openWrite(dir, WriteMode.Overwrite))
        }

    @Test
    fun `close is idempotent and a write after close fails with IOException`() =
        runTest {
            val sink = provider.openWrite(root.child("a.txt"), WriteMode.Create).value()
            sink.write("abc".toByteArray())
            sink.close()
            sink.close()

            try {
                sink.write(1)
                fail("a write after close must throw")
            } catch (expected: IOException) {
                assertNotNull(expected)
            }
            assertEquals("abc", read(root.child("a.txt")))
        }

    @Test
    fun `rename keeps the content under the new name`() =
        runTest {
            val path = write(root.child("old.txt"), "data")

            val renamed = provider.rename(path, "new.txt").value()

            assertEquals("new.txt", renamed.name)
            assertEquals("data", read(root.at(renamed)))
            assertError(FsError.NotFound, provider.stat(root.child("old.txt")))
        }

    @Test
    fun `rename onto an existing name is AlreadyExists and both entries keep their bytes`() =
        runTest {
            val first = write(root.child("a.txt"), "first")
            val second = write(root.child("b.txt"), "second")

            assertError(FsError.AlreadyExists, provider.rename(first, "b.txt"))

            assertEquals(listOf("a.txt", "b.txt"), names(root))
            assertEquals("first", read(first.byName()))
            assertEquals("second", read(second.byName()))
        }

    @Test
    fun `rename with a bad name or a missing entry fails`() =
        runTest {
            val path = write(root.child("a.txt"), "x")

            assertError(FsError.InvalidName, provider.rename(path, "x/y"))
            assertError(FsError.InvalidName, provider.rename(path, ""))
            assertError(FsError.NotFound, provider.rename(root.child("missing"), "z"))
        }

    @Test
    fun `move puts the entry under the new path`() =
        runTest {
            assumeTrue(caps.move)
            val path = write(root.child("a.txt"), "data")
            val target = makeFolder("target")

            val moved = provider.move(path, target.child("b.txt")).value()

            assertEquals("b.txt", moved.name)
            assertEquals("data", read(target.child("b.txt", moved.ref)))
            assertError(FsError.NotFound, provider.stat(root.child("a.txt")))
        }

    @Test
    fun `move onto an existing name is AlreadyExists and both entries keep their bytes`() =
        runTest {
            assumeTrue(caps.move)
            val first = write(root.child("a.txt"), "first")
            val target = makeFolder("target")
            val second = write(target.child("a.txt"), "second")

            assertError(FsError.AlreadyExists, provider.move(first, target.child("a.txt")))

            assertEquals(listOf("a.txt", "target"), names(root))
            assertEquals(listOf("a.txt"), names(target))
            assertEquals("first", read(first.byName()))
            assertEquals("second", read(second.byName()))
        }

    @Test
    fun `move of a folder carries its children and refuses to go into itself`() =
        runTest {
            assumeTrue(caps.move)
            val outer = makeFolder("outer")
            write(outer.child("inner.txt"), "in")
            val target = makeFolder("target")

            provider.move(outer, target.child("outer")).value()
            assertError(FsError.InvalidName, provider.move(target, target.child("outer").child("target")))

            assertEquals("in", read(target.child("outer").child("inner.txt")))
        }

    @Test
    fun `move is Unsupported without the move capability`() =
        runTest {
            assumeTrue(!caps.move)
            val path = write(root.child("a.txt"), "x")

            assertError(FsError.Unsupported, provider.move(path, root.child("b.txt")))
        }

    @Test
    fun `move to another volume is Unsupported`() =
        runTest {
            val path = write(root.child("a.txt"), "x")
            val elsewhere =
                FsPath(VolumeId("other-volume"), listOf("a.txt"), null, root.caseInsensitive, root.idAddressed)

            assertError(FsError.Unsupported, provider.move(path, elsewhere))
            assertEquals("x", read(path))
        }

    @Test
    fun `copy leaves the source and makes a second file`() =
        runTest {
            assumeTrue(caps.serverCopy)
            val source = write(root.child("a.txt"), "data")

            val copy = provider.copy(source, root.child("b.txt")).value()

            assertEquals("data", read(source))
            assertEquals("data", read(root.at(copy)))
        }

    @Test
    fun `copy onto an existing name is AlreadyExists and both entries keep their bytes`() =
        runTest {
            assumeTrue(caps.serverCopy)
            val first = write(root.child("a.txt"), "first")
            val second = write(root.child("b.txt"), "second")

            assertError(FsError.AlreadyExists, provider.copy(first, root.child("b.txt")))

            assertEquals(listOf("a.txt", "b.txt"), names(root))
            assertEquals("first", read(first.byName()))
            assertEquals("second", read(second.byName()))
        }

    @Test
    fun `copy of a folder is Unsupported`() =
        runTest {
            assumeTrue(caps.serverCopy)
            val dir = makeFolder("d")

            assertError(FsError.Unsupported, provider.copy(dir, root.child("e")))
        }

    @Test
    fun `copy is Unsupported without the serverCopy capability`() =
        runTest {
            assumeTrue(!caps.serverCopy)
            val path = write(root.child("a.txt"), "x")

            assertError(FsError.Unsupported, provider.copy(path, root.child("b.txt")))
        }

    @Test
    fun `setTimes changes the modified time when the provider can`() =
        runTest {
            assumeTrue(caps.setTimes)
            val path = write(root.child("a.txt"), "x")
            val when1 = Instant.parse("2020-01-02T03:04:05Z")

            provider.setTimes(path, when1).value()

            assertEquals(when1, provider.stat(path).value().modified)
        }

    @Test
    fun `setTimes is Unsupported without the setTimes capability`() =
        runTest {
            assumeTrue(!caps.setTimes)
            val path = write(root.child("a.txt"), "x")

            assertError(FsError.Unsupported, provider.setTimes(path, Instant.EPOCH))
        }

    @Test
    fun `freeSpace is null or not negative`() =
        runTest {
            val free = provider.freeSpace(root.volume).value()

            assertTrue(free == null || free >= 0)
        }

    @Test
    fun `delete removes a file, and a folder with everything in it`() =
        runTest {
            val file = write(root.child("a.txt"), "x")
            val dir = makeFolder("d")
            write(dir.child("inner.txt"), "y")

            provider.delete(file).value()
            provider.delete(dir).value()

            assertEquals(emptyList<Entry>(), provider.list(root).toList().flatMap { it.value() })
            assertError(FsError.NotFound, provider.delete(file))
        }

    // Real watchers run on their own threads, so this test waits in real time, not on the test clock.
    @Suppress("InjectDispatcher")
    @Test
    fun `observe signals a change directly in the folder when the provider can watch`() =
        runTest {
            assumeTrue(caps.watch)
            val dir = makeFolder("watched")
            val signal = CompletableDeferred<Unit>()
            val watcher = launch(Dispatchers.Default) { signal.complete(provider.observe(dir).first()) }

            // Real time, not the test clock, and keep changing the folder: a real watcher needs a moment
            // to start, and a change made before it listens is not one it has to report.
            val seen =
                withContext(Dispatchers.Default) {
                    withTimeoutOrNull(WAIT_MS) {
                        var n = 0
                        while (!signal.isCompleted) {
                            write(dir.child("new${n++}.txt"), "x")
                            delay(POLL_MS)
                        }
                    }
                }
            watcher.cancel()

            assertNotNull("observe sent nothing within $WAIT_MS ms", seen)
        }

    @Test
    fun `a cancelled caller gets CancellationException, never an Err`() =
        runTest {
            val path = write(root.child("a.txt"), "x")
            val calls: List<suspend () -> FsResult<*>> =
                listOf(
                    { provider.stat(path) },
                    { provider.openRead(path).also { (it as? FsResult.Ok)?.value?.close() } },
                    { provider.list(root).first() },
                    { provider.createDirectory(root.child("d")) },
                )

            calls.forEach { call ->
                // Kept from inside the scope: the cancelled scope throws on exit whatever the call returned.
                var returned: FsResult<*>? = null
                val thrown =
                    runCatching {
                        coroutineScope {
                            cancel()
                            returned = call()
                        }
                    }.exceptionOrNull()

                assertFalse("cancellation became $returned", returned is FsResult.Err)
                assertTrue("unexpected $thrown", thrown is CancellationException)
            }
        }

    @Test
    fun `on a name-addressed volume a stale ref is ignored and the name decides`() =
        runTest {
            assumeTrue(!caps.idAddressed)
            write(root.child("a.txt"), "x")
            val stale = root.child("a.txt", ProviderRef("stale"))

            assertEquals("a.txt", provider.stat(stale).value().name)
            assertEquals("x", read(stale))
            val renamed = provider.rename(stale, "b.txt").value()
            provider.delete(root.child("b.txt", ProviderRef("stale"))).value()

            assertEquals("b.txt", renamed.name)
            assertEquals(emptyList<String>(), names(root))
        }

    @Test
    fun `names that differ only by case collide on a case-insensitive volume and not otherwise`() =
        runTest {
            val upper = write(root.child("Name.txt"), "upper")
            val second = provider.openWrite(root.child("name.txt"), WriteMode.Create)

            if (root.caseInsensitive) {
                assertError(FsError.AlreadyExists, second)
                assertEquals("upper", read(upper))
                assertEquals("upper", read(root.child("NAME.TXT", upper.ref)))
            } else {
                second.value().use { it.write("lower".toByteArray()) }
                assertEquals("upper", read(upper))
                assertEquals("lower", read(root.child("name.txt")))
            }
        }

    @Test
    fun `rename onto a name that differs only by case follows the volume's case rule`() =
        runTest {
            val first = write(root.child("a.txt"), "first")
            write(root.child("B.txt"), "second")

            val result = provider.rename(first, "b.txt")

            if (root.caseInsensitive) {
                assertError(
                    FsError.AlreadyExists,
                    result,
                )
            } else {
                assertEquals("b.txt", result.value().name)
            }
        }

    @Test
    fun `a case-only rename of the entry itself is allowed on a case-insensitive volume`() =
        runTest {
            assumeTrue(root.caseInsensitive)
            val path = write(root.child("a.txt"), "x")

            val renamed = provider.rename(path, "A.txt").value()

            assertEquals("A.txt", renamed.name)
            assertEquals("x", read(root.child("A.txt")))
        }

    @Test
    fun `on an id-addressed volume a rename keeps the ref, and the old path still finds the entry`() =
        runTest {
            assumeTrue(root.idAddressed)
            val path = write(root.child("a.txt"), "x")
            assertNotNull(path.ref)

            val renamed = provider.rename(path, "b.txt").value()

            assertEquals(path.ref, renamed.ref)
            assertEquals("x", read(path))
            assertError(FsError.NotFound, provider.stat(root.child("a.txt")))
        }

    protected companion object {
        const val WAIT_MS = 5_000L
        const val POLL_MS = 50L
    }
}
