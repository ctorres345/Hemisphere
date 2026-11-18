package io.github.ctorres345.hemisphere.core.domain.settings

import io.github.ctorres345.hemisphere.core.data.settings.SettingsRepository
import io.github.ctorres345.hemisphere.core.model.Accent
import javax.inject.Inject

/** Saves the accent. */
class SetAccentUseCase
    @Inject
    constructor(
        private val settings: SettingsRepository,
    ) {
        suspend operator fun invoke(accent: Accent) = settings.setAccent(accent)
    }
