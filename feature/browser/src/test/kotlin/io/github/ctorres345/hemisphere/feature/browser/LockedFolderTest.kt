package io.github.ctorres345.hemisphere.feature.browser

import io.github.ctorres345.hemisphere.core.fs.LockedFolders
import io.github.ctorres345.hemisphere.core.fs.testing.file
import io.github.ctorres345.hemisphere.core.fs.testing.folder
import io.github.ctorres345.hemisphere.core.model.Entry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Android/data and Android/obb are locked on Android 11 and up, and normal folders below. */
class LockedFolderTest : BrowserViewModelFixture() {
    @Test
    fun `a locked folder is not read, and its parent marks it`() =
        runTest(main.dispatcher) {
            provider.addFolder(home, folder("Android"))
            provider.addFolder(home.child("Android"), folder("data"), folder("media"))
            val viewModel = viewModel()

            viewModel.open("Android")
            assertEquals(setOf("data", "obb"), viewModel.tab.lock.lockedChildren)
            val calls = provider.listCalls
            viewModel.open("data")

            assertTrue(viewModel.tab.lock.locked)
            assertEquals(emptyList<Entry>(), viewModel.tab.listing.entries)
            assertTrue(viewModel.tab.listing.complete)
            assertEquals(calls, provider.listCalls)
        }

    @Test
    fun `below Android 11 the same folder is read like any other`() =
        runTest(main.dispatcher) {
            provider.addFolder(home, folder("Android"))
            provider.addFolder(home.child("Android"), folder("data"))
            provider.addFolder(home.child("Android").child("data"), file("save.dat"))
            val viewModel = viewModel(sdkInt = LockedFolders.API_R - 1)

            viewModel.open("Android")
            viewModel.open("data")

            assertFalse(viewModel.tab.lock.locked)
            assertEquals(
                listOf("save.dat"),
                viewModel.tab.listing.entries
                    .map { it.name },
            )
        }
}
