package io.github.ctorres345.hemisphere.feature.browser.pane

import io.github.ctorres345.hemisphere.core.model.FsPath

/** History is capped per tab so saved state stays small (architecture §7). */
const val BACK_STACK_LIMIT = 50

/** [folder] on top; past [BACK_STACK_LIMIT] the oldest folders drop off the bottom. */
internal fun List<FsPath>.pushed(folder: FsPath): List<FsPath> = (this + folder).takeLast(BACK_STACK_LIMIT)

/** The stack without its top, or unchanged at its root folder. */
internal fun List<FsPath>.popped(): List<FsPath> = if (size > 1) dropLast(1) else this
