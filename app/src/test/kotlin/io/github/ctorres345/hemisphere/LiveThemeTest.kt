package io.github.ctorres345.hemisphere

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowCompat
import androidx.test.core.app.ApplicationProvider
import com.lemonappdev.konsist.api.Konsist
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereColors
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme
import io.github.ctorres345.hemisphere.core.designsystem.ThemeStyle
import io.github.ctorres345.hemisphere.core.domain.settings.ObserveSettingsUseCase
import io.github.ctorres345.hemisphere.core.domain.settings.SetAccentUseCase
import io.github.ctorres345.hemisphere.core.domain.settings.SetThemeUseCase
import io.github.ctorres345.hemisphere.core.model.Accent
import io.github.ctorres345.hemisphere.core.model.Settings
import io.github.ctorres345.hemisphere.core.model.ThemeSetting
import io.github.ctorres345.hemisphere.core.testing.FakeSettingsRepository
import io.github.ctorres345.hemisphere.feature.settings.token
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import io.github.ctorres345.hemisphere.core.designsystem.Accent as AccentToken

/**
 * The app root theme follows the saved settings while it is on screen: a change recomposes it with the
 * new colors, in the same composition (no Activity recreate).
 */
@RunWith(RobolectricTestRunner::class)
// Robolectric needs Java 21 for API 35 and up; the build runs on JDK 17 (ADR 0003).
@Config(sdk = [34])
class LiveThemeTest {
    private val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * The app's manifest does not merge test dependencies, so the empty activity the compose rule starts
     * is registered with Robolectric's package manager first.
     */
    private val testActivity =
        object : ExternalResource() {
            override fun before() {
                val context = ApplicationProvider.getApplicationContext<Context>()
                val component = ComponentName(context, ComponentActivity::class.java)
                val packages = shadowOf(context.packageManager)
                packages.addActivityIfNotPresent(component)
                packages.addIntentFilterForActivity(
                    component,
                    IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
                )
            }
        }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(testActivity).around(compose)

    @Test
    fun `a theme and accent saved while the app shows apply at once`() {
        val repository = FakeSettingsRepository(Settings(theme = ThemeSetting.Light))
        var shown: HemisphereColors? = null
        var amoledTeal: HemisphereColors? = null
        var light: HemisphereColors? = null
        var compositions = 0
        compose.setContent {
            HemisphereTheme(
                style = ThemeStyle.Amoled,
                accent = AccentToken.Teal,
            ) { amoledTeal = HemisphereTheme.colors }
            HemisphereTheme(style = ThemeStyle.Light) { light = HemisphereTheme.colors }
            HemisphereAppTheme(settings = ObserveSettingsUseCase(repository)()) {
                shown = HemisphereTheme.colors
                compositions++
            }
        }
        compose.waitForIdle()
        assertEquals(light, shown)
        assertTrue(lightStatusBarIcons())

        runBlocking {
            SetThemeUseCase(repository)(ThemeSetting.AMOLED)
            SetAccentUseCase(repository)(Accent.Teal)
        }
        compose.waitForIdle()

        assertEquals(amoledTeal, shown)
        // Light icons on the dark bar, whatever the device's own dark mode.
        assertFalse(lightStatusBarIcons())
        // The root content was recomposed in place, never set again.
        assertTrue(compositions > 1)
    }

    /** True when the status bar draws dark icons, for a light app bar. */
    private fun lightStatusBarIcons(): Boolean {
        val window = compose.activity.window
        return WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars
    }

    @Test
    fun `the app root draws through the live theme`() {
        val activity = Konsist.scopeFromProduction().classes().single { it.name == "MainActivity" }
        assertTrue(activity.text.contains("HemisphereAppTheme(settings = "))
        assertFalse(activity.text.contains("HemisphereTheme("))
    }

    @Test
    fun `nothing is drawn until the settings are read`() {
        var shown: HemisphereColors? = null
        compose.setContent {
            HemisphereAppTheme(settings = emptyFlow()) { shown = HemisphereTheme.colors }
        }
        compose.waitForIdle()

        assertNull(shown)
    }

    @Test
    fun `every saved accent has a design token`() {
        Accent.entries.forEach { assertEquals(it.name, it.token().name) }
        assertEquals(Accent.entries.size, AccentToken.entries.size)
    }

    @Test
    fun `System follows the device's dark mode`() {
        assertEquals(ThemeStyle.Dark, ThemeSetting.FollowSystem.style(systemDark = true))
        assertEquals(ThemeStyle.Light, ThemeSetting.FollowSystem.style(systemDark = false))
        assertEquals(ThemeStyle.Amoled, ThemeSetting.AMOLED.style(systemDark = false))
    }
}
