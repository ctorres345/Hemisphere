package io.github.ctorres345.hemisphere.feature.browser

import android.text.format.Formatter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import io.github.ctorres345.hemisphere.core.designsystem.FocusFillRipple
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereRowHeight
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereShapes
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereSizes
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereSpacing
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme
import io.github.ctorres345.hemisphere.core.designsystem.LocalInputMode
import io.github.ctorres345.hemisphere.core.designsystem.LocalSteppedDown
import io.github.ctorres345.hemisphere.core.designsystem.focusRing
import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneIntent
import io.github.ctorres345.hemisphere.feature.browser.pane.ScrollAnchor
import io.github.ctorres345.hemisphere.feature.browser.pane.TabState
import io.github.ctorres345.hemisphere.feature.browser.selection.SelectionAction
import io.github.ctorres345.hemisphere.feature.browser.selection.SelectionCheck
import io.github.ctorres345.hemisphere.feature.browser.selection.SelectionMenu
import io.github.ctorres345.hemisphere.feature.browser.selection.dividerStart
import io.github.ctorres345.hemisphere.feature.browser.selection.keyOf
import io.github.ctorres345.hemisphere.feature.browser.selection.secondaryClick
import io.github.ctorres345.hemisphere.feature.browser.selection.selecting

/**
 * The open folder's rows. Opening a folder hands the pane the anchor of the folder being left; when the
 * user comes back, the list scrolls to that anchor once its row has loaded, and a gamepad or keyboard
 * user gets focus back on the row they opened (architecture §7).
 */
@Composable
internal fun EntryList(
    tab: TabState,
    onIntent: (PaneIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val keys = remember(tab.listing.entries) { rowKeys(tab.listing.entries) }
    val focusAnchor = remember { FocusRequester() }
    RestoreAnchor(tab, keys, listState, focusAnchor)
    // TabContent already keys EntryList on the folder; tab.folder here keeps `from` right without that.
    val onOpen: (Int) -> Unit =
        remember(tab.folder, tab.listing.entries, keys, listState, onIntent) {
            { index ->
                // The first visible entry row; the error row above the entries is not one.
                val first = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key != ERROR_ROW_KEY }
                val offset =
                    if (first?.index ==
                        listState.firstVisibleItemIndex
                    ) {
                        listState.firstVisibleItemScrollOffset
                    } else {
                        0
                    }
                onIntent(
                    PaneIntent.OpenEntry(
                        entry = tab.listing.entries[index],
                        from = tab.folder,
                        leaving =
                            ScrollAnchor(
                                focusedRow = keys[index],
                                firstVisibleRow = first?.key as? String,
                                firstVisibleOffset = offset,
                            ),
                    ),
                )
            }
        }
    // The prototype's .list ends with an s2 gap below the last row.
    val callbacks =
        remember(tab.folder, tab.listing.entries, onOpen, onIntent) {
            RowCallbacks(
                onOpen = onOpen,
                onLongPress = { index ->
                    onIntent(PaneIntent.LongPress(tab.listing.entries[index], from = tab.folder))
                },
                onAction = { index, action -> onIntent(PaneIntent.Act(action, entry = tab.listing.entries[index])) },
            )
        }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = HemisphereSpacing.s),
    ) {
        entryRows(
            tab = tab,
            keys = keys,
            onRetry = { onIntent(PaneIntent.Retry) },
            callbacks = callbacks,
            focusAnchor = focusAnchor,
        )
    }
}

/**
 * Scrolls to the open folder's anchor once its first visible row is listed; then, on a gamepad or a
 * keyboard, focuses the row opened.
 */
@Composable
private fun RestoreAnchor(
    tab: TabState,
    keys: List<String>,
    listState: LazyListState,
    focusAnchor: FocusRequester,
) {
    val anchor = tab.anchor ?: return
    val inputMode = LocalInputMode.current
    // Saveable, so a rotation or process death keeps where the user scrolled since, instead of jumping back.
    var restored by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(keys, tab.listing.complete) {
        if (restored) return@LaunchedEffect
        val offset = if (tab.listing.error != null) 1 else 0 // the error row sits above the entries
        val first = keys.indexOf(anchor.firstVisibleRow)
        if (first < 0 && !tab.listing.complete) return@LaunchedEffect // not loaded yet
        restored = true
        if (first >= 0) listState.scrollToItem(first + offset, anchor.firstVisibleOffset)
        val focused = keys.indexOf(anchor.focusedRow) // -1 when the row left the listing: no row to focus
        val visible = listState.layoutInfo.visibleItemsInfo
        if (inputMode.showsFocus && focused >= 0 && visible.any { it.index == focused + offset }) {
            focusAnchor.requestFocus()
        }
    }
}

/** What a row does on a tap, a long-press and a choice in its menu, by its index in the listing. */
internal class RowCallbacks(
    val onOpen: (Int) -> Unit = {},
    val onLongPress: (Int) -> Unit = {},
    val onAction: (Int, SelectionAction) -> Unit = { _, _ -> },
)

