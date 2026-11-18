package io.github.ctorres345.hemisphere.core.domain.settings

import io.github.ctorres345.hemisphere.core.data.settings.SettingsRepository
import javax.inject.Inject

/** Saves how many days Trash keeps an item. */
class SetTrashRetentionUseCase
    @Inject
    constructor(
        private val settings: SettingsRepository,
    ) {
        suspend operator fun invoke(days: Int) = settings.setTrashRetentionDays(days)
    }
