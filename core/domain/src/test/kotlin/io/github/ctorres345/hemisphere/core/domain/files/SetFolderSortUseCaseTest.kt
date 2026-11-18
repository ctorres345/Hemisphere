package io.github.ctorres345.hemisphere.core.domain.files

import io.github.ctorres345.hemisphere.core.fs.testing.FakeVolumeRepository
import io.github.ctorres345.hemisphere.core.model.FolderSort
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.testing.FakeFolderViewRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SetFolderSortUseCaseTest {
    private val views = FakeFolderViewRepository()
    private val setSort = SetFolderSortUseCase(views)
    private val home = FsPath.root(FakeVolumeRepository.TEST_VOLUME)

    @Test
    fun `saves the sort for that folder only`() =
        runTest {
            val byDate = FolderSort(FolderSort.Field.Date, descending = true)

            setSort(home, byDate)

            assertEquals(byDate, views.observe(home).first())
            assertEquals(FolderSort.DEFAULT, views.observe(home.child("Music")).first())
        }
}
