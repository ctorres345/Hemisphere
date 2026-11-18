package io.github.ctorres345.hemisphere.core.domain.settings

import io.github.ctorres345.hemisphere.core.testing.FakeSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SetTrashRetentionUseCaseTest {
    private val repo = FakeSettingsRepository()

    @Test
    fun `passes the days to the repository`() =
        runTest {
            SetTrashRetentionUseCase(repo)(7)
            assertEquals(7, repo.settings.first().trashRetentionDays)
        }
}
