package io.github.ctorres345.hemisphere

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.ctorres345.hemisphere.feature.browser.BrowserRoute

/**
 * The app back stack (architecture §7). It is saved across rotation and process death, and
 * Browser is the start screen. Each entry gets its own saved state and ViewModel store, so a
 * screen's ViewModel lives while its key is on the stack and is cleared when it is popped.
 */
@Composable
fun HemisphereNavHost(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(BrowserKey)
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<BrowserKey> { BrowserRoute(viewModel = hiltViewModel()) }
            },
    )
}
