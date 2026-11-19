package io.github.ctorres345.hemisphere.core.designsystem

import androidx.compose.ui.unit.Dp

/**
 * The spacing scale from `tokens/base.json` (design-system §7), a 4dp grid. UI code uses these,
 * never `dp` literals.
 */
object HemisphereSpacing {
    val xs: Dp = ThemeTokens.spaceXs
    val s: Dp = ThemeTokens.spaceS
    val m: Dp = ThemeTokens.spaceM
    val l: Dp = ThemeTokens.spaceL
    val xl: Dp = ThemeTokens.spaceXl
    val xxl: Dp = ThemeTokens.spaceXxl
}

/** Row heights per density, from `tokens/base.json` (docs/design-system.md §7). */
object HemisphereRowHeight {
    val comfortable: Dp = ThemeTokens.rowComfortable
    val default: Dp = ThemeTokens.rowDefault
    val compact: Dp = ThemeTokens.rowCompact
}

/** Component sizes from `tokens/base.json`. */
object HemisphereSizes {
    /** The top app bar in touch mode (prototype `.appbar`). */
    val appBar: Dp = ThemeTokens.sizeAppBar

    /** The Dual layout's top bar above both panes (prototype `.topbar`). */
    val topBar: Dp = ThemeTokens.sizeTopBar

    /** The outline of buttons and fields (prototype `.btn`). */
    val border: Dp = ThemeTokens.sizeBorder

    /** The gamepad focus ring's stroke (prototype `.gf`). */
    val focusRing: Dp = ThemeTokens.sizeFocusRing

    /** The gap between a filled control and its focus ring (prototype `outline-offset: 2px`). */
    val focusRingGap: Dp = ThemeTokens.sizeFocusRingGap

    /** The button-hint footer's height (prototype `.hints`). */
    val hintFooter: Dp = ThemeTokens.sizeHintFooter

    /** One button glyph in the hint footer (prototype `.gl`). */
    val hintGlyph: Dp = ThemeTokens.sizeHintGlyph

    /** The outline of a hint glyph (prototype `.gl` border). */
    val hintGlyphBorder: Dp = ThemeTokens.sizeHintGlyphBorder

    /** Window width from which Auto shows two panes; narrower windows use phone spacing. */
    val dualMinWidth: Dp = ThemeTokens.sizeDualMinWidth

    /** The accent bar on top of the active pane's header in Dual (prototype `.pane.active .phead::before`). */
    val activePaneBar: Dp = ThemeTokens.sizeActivePaneBar

    /** A row's selection check while selecting (prototype `.chk`). */
    val selectionCheck: Dp = ThemeTokens.sizeSelectionCheck

    /** The outline of an unselected row's check (prototype `.chk` border). */
    val selectionCheckBorder: Dp = ThemeTokens.sizeSelectionCheckBorder

    /** The check mark inside a selected row's check (prototype `.chk svg`). */
    val checkGlyph: Dp = ThemeTokens.sizeCheckGlyph

    /** The active pane's selection header in Dual (prototype `.psel`). */
    val selectionBar: Dp = ThemeTokens.sizeSelectionBar

    /** A button in the Dual selection header (prototype `.psel .btn`). */
    val selectionButton: Dp = ThemeTokens.sizeSelectionButton

    /** An icon inside a text button (prototype `.btn svg`). */
    val buttonIcon: Dp = ThemeTokens.sizeButtonIcon

    /** One action in the phone's selection action bar (prototype `.act`). */
    val actionMinWidth: Dp = ThemeTokens.sizeActionMinWidth

    /** The narrowest Dual pane whose selection header labels Copy and Move; below it they are icons only. */
    val selectionLabelsMinWidth: Dp = ThemeTokens.sizeSelectionLabelsMinWidth

    /** Minimum height of a Settings row (prototype `.set-row`). */
    val settingRow: Dp = ThemeTokens.sizeSettingRow

    /** A choice chip in a Settings section (prototype `.segs .chip`). */
    val settingChip: Dp = ThemeTokens.sizeSettingChip

    /** The touch target of an accent swatch (prototype `.sw`). */
    val swatch: Dp = ThemeTokens.sizeSwatch

    /** The colored dot inside an accent swatch (prototype `.sw::before`). */
    val swatchDot: Dp = ThemeTokens.sizeSwatchDot

    /** The ring around the selected accent swatch (prototype `.sw[aria-checked]::after`). */
    val swatchRing: Dp = ThemeTokens.sizeSwatchRing
}
