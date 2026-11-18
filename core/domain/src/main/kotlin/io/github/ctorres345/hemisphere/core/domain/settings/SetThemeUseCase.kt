package io.github.ctorres345.hemisphere.core.domain.settings

import io.github.ctorres345.hemisphere.core.data.settings.SettingsRepository
import io.github.ctorres345.hemisphere.core.model.ThemeSetting
import javax.inject.Inject

/** Saves the theme. */
class SetThemeUseCase
    @Inject
    constructor(
        private val settings: SettingsRepository,
    ) {
        suspend operator fun invoke(theme: ThemeSetting) = settings.setTheme(theme)
    }
