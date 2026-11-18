package io.github.ctorres345.hemisphere.feature.browser.pane

import android.os.Bundle

/**
 * What survives process death (architecture §6.1): each tab's back stack and scroll anchors, never
 * listings, locks or selection. Paths use the stable `FsPath` string form from `core:model`.
 *
 * Per pane and tab, under `<pane>.<tab>.`:
 * - `base`: the bottom of the stack as a full path.
 * - `names`, `refs`, `jumps`: one entry per level above it. A step down holds the folder name and its
 *   ref (`""` for none, else `~` then the ref) and rebuilds as `<level below>.child(name, ref)`. A jump
 *   (a level that is not a child of the one below) holds its full path in `names`.
 * - `anchorLevels`, `anchorFocused`, `anchorFirst`, `anchorOffsets`: anchors by stack index.
 *
 * The whole bundle stays under [BUDGET_BYTES]: over it, [save] drops the oldest anchors, then the
 * oldest stack levels, so the binder never gets an oversized state. Format: `SavedPanesFormat.kt`.
 */
internal object SavedPanes {
    /** The key of the bundle in `SavedStateHandle`. */
    const val KEY = "browser.panes"

    /** The most the saved panes may take, counted by [encodedSize]. */
    const val BUDGET_BYTES = 200 * 1024

    fun save(panes: Map<PaneId, PaneState>): Bundle = Bundle().also { BundleSink(it).writePanes(trimmed(panes)) }

    /** What [save] would write for [panes] before any trim: UTF-16 bytes of every key and string, four per int. */
    fun encodedSize(panes: Map<PaneId, PaneState>): Int = SizeCounter().also { it.writePanes(panes) }.bytes

    /**
     * [panes] cut down to [BUDGET_BYTES]: the oldest anchors go first, then the oldest stack levels.
     * Nothing at all when even the open folders alone are over it, so both panes start on home.
     */
    fun trimmed(panes: Map<PaneId, PaneState>): Map<PaneId, PaneState> {
        var current = panes
        while (encodedSize(current) > BUDGET_BYTES) {
            current = current.withoutOldestAnchor() ?: current.withoutOldestLevel() ?: return emptyMap()
        }
        return current
    }

    /** The saved panes, or null for a pane that was not saved or no longer reads back. */
    fun restore(bundle: Bundle?): Map<PaneId, PaneState> = bundle?.readPanes().orEmpty()
}
