package io.github.ctorres345.hemisphere.feature.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.ctorres345.hemisphere.core.designsystem.FaceButtonLayout
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme
import io.github.ctorres345.hemisphere.core.designsystem.InputMode
import io.github.ctorres345.hemisphere.core.designsystem.LocalInputMode
import io.github.ctorres345.hemisphere.core.model.Accent
import io.github.ctorres345.hemisphere.core.model.FaceButtons
import io.github.ctorres345.hemisphere.core.model.LayoutPin
import io.github.ctorres345.hemisphere.core.model.Orientation
import io.github.ctorres345.hemisphere.core.model.Settings
import io.github.ctorres345.hemisphere.core.model.ThemeSetting
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Each chip and swatch on the screen sends its own intent, and the saved values show selected. */
@RunWith(RobolectricTestRunner::class)
// Robolectric needs Java 21 for API 35 and up; the build runs on JDK 17 (ADR 0003).
@Config(sdk = [34])
class SettingsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val sent = mutableListOf<SettingsIntent>()

    private fun show(
        settings: Settings,
        input: InputMode = InputMode.Touch,
        device: SettingsDevice = SettingsDevice(version = "0.1.0"),
    ) {
        compose.setContent {
            CompositionLocalProvider(LocalInputMode provides input) {
                HemisphereTheme {
                    SettingsScreen(
                        state = SettingsUiState(settings = settings),
                        device = device,
                        onIntent = { sent += it },
                        onBack = {},
                    )
                }
            }
        }
    }

    @Test
    fun `a gamepad starts on the saved theme`() {
        show(Settings(theme = ThemeSetting.Light), input = InputMode.Gamepad)

        compose.onNodeWithText("Light").assertIsFocused()
    }

    @Test
    fun `touch focuses nothing at start`() {
        show(Settings(theme = ThemeSetting.Light))

        compose.onNodeWithText("Light").assertIsNotFocused()
    }

    private fun press(text: String) {
        compose.onNodeWithText(text).performScrollTo().performClick()
    }

    /** Opens a picker row by its title and picks a choice in it. */
    private fun pick(
        row: String,
        choice: String,
    ) {
        press(row)
        compose.onNodeWithText(choice).performClick()
    }

    @Test
    fun `each choice sends its intent`() {
        show(Settings.DEFAULT)

        press("AMOLED")
        compose.onNodeWithContentDescription("Violet").performScrollTo().performClick()
        pick("Portrait", "Dual")
        pick("Landscape", "Single")
        pick("Face buttons", "Nintendo")
        pick("Keep deleted files", "90 days")

        assertEquals(
            listOf(
                SettingsIntent.ThemeChosen(ThemeSetting.AMOLED),
                SettingsIntent.AccentChosen(Accent.Violet),
                SettingsIntent.LayoutPinChosen(Orientation.Portrait, LayoutPin.Dual),
                SettingsIntent.LayoutPinChosen(Orientation.Landscape, LayoutPin.Single),
                SettingsIntent.FaceButtonsChosen(FaceButtons.Nintendo),
                SettingsIntent.TrashRetentionChosen(90),
            ),
            sent,
        )
    }

    @Test
    fun `the saved values show`() {
        show(
            Settings(
                theme = ThemeSetting.Dark,
                accent = Accent.Pink,
                layoutPinLandscape = LayoutPin.Single,
                faceButtons = FaceButtons.Xbox,
                trashRetentionDays = 7,
            ),
        )

        compose.onNodeWithText("Dark").assertIsSelected()
        compose.onNodeWithContentDescription("Pink").assertIsSelected()
        compose.onNodeWithText("Portrait").assert(hasText("Auto"))
        compose.onNodeWithText("Landscape").assert(hasText("Single"))
        compose.onNodeWithText("Face buttons").assert(hasText("Xbox"))
        compose.onNodeWithText("Keep deleted files").assert(hasText("7 days"))
    }

    @Test
    fun `Auto names the layout it picks for the connected pad`() {
        show(Settings.DEFAULT, device = SettingsDevice("0.1.0", autoFaceButtons = FaceButtonLayout.Nintendo))

        compose.onNodeWithText("Face buttons").assert(hasText("Auto (Nintendo)"))
    }

    @Test
    fun `a picker marks the saved choice`() {
        show(Settings(layoutPinLandscape = LayoutPin.Single))

        press("Landscape")

        compose.onAllNodesWithText("Single").filterToOne(isSelected()).assertExists()
    }
}
