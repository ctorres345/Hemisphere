package io.github.ctorres345.hemisphere.feature.browser.pane

import android.os.Bundle
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.ProviderRef
import io.github.ctorres345.hemisphere.core.model.decodeFsPath
import io.github.ctorres345.hemisphere.core.model.encode

// The bundle form of `SavedPanes`. Writing goes through a [SavedSink], so the size count and the
// bundle never disagree about what is saved.

internal fun SavedSink.writePanes(panes: Map<PaneId, PaneState>) =
    panes.forEach { (id, pane) ->
        putInt("${id.name}.$ACTIVE_TAB", pane.activeTab)
        putInt("${id.name}.$TAB_COUNT", pane.tabs.size)
        pane.tabs.forEachIndexed { index, tab -> putTab("${id.name}.$index", tab) }
    }

private fun SavedSink.putTab(
    prefix: String,
    tab: TabState,
) {
    val stack = tab.backStack
    // Per level above the base: the level itself when it is a step down, else null (a jump).
    val steps = stack.zipWithNext { below, level -> level.takeIf { it.isStepFrom(below) } }
    putString("$prefix.$BASE", stack.first().encode())
    putStrings("$prefix.$NAMES", steps.mapIndexed { i, step -> step?.name ?: stack[i + 1].encode() })
    putStrings("$prefix.$REFS", steps.map { step -> step?.ref.encodeRef() })
    putInts("$prefix.$JUMPS", steps.map { if (it == null) 1 else 0 }.toIntArray())
    val anchors = tab.anchors.byLevel(stack)
    putInts("$prefix.$ANCHOR_LEVELS", anchors.map { it.first }.toIntArray())
    putStrings("$prefix.$ANCHOR_FOCUSED", anchors.map { it.second.focusedRow.orEmpty() })
    putStrings("$prefix.$ANCHOR_FIRST", anchors.map { it.second.firstVisibleRow.orEmpty() })
    putInts("$prefix.$ANCHOR_OFFSETS", anchors.map { it.second.firstVisibleOffset }.toIntArray())
}

internal fun Bundle.readPanes(): Map<PaneId, PaneState> =
    PaneId.entries
        .mapNotNull { id ->
            val count = getInt("${id.name}.$TAB_COUNT")
            val tabs = (0 until count).mapNotNull { getTab("${id.name}.$it") }
            if (tabs.isEmpty() || tabs.size != count) return@mapNotNull null
            id to PaneState(tabs, getInt("${id.name}.$ACTIVE_TAB").coerceIn(tabs.indices))
        }.toMap()

/** Null when the tab is missing or a level no longer rebuilds; the caller then starts that pane over. */
private fun Bundle.getTab(prefix: String): TabState? {
    val full = getBackStack(prefix) ?: return null
    val backStack = full.takeLast(BACK_STACK_LIMIT)
    val levels = getIntArray("$prefix.$ANCHOR_LEVELS") ?: IntArray(0)
    val focused = getStringArrayList("$prefix.$ANCHOR_FOCUSED").orEmpty()
    val first = getStringArrayList("$prefix.$ANCHOR_FIRST").orEmpty()
    val offsets = getIntArray("$prefix.$ANCHOR_OFFSETS") ?: IntArray(0)
    val anchors =
        levels
            .withIndex()
            .mapNotNull { (i, level) ->
                full.getOrNull(level)?.let { path ->
                    path.key to
                        ScrollAnchor(
                            focusedRow = focused.getOrNull(i)?.ifEmpty { null },
                            firstVisibleRow = first.getOrNull(i)?.ifEmpty { null },
                            firstVisibleOffset = offsets.getOrElse(i) { 0 },
                        )
                }
            }.toMap()
    return TabState(backStack = backStack, anchors = anchors.keptFor(backStack))
}

/** Null when the stack is missing, or any level no longer decodes or rebuilds. */
private fun Bundle.getBackStack(prefix: String): List<FsPath>? {
    val base = getString("$prefix.$BASE")?.let { decodeFsPath(it).okOrNull() }
    val names = getStringArrayList("$prefix.$NAMES").orEmpty()
    val refs = getStringArrayList("$prefix.$REFS").orEmpty()
    val jumps = getIntArray("$prefix.$JUMPS") ?: IntArray(0)
    if (base == null || refs.size != names.size || jumps.size != names.size) return null
    val stack =
        names.indices.runningFold<Int, FsPath?>(base) { below, i ->
            below?.let { rebuildLevel(it, names[i], refs[i], jump = jumps[i] != 0) }
        }
    return stack.takeIf { null !in it }?.filterNotNull()
}

/** The level above [below]: a full path for a jump, else [below]'s child [text] with its saved ref. */
private fun rebuildLevel(
    below: FsPath,
    text: String,
    ref: String,
    jump: Boolean,
): FsPath? =
    when {
        jump -> decodeFsPath(text).okOrNull()
        !text.isValidName() -> null
        ref.isEmpty() -> below.child(text)
        ref.startsWith(REF_MARK) -> below.child(text, ProviderRef(ref.substring(1)))
        else -> null
    }

/** True when this path is [below]'s child with the same flags, so a name and ref rebuild it. */
private fun FsPath.isStepFrom(below: FsPath): Boolean =
    name?.isValidName() == true &&
        volume == below.volume &&
        caseInsensitive == below.caseInsensitive &&
        idAddressed == below.idAddressed &&
        segments.size == below.segments.size + 1 &&
        segments.subList(0, below.segments.size) == below.segments

private fun ProviderRef?.encodeRef(): String = this?.let { REF_MARK + it.value }.orEmpty()

private fun String.isValidName(): Boolean = isNotEmpty() && '/' !in this

private fun <T> FsResult<T>.okOrNull(): T? = (this as? FsResult.Ok<T>)?.value

private const val REF_MARK = "~"
private const val ACTIVE_TAB = "activeTab"
private const val TAB_COUNT = "tabCount"
private const val BASE = "base"
private const val NAMES = "names"
private const val REFS = "refs"
private const val JUMPS = "jumps"
private const val ANCHOR_LEVELS = "anchorLevels"
private const val ANCHOR_FOCUSED = "anchorFocused"
private const val ANCHOR_FIRST = "anchorFirst"
private const val ANCHOR_OFFSETS = "anchorOffsets"
