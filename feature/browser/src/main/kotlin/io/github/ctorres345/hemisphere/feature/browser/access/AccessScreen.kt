package io.github.ctorres345.hemisphere.feature.browser.access

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereShapes
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereSizes
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereSpacing
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme
import io.github.ctorres345.hemisphere.core.designsystem.ThemeStyle
import io.github.ctorres345.hemisphere.core.designsystem.ThemeStylePreviews
import io.github.ctorres345.hemisphere.core.model.StorageAccessState
import io.github.ctorres345.hemisphere.feature.browser.R

private val runtimePermissions =
    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

/**
 * The access screen bound to its ViewModel. It also opens the system pages the ViewModel asks for,
 * because that needs a Context.
 */
@Composable
fun AccessRoute(
    viewModel: AccessViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            // A cancelled request returns no results: neither a grant nor a denial, so the state stays.
            if (results.isEmpty()) return@rememberLauncherForActivityResult
            // After a denial the rationale flag is false only when the system will not ask again.
            val canAskAgain =
                context.findActivity()?.let {
                    ActivityCompat.shouldShowRequestPermissionRationale(it, runtimePermissions.first())
                } ?: true
            viewModel.onIntent(AccessIntent.PermissionResult(results.values.all { it }, canAskAgain))
        }
    LaunchedEffect(state.launch) {
        val launch = state.launch ?: return@LaunchedEffect
        when (launch.target(Build.VERSION.SDK_INT)) {
            AccessTarget.RuntimePermission -> permissionLauncher.launch(runtimePermissions)
            AccessTarget.AllFilesSettings -> context.openAllFilesSettings()
            AccessTarget.AppSettings -> context.openAppSettings()
        }
        viewModel.onIntent(AccessIntent.LaunchHandled)
    }
    AccessScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** Stateless: state in, intents out. The design system's empty state: icon, one line, one primary button. */
@Composable
fun AccessScreen(
    state: AccessUiState,
    onIntent: (AccessIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val needsSettings = state.access == StorageAccessState.NeedsSettings
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .fillMaxSize()
                    .padding(HemisphereSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HemisphereSpacing.m, Alignment.CenterVertically),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_access_folder),
                contentDescription = null,
                tint = HemisphereTheme.colors.textSecondary,
            )
            Text(
                text = stringResource(if (needsSettings) R.string.access_reason_settings else R.string.access_reason),
                style = HemisphereTheme.typography.heading,
                color = HemisphereTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            val grantInteraction = remember { MutableInteractionSource() }
            val pickInteraction = remember { MutableInteractionSource() }
            val focusRing = BorderStroke(HemisphereSizes.border * 2, HemisphereTheme.colors.accent.focus)
            // The one primary action: the accent fill.
            Button(
                onClick = { onIntent(AccessIntent.GrantClicked) },
                modifier = Modifier.focusOutline(grantInteraction.collectIsFocusedAsState().value, focusRing),
                interactionSource = grantInteraction,
                shape = HemisphereShapes.row,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = HemisphereTheme.colors.accent.fill,
                        contentColor = HemisphereTheme.colors.accent.onFill,
                    ),
                contentPadding = PaddingValues(horizontal = HemisphereSpacing.l),
            ) {
                Text(stringResource(if (needsSettings) R.string.access_open_settings else R.string.access_grant))
            }
            OutlinedButton(
                // A placeholder: the SAF fallback (HEM-41) gives this its folder picker.
                onClick = { onIntent(AccessIntent.PickFolderClicked) },
                modifier = Modifier.focusOutline(pickInteraction.collectIsFocusedAsState().value, focusRing),
                interactionSource = pickInteraction,
                shape = HemisphereShapes.row,
                border = BorderStroke(HemisphereSizes.border, HemisphereTheme.colors.borderStrong),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = HemisphereTheme.colors.textSecondary),
                contentPadding = PaddingValues(horizontal = HemisphereSpacing.l),
            ) {
                Text(stringResource(R.string.access_pick_folder))
            }
        }
    }
}

/**
 * A visible focus state for D-pad and gamepad users (design §3): an `accent.focus` outline with a gap of
 * the ring's width, so it reads against the accent fill (today `accent.focus` is `accent.fill`). The space
 * is always reserved, so focus never moves the layout. Replace with the shared `Modifier.focusRing` from
 * `core:designsystem` once HEM-33 is in.
 */
private fun Modifier.focusOutline(
    focused: Boolean,
    ring: BorderStroke,
): Modifier = (if (focused) border(ring, HemisphereShapes.row) else this).padding(ring.width * 2)

@Composable
private fun AccessPreview(
    style: ThemeStyle,
    state: AccessUiState,
) {
    HemisphereTheme(style = style) { AccessScreen(state = state, onIntent = {}) }
}

@Preview
@Composable
private fun AccessScreenDeniedPreview(
    @PreviewParameter(ThemeStylePreviews::class) style: ThemeStyle,
) {
    AccessPreview(style, AccessUiState(access = StorageAccessState.Denied))
}

@Preview
@Composable
private fun AccessScreenNeedsSettingsPreview(
    @PreviewParameter(ThemeStylePreviews::class) style: ThemeStyle,
) {
    AccessPreview(style, AccessUiState(access = StorageAccessState.NeedsSettings))
}
