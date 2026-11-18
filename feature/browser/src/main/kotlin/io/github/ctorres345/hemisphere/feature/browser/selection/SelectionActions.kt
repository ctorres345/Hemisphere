package io.github.ctorres345.hemisphere.feature.browser.selection

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereShapes
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereSizes
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme
import io.github.ctorres345.hemisphere.core.designsystem.focusRing
import io.github.ctorres345.hemisphere.core.designsystem.reportsInput
import io.github.ctorres345.hemisphere.feature.browser.R

@StringRes
internal fun SelectionAction.label(): Int =
    when (this) {
        SelectionAction.Copy -> R.string.action_copy
        SelectionAction.Move -> R.string.action_move
        SelectionAction.Delete -> R.string.action_delete
        SelectionAction.Rename -> R.string.action_rename
        SelectionAction.More -> R.string.action_more
    }

@DrawableRes
internal fun SelectionAction.icon(): Int =
    when (this) {
        SelectionAction.Copy -> R.drawable.ic_copy
        SelectionAction.Move -> R.drawable.ic_move
        SelectionAction.Delete -> R.drawable.ic_delete
        SelectionAction.Rename -> R.drawable.ic_rename
        SelectionAction.More -> R.drawable.ic_more
    }

/** Delete is drawn in `danger`, with its icon and label (design-system §10). A disabled action is tertiary. */
@Composable
internal fun SelectionAction.color(enabled: Boolean) =
    when {
        !enabled -> HemisphereTheme.colors.textTertiary
        this == SelectionAction.Delete -> HemisphereTheme.colors.danger
        else -> HemisphereTheme.colors.textPrimary
    }

/**
 * A menu of selection actions: a row's context menu (every action, for that row alone), or the Dual
 * header's overflow, which leads with Select all.
 */
@Composable
internal fun SelectionMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onAction: (SelectionAction) -> Unit,
    actions: List<SelectionAction> = SelectionAction.entries,
    onSelectAll: (() -> Unit)? = null,
) {
    // The prototype's `.menu`: the overlay surface, the sheet radius and a hairline border.
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = HemisphereShapes.sheet,
        containerColor = HemisphereTheme.colors.surfaceOverlay,
        border = BorderStroke(HemisphereSizes.border, HemisphereTheme.colors.borderDefault),
        // The menu is its own window: report its input, so a tap in it hides the focus ring.
        modifier = Modifier.reportsInput(),
    ) {
        onSelectAll?.let { selectAll ->
            val colors = MenuDefaults.itemColors(leadingIconColor = HemisphereTheme.colors.textSecondary)
            MenuItem(R.string.selection_select_all, R.drawable.ic_select_all, colors) {
                onDismiss()
                selectAll()
            }
        }
        actions.forEach { action ->
            val color = action.color(enabled = true)
            // Danger colours Delete's icon too; the others keep a quieter icon (prototype `.menu svg`).
            val icon = if (action == SelectionAction.Delete) color else HemisphereTheme.colors.textSecondary
            val colors = MenuDefaults.itemColors(textColor = color, leadingIconColor = icon)
            MenuItem(action.label(), action.icon(), colors) {
                onDismiss()
                onAction(action)
            }
        }
    }
}

@Composable
private fun MenuItem(
    @StringRes label: Int,
    @DrawableRes icon: Int,
    colors: MenuItemColors,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(stringResource(label), style = HemisphereTheme.typography.body) },
        leadingIcon = { Icon(painter = painterResource(icon), contentDescription = null) },
        onClick = onClick,
        colors = colors,
        modifier = Modifier.focusRing(fill = true),
    )
}
