package io.github.ctorres345.hemisphere.core.domain.files

import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.testing.FakeVolumeRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class GetHomeFolderUseCaseTest {
    @Test
    fun `returns the volumes' home folder`() {
        val home = FsPath.root(FakeVolumeRepository.TEST_VOLUME).child("Home")
        assertEquals(home, GetHomeFolderUseCase(FakeVolumeRepository(home = home))())
    }
}
