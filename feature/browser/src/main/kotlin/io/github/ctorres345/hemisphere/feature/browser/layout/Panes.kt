package io.github.ctorres345.hemisphere.feature.browser.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereRowHeight
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereSizes
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereSpacing
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme
import io.github.ctorres345.hemisphere.core.designsystem.SteppedDownText
import io.github.ctorres345.hemisphere.feature.browser.BrowserIntent
import io.github.ctorres345.hemisphere.feature.browser.BrowserUiState
import io.github.ctorres345.hemisphere.feature.browser.R
import io.github.ctorres345.hemisphere.feature.browser.TabContent
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneId
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneIntent
import io.github.ctorres345.hemisphere.feature.browser.pane.TabState
import io.github.ctorres345.hemisphere.feature.browser.selection.PaneSelectionHeader
import io.github.ctorres345.hemisphere.feature.browser.selection.selecting
import io.github.ctorres345.hemisphere.feature.browser.selection.selectionSummary

/**
 * The panes on screen: in Single the active pane alone, in Dual both side by side, the left one first
 * (mirrored in RTL), with a border between them. Both layouts go through this one keyed loop, so a pane
 * keeps its list state, scroll position included, when the layout switches.
 */
@Composable
internal fun Panes(
    state: BrowserUiState,
    onIntent: (BrowserIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dual = state.shell.layout == PaneLayout.Dual
    // The active pane's selection row; the other pane keeps the row's height, so the lists line up.
    val selectionRow = dual && state.panes[state.shell.activePane]?.tab?.selecting == true
    Row(modifier.fillMaxSize()) {
        state.shownPanes.forEachIndexed { index, id ->
            if (index >
                0
            ) {
                VerticalDivider(thickness = HemisphereSizes.border, color = HemisphereTheme.colors.borderDefault)
            }
            key(id) {
                val active = id == state.shell.activePane
                Pane(
                    id,
                    state.panes.getValue(id).tab,
                    PaneLook(dual, active, selectionRow),
                    onIntent,
                    Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * How a pane is drawn: with a header in Dual, and stepped down when it is not the active one. While the
 * active pane is selecting, its header holds a [selectionRow] (prototype `.psel`).
 */
private data class PaneLook(
    val dual: Boolean,
    val active: Boolean,
    val selectionRow: Boolean,
)

/** One pane: in Dual its header, then its folder. In Dual a touch or focus in it makes it active. */
@Composable
private fun Pane(
    id: PaneId,
    tab: TabState,
    look: PaneLook,
    onIntent: (BrowserIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onPaneIntent: (PaneIntent) -> Unit = remember(id, onIntent) { { onIntent(BrowserIntent.Pane(id, it)) } }
    val activate by rememberUpdatedState { if (!look.active) onIntent(BrowserIntent.Activate(id)) }
    // Named by the side it is drawn on: RTL mirrors the row, so the left pane shows on the right.
    val onLeft = (id == PaneId.Left) == (LocalLayoutDirection.current == LayoutDirection.Ltr)
    val name = stringResource(if (onLeft) R.string.browser_pane_left else R.string.browser_pane_right)
    val label = if (look.active) stringResource(R.string.browser_pane_active, name) else name
    Column(
        modifier =
            modifier
                .fillMaxHeight()
                // The Initial pass sees the touch before the row does, and leaves it to the row: a tap on a
                // row of the inactive pane makes the pane active and opens the row.
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        activate()
                    }
                }.onFocusChanged { if (it.hasFocus) activate() }
                .semantics {
                    if (look.dual) {
                        isTraversalGroup = true
                        paneTitle = label
                    }
                },
    ) {
        // Composed in both layouts (empty in Single), so the content below keeps its place on a switch.
        // The selection row sits above the folder name, as the prototype's `.psel` above its crumbs.
        if (look.dual) {
            PaneHeader(tab, look.active) {
                when {
                    !look.selectionRow -> {}

                    look.active -> {
                        val summary = remember(tab.listing, tab.selection) { tab.selectionSummary() }
                        PaneSelectionHeader(summary, onPaneIntent)
                    }

                    else -> {
                        Spacer(Modifier.height(HemisphereSizes.selectionBar))
                    }
                }
            }
        }
        SteppedDownText(enabled = !look.active) { TabContent(tab, onPaneIntent, Modifier.weight(1f)) }
    }
}

/**
 * The pane's header (prototype `.phead`): the accent bar on top in the active pane, then [above] (the
 * selection row, if any), then the folder name.
 */
@Composable
private fun PaneHeader(
    tab: TabState,
    active: Boolean,
    above: @Composable () -> Unit,
) {
    val colors = HemisphereTheme.colors
    Column(Modifier.fillMaxWidth()) {
        PaneAccentBar(active)
        above()
        Box(Modifier.fillMaxWidth().height(HemisphereRowHeight.compact), contentAlignment = Alignment.CenterStart) {
            Text(
                text = tab.folder.name ?: stringResource(R.string.browser_home),
                style = HemisphereTheme.typography.body,
                color = if (active) colors.textPrimary else colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = HemisphereSpacing.l, end = HemisphereSpacing.m),
            )
        }
    }
}

/**
 * The bar on top of a Dual pane's header, filled in the active pane (prototype `.pane.active
 * .phead::before`). Its height is kept when inactive, so the headers line up and nothing shifts on a switch.
 */
@Composable
private fun PaneAccentBar(active: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(HemisphereSizes.activePaneBar)
            .then(if (active) Modifier.background(HemisphereTheme.colors.accent.fill) else Modifier),
    )
}
