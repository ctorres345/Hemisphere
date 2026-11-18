package io.github.ctorres345.hemisphere.core.domain.settings

import io.github.ctorres345.hemisphere.core.data.settings.SettingsRepository
import io.github.ctorres345.hemisphere.core.model.Settings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** All saved settings, emitted again after each change. */
class ObserveSettingsUseCase
    @Inject
    constructor(
        private val settings: SettingsRepository,
    ) {
        operator fun invoke(): Flow<Settings> = settings.settings
    }
