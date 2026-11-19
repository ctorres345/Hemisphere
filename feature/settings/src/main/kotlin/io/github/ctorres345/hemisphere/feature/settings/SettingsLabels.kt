package io.github.ctorres345.hemisphere.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.ctorres345.hemisphere.core.designsystem.FaceButtonLayout
import io.github.ctorres345.hemisphere.core.model.Accent
import io.github.ctorres345.hemisphere.core.model.FaceButtons
import io.github.ctorres345.hemisphere.core.model.LayoutPin
import io.github.ctorres345.hemisphere.core.model.Orientation
import io.github.ctorres345.hemisphere.core.model.Settings
import io.github.ctorres345.hemisphere.core.model.ThemeSetting
import io.github.ctorres345.hemisphere.core.designsystem.Accent as AccentToken

// The string for each setting value shown on the Settings screen.

/** The prototype's theme order: System first, as the default. */
internal val THEME_CHOICES =
    listOf(ThemeSetting.FollowSystem, ThemeSetting.Light, ThemeSetting.Dark, ThemeSetting.AMOLED)

/** The Trash retention choices, in days (prototype: "7, 30, 90 days"). */
internal val TRASH_RETENTION_CHOICES = listOf(7, Settings.DEFAULT_TRASH_RETENTION_DAYS, 90)

/** The saved accent's design token. Both enums are named after docs/design-system.md. */
fun Accent.token(): AccentToken = AccentToken.valueOf(name)

/** A Face buttons choice; Auto names the layout it picks, as "Auto (Xbox)" in the prototype. */
@Composable
internal fun faceButtonsLabel(
    choice: FaceButtons,
    auto: FaceButtonLayout,
): String =
    if (choice == FaceButtons.Auto) {
        val picked = if (auto == FaceButtonLayout.Nintendo) FaceButtons.Nintendo else FaceButtons.Xbox
        stringResource(R.string.settings_face_auto_resolved, stringResource(picked.label()))
    } else {
        stringResource(choice.label())
    }

internal fun ThemeSetting.label(): Int =
    when (this) {
        ThemeSetting.FollowSystem -> R.string.settings_theme_system
        ThemeSetting.Light -> R.string.settings_theme_light
        ThemeSetting.Dark -> R.string.settings_theme_dark
        ThemeSetting.AMOLED -> R.string.settings_theme_amoled
    }

internal fun Orientation.label(): Int =
    when (this) {
        Orientation.Portrait -> R.string.settings_layout_portrait
        Orientation.Landscape -> R.string.settings_layout_landscape
    }

internal fun Orientation.hint(): Int =
    when (this) {
        Orientation.Portrait -> R.string.settings_layout_portrait_hint
        Orientation.Landscape -> R.string.settings_layout_landscape_hint
    }

internal fun LayoutPin.label(): Int =
    when (this) {
        LayoutPin.Auto -> R.string.settings_layout_auto
        LayoutPin.Single -> R.string.settings_layout_single
        LayoutPin.Dual -> R.string.settings_layout_dual
    }

internal fun FaceButtons.label(): Int =
    when (this) {
        FaceButtons.Auto -> R.string.settings_face_auto
        FaceButtons.Xbox -> R.string.settings_face_xbox
        FaceButtons.Nintendo -> R.string.settings_face_nintendo
    }

internal fun Accent.label(): Int =
    when (this) {
        Accent.Blue -> R.string.settings_accent_blue
        Accent.Teal -> R.string.settings_accent_teal
        Accent.Green -> R.string.settings_accent_green
        Accent.Amber -> R.string.settings_accent_amber
        Accent.Orange -> R.string.settings_accent_orange
        Accent.Red -> R.string.settings_accent_red
        Accent.Pink -> R.string.settings_accent_pink
        Accent.Violet -> R.string.settings_accent_violet
    }
