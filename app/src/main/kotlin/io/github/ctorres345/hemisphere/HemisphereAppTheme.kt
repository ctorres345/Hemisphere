package io.github.ctorres345.hemisphere

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme
import io.github.ctorres345.hemisphere.core.designsystem.ThemeStyle
import io.github.ctorres345.hemisphere.core.model.Settings
import io.github.ctorres345.hemisphere.core.model.ThemeSetting
import io.github.ctorres345.hemisphere.feature.settings.token
import kotlinx.coroutines.flow.Flow
import io.github.ctorres345.hemisphere.core.model.Accent as AccentSetting

/**
 * The app theme from the saved settings. It recomposes when they change, so a theme or accent chosen
 * in Settings shows at once, with no Activity recreate. Until the settings are read (a few
 * milliseconds at start) it draws nothing, so a saved AMOLED or Light theme never flashes the default
 * first. After a recreate (rotation) it draws the last theme it showed while they are read again.
 */
@Composable
fun HemisphereAppTheme(
    settings: Flow<Settings>,
    content: @Composable () -> Unit,
) {
    val current = settings.collectAsStateWithLifecycle(initialValue = null).value
    var lastTheme by rememberSaveable { mutableStateOf<ThemeSetting?>(null) }
    var lastAccent by rememberSaveable { mutableStateOf<AccentSetting?>(null) }
    if (current != null) {
        SideEffect {
            lastTheme = current.theme
            lastAccent = current.accent
        }
    }
    val theme = current?.theme ?: lastTheme ?: return
    val accent = current?.accent ?: lastAccent ?: return
    val style = theme.style(isSystemInDarkTheme())
    SystemBarIcons(dark = style != ThemeStyle.Light)
    HemisphereTheme(
        style = style,
        accent = accent.token(),
        content = content,
    )
}

/**
 * The status and navigation bar icons follow the app theme, not the device's dark mode, so the clock
 * stays readable when the two differ. The scrims are androidx's `enableEdgeToEdge` defaults.
 */
@Composable
private fun SystemBarIcons(dark: Boolean) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    // Applied when the composition commits, before the first frame draws, so the icons never flip late.
    DisposableEffect(activity, dark) {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
        )
        onDispose {}
    }
}

private val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

/** The theme to draw for a saved choice; System follows the device's dark mode. */
internal fun ThemeSetting.style(systemDark: Boolean): ThemeStyle =
    when (this) {
        ThemeSetting.Light -> ThemeStyle.Light
        ThemeSetting.Dark -> ThemeStyle.Dark
        ThemeSetting.AMOLED -> ThemeStyle.Amoled
        ThemeSetting.FollowSystem -> if (systemDark) ThemeStyle.Dark else ThemeStyle.Light
    }
