package io.github.ctorres345.hemisphere.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.ctorres345.hemisphere.core.designsystem.FocusFillRipple
import io.github.ctorres345.hemisphere.core.designsystem.FocusRingPlacement
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereRowHeight
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereShapes
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereSizes
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereSpacing
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme
import io.github.ctorres345.hemisphere.core.designsystem.InputMode
import io.github.ctorres345.hemisphere.core.designsystem.LocalInputMode
import io.github.ctorres345.hemisphere.core.designsystem.focusRing
import io.github.ctorres345.hemisphere.core.model.Accent

/** A section title in the accent's text color (prototype `.sec`). */
@Composable
internal fun SectionHeader(text: String) {
    val caption = HemisphereTheme.typography.caption
    Text(
        text = text,
        style = caption.copy(fontWeight = HemisphereTheme.typography.heading.fontWeight),
        color = HemisphereTheme.colors.accent.text,
        modifier =
            Modifier
                .padding(
                    start = HemisphereSpacing.l,
                    end = HemisphereSpacing.l,
                    top = HemisphereSpacing.l,
                    bottom = HemisphereSpacing.xs,
                ).semantics { heading() },
    )
}

/** A tappable setting row that opens something, with an optional value at the end (prototype `.set-row`). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingLink(
    title: String,
    hint: String,
    onClick: () -> Unit,
    value: String? = null,
) {
    CompositionLocalProvider(LocalRippleConfiguration provides FocusFillRipple) {
        SettingRow(
            title = title,
            hint = hint,
            value = value,
            modifier =
                Modifier
                    .focusRing(fill = true)
                    .clip(HemisphereShapes.row)
                    .clickable(role = Role.Button, onClick = onClick),
        )
    }
}

/**
 * A setting's name and hint, with an optional value at the end (prototype `.set-row`). Read as one
 * item by TalkBack; [modifier] makes it tappable.
 */
@Composable
internal fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    value: String? = null,
) {
    val gamepad = LocalInputMode.current == InputMode.Gamepad
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                // The row's margin, outside its ring and press highlight (prototype `.set-row` margin-inline).
                .padding(horizontal = HemisphereSpacing.xs)
                .then(modifier)
                .semantics(mergeDescendants = true) {}
                // Shorter with a gamepad, as the prototype's `.dev[data-in="pad"] .set-row` (48dp).
                .heightIn(min = if (gamepad) HemisphereRowHeight.default else HemisphereSizes.settingRow)
                .padding(horizontal = HemisphereSpacing.l, vertical = HemisphereSpacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HemisphereSpacing.m),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = HemisphereTheme.typography.body, color = HemisphereTheme.colors.textPrimary)
            if (hint != null) {
                Text(hint, style = HemisphereTheme.typography.meta, color = HemisphereTheme.colors.textSecondary)
            }
        }
        if (value != null) {
            Text(value, style = HemisphereTheme.typography.meta, color = HemisphereTheme.colors.textSecondary)
        }
    }
}

/** One choice of several, saved on press (prototype `.segs` with `.chip` radios). */
@Composable
internal fun <T> ChoiceChips(
    choices: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onChoose: (T) -> Unit,
    selectedFocus: FocusRequester? = null,
) {
    FlowRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = HemisphereSpacing.l, end = HemisphereSpacing.l, bottom = HemisphereSpacing.s)
                .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(HemisphereSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(HemisphereSpacing.xs),
    ) {
        choices.forEach { choice ->
            val isSelected = choice == selected
            ChoiceChip(
                text = label(choice),
                selected = isSelected,
                onClick = { onChoose(choice) },
                modifier =
                    if (isSelected &&
                        selectedFocus != null
                    ) {
                        Modifier.focusRequester(selectedFocus)
                    } else {
                        Modifier
                    },
            )
        }
    }
}

@Composable
private fun ChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HemisphereTheme.colors
    val shape = HemisphereShapes.chip
    Row(
        modifier =
            modifier
                // 36dp in layout; touch still reaches 48dp, since Compose widens a small target's hit area
                // (prototype `::after`) without moving the chips apart.
                .focusRing(shape = shape, placement = FocusRingPlacement.Outside)
                .clip(shape)
                .then(if (selected) Modifier.background(colors.accent.tint) else Modifier)
                // Always a border, the tint's own color when selected, so a chip keeps its width (prototype
                // `.chip[aria-checked=true]` turns the border transparent).
                .border(
                    BorderStroke(HemisphereSizes.border, if (selected) colors.accent.tint else colors.borderDefault),
                    shape,
                ).selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .heightIn(min = HemisphereSizes.settingChip)
                .padding(horizontal = HemisphereSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HemisphereSpacing.xs),
    ) {
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_chip_check),
                contentDescription = null,
                tint = colors.textPrimary,
            )
        }
        val meta = HemisphereTheme.typography.meta
        Text(
            text = text,
            style = if (selected) meta.copy(fontWeight = HemisphereTheme.typography.heading.fontWeight) else meta,
            color = if (selected) colors.textPrimary else colors.textSecondary,
        )
    }
}

/** The 8 accents as round swatches (prototype `.swatches`). The selected one has a ring and a check. */
@Composable
internal fun AccentSwatches(
    selected: Accent,
    onChoose: (Accent) -> Unit,
) {
    FlowRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = HemisphereSpacing.m,
                    end = HemisphereSpacing.m,
                    top = HemisphereSpacing.xs,
                    bottom = HemisphereSpacing.m,
                ).selectableGroup(),
    ) {
        Accent.entries.forEach { accent ->
            Swatch(accent, selected = accent == selected, onClick = { onChoose(accent) })
        }
    }
}

@Composable
private fun Swatch(
    accent: Accent,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = HemisphereTheme.colors.accents.getValue(accent.token())
    val name = stringResource(accent.label())
    Box(
        modifier =
            Modifier
                .size(HemisphereSizes.swatch)
                .focusRing(shape = CircleShape, placement = FocusRingPlacement.Outside)
                .clip(CircleShape)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(HemisphereSizes.swatchDot).background(colors.fill, CircleShape))
        if (selected) {
            Box(
                Modifier
                    .matchParentSize()
                    .padding(HemisphereSizes.swatchRing)
                    .border(HemisphereSizes.swatchRing, colors.fill, CircleShape),
            )
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = colors.onFill,
            )
        }
    }
}
