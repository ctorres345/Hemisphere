package io.github.ctorres345.hemisphere.feature.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereSizes
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereSpacing
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme
import io.github.ctorres345.hemisphere.core.designsystem.LocalInputMode
import io.github.ctorres345.hemisphere.core.designsystem.ThemeStyle
import io.github.ctorres345.hemisphere.core.designsystem.ThemeStylePreviews
import io.github.ctorres345.hemisphere.core.designsystem.focusRing
import io.github.ctorres345.hemisphere.core.input.FaceButtonLayoutResolver
import io.github.ctorres345.hemisphere.core.input.connectedGamepadVendorId
import io.github.ctorres345.hemisphere.core.model.FaceButtons
import io.github.ctorres345.hemisphere.core.model.LayoutPin
import io.github.ctorres345.hemisphere.core.model.Orientation
import io.github.ctorres345.hemisphere.core.model.Settings

/** Where "Open-source licenses" goes: the license files in the public repository. */
private const val LICENSES_URL = "https://github.com/ctorres345/Hemisphere/blob/HEAD/LICENSE"

/** The Settings screen bound to its ViewModel. It reads the app version and the connected pad. */
@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val version = remember(context) { context.appVersion() }
    val vendorId by remember(context) { connectedGamepadVendorId(context) }.collectAsStateWithLifecycle(null)
    val device = SettingsDevice(version, FaceButtonLayoutResolver.resolve(FaceButtons.Auto, vendorId))
    SettingsScreen(
        state = state,
        device = device,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        modifier = modifier,
    )
}

/**
 * Stateless: state in, intents out. The prototype's Settings screen: an app bar with Back, then the
 * Appearance, Layout, Controls, Files and About sections. Theme and accent save on one press; the
 * other choices open a picker, as in the prototype.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    device: SettingsDevice,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        // A handheld's wide screen: the Dual top bar, and more room at the sides (prototype `.dev.handheld`).
        val wide = maxWidth >= HemisphereSizes.dualMinWidth
        Scaffold(
            topBar = { SettingsTopBar(wide, onBack) },
            contentWindowInsets = WindowInsets.safeDrawing,
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .then(if (wide) Modifier.padding(horizontal = HemisphereSpacing.xl) else Modifier)
                    .padding(bottom = HemisphereSpacing.l),
            ) {
                // Rendered only once the saved values are read, so a chip never flashes the default first.
                state.settings?.let { SettingsSections(it, device, onIntent) }
            }
        }
    }
}

/**
 * Single: the app bar with Back (prototype `.appbar`). Wide: the Dual layout's lower top bar, with a
 * border under it (prototype `.topbar`); Back stays there until Places (HEM-56) adds the rail.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(
    wide: Boolean,
    onBack: () -> Unit,
) {
    Column {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = if (wide) HemisphereTheme.typography.heading else LocalTextStyle.current,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.focusRing()) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = stringResource(R.string.settings_back),
                        tint = HemisphereTheme.colors.textSecondary,
                    )
                }
            },
            expandedHeight = if (wide) HemisphereSizes.topBar else HemisphereSizes.appBar,
            windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
        )
        if (wide) HorizontalDivider(thickness = HemisphereSizes.border, color = HemisphereTheme.colors.borderDefault)
    }
}

@Composable
private fun SettingsSections(
    settings: Settings,
    device: SettingsDevice,
    onIntent: (SettingsIntent) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val themeFocus = rememberStartFocus()
    SectionHeader(stringResource(R.string.settings_section_appearance))
    SettingRow(stringResource(R.string.settings_theme), hint = stringResource(R.string.settings_theme_hint))
    ChoiceChips(
        choices = THEME_CHOICES,
        selected = settings.theme,
        label = { stringResource(it.label()) },
        onChoose = { onIntent(SettingsIntent.ThemeChosen(it)) },
        selectedFocus = themeFocus,
    )
    SettingRow(stringResource(R.string.settings_accent), hint = stringResource(R.string.settings_accent_hint))
    AccentSwatches(settings.accent, onChoose = { onIntent(SettingsIntent.AccentChosen(it)) })

    SectionHeader(stringResource(R.string.settings_section_layout))
    val pins = LayoutPin.entries.map { it to stringResource(it.label()) }
    Orientation.entries.forEach { orientation ->
        SettingPicker(
            title = stringResource(orientation.label()),
            hint = stringResource(orientation.hint()),
            choices = pins,
            selected = settings.pinFor(orientation),
            onChoose = { onIntent(SettingsIntent.LayoutPinChosen(orientation, it)) },
        )
    }

    SectionHeader(stringResource(R.string.settings_section_controls))
    SettingPicker(
        title = stringResource(R.string.settings_face_buttons),
        hint = stringResource(R.string.settings_face_buttons_hint),
        choices = FaceButtons.entries.map { it to faceButtonsLabel(it, device.autoFaceButtons) },
        selected = settings.faceButtons,
        onChoose = { onIntent(SettingsIntent.FaceButtonsChosen(it)) },
    )

    SectionHeader(stringResource(R.string.settings_section_files))
    // A value saved outside the choices (a restored backup) still shows, selected.
    val days =
        remember(settings.trashRetentionDays) {
            (TRASH_RETENTION_CHOICES + settings.trashRetentionDays).distinct().sorted()
        }
    SettingPicker(
        title = stringResource(R.string.settings_trash),
        hint = stringResource(R.string.settings_trash_hint),
        choices = days.map { it to pluralStringResource(R.plurals.settings_trash_days, it, it) },
        selected = settings.trashRetentionDays,
        onChoose = { onIntent(SettingsIntent.TrashRetentionChosen(it)) },
    )

    SectionHeader(stringResource(R.string.settings_section_about))
    SettingRow(stringResource(R.string.settings_version), value = device.version)
    SettingLink(
        title = stringResource(R.string.settings_licenses),
        hint = stringResource(R.string.settings_licenses_hint),
        onClick = {
            try {
                uriHandler.openUri(LICENSES_URL)
            } catch (_: IllegalArgumentException) {
                // No browser on the device: nothing can open the page, so the row does nothing.
            }
        },
    )
}

/**
 * Where a gamepad or keyboard user starts: the saved theme, as in the prototype. Once per visit, so a
 * rotation does not pull focus back.
 */
@Composable
private fun rememberStartFocus(): FocusRequester {
    val start = remember { FocusRequester() }
    val showsFocus = LocalInputMode.current.showsFocus
    var done by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (showsFocus && !done) start.requestFocus()
        done = true
    }
    return start
}

private fun Context.appVersion(): String =
    try {
        packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
    } catch (_: PackageManager.NameNotFoundException) {
        ""
    }

@Preview
@Composable
private fun SettingsPreview(
    @PreviewParameter(ThemeStylePreviews::class) style: ThemeStyle,
) {
    HemisphereTheme(style = style) {
        SettingsScreen(
            state = SettingsUiState(settings = Settings.DEFAULT),
            device = SettingsDevice(version = "0.1.0"),
            onIntent = {},
            onBack = {},
        )
    }
}
