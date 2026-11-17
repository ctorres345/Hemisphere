package io.github.ctorres345.hemisphere.feature.browser

import android.text.format.Formatter
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.ctorres345.hemisphere.core.model.Entry

@Composable
internal fun EntryList(
    state: BrowserUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        entryRows(state, onRetry)
    }
}

/** The list's rows: the error row when a listing failed part-way, then one row per entry. */
internal fun LazyListScope.entryRows(
    state: BrowserUiState,
    onRetry: () -> Unit,
) {
    val keys = rowKeys(state.entries)
    state.error?.let { error ->
        item(key = ERROR_ROW_KEY) {
            ErrorMessage(error, onRetry = onRetry, modifier = Modifier.fillMaxWidth())
        }
    }
    itemsIndexed(state.entries, key = { index, _ -> keys[index] }) { _, entry ->
        ListItem(
            headlineContent = { Text(entry.name) },
            supportingContent = { Text(entry.description()) },
        )
    }
}

@Composable
private fun Entry.description(): String =
    when (kind) {
        Entry.Kind.Folder -> stringResource(R.string.browser_kind_folder)
        Entry.Kind.File -> size?.let { Formatter.formatShortFileSize(LocalContext.current, it) }.orEmpty()
    }

/**
 * One lazy-list key per entry, unique within the listing, as a String so it can be saved.
 *
 * An entry with a provider ref (a SAF document id) is keyed by it, so it keeps its key when later
 * batches land. Otherwise the key is the kind and name; an equal one gets a `#n` suffix in listing
 * order, because some providers return two entries with the same name.
 */
internal fun rowKeys(entries: List<Entry>): List<String> {
    val used = HashSet<String>(entries.size * 2)
    return entries.map { entry ->
        val base = "entry:${entry.kind}:" + (entry.ref?.let { "ref:${it.value}" } ?: "name:${entry.name}")
        var key = base
        var n = 1
        while (!used.add(key)) key = "$base#${n++}"
        key
    }
}

/** Entry row keys start with `entry:`, so this key can never collide with one. */
private const val ERROR_ROW_KEY = "error-row"
