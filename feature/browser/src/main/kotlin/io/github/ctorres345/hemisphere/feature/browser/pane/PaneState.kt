package io.github.ctorres345.hemisphere.feature.browser.pane

import io.github.ctorres345.hemisphere.core.domain.files.FolderLock
import io.github.ctorres345.hemisphere.core.model.EntryKey
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.Listing

/** The two panes. Single shows [Left] only; Dual (HEM-54) shows both. */
enum class PaneId { Left, Right }

/** One pane: its tabs and which one is shown. The MVP has one tab per pane (architecture §6.1). */
data class PaneState(
    val tabs: List<TabState>,
    val activeTab: Int = 0,
) {
    val tab: TabState get() = tabs[activeTab]

    companion object {
        /** A pane with one tab on [folder]. */
        fun on(folder: FsPath): PaneState = PaneState(tabs = listOf(TabState(backStack = listOf(folder))))
    }
}

/**
 * One tab: its history, where the user was in each folder on it, the open folder's listing and the
 * selection. [backStack] is never empty; its last folder is the open one. [lock] says whether Android
 * keeps the open folder, or some of its children, private.
 */
data class TabState(
    val backStack: List<FsPath>,
    val anchors: Map<EntryKey, ScrollAnchor> = emptyMap(),
    val listing: Listing = LOADING,
    val selection: Set<EntryKey> = emptySet(),
    val lock: FolderLock = FolderLock.NONE,
) {
    init {
        require(backStack.isNotEmpty()) { "a tab always has a folder open" }
    }

    /** The open folder. */
    val folder: FsPath get() = backStack.last()

    /** Where to put the list back when the open folder shows again, or null to start at the top. */
    val anchor: ScrollAnchor? get() = anchors[folder.key]

    val canGoBack: Boolean get() = backStack.size > 1

    companion object {
        /** A folder being read for the first time: no rows yet. */
        val LOADING = Listing(entries = emptyList(), complete = false)
    }
}
