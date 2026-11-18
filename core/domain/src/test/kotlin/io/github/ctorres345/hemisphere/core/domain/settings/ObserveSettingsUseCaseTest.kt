package io.github.ctorres345.hemisphere.core.domain.settings

import io.github.ctorres345.hemisphere.core.model.Accent
import io.github.ctorres345.hemisphere.core.model.Settings
import io.github.ctorres345.hemisphere.core.model.ThemeSetting
import io.github.ctorres345.hemisphere.core.testing.FakeSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveSettingsUseCaseTest {
    private val saved = Settings.DEFAULT.copy(theme = ThemeSetting.AMOLED, accent = Accent.Violet)
    private val repo = FakeSettingsRepository(saved)

    @Test
    fun `passes the repository's settings through`() =
        runTest {
            assertEquals(saved, ObserveSettingsUseCase(repo)().first())
        }
}
