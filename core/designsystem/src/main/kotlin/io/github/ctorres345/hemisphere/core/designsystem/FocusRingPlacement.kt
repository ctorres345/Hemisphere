package io.github.ctorres345.hemisphere.core.designsystem

/** Where the ring sits (prototype `.gf`): inside a row's edge, or outside a filled control with a gap. */
enum class FocusRingPlacement {
    /** Rows and plain controls: the ring is drawn just inside the edge (`outline-offset: -2px`). */
    Inside,

    /**
     * Buttons, chips, swatches and other filled or selected controls: the ring sits outside with a gap,
     * so focus never blends with the fill or the selected state (`outline-offset: 2px`).
     */
    Outside,
}
