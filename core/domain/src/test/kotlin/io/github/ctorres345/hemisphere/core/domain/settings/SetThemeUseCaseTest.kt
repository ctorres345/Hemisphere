package io.github.ctorres345.hemisphere.core.domain.settings

import io.github.ctorres345.hemisphere.core.model.ThemeSetting
import io.github.ctorres345.hemisphere.core.testing.FakeSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SetThemeUseCaseTest {
    private val repo = FakeSettingsRepository()

    @Test
    fun `passes the theme to the repository`() =
        runTest {
            SetThemeUseCase(repo)(ThemeSetting.AMOLED)
            assertEquals(ThemeSetting.AMOLED, repo.settings.first().theme)
        }
}
