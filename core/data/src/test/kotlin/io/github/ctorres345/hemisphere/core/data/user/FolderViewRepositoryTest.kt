package io.github.ctorres345.hemisphere.core.data.user

import androidx.room.Room
import io.github.ctorres345.hemisphere.core.model.FolderSort
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.ProviderRef
import io.github.ctorres345.hemisphere.core.model.VolumeId
import io.github.ctorres345.hemisphere.core.model.encode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FolderViewRepositoryTest {
    private val db =
        Room
            .inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), UserDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val repo = RoomFolderViewRepository(db.folderViews())

    private val music = FsPath.root(VolumeId("primary")).child("Music")
    private val bySize = FolderSort(FolderSort.Field.Size, descending = true)
    private val byDate = FolderSort(FolderSort.Field.Date)

    /** An SD card through SAF: paths key by their document id. */
    private val card = FsPath.root(VolumeId("sd"), caseInsensitive = true, idAddressed = true)

    @After
    fun tearDown() = db.close()

    @Test
    fun `a folder with nothing saved reads the default sort`() =
        runBlocking {
            assertEquals(FolderSort.DEFAULT, repo.observe(music).first())
        }

    @Test
    fun `a saved sort is read back by key, and other folders keep the default`() =
        runBlocking {
            repo.setSort(music, bySize)

            assertEquals(bySize, repo.observe(FsPath.root(VolumeId("primary")).child("Music")).first())
            assertEquals(FolderSort.DEFAULT, repo.observe(music.child("Rock")).first())
            assertEquals(listOf(music.key.encode()), savedKeys())
        }

    @Test
    fun `a refreshed ref on the same path still finds the sort, and both refs keep seeing it`() =
        runBlocking {
            val before = card.child("Photos", ProviderRef("doc-1"))
            val after = card.child("Photos", ProviderRef("doc-2"))
            repo.setSort(before, bySize)

            assertEquals(bySize, repo.observe(after).first())
            // Reading moves nothing: a pane still on the old ref keeps its sort.
            assertEquals(bySize, repo.observe(before).first())
            assertEquals(listOf(before.key.encode()), savedKeys())
        }

    @Test
    fun `a row whose key no longer matches is found by its path and re-keyed on the next save`() =
        runBlocking {
            // As if the case fold of the key changed with an OS update: the stored key is stale.
            repo.setSort(music, bySize)
            db.openHelper.writableDatabase.execSQL("UPDATE folder_view SET entry_key = 'v1|k|primary|/stale'")

            assertEquals(bySize, repo.observe(music).first())

            repo.setSort(music, byDate)
            assertEquals(byDate, repo.observe(music).first())
            assertEquals(listOf(music.key.encode()), savedKeys())
        }

    @Test
    fun `open observers under two keys of one folder both follow a change`() =
        runBlocking {
            val before = card.child("Photos", ProviderRef("doc-1"))
            val after = card.child("Photos", ProviderRef("doc-2"))
            repo.setSort(before, bySize)
            val old = Channel<FolderSort>(Channel.UNLIMITED)
            val new = Channel<FolderSort>(Channel.UNLIMITED)
            val jobs =
                listOf(
                    launch(Dispatchers.Default) { repo.observe(before).collect { old.send(it) } },
                    launch(Dispatchers.Default) { repo.observe(after).collect { new.send(it) } },
                )
            withTimeout(TIMEOUT_MS) {
                assertEquals(bySize, old.receive())
                assertEquals(bySize, new.receive())
                repo.setSort(after, byDate)
                assertEquals(byDate, old.receive())
                assertEquals(byDate, new.receive())
            }
            jobs.forEach { it.cancel() }
        }

    @Test
    fun `changes reach an open observer, and the default sort removes the row`() =
        runBlocking {
            val seen = Channel<FolderSort>(Channel.UNLIMITED)
            val job = launch(Dispatchers.Default) { repo.observe(music).collect { seen.send(it) } }
            // Each value is awaited before the next write, so no write lands before the observer sees the last.
            withTimeout(TIMEOUT_MS) {
                assertEquals(FolderSort.DEFAULT, seen.receive())
                repo.setSort(music, bySize)
                assertEquals(bySize, seen.receive())
                repo.setSort(music, FolderSort.DEFAULT)
                assertEquals(FolderSort.DEFAULT, seen.receive())
            }
            job.cancel()

            assertEquals(emptyList<String>(), savedKeys())
        }

    private fun savedKeys(): List<String> =
        db.query("SELECT entry_key FROM folder_view", null).use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
