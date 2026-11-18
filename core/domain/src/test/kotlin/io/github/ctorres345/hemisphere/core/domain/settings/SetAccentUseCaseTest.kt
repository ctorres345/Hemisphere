package io.github.ctorres345.hemisphere.core.domain.settings

import io.github.ctorres345.hemisphere.core.model.Accent
import io.github.ctorres345.hemisphere.core.testing.FakeSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SetAccentUseCaseTest {
    private val repo = FakeSettingsRepository()

    @Test
    fun `passes the accent to the repository`() =
        runTest {
            SetAccentUseCase(repo)(Accent.Teal)
            assertEquals(Accent.Teal, repo.settings.first().accent)
        }
}
