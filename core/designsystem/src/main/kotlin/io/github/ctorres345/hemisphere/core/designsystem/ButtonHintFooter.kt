package io.github.ctorres345.hemisphere.core.designsystem

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.LayoutDirection

/**
 * The gamepad button-hint footer: `Ⓐ Open  Ⓑ Back  Ⓧ Menu  Ⓨ Mark  L1/R1 Pane` (design §3). It shows only
 * in [InputMode.Gamepad]; in touch and keyboard mode it takes no space. With [FaceButtonLayout.Nintendo]
 * the glyphs swap A↔B and X↔Y, so each action keeps its physical button.
 *
 * L1 and R1 are physical buttons, so their pair stays in physical order (L1 left of R1) at the end of the
 * footer, also in RTL (design §13).
 */
@Composable
fun ButtonHintFooter(
    layout: FaceButtonLayout,
    modifier: Modifier = Modifier,
) {
    if (!LocalInputMode.current.showsHints) return
    val colors = HemisphereTheme.colors
    // Phone widths use the tighter m spacing, two-pane widths l (prototype .hints).
    val windowWidth =
        with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.width
                .toDp()
        }
    val gap = if (windowWidth < HemisphereSizes.dualMinWidth) HemisphereSpacing.m else HemisphereSpacing.l
    Column(modifier.fillMaxWidth().background(colors.surfaceRaised)) {
        HorizontalDivider(thickness = HemisphereSizes.border, color = colors.borderDefault)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(HemisphereSizes.hintFooter)
                    .clipToBounds()
                    .padding(horizontal = gap),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            faceHints(layout).forEach { (glyph, label) -> Hint(glyph = { Glyph(glyph, CircleShape) }, label = label) }
            Spacer(Modifier.weight(1f))
            Hint(
                glyph = {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(horizontalArrangement = Arrangement.spacedBy(HemisphereSpacing.xs)) {
                            Glyph(stringResource(R.string.designsystem_glyph_l1), HemisphereShapes.chip)
                            Glyph(stringResource(R.string.designsystem_glyph_r1), HemisphereShapes.chip)
                        }
                    }
                },
                label = R.string.designsystem_hint_pane,
            )
        }
    }
}

/** The four face-button hints in footer order, as (glyph, label). Each action keeps its physical button. */
internal fun faceHints(layout: FaceButtonLayout): List<Pair<String, Int>> {
    val confirm = if (layout == FaceButtonLayout.Xbox) "A" else "B"
    val cancel = if (layout == FaceButtonLayout.Xbox) "B" else "A"
    val menu = if (layout == FaceButtonLayout.Xbox) "X" else "Y"
    val mark = if (layout == FaceButtonLayout.Xbox) "Y" else "X"
    return listOf(
        confirm to R.string.designsystem_hint_open,
        cancel to R.string.designsystem_hint_back,
        menu to R.string.designsystem_hint_menu,
        mark to R.string.designsystem_hint_mark,
    )
}

@Composable
private fun Hint(
    glyph: @Composable () -> Unit,
    @StringRes label: Int,
) {
    Row(
        // One node for TalkBack: "A, Open".
        modifier = Modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(HemisphereSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        glyph()
        Text(
            stringResource(label),
            style = HemisphereTheme.typography.meta,
            color = HemisphereTheme.colors.textSecondary,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun Glyph(
    text: String,
    shape: Shape,
) {
    Box(
        modifier =
            Modifier
                .defaultMinSize(minWidth = HemisphereSizes.hintGlyph, minHeight = HemisphereSizes.hintGlyph)
                .border(HemisphereSizes.hintGlyphBorder, HemisphereTheme.colors.textSecondary, shape)
                .padding(horizontal = HemisphereSpacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = HemisphereTheme.typography.caption.copy(fontWeight = HemisphereTheme.typography.heading.fontWeight),
            color = HemisphereTheme.colors.textPrimary,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Preview
@Composable
private fun ButtonHintFooterPreview(
    @PreviewParameter(ThemeStylePreviews::class) style: ThemeStyle,
) {
    HemisphereTheme(style = style) {
        CompositionLocalProvider(LocalInputMode provides InputMode.Gamepad) {
            ButtonHintFooter(layout = FaceButtonLayout.Xbox)
        }
    }
}
