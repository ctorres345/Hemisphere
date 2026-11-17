package io.github.ctorres345.hemisphere.feature.browser

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.testing.file
import io.github.ctorres345.hemisphere.core.testing.folder
import org.junit.Assert.assertEquals
import org.junit.Test

/** Drives the list's row builder without a device: records the key of every row it adds. */
class EntryRowsTest {
    private class RecordingScope : LazyListScope {
        val keys = mutableListOf<Any?>()

        override fun item(
            key: Any?,
            contentType: Any?,
            content: @Composable LazyItemScope.() -> Unit,
        ) {
            keys += key
        }

        override fun items(
            count: Int,
            key: ((index: Int) -> Any)?,
            contentType: (index: Int) -> Any?,
            itemContent: @Composable LazyItemScope.(index: Int) -> Unit,
        ) {
            repeat(count) { keys += key?.invoke(it) }
        }

        @Deprecated("Not used by the browser list", level = DeprecationLevel.HIDDEN)
        override fun stickyHeader(
            key: Any?,
            contentType: Any?,
            content: @Composable LazyItemScope.() -> Unit,
        ): Unit = error("not used")
    }

    private fun keysFor(state: BrowserUiState): List<Any?> = RecordingScope().apply { entryRows(state) {} }.keys

    @Test
    fun `a listing with two equal names gets a row for each with its own key`() {
        val keys = keysFor(BrowserUiState(entries = listOf(file("a.txt"), file("a.txt"), folder("a.txt"))))

        assertEquals(3, keys.size)
        assertEquals(3, keys.filterNotNull().toSet().size)
    }

    @Test
    fun `the error row never shares a key with an entry`() {
        val state = BrowserUiState(entries = listOf(file("error-row"), file("error-row")), error = FsError.Io(null))

        val keys = keysFor(state)

        assertEquals(3, keys.size)
        assertEquals(3, keys.filterNotNull().toSet().size)
    }
}
