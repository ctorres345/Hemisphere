package io.github.ctorres345.hemisphere.core.jobs

import io.github.ctorres345.hemisphere.core.fs.testing.InMemoryProvider
import io.github.ctorres345.hemisphere.core.fs.testing.file
import io.github.ctorres345.hemisphere.core.fs.testing.folder
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.VolumeId
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** `core:jobs` tests reach `InMemoryProvider` through `core:fs-testing` alone, without `core:testing`. */
class InternalEntriesTest {
    private val root = FsPath.root(VolumeId("test"))

    @Test
    fun `the app's own entries are told apart in a listing`() =
        runTest {
            val provider =
                InMemoryProvider().apply {
                    addFolder(
                        root,
                        folder(".Trash-hemisphere"),
                        file(".hemisphere-part-1"),
                        file(".hemisphere-rename-2"),
                        file("holiday.jpg"),
                        folder("Documents"),
                    )
                }

            val names =
                provider
                    .list(root)
                    .toList()
                    .flatMap { (it as FsResult.Ok).value }
                    .map { it.name }

            assertEquals(
                listOf("Documents", "holiday.jpg"),
                names.filterNot(InternalEntries::isInternal).sorted(),
            )
        }
}
