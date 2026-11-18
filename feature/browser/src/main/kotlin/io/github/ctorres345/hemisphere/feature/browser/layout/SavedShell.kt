package io.github.ctorres345.hemisphere.feature.browser.layout

import android.os.Bundle
import androidx.core.os.bundleOf
import io.github.ctorres345.hemisphere.feature.browser.ShellState
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneId

/**
 * The shell's part of the saved state: the active pane and the layout shown. Kept apart from the panes'
 * history (`SavedPanes`), under its own key.
 */
internal object SavedShell {
    const val KEY = "browser.shell"
    private const val ACTIVE = "active"
    private const val LAYOUT = "layout"

    fun save(shell: ShellState): Bundle = bundleOf(ACTIVE to shell.activePane.name, LAYOUT to shell.layout.name)

    /** The saved shell, or the default for anything missing or unknown. */
    fun restore(saved: Bundle?): ShellState {
        val default = ShellState()
        if (saved == null) return default
        return ShellState(
            activePane = PaneId.entries.firstOrNull { it.name == saved.getString(ACTIVE) } ?: default.activePane,
            layout = PaneLayout.entries.firstOrNull { it.name == saved.getString(LAYOUT) } ?: default.layout,
        )
    }
}
