package io.github.ctorres345.hemisphere.feature.browser

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme
import io.github.ctorres345.hemisphere.core.designsystem.ThemeStyle
import io.github.ctorres345.hemisphere.core.designsystem.ThemeStylePreviews
import io.github.ctorres345.hemisphere.core.domain.files.FolderLock
import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.Listing
import io.github.ctorres345.hemisphere.core.model.VolumeId
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneId
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneState
import io.github.ctorres345.hemisphere.feature.browser.pane.TabState

@Composable
private fun BrowserPreview(
    style: ThemeStyle,
    tab: TabState,
) {
    HemisphereTheme(style = style) {
        BrowserScreen(state = BrowserUiState(panes = mapOf(PaneId.Left to PaneState(listOf(tab)))), onIntent = {})
    }
}

private val previewFolder = FsPath.root(VolumeId("primary"))

@Preview
@Composable
private fun BrowserScreenListPreview(
    @PreviewParameter(ThemeStylePreviews::class) style: ThemeStyle,
) {
    BrowserPreview(
        style,
        TabState(
            backStack = listOf(previewFolder),
            listing =
                Listing(
                    entries =
                        listOf(
                            Entry("Music", Entry.Kind.Folder),
                            Entry(
                                "Legend of the Twin Hemispheres - Director's Cut (USA, Europe) (Rev 2).zip",
                                Entry.Kind.File,
                                size = 2048,
                            ),
                            Entry("Readme.txt", Entry.Kind.File, size = 2048),
                        ),
                    complete = true,
                ),
        ),
    )
}

@Preview
@Composable
private fun BrowserScreenEmptyPreview(
    @PreviewParameter(ThemeStylePreviews::class) style: ThemeStyle,
) {
    BrowserPreview(style, TabState(backStack = listOf(previewFolder), listing = Listing(emptyList(), complete = true)))
}

@Preview
@Composable
private fun BrowserScreenErrorPreview(
    @PreviewParameter(ThemeStylePreviews::class) style: ThemeStyle,
) {
    BrowserPreview(
        style,
        TabState(
            backStack = listOf(previewFolder),
            listing = Listing(emptyList(), complete = true, error = FsError.AccessDenied),
        ),
    )
}

@Preview
@Composable
private fun BrowserScreenLockedPreview(
    @PreviewParameter(ThemeStylePreviews::class) style: ThemeStyle,
) {
    BrowserPreview(
        style,
        TabState(
            backStack = listOf(previewFolder.child("Android"), previewFolder.child("Android").child("data")),
            listing = Listing(emptyList(), complete = true),
            lock = FolderLock(locked = true),
        ),
    )
}
