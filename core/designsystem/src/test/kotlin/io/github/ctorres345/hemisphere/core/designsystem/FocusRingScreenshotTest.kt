package io.github.ctorres345.hemisphere.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The focus ring on a focused row (inside the edge) and on a focused filled control (outside, with a
 * gap), in each theme and input mode. In Touch mode the same focused elements show no ring.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Robolectric needs Java 21 for API 35 and up; the build runs on JDK 17 (ADR 0003).
@Config(sdk = [34])
class FocusRingScreenshotTest(
    private val mode: InputMode,
    private val style: ThemeStyle,
) {
    @Test
    fun row() = capture("row") { FocusedRow() }

    @Test
    fun filled() = capture("filled") { FocusedFilledControl() }

    private fun capture(
        element: String,
        content: @Composable () -> Unit,
    ) {
        RuntimeEnvironment.setQualifiers(ScreenshotDevice.Phone.qualifiers)
        captureRoboImage(
            filePath =
                "src/test/screenshots/FocusRing_${element}_${mode.name.lowercase()}_" +
                    "${style.name.lowercase()}.png",
            roborazziOptions = exactMatch(),
        ) {
            HemisphereTheme(style = style) {
                CompositionLocalProvider(LocalInputMode provides mode) {
                    Box(
                        Modifier.background(HemisphereTheme.colors.surfaceCanvas).padding(HemisphereSpacing.l),
                    ) { content() }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun parameters(): List<Array<Any>> =
            InputMode.entries.flatMap { mode -> ThemeStyle.entries.map { style -> arrayOf<Any>(mode, style) } }
    }
}

@Composable
private fun FocusedRow() {
    val focus = remember { FocusRequester() }
    Column(verticalArrangement = Arrangement.spacedBy(HemisphereSpacing.xs)) {
        SampleRow("Download")
        SampleRow("ROMs", Modifier.focusRequester(focus))
        SampleRow("Music")
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}

@Composable
private fun SampleRow(
    name: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .focusRing()
                .focusable()
                .fillMaxWidth()
                .height(LocalInputMode.current.rowHeight)
                .padding(horizontal = HemisphereSpacing.l),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(name, style = HemisphereTheme.typography.body, color = HemisphereTheme.colors.textPrimary)
    }
}

@Composable
private fun FocusedFilledControl() {
    val focus = remember { FocusRequester() }
    Box(
        modifier =
            Modifier
                .padding(HemisphereSpacing.xs)
                .focusRequester(focus)
                .focusRing(HemisphereShapes.row, FocusRingPlacement.Outside)
                .focusable()
                .background(HemisphereTheme.colors.accent.fill, HemisphereShapes.row)
                .padding(horizontal = HemisphereSpacing.l, vertical = HemisphereSpacing.m),
    ) {
        Text("Grant access", style = HemisphereTheme.typography.body, color = HemisphereTheme.colors.accent.onFill)
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}
