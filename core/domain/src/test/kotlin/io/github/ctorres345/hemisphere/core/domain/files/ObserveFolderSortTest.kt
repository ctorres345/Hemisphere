package io.github.ctorres345.hemisphere.core.domain.files

import io.github.ctorres345.hemisphere.core.fs.testing.FakeVolumeRepository
import io.github.ctorres345.hemisphere.core.fs.testing.InMemoryProvider
import io.github.ctorres345.hemisphere.core.fs.testing.file
import io.github.ctorres345.hemisphere.core.fs.testing.folder
import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FolderSort
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.Listing
import io.github.ctorres345.hemisphere.core.testing.FakeFolderViewRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class) // advanceUntilIdle
class ObserveFolderSortTest {
    private val dispatcher = StandardTestDispatcher()
    private val home = FsPath.root(FakeVolumeRepository.TEST_VOLUME)
    private val views = FakeFolderViewRepository()
    private val provider =
        InMemoryProvider().apply {
            addFolder(
                home,
                file("file10", size = 500),
                file("file2", size = 5),
                folder("b"),
                file("File1", size = 50),
                folder("a"),
            )
        }
    private val observe = ObserveFolderUseCase(FakeVolumeRepository(provider, home), views, dispatcher)

    private fun Listing.names() = entries.map { it.name }

    @Test
    fun `names sort naturally, folders first`() =
        runTest(dispatcher) {
            val seen = mutableListOf<Listing>()
            val job = launch { observe(home).collect { seen += it } }
            advanceUntilIdle()
            job.cancel()

            assertEquals(listOf("a", "b", "File1", "file2", "file10"), seen.last().names())
        }

    @Test
    fun `entries come out in the saved order and re-sort when the sort changes`() =
        runTest(dispatcher) {
            views.setSort(home, FolderSort(FolderSort.Field.Size, descending = true))
            val seen = mutableListOf<Listing>()
            val job = launch { observe(home).collect { seen += it } }
            advanceUntilIdle()

            assertEquals(listOf("a", "b", "file10", "File1", "file2"), seen.last().names())
            assertEquals(true, seen.last().complete)

            views.setSort(home, FolderSort(FolderSort.Field.Size))
            advanceUntilIdle()
            job.cancel()

            assertEquals(listOf("a", "b", "file2", "File1", "file10"), seen.last().names())
            assertEquals(true, seen.last().complete)
        }

    @Test
    fun `the natural order is total, so sorting never throws and ties keep a fixed order`() {
        val random = Random(7)
        val pieces = listOf("a", "A", "b", "0", "00", "1", "01", "10", "2", "-", ".", "é", "É", "z9", "Z09")
        val names = List(2_000) { List(random.nextInt(1, 5)) { pieces.random(random) }.joinToString("") }

        val once = names.sortedWith(NaturalOrder)

        assertEquals(once, names.shuffled(random).sortedWith(NaturalOrder))
        once.zipWithNext { a, b -> assertTrue("$a > $b", NaturalOrder.compare(a, b) <= 0) }
        assertEquals(listOf("01", "1", "2", "10"), listOf("10", "2", "1", "01").sortedWith(NaturalOrder))
    }

    @Test
    fun `files without a size or date come first when sorting by them`() {
        val entries = listOf(file("x", size = 1), Entry("y", Entry.Kind.File), file("w", size = 2))
        val order = FolderSort(FolderSort.Field.Size).comparator()

        assertEquals(listOf("y", "x", "w"), entries.sortedWith(order).map { it.name })
    }
}
