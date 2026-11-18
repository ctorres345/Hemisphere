package io.github.ctorres345.hemisphere.feature.browser.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereSizes
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereSpacing
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme
import io.github.ctorres345.hemisphere.feature.browser.R

/** Where a row's divider starts: after the check while selecting, as the prototype's `.list.selecting`. */
internal fun dividerStart(selecting: Boolean): Dp =
    if (selecting) HemisphereSpacing.l + HemisphereSizes.selectionCheck + HemisphereSpacing.m else HemisphereSpacing.l

/** The round check before a row's name while selecting (prototype `.chk`): an outline, or filled with a mark. */
@Composable
internal fun SelectionCheck(selected: Boolean) {
    val colors = HemisphereTheme.colors
    Box(
        modifier =
            Modifier
                .size(HemisphereSizes.selectionCheck)
                .clip(CircleShape)
                .then(
                    if (selected) {
                        Modifier.background(colors.accent.fill)
                    } else {
                        Modifier.border(HemisphereSizes.selectionCheckBorder, colors.borderStrong, CircleShape)
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = colors.accent.onFill,
                modifier = Modifier.size(HemisphereSizes.checkGlyph),
            )
        }
    }
}

/** Calls [onClick] on a secondary (right) mouse click, and keeps the click from the row. */
internal fun Modifier.secondaryClick(onClick: () -> Unit): Modifier =
    pointerInput(onClick) {
        awaitEachGesture {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                event.changes.forEach { it.consume() }
                onClick()
            }
        }
    }
