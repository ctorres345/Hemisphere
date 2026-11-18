package io.github.ctorres345.hemisphere.core.domain.settings

import io.github.ctorres345.hemisphere.core.data.settings.SettingsRepository
import io.github.ctorres345.hemisphere.core.model.LayoutPin
import io.github.ctorres345.hemisphere.core.model.Orientation
import javax.inject.Inject

/** Saves the layout pin for one orientation on this device class. */
class SetLayoutPinUseCase
    @Inject
    constructor(
        private val settings: SettingsRepository,
    ) {
        suspend operator fun invoke(
            orientation: Orientation,
            pin: LayoutPin,
        ) = settings.setLayoutPin(orientation, pin)
    }
