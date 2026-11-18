package io.github.ctorres345.hemisphere.feature.browser

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereShapes
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereSizes
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereSpacing
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme
import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneIntent
import io.github.ctorres345.hemisphere.feature.browser.pane.TabState

/** The Browser screen bound to its ViewModel. The only composable that sees the ViewModel. */
@Composable
fun BrowserRoute(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BrowserScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/**
 * Stateless: state in, intents out. Single layout: the left pane fills the screen. System back pops
 * the pane's history; at its root folder the system handles back (architecture §7).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    state: BrowserUiState,
    onIntent: (BrowserIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val paneId = state.shell.activePane
    val tab = state.panes[paneId]?.tab
    val onPaneIntent: (PaneIntent) -> Unit = remember(paneId, onIntent) { { onIntent(BrowserIntent.Pane(paneId, it)) } }
    BackHandler(enabled = tab?.canGoBack == true) { onPaneIntent(PaneIntent.Back) }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        // safeDrawing, not Material's systemBars default, keeps text clear of a side camera cutout.
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = tab?.folder?.name ?: stringResource(R.string.browser_home),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                expandedHeight = HemisphereSizes.appBar,
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
            )
        },
    ) { padding ->
        val content = Modifier.padding(padding).consumeWindowInsets(padding)
        if (tab != null) TabContent(tab, onPaneIntent, content)
    }
}

@Composable
private fun TabContent(
    tab: TabState,
    onIntent: (PaneIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listing = tab.listing
    val error = listing.error
    when {
        tab.lock.locked -> {
            CenteredText(stringResource(R.string.browser_locked_folder), modifier)
        }

        listing.entries.isEmpty() && !listing.complete -> {
            Centered(modifier) { CircularProgressIndicator() }
        }

        listing.entries.isEmpty() && error != null -> {
            Centered(modifier) { ErrorMessage(error, onRetry = { onIntent(PaneIntent.Retry) }) }
        }

        listing.entries.isEmpty() -> {
            CenteredText(stringResource(R.string.browser_empty_folder), modifier)
        }

        else -> {
            // A new list state for each visit to a folder, so a scroll anchor is restored once per visit.
            key(tab.backStack.size, tab.folder.key) {
                EntryList(tab, onIntent = onIntent, modifier = modifier)
            }
        }
    }
}

/** The prototype's .empty: a centred block at the top of the list. */
@Composable
private fun CenteredText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(vertical = HemisphereSpacing.xxl, horizontal = HemisphereSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = HemisphereTheme.typography.heading,
            color = HemisphereTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun ErrorMessage(
    error: FsError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(HemisphereSpacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HemisphereSpacing.s),
    ) {
        Text(
            text = stringResource(error.messageRes()),
            style = HemisphereTheme.typography.body,
            color = HemisphereTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (error.canRetry()) {
            // The prototype's plain .btn: outlined in border.strong, row radius.
            OutlinedButton(
                onClick = onRetry,
                shape = HemisphereShapes.row,
                border = BorderStroke(HemisphereSizes.border, HemisphereTheme.colors.borderStrong),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = HemisphereTheme.colors.textPrimary),
                contentPadding = PaddingValues(horizontal = HemisphereSpacing.l),
            ) { Text(stringResource(R.string.browser_retry)) }
        }
    }
}

@Composable
private fun Centered(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

/** The user-facing message for each error. Raw exception text never reaches the screen (architecture §3). */
@StringRes
internal fun FsError.messageRes(): Int =
    when (this) {
        FsError.NotFound -> R.string.browser_error_not_found
        FsError.AccessDenied -> R.string.browser_error_access_denied
        FsError.VolumeUnavailable -> R.string.browser_error_volume_unavailable
        FsError.Offline, FsError.Timeout, FsError.AuthRequired -> R.string.browser_error_unreachable
        else -> R.string.browser_error_generic
    }

/** Errors that a second try can fix. */
internal fun FsError.canRetry(): Boolean = this != FsError.InvalidName && this != FsError.Unsupported
