package io.github.ctorres345.hemisphere.core.domain.files

import io.github.ctorres345.hemisphere.core.fs.FileSystemProvider
import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.Listing
import io.github.ctorres345.hemisphere.core.model.VolumeId
import io.github.ctorres345.hemisphere.core.testing.FakeVolumeRepository
import io.github.ctorres345.hemisphere.core.testing.InMemoryProvider
import io.github.ctorres345.hemisphere.core.testing.file
import io.github.ctorres345.hemisphere.core.testing.folder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveFolderUseCaseTest {
    private val dispatcher = StandardTestDispatcher()
    private val home = FsPath.root(FakeVolumeRepository.TEST_VOLUME)

    private fun observe(provider: InMemoryProvider) =
        ObserveFolderUseCase(FakeVolumeRepository(provider, home), dispatcher)

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) { block() }

    @Test
    fun `sorts folders first, then by name ignoring case`() =
        test {
            val provider =
                InMemoryProvider().apply {
                    addFolder(home, file("b.txt"), folder("zeta"), file("A.txt"), folder("Alpha"))
                }

            val listing = observe(provider)(home).last()

            assertEquals(listOf("Alpha", "zeta", "A.txt", "b.txt"), listing.entries.map { it.name })
            assertEquals(true, listing.complete)
        }

    @Test
    fun `hides the app's own entries`() =
        test {
            val provider =
                InMemoryProvider().apply {
                    addFolder(
                        home,
                        folder(".Trash-hemisphere"),
                        file(".hemisphere-part-1"),
                        file(".hemisphere-rename-2"),
                        file("keep"),
                    )
                }

            assertEquals(listOf("keep"), observe(provider)(home).last().entries.map { it.name })
        }

    @Test
    fun `each batch adds to what was read, complete only at the end`() =
        test {
            val provider = InMemoryProvider(batchSize = 2).apply { addFolder(home, file("c"), file("a"), file("b")) }

            val listings = observe(provider)(home).toList()

            assertEquals(
                listOf(
                    listOf("a", "c"),
                    listOf("a", "b", "c"),
                    listOf("a", "b", "c"),
                ),
                listings.map { l ->
                    l.entries.map {
                        it.name
                    }
                },
            )
            assertEquals(listOf(false, false, true), listings.map { it.complete })
        }

    @Test
    fun `an error part-way keeps the entries read so far`() =
        test {
            val provider =
                InMemoryProvider(batchSize = 1).apply {
                    addFolder(home, file("a"), file("b"))
                    failNext(home, FsError.Io("disk"), failAfterBatches = 1)
                }

            val listing = observe(provider)(home).last()

            assertEquals(Listing(listOf(file("a")), complete = true, error = FsError.Io("disk")), listing)
        }

    @Test
    fun `an empty folder completes with no entries`() =
        test {
            val provider = InMemoryProvider().apply { addFolder(home) }
            assertEquals(listOf(Listing(emptyList(), complete = true)), observe(provider)(home).toList())
        }

    @Test
    fun `a folder on an unknown volume is VolumeUnavailable`() =
        test {
            val listing = observe(InMemoryProvider())(FsPath.root(VolumeId("gone"))).toList()
            assertEquals(listOf(Listing(emptyList(), complete = true, error = FsError.VolumeUnavailable)), listing)
        }

    @OptIn(ExperimentalCoroutinesApi::class) // advanceUntilIdle
    @Test
    fun `a cancelled listing ends cancelled and reports no error`() =
        test {
            val stalls =
                object : FileSystemProvider {
                    override fun list(dir: FsPath) =
                        flow<FsResult<List<Entry>>> {
                            emit(FsResult.Ok(listOf(folder("Alpha"))))
                            awaitCancellation()
                        }
                }
            val useCase = ObserveFolderUseCase(FakeVolumeRepository(stalls, home), dispatcher)
            val seen = mutableListOf<Listing>()

            val job = launch { useCase(home).collect { seen += it } }
            advanceUntilIdle()
            job.cancel()
            advanceUntilIdle()

            assertTrue(job.isCancelled)
            assertEquals(listOf(listOf("Alpha")), seen.map { listing -> listing.entries.map { it.name } })
            assertTrue(seen.none { it.error != null || it.complete })
        }
}
