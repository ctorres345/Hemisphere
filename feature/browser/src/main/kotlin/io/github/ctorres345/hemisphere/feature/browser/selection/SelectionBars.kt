package io.github.ctorres345.hemisphere.feature.browser.selection

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereShapes
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereSizes
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereSpacing
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme
import io.github.ctorres345.hemisphere.core.designsystem.focusRing
import io.github.ctorres345.hemisphere.feature.browser.R
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneIntent

/** "3 selected", or "3 selected · 1.2 GB" when only files of known size are selected. */
@Composable
internal fun selectionTitle(summary: SelectionSummary): String {
    val bytes =
        summary.fileBytes ?: return pluralStringResource(R.plurals.selection_count, summary.count, summary.count)
    return pluralStringResource(
        R.plurals.selection_count_size,
        summary.count,
        summary.count,
        Formatter.formatShortFileSize(LocalContext.current, bytes),
    )
}

/**
 * Single's app bar while selecting (prototype `.appbar.selhead`): Leave selection, the count over the
 * open folder's name (null at a root, shown as Home), Select all.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectionTopBar(
    summary: SelectionSummary,
    folderName: String?,
    onIntent: (PaneIntent) -> Unit,
) {
    val colors = HemisphereTheme.colors
    TopAppBar(
        title = {
            Column {
                Text(
                    text = selectionTitle(summary),
                    style = HemisphereTheme.typography.heading.merge(TABULAR),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = folderName ?: stringResource(R.string.browser_home),
                    style = HemisphereTheme.typography.meta,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = { LeaveSelection(onIntent) },
        actions = {
            SelectAll(onIntent)
        },
        expandedHeight = HemisphereSizes.appBar,
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = colors.accent.tint,
                titleContentColor = colors.textPrimary,
            ),
    )
}

@Composable
private fun SelectAll(
    onIntent: (PaneIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = { onIntent(PaneIntent.SelectAll) }, modifier = modifier.focusRing()) {
        Icon(
            painter = painterResource(R.drawable.ic_select_all),
            contentDescription = stringResource(R.string.selection_select_all),
            tint = HemisphereTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun LeaveSelection(
    onIntent: (PaneIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = { onIntent(PaneIntent.ClearSelection) }, modifier = modifier.focusRing()) {
        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = stringResource(R.string.selection_leave),
            tint = HemisphereTheme.colors.textSecondary,
        )
    }
}

/** The phone's action bar at the bottom while selecting (prototype `.actbar`): icon over label. */
@Composable
internal fun SelectionActionBar(
    summary: SelectionSummary,
    onIntent: (PaneIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HemisphereTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.surfaceRaised)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            ),
    ) {
        HorizontalDivider(thickness = HemisphereSizes.border, color = colors.borderDefault)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = HemisphereSpacing.xs),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            SelectionAction.entries.forEach { action -> BarAction(action, summary.allows(action), onIntent) }
        }
    }
}

@Composable
private fun BarAction(
    action: SelectionAction,
    enabled: Boolean,
    onIntent: (PaneIntent) -> Unit,
) {
    val color = action.color(enabled)
    TextButton(
        onClick = { onIntent(PaneIntent.Act(action)) },
        enabled = enabled,
        shape = HemisphereShapes.row,
        contentPadding = PaddingValues(horizontal = HemisphereSpacing.s, vertical = HemisphereSpacing.xs),
        modifier =
            Modifier
                .widthIn(min = HemisphereSizes.actionMinWidth)
                .focusRing(),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painter = painterResource(action.icon()), contentDescription = null, tint = color)
            Text(text = stringResource(action.label()), style = HemisphereTheme.typography.caption, color = color)
        }
    }
}

/**
 * The active pane's selection row in Dual (prototype `.psel`): Leave selection, the count, Copy →
 * and Move → to the other pane, Delete, and an overflow with Select all, Rename and More. A narrow pane
 * (a 640dp handheld) shows Copy and Move as icons, so the count keeps its room.
 */
