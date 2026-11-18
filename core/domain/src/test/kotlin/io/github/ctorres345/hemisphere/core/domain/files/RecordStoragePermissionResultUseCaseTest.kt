package io.github.ctorres345.hemisphere.core.domain.files

import io.github.ctorres345.hemisphere.core.model.StorageAccessState
import io.github.ctorres345.hemisphere.core.testing.FakeStorageAccess
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordStoragePermissionResultUseCaseTest {
    @Test
    fun `passes the answer to storage access`() {
        val access = FakeStorageAccess(StorageAccessState.Denied)

        RecordStoragePermissionResultUseCase(access)(granted = false, canAskAgain = false)

        assertEquals(listOf(false to false), access.results)
        assertEquals(StorageAccessState.NeedsSettings, access.state.value)
    }
}