/** The list's rows: the error row when a listing failed part-way, then one row per entry. */
internal fun LazyListScope.entryRows(
    tab: TabState,
    keys: List<String> = rowKeys(tab.listing.entries),
    onRetry: () -> Unit = {},
    callbacks: RowCallbacks = RowCallbacks(),
    focusAnchor: FocusRequester? = null,
) {
    val selecting = tab.selecting
    tab.listing.error?.let { error ->
        item(key = ERROR_ROW_KEY) {
            ErrorMessage(error, onRetry = onRetry, modifier = Modifier.fillMaxWidth())
        }
    }
    val anchored = tab.anchor?.focusedRow
    itemsIndexed(tab.listing.entries, key = { index, _ -> keys[index] }) { index, entry ->
        val locked = entry.kind == Entry.Kind.Folder && tab.lock.isChildLocked(entry.name)
        val focus =
            if (focusAnchor != null &&
                keys[index] == anchored
            ) {
                Modifier.focusRequester(focusAnchor)
            } else {
                Modifier
            }
        val look =
            RowLook(
                locked = locked,
                showDivider = index > 0,
                selecting = selecting,
                selected = selecting && tab.keyOf(entry) in tab.selection,
            )
        EntryRow(entry, look, callbacks.forRow(index), modifier = focus)
    }
}

/** How one row is drawn. While [selecting], every row shows its check; a [selected] row is tinted. */
private data class RowLook(
    val locked: Boolean,
    val showDivider: Boolean,
    val selecting: Boolean,
    val selected: Boolean,
)

/** One row's callbacks, bound to its index. */
private class RowHandlers(
    val onClick: () -> Unit,
    val onLongClick: () -> Unit,
    val onAction: (SelectionAction) -> Unit,
)

private fun RowCallbacks.forRow(index: Int) =
    RowHandlers(
        onClick = { onOpen(index) },
        onLongClick = { onLongPress(index) },
        onAction = { onAction(index, it) },
    )

/**
 * One file or folder row, as in the prototype's `.row`: name on the left, size or kind on the right. A
 * folder Android keeps private shows a lock. Rows take focus, with the shared ring, for D-pad users.
 * A long-press selects; a secondary click (mouse) or the Menu key opens the row's menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryRow(
    entry: Entry,
    look: RowLook,
    handlers: RowHandlers,
    modifier: Modifier = Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalRippleConfiguration provides FocusFillRipple) {
        Box(modifier = modifier.fillMaxWidth().padding(horizontal = HemisphereSpacing.xs)) {
            val onMenu = remember { { menu = true } }
            EntryRowContent(entry, look, handlers, onMenu)
            SelectionMenu(expanded = menu, onDismiss = { menu = false }, onAction = handlers.onAction)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryRowContent(
    entry: Entry,
    look: RowLook,
    handlers: RowHandlers,
    onMenu: () -> Unit,
) {
    val colors = HemisphereTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRing(fill = true)
                // After the ring, so the press highlight is rounded as the prototype's .row:active.
                .clip(HemisphereShapes.row)
                .then(if (look.selected) Modifier.background(colors.accent.tint) else Modifier)
                .secondaryClick(onMenu)
                .onPreviewKeyEvent { event ->
                    val menuKey = event.key == Key.Menu && event.type == KeyEventType.KeyUp
                    if (menuKey) onMenu()
                    menuKey
                }.combinedClickable(
                    onClick = handlers.onClick,
                    onLongClick = handlers.onLongClick,
                    onLongClickLabel = stringResource(R.string.selection_select),
                )
                // One screen-reader stop per row (name, then size or kind), as the prototype's aria-label.
                .semantics(mergeDescendants = true) { if (look.selecting) selected = look.selected }
                .heightIn(min = HemisphereRowHeight.default)
                .padding(start = HemisphereSpacing.l, end = HemisphereSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HemisphereSpacing.m),
    ) {
        if (look.selecting) SelectionCheck(look.selected)
        Text(
            text = entry.name,
            style = HemisphereTheme.typography.body,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            modifier = Modifier.weight(1f),
        )
        if (look.locked) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = stringResource(R.string.browser_locked_row),
                tint = colors.textSecondary,
            )
        } else {
            Text(
                text = entry.description(),
                style = HemisphereTheme.typography.meta,
                // Stepped down, primary is the base secondary: a selected row keeps that on the tint.
                color = if (look.selected && LocalSteppedDown.current) colors.textPrimary else colors.textSecondary,
                maxLines = 1,
            )
        }
    }
    if (look.showDivider) {
        HorizontalDivider(
            thickness = HemisphereSizes.border,
            color = colors.borderDefault,
            // While selecting the divider starts after the check, as the prototype's .list.selecting.
            modifier = Modifier.padding(start = dividerStart(look.selecting), end = HemisphereSpacing.m),
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
