package io.github.ctorres345.hemisphere.core.domain.files

import io.github.ctorres345.hemisphere.core.model.StorageAccessState
import io.github.ctorres345.hemisphere.core.testing.FakeStorageAccess
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveStorageAccessUseCaseTest {
    @Test
    fun `emits the current access, then each change`() =
        runTest {
            val access = FakeStorageAccess(StorageAccessState.Denied)
            val observe = ObserveStorageAccessUseCase(access)
            assertEquals(StorageAccessState.Denied, observe().first())

            access.set(StorageAccessState.Granted)

            assertEquals(StorageAccessState.Granted, observe().first())
        }
}
