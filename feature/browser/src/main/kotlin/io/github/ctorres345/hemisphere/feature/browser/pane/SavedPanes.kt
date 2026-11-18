package io.github.ctorres345.hemisphere.feature.browser.pane

import android.os.Bundle
import io.github.ctorres345.hemisphere.core.model.EntryKey
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.decodeEntryKey
import io.github.ctorres345.hemisphere.core.model.decodeFsPath
import io.github.ctorres345.hemisphere.core.model.encode

/**
 * What survives process death (architecture §6.1): each tab's back stack and scroll anchors, never
 * listings, locks or selection. Paths use the stable `FsPath` string form from `core:model`.
 *
 * The bundle holds, per pane and tab, `<pane>.<tab>.stack` (encoded paths, bottom first) and four
 * parallel anchor lists. With the 50-folder cap a full Dual state stays a few KB, far below the
 * binder limit.
 */
internal object SavedPanes {
    /** The key of the bundle in `SavedStateHandle`. */
    const val KEY = "browser.panes"

    fun save(panes: Map<PaneId, PaneState>): Bundle =
        Bundle().apply {
            panes.forEach { (id, pane) ->
                putInt("${id.name}.$ACTIVE_TAB", pane.activeTab)
                putInt("${id.name}.$TAB_COUNT", pane.tabs.size)
                pane.tabs.forEachIndexed { index, tab -> putTab("${id.name}.$index", tab) }
            }
        }

    /** The saved panes, or null for a pane that was not saved or no longer reads back. */
    fun restore(bundle: Bundle?): Map<PaneId, PaneState> {
        if (bundle == null) return emptyMap()
        return PaneId.entries
            .mapNotNull { id ->
                val count = bundle.getInt("${id.name}.$TAB_COUNT")
                val tabs = (0 until count).map { bundle.getTab("${id.name}.$it") ?: return@mapNotNull null }
                if (tabs.isEmpty()) return@mapNotNull null
                id to PaneState(tabs, bundle.getInt("${id.name}.$ACTIVE_TAB").coerceIn(tabs.indices))
            }.toMap()
    }

    private fun Bundle.putTab(
        prefix: String,
        tab: TabState,
    ) {
        putStringArrayList("$prefix.$STACK", ArrayList(tab.backStack.map { it.encode() }))
        val anchors = tab.anchors.entries.toList()
        putStringArrayList("$prefix.$ANCHOR_KEYS", ArrayList(anchors.map { it.key.encode() }))
        putStringArrayList("$prefix.$ANCHOR_FOCUSED", ArrayList(anchors.map { it.value.focusedRow.orEmpty() }))
        putStringArrayList("$prefix.$ANCHOR_FIRST", ArrayList(anchors.map { it.value.firstVisibleRow.orEmpty() }))
        putIntArray("$prefix.$ANCHOR_OFFSETS", anchors.map { it.value.firstVisibleOffset }.toIntArray())
    }

    /** Null when the tab is missing or a path no longer decodes; the caller then starts that pane over. */
    private fun Bundle.getTab(prefix: String): TabState? {
        val backStack = getBackStack(prefix) ?: return null
        val keys = getStringArrayList("$prefix.$ANCHOR_KEYS").orEmpty()
        val focused = getStringArrayList("$prefix.$ANCHOR_FOCUSED").orEmpty()
        val first = getStringArrayList("$prefix.$ANCHOR_FIRST").orEmpty()
        val offsets = getIntArray("$prefix.$ANCHOR_OFFSETS") ?: IntArray(0)
        val anchors = mutableMapOf<EntryKey, ScrollAnchor>()
        keys.forEachIndexed { i, text ->
            val key = (decodeEntryKey(text) as? FsResult.Ok<EntryKey>)?.value ?: return@forEachIndexed
            anchors[key] =
                ScrollAnchor(
                    focusedRow = focused.getOrNull(i)?.ifEmpty { null },
                    firstVisibleRow = first.getOrNull(i)?.ifEmpty { null },
                    firstVisibleOffset = offsets.getOrElse(i) { 0 },
                )
        }
        return TabState(backStack = backStack, anchors = anchors.keptFor(backStack))
    }

    /** Null when the stack is missing, empty, or any path no longer decodes. */
    private fun Bundle.getBackStack(prefix: String): List<FsPath>? {
        val encoded = getStringArrayList("$prefix.$STACK") ?: return null
        val paths = encoded.mapNotNull { (decodeFsPath(it) as? FsResult.Ok<FsPath>)?.value }
        return paths.takeIf { it.isNotEmpty() && it.size == encoded.size }?.takeLast(BACK_STACK_LIMIT)
    }

    private const val ACTIVE_TAB = "activeTab"
    private const val TAB_COUNT = "tabCount"
    private const val STACK = "stack"
    private const val ANCHOR_KEYS = "anchorKeys"
    private const val ANCHOR_FOCUSED = "anchorFocused"
    private const val ANCHOR_FIRST = "anchorFirst"
    private const val ANCHOR_OFFSETS = "anchorOffsets"
}
