package io.github.ctorres345.hemisphere.feature.browser.layout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme
import io.github.ctorres345.hemisphere.core.fs.testing.file
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.Listing
import io.github.ctorres345.hemisphere.core.model.VolumeId
import io.github.ctorres345.hemisphere.feature.browser.BrowserScreen
import io.github.ctorres345.hemisphere.feature.browser.BrowserUiState
import io.github.ctorres345.hemisphere.feature.browser.ShellState
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneId
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneState
import io.github.ctorres345.hemisphere.feature.browser.pane.TabState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A layout switch keeps the active pane's list where the user scrolled it. */
@RunWith(RobolectricTestRunner::class)
// Robolectric needs Java 21 for API 35 and up; the build runs on JDK 17 (ADR 0003).
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-hdpi")
class PaneLayoutSwitchTest {
    @get:Rule
    val compose = createComposeRule()

    private val root = FsPath.root(VolumeId("v"))

    private fun pane(prefix: String) =
        PaneState(
            listOf(
                TabState(
                    backStack = listOf(root),
                    listing = Listing((1..100).map { file("$prefix $it.txt") }, complete = true),
                ),
            ),
        )

    @Test
    fun `switching to Dual and back keeps the active pane's scroll position`() {
        var state by mutableStateOf(
            BrowserUiState(panes = mapOf(PaneId.Left to pane("left"), PaneId.Right to pane("right"))),
        )
        compose.setContent { HemisphereTheme { BrowserScreen(state = state, onIntent = {}) } }
        compose.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToIndex(60)
        compose.onNodeWithText("left 61.txt").assertIsDisplayed()

        state = state.copy(shell = ShellState(layout = PaneLayout.Dual))
        compose.onNodeWithText("left 61.txt").assertIsDisplayed()

        state = state.copy(shell = ShellState(layout = PaneLayout.Single))
        compose.onNodeWithText("left 61.txt").assertIsDisplayed()
    }
}
