package io.github.ctorres345.hemisphere.feature.settings

import io.github.ctorres345.hemisphere.core.domain.settings.ObserveSettingsUseCase
import io.github.ctorres345.hemisphere.core.domain.settings.SetAccentUseCase
import io.github.ctorres345.hemisphere.core.domain.settings.SetFaceButtonsUseCase
import io.github.ctorres345.hemisphere.core.domain.settings.SetLayoutPinUseCase
import io.github.ctorres345.hemisphere.core.domain.settings.SetThemeUseCase
import io.github.ctorres345.hemisphere.core.domain.settings.SetTrashRetentionUseCase
import io.github.ctorres345.hemisphere.core.model.Accent
import io.github.ctorres345.hemisphere.core.model.FaceButtons
import io.github.ctorres345.hemisphere.core.model.LayoutPin
import io.github.ctorres345.hemisphere.core.model.Orientation
import io.github.ctorres345.hemisphere.core.model.Settings
import io.github.ctorres345.hemisphere.core.model.ThemeSetting
import io.github.ctorres345.hemisphere.core.testing.FakeSettingsRepository
import io.github.ctorres345.hemisphere.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Real use cases over a fake repository: each choice is saved and shown. No mocks. */
class SettingsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun `the screen shows the saved settings`() =
        runTest(main.dispatcher) {
            val saved = Settings(theme = ThemeSetting.Dark, faceButtons = FaceButtons.Nintendo)

            val viewModel = viewModel(FakeSettingsRepository(saved))

            assertEquals(SettingsUiState(settings = saved), viewModel.state.value)
        }

    @Test
    fun `each choice is saved and shown`() =
        runTest(main.dispatcher) {
            val repository = FakeSettingsRepository()
            val viewModel = viewModel(repository)

            viewModel.onIntent(SettingsIntent.ThemeChosen(ThemeSetting.AMOLED))
            viewModel.onIntent(SettingsIntent.AccentChosen(Accent.Teal))
            viewModel.onIntent(SettingsIntent.LayoutPinChosen(Orientation.Landscape, LayoutPin.Dual))
            viewModel.onIntent(SettingsIntent.LayoutPinChosen(Orientation.Portrait, LayoutPin.Single))
            viewModel.onIntent(SettingsIntent.FaceButtonsChosen(FaceButtons.Xbox))
            viewModel.onIntent(SettingsIntent.TrashRetentionChosen(90))

            val expected =
                Settings(
                    theme = ThemeSetting.AMOLED,
                    accent = Accent.Teal,
                    faceButtons = FaceButtons.Xbox,
                    trashRetentionDays = 90,
                    layoutPinPortrait = LayoutPin.Single,
                    layoutPinLandscape = LayoutPin.Dual,
                )
            assertEquals(expected, viewModel.state.value.settings)
        }

    private fun viewModel(repository: FakeSettingsRepository) =
        SettingsViewModel(
            ObserveSettingsUseCase(repository),
            SetThemeUseCase(repository),
            SetAccentUseCase(repository),
            SetLayoutPinUseCase(repository),
            SetFaceButtonsUseCase(repository),
            SetTrashRetentionUseCase(repository),
        )
}
