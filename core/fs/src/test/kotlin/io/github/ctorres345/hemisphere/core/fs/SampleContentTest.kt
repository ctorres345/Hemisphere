package io.github.ctorres345.hemisphere.core.fs

import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SampleContentTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val home get() = File(temp.root, "home")
    private val marker get() = File(temp.root, SampleContent.MARKER_NAME)
    private val samples = listOf("Documents", "Music", "Pictures", "ROMs", "Readme.txt", "notes.md")

    private fun homeNames() = home.list()!!.sorted()

    @Test
    fun `an empty home gets every sample and is marked done`() {
        assertTrue(SampleContent.seed(home, marker))
        assertEquals(samples.sorted(), homeNames())
        assertTrue(SampleContent.isDone(marker))
    }

    @Test
    fun `home is created even when it does not exist yet`() {
        SampleContent.seed(home, marker)
        assertTrue(home.isDirectory)
    }

    @Test
    fun `a seed stopped halfway is completed on the next run`() {
        // The app was killed after the first folder: the marker says started, not done.
        home.mkdirs()
        File(home, "Documents").mkdir()
        marker.writeText(SampleContent.STARTED)

        assertTrue(SampleContent.seed(home, marker))
        assertEquals(samples.sorted(), homeNames())
        assertTrue(SampleContent.isDone(marker))
    }

    @Test
    fun `a seed that fails is not marked done and is completed once the fault is gone`() {
        home.mkdirs()
        marker.writeText(SampleContent.STARTED)
        home.setWritable(false) // every sample write now fails
        assumeFalse("root ignores the read-only bit", home.canWrite())

        assertFalse(SampleContent.seed(home, marker))
        assertFalse(SampleContent.isDone(marker))

        home.setWritable(true)
        assertTrue(SampleContent.seed(home, marker))
        assertEquals(samples.sorted(), homeNames())
        assertTrue(SampleContent.isDone(marker))
    }

    @Test
    fun `a seed cut short before the marker existed is completed`() {
        home.mkdirs()
        File(home, "Documents").mkdir()
        File(home, "Music").mkdir()

        assertTrue(SampleContent.seed(home, marker))
        assertEquals(samples.sorted(), homeNames())
        assertTrue(SampleContent.isDone(marker))
    }

    @Test
    fun `a home that cannot be made is retried, not marked done`() {
        home.writeText("") // a plain file where the home folder should be

        assertFalse(SampleContent.seed(home, marker))
        assertFalse(marker.exists())
    }

    @Test
    fun `completing a seed never overwrites a file that is already there`() {
        home.mkdirs()
        marker.writeText(SampleContent.STARTED)
        File(home, "Readme.txt").writeText("mine")

        SampleContent.seed(home, marker)
        assertEquals("mine", File(home, "Readme.txt").readText())
    }

    @Test
    fun `a home with files and no marker is left alone`() {
        home.mkdirs()
        File(home, "mine.txt").writeText("")

        assertTrue(SampleContent.seed(home, marker))
        assertEquals(listOf("mine.txt"), homeNames())
        assertTrue(SampleContent.isDone(marker))
    }

    @Test
    fun `a done home is not seeded again after the user deletes samples`() {
        SampleContent.seed(home, marker)
        File(home, "Music").delete()

        SampleContent.seed(home, marker)
        assertFalse(File(home, "Music").exists())
    }

    @Test
    fun `an unreadable marker counts as started`() {
        home.mkdirs()
        marker.writeText("") // a write cut short by a crash

        SampleContent.seed(home, marker)
        assertEquals(samples.sorted(), homeNames())
    }

    @Test
    fun `an empty marker beside a home with user files leaves the home alone`() {
        home.mkdirs()
        File(home, "mine.txt").writeText("")
        marker.writeText("")

        assertTrue(SampleContent.seed(home, marker))
        assertEquals(listOf("mine.txt"), homeNames())
    }

    @Test
    fun `no temporary marker file is left behind`() {
        SampleContent.seed(home, marker)
        assertEquals(listOf("home", SampleContent.MARKER_NAME).sorted(), temp.root.list()!!.sorted())
    }

    @Test
    fun `the marker never shows in the home listing`() =
        runTest(StandardTestDispatcher()) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val repo = AppHomeVolumeRepository(home, marker, dispatcher, seedSamples = true)
            val provider = (repo.providerFor(AppHomeVolume) as FsResult.Ok).value

            val names =
                provider
                    .list(FsPath.root(AppHomeVolume))
                    .toList()
                    .filterIsInstance<FsResult.Ok<List<Entry>>>()
                    .flatMap { it.value }
                    .map { it.name }
            assertFalse(names.contains(SampleContent.MARKER_NAME))
            assertTrue(names.contains("Music"))
        }

    @Test
    fun `a failed seed still lists what is in home`() =
        runTest(StandardTestDispatcher()) {
            home.mkdirs()
            File(home, "Documents").mkdir() // the part written before it failed
            marker.writeText(SampleContent.STARTED)
            home.setWritable(false) // every sample write now fails
            assumeFalse("root ignores the read-only bit", home.canWrite())
            val dispatcher = StandardTestDispatcher(testScheduler)
            val repo = AppHomeVolumeRepository(home, marker, dispatcher, seedSamples = true)
            val provider = (repo.providerFor(AppHomeVolume) as FsResult.Ok).value

            val results = provider.list(FsPath.root(AppHomeVolume)).toList()
            home.setWritable(true) // so the temporary folder can be deleted

            val batch = results.single() as FsResult.Ok
            assertEquals(listOf("Documents"), batch.value.map(Entry::name))
            assertFalse(SampleContent.isDone(marker))
        }

    @Test
    fun `without samples a fresh home lists empty and nothing is written`() =
        runTest(StandardTestDispatcher()) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val repo = AppHomeVolumeRepository(home, marker, dispatcher, seedSamples = false)
            val provider = (repo.providerFor(AppHomeVolume) as FsResult.Ok).value

            // An empty folder completes without a batch; an error would show up here.
            assertEquals(emptyList<FsResult<List<Entry>>>(), provider.list(FsPath.root(AppHomeVolume)).toList())
            assertEquals(emptyList<String>(), homeNames())
            assertFalse(marker.exists())
        }
}
