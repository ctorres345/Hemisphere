package io.github.ctorres345.hemisphere.feature.settings

import io.github.ctorres345.hemisphere.core.model.Accent
import io.github.ctorres345.hemisphere.core.model.Settings
import io.github.ctorres345.hemisphere.core.model.ThemeSetting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The reducer alone: pure state in, state out. */
class SettingsReducerTest {
    @Test
    fun `the first state waits for the saved settings`() {
        assertNull(SettingsUiState().settings)
    }

    @Test
    fun `loaded settings replace the state`() {
        val saved = Settings(theme = ThemeSetting.AMOLED, accent = Accent.Violet, trashRetentionDays = 7)

        val state = reduce(SettingsUiState(), SettingsResult.SettingsLoaded(saved))

        assertEquals(SettingsUiState(settings = saved), state)
    }
}
