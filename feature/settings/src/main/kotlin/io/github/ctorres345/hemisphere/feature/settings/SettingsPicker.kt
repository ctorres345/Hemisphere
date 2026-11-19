package io.github.ctorres345.hemisphere.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereShapes
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereSizes
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme
import io.github.ctorres345.hemisphere.core.designsystem.focusRing
import io.github.ctorres345.hemisphere.core.designsystem.reportsInput

/**
 * A setting row with its value at the end that opens a picker of the choices (prototype `.set-row`
 * with `.val`, "Opens a picker"). The picker is the app's menu surface, as the row menu in Browser.
 * [choices] pairs each value with its label.
 */
@Composable
internal fun <T> SettingPicker(
    title: String,
    hint: String,
    choices: List<Pair<T, String>>,
    selected: T,
    onChoose: (T) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box {
        SettingLink(
            title = title,
            hint = hint,
            value = choices.firstOrNull { it.first == selected }?.second,
            onClick = { open = true },
        )
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = HemisphereShapes.sheet,
            containerColor = HemisphereTheme.colors.surfaceOverlay,
            border = BorderStroke(HemisphereSizes.border, HemisphereTheme.colors.borderDefault),
            // The menu is its own window: report its input, so a tap in it hides the focus ring.
            modifier = Modifier.reportsInput(),
        ) {
            choices.forEach { (choice, label) ->
                val isSelected = choice == selected
                DropdownMenuItem(
                    text = { Text(label, style = HemisphereTheme.typography.body) },
                    trailingIcon = {
                        if (isSelected) Icon(painterResource(R.drawable.ic_chip_check), contentDescription = null)
                    },
                    onClick = {
                        open = false
                        onChoose(choice)
                    },
                    colors =
                        MenuDefaults.itemColors(
                            textColor = HemisphereTheme.colors.textPrimary,
                            trailingIconColor = HemisphereTheme.colors.accent.text,
                        ),
                    modifier = Modifier.focusRing(fill = true).semantics { this.selected = isSelected },
                )
            }
        }
    }
}
