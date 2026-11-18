package io.github.ctorres345.hemisphere.core.domain.settings

import io.github.ctorres345.hemisphere.core.data.settings.SettingsRepository
import io.github.ctorres345.hemisphere.core.model.FaceButtons
import javax.inject.Inject

/** Saves the Face buttons choice. */
class SetFaceButtonsUseCase
    @Inject
    constructor(
        private val settings: SettingsRepository,
    ) {
        suspend operator fun invoke(faceButtons: FaceButtons) = settings.setFaceButtons(faceButtons)
    }
