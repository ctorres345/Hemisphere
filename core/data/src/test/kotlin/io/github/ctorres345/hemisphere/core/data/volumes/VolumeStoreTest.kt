package io.github.ctorres345.hemisphere.core.data.volumes

import androidx.room.Room
import io.github.ctorres345.hemisphere.core.fs.RemovableMatch
import io.github.ctorres345.hemisphere.core.fs.TreeGrant
import io.github.ctorres345.hemisphere.core.model.VolumeId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** `volumes.db` on a real file in a temp dir, so "reopen" really closes and reads the file again. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VolumeStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val open = mutableListOf<VolumesDatabase>()

    @After
    fun tearDown() = open.forEach { it.close() }

    private fun store(): RoomVolumeStore {
        val context = RuntimeEnvironment.getApplication()
        val db =
            Room
                .databaseBuilder(context, VolumesDatabase::class.java, folder.root.resolve("volumes.db").path)
                .build()
        open += db
        return RoomVolumeStore(db.dao())
    }

    private fun closeAll() {
        open.forEach { it.close() }
        open.clear()
    }

    @Test
    fun `a saved grant reads back after reopening`() =
        runBlocking {
            val sd = VolumeId("1234-ABCD")
            store().saveGrant(sd, TreeGrant("content://tree/1234-ABCD%3A"))
            closeAll()
            val reopened = store()
            assertEquals(TreeGrant("content://tree/1234-ABCD%3A"), reopened.grantFor(sd))
            assertEquals(mapOf(sd to TreeGrant("content://tree/1234-ABCD%3A")), reopened.observeGrants().first())
        }

    @Test
    fun `a grant is replaced and removed`() =
        runBlocking {
            val sd = VolumeId("1234-ABCD")
            val s = store()
            s.saveGrant(sd, TreeGrant("a"))
            s.saveGrant(sd, TreeGrant("b"))
            assertEquals(TreeGrant("b"), s.grantFor(sd))
            s.removeGrant(sd)
            assertNull(s.grantFor(sd))
        }

    @Test
    fun `a saved no-UUID id reads back after reopening`() =
        runBlocking {
            val id = store().createRemovableId("CARD", 32_000_000_000L)
            closeAll()
            assertEquals(RemovableMatch.One(id), store().findRemovableId("CARD", 32_000_000_000L))
        }

    @Test
    fun `an unknown card matches nothing`() =
        runBlocking {
            val s = store()
            s.createRemovableId("CARD", 32L)
            assertEquals(RemovableMatch.None, s.findRemovableId("OTHER", 32L))
        }

    @Test
    fun `same label with different capacity gets different ids`() =
        runBlocking {
            val s = store()
            val small = s.createRemovableId("CARD", 16L)
            val big = s.createRemovableId("CARD", 32L)
            assertNotEquals(small, big)
            assertEquals(RemovableMatch.One(small), s.findRemovableId("CARD", 16L))
            assertEquals(RemovableMatch.One(big), s.findRemovableId("CARD", 32L))
        }

    @Test
    fun `same label and capacity is ambiguous, never a guess`() =
        runBlocking {
            val s = store()
            val a = s.createRemovableId("CARD", 32L)
            val b = s.createRemovableId("CARD", 32L)
            val c = s.createRemovableId("CARD", 32L)
            val match = s.findRemovableId("CARD", 32L) as RemovableMatch.Ambiguous
            assertEquals(setOf(a, b, c), match.candidates.toSet())
            assertEquals(match, s.findOrCreateRemovableId("CARD", 32L))
        }

    @Test
    fun `a rejected stale id is removed and the match settles`() =
        runBlocking {
            val s = store()
            val stale = s.createRemovableId("CARD", 32L)
            val real = s.createRemovableId("CARD", 32L)
            s.saveGrant(stale, TreeGrant("content://tree/stale"))
            s.removeRemovableId(stale)
            assertEquals(RemovableMatch.One(real), s.findRemovableId("CARD", 32L))
            assertNull(s.grantFor(stale))
        }

    @Test
    fun `two lookups of one new card at once make one id`() =
        runBlocking {
            val s = store()
            val results = List(8) { async(Dispatchers.Default) { s.findOrCreateRemovableId("CARD", 32L) } }.awaitAll()
            assertEquals(1, results.toSet().size)
            assertTrue(results.first() is RemovableMatch.One)
        }
}
