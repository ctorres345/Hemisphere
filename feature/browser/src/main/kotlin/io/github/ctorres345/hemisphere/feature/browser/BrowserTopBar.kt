package io.github.ctorres345.hemisphere.feature.browser

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereSizes
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme
import io.github.ctorres345.hemisphere.core.designsystem.focusRing
import io.github.ctorres345.hemisphere.feature.browser.pane.TabState

/** The top bar: the open folder's name in Single, the app's name over both panes in Dual. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BrowserTopBar(
    tab: TabState?,
    dual: Boolean,
    onIntent: (BrowserIntent) -> Unit,
) {
    // Single: the folder's app bar (prototype `.appbar`). Dual: the lower top bar above both panes,
    // with a border under it (prototype `.topbar`).
    Column {
        TopAppBar(
            title = {
                Text(
                    text =
                        if (dual) {
                            stringResource(R.string.browser_title_dual)
                        } else {
                            tab?.folder?.name ?: stringResource(R.string.browser_home)
                        },
                    style = if (dual) HemisphereTheme.typography.heading else LocalTextStyle.current,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            actions = { LayoutToggle(dual, onClick = { onIntent(BrowserIntent.ToggleLayout) }) },
            expandedHeight = if (dual) HemisphereSizes.topBar else HemisphereSizes.appBar,
            windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
        )
        if (dual) {
            HorizontalDivider(
                thickness = HemisphereSizes.border,
                color = HemisphereTheme.colors.borderDefault,
            )
        }
    }
}

/** The toolbar toggle: it names and shows the layout it switches to (prototype `.ib` with `data-act="layout"`). */
@Composable
private fun LayoutToggle(
    dual: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.focusRing()) {
        Icon(
            painter = painterResource(if (dual) R.drawable.ic_layout_single else R.drawable.ic_layout_dual),
            contentDescription =
                stringResource(
                    if (dual) R.string.browser_switch_to_single else R.string.browser_switch_to_dual,
                ),
            tint = HemisphereTheme.colors.textSecondary,
        )
    }
}
