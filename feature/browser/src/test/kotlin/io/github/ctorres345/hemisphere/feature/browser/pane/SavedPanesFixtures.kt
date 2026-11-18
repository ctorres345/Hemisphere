package io.github.ctorres345.hemisphere.feature.browser.pane

import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.ProviderRef
import io.github.ctorres345.hemisphere.core.model.VolumeId

/** The longest name most file systems allow. */
internal const val MAX_NAME = 255

/** A [MAX_NAME]-character name that differs by [level] and [pane]. */
internal fun longName(
    level: Int,
    pane: Char = 'L',
): String = "$pane$level-".padEnd(MAX_NAME, 'x')

/** The root of an id-addressed volume (SAF), where paths key by their ref. */
internal val idRoot: FsPath = FsPath.root(VolumeId("sdcard-1234"), caseInsensitive = true, idAddressed = true)

/** [BACK_STACK_LIMIT] levels from [idRoot], each a long-named child with its own ref. */
internal fun deepStack(pane: Char): List<FsPath> =
    (1 until BACK_STACK_LIMIT).runningFold(idRoot) { below, level ->
        below.child(longName(level, pane), ProviderRef("doc-$pane-$level-0123456789abcdef"))
    }

/** A tab on [stack] with an anchor on every folder below the top, naming long rows. */
internal fun tabWithAnchors(stack: List<FsPath>): TabState =
    TabState(
        backStack = stack,
        anchors =
            stack.dropLast(1).withIndex().associate { (i, path) ->
                path.key to
                    ScrollAnchor(
                        focusedRow = longName(i, 'f'),
                        firstVisibleRow = longName(i, 'v'),
                        firstVisibleOffset = i,
                    )
            },
    )

/**
 * The ticket's worst case: both panes (Dual), 50 deep, 255-character names, short refs and anchors
 * everywhere. Refs that grow with depth (path-shaped SAF ids) go over budget and are trimmed instead.
 */
internal fun worstCasePanes(): Map<PaneId, PaneState> =
    mapOf(
        PaneId.Left to PaneState(listOf(tabWithAnchors(deepStack('L')))),
        PaneId.Right to PaneState(listOf(tabWithAnchors(deepStack('R')))),
    )
