package io.github.ctorres345.hemisphere.feature.browser

import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereSpacing
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme
import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsError

/** The Browser screen bound to its ViewModel. The only composable that sees the ViewModel. */
@Composable
fun BrowserRoute(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BrowserScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** Stateless: state in, intents out. */
@Composable
fun BrowserScreen(
    state: BrowserUiState,
    onIntent: (BrowserIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            Text(
                text = state.folder?.name ?: stringResource(R.string.browser_home),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(HemisphereSpacing.l),
            )
            val error = state.error
            when {
                state.entries.isEmpty() && state.loading -> {
                    Centered { CircularProgressIndicator() }
                }

                state.entries.isEmpty() && error != null -> {
                    Centered { ErrorMessage(error, onRetry = { onIntent(BrowserIntent.Retry) }) }
                }

                state.entries.isEmpty() -> {
                    Centered { Text(stringResource(R.string.browser_empty_folder)) }
                }

                else -> {
                    EntryList(state, onRetry = { onIntent(BrowserIntent.Retry) })
                }
            }
        }
    }
}

@Composable
private fun EntryList(
    state: BrowserUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        state.error?.let { error ->
            item(key = ERROR_ROW_KEY) {
                ErrorMessage(error, onRetry = onRetry, modifier = Modifier.fillMaxWidth())
            }
        }
        items(state.entries, key = { "entry:${it.name}" }) { entry ->
            ListItem(
                headlineContent = { Text(entry.name) },
                supportingContent = { Text(entry.description()) },
            )
        }
    }
}

@Composable
private fun Entry.description(): String =
    when (kind) {
        Entry.Kind.Folder -> stringResource(R.string.browser_kind_folder)
        Entry.Kind.File -> size?.let { Formatter.formatShortFileSize(LocalContext.current, it) }.orEmpty()
    }

@Composable
private fun ErrorMessage(
    error: FsError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(HemisphereSpacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HemisphereSpacing.s),
    ) {
        Text(text = stringResource(error.messageRes()), textAlign = TextAlign.Center)
        if (error.canRetry()) {
            Button(onClick = onRetry) { Text(stringResource(R.string.browser_retry)) }
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

@Preview
@Composable
private fun BrowserScreenListPreview() {
    HemisphereTheme {
        BrowserScreen(
            state =
                BrowserUiState(
                    entries =
                        listOf(
                            Entry("Music", Entry.Kind.Folder),
                            Entry("Readme.txt", Entry.Kind.File, size = 2048),
                        ),
                    loading = false,
                ),
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun BrowserScreenErrorPreview() {
    HemisphereTheme {
        BrowserScreen(state = BrowserUiState(loading = false, error = FsError.AccessDenied), onIntent = {})
    }
}

/** Entry rows are keyed `entry:<name>`, so this key can never collide with one. */
private const val ERROR_ROW_KEY = "error-row"
