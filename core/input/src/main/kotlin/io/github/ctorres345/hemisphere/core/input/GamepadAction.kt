package io.github.ctorres345.hemisphere.core.input

/**
 * What a gamepad press means to the app (docs/design-system.md §3, default mapping). Screens react to
 * these, never to raw key codes. The names follow the Xbox layout's roles, not the printed labels.
 */
enum class GamepadAction {
    /** Ⓐ on Xbox: open. */
    Confirm,

    /** Ⓑ on Xbox: leave the selection, else back. */
    Back,

    /** Ⓧ on Xbox: the context menu. */
    Menu,

    /** Ⓨ on Xbox, tapped: toggle the selection of the focused row. */
    Mark,

    /** Ⓨ on Xbox, held past the hold threshold: start a range mark (then D-pad extends it). */
    MarkRange,

    /** L1: switch to the other pane (previous). */
    PrevPane,

    /** R1: switch to the other pane (next). */
    NextPane,

    /** L2: previous tab. */
    PrevTab,

    /** R2: next tab. */
    NextTab,

    /** Right stick up: page up. */
    PageUp,

    /** Right stick down: page down. */
    PageDown,

    /** Start: the actions menu. */
    Start,

    /** Select: layout mode. */
    Select,

    /** D-pad or left stick up. */
    Up,

    /** D-pad or left stick down. */
    Down,

    /** D-pad or left stick left. */
    Left,

    /** D-pad or left stick right. */
    Right,
}