@Composable
internal fun PaneSelectionHeader(
    summary: SelectionSummary,
    onIntent: (PaneIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val compact = maxWidth < HemisphereSizes.selectionLabelsMinWidth
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(HemisphereSizes.selectionBar)
                    .background(HemisphereTheme.colors.accent.tint)
                    .padding(start = HemisphereSpacing.xs, end = HemisphereSpacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HemisphereSpacing.xs),
        ) {
            LeaveSelection(onIntent, Modifier.size(HemisphereSizes.selectionBar))
            Text(
                text = selectionTitle(summary),
                style = HemisphereTheme.typography.body.merge(TABULAR),
                fontWeight = HemisphereTheme.typography.heading.fontWeight,
                color = HemisphereTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            HEADER_ACTIONS.forEach { action ->
                HeaderAction(action, summary.allows(action), compact = compact, onIntent = onIntent)
            }
            HeaderOverflow(summary, onIntent)
        }
    }
}

/** The header's own buttons; the rest sit in its overflow. */
private val HEADER_ACTIONS = listOf(SelectionAction.Copy, SelectionAction.Move, SelectionAction.Delete)

/** Digits of equal width, so the count does not shift as it changes (prototype `tabular-nums`). */
private val TABULAR = TextStyle(fontFeatureSettings = "tnum")

/**
 * Copy and Move show their label and an arrow to the other pane (prototype `.psel .btn`) unless [compact];
 * the rest are icon buttons, named for screen readers.
 */
@Composable
private fun HeaderAction(
    action: SelectionAction,
    enabled: Boolean,
    compact: Boolean,
    onIntent: (PaneIntent) -> Unit,
) {
    val color = action.color(enabled)
    val onClick = { onIntent(PaneIntent.Act(action)) }
    // Copy and Move go to the other pane; screen readers hear that, not only the verb.
    val name =
        when (action) {
            SelectionAction.Copy -> stringResource(R.string.selection_copy_to_other)
            SelectionAction.Move -> stringResource(R.string.selection_move_to_other)
            else -> stringResource(action.label())
        }
    if (compact || action == SelectionAction.Delete) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(HemisphereSizes.selectionBar).focusRing(),
        ) {
            Icon(
                painter = painterResource(action.icon()),
                contentDescription = name,
                tint = color,
                modifier = Modifier.size(HemisphereSizes.buttonIcon),
            )
        }
        return
    }
    TextButton(
        onClick = onClick,
        enabled = enabled,
        shape = HemisphereShapes.row,
        contentPadding = PaddingValues(horizontal = HemisphereSpacing.s),
        modifier =
            Modifier
                .height(HemisphereSizes.selectionButton)
                .semantics(mergeDescendants = true) { contentDescription = name }
                .focusRing(),
    ) {
        val icon = Modifier.size(HemisphereSizes.buttonIcon)
        if (action == SelectionAction.Copy) {
            Icon(painterResource(action.icon()), contentDescription = null, tint = color, modifier = icon)
        }
        Text(
            text = stringResource(action.label()),
            style = HemisphereTheme.typography.meta,
            fontWeight = HemisphereTheme.typography.heading.fontWeight,
            color = color,
            maxLines = 1,
            // The prototype's .btn gap between icon, label and arrow; Move has no icon before its label.
            modifier =
                if (action == SelectionAction.Copy) {
                    Modifier.padding(horizontal = HemisphereSpacing.s)
                } else {
                    Modifier.padding(end = HemisphereSpacing.s)
                },
        )
        // The arrow says where it goes: the other pane (design-system §10, "Copy → other pane").
        Icon(painterResource(R.drawable.ic_move), contentDescription = null, tint = color, modifier = icon)
    }
}

/** The header's More button: Select all, Rename (one item only) and the further actions. */
@Composable
private fun HeaderOverflow(
    summary: SelectionSummary,
    onIntent: (PaneIntent) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { open = true },
            modifier = Modifier.size(HemisphereSizes.selectionBar).focusRing(),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_more),
                contentDescription = stringResource(R.string.action_more),
                tint = HemisphereTheme.colors.textSecondary,
                modifier = Modifier.size(HemisphereSizes.buttonIcon),
            )
        }
        SelectionMenu(
            expanded = open,
            onDismiss = { open = false },
            onSelectAll = { onIntent(PaneIntent.SelectAll) },
            actions = listOf(SelectionAction.Rename, SelectionAction.More).filter(summary::allows),
            onAction = { onIntent(PaneIntent.Act(it)) },
        )
    }
}
