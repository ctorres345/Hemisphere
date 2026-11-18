package io.github.ctorres345.hemisphere.core.designsystem

/**
 * Which labels the face buttons carry. Nintendo-style pads swap A↔B and X↔Y compared with Xbox-style
 * pads (docs/design-system.md §3). `core:input` resolves the Face buttons setting (Auto, Xbox, Nintendo)
 * to one of these.
 */
enum class FaceButtonLayout { Xbox, Nintendo }
