package io.github.ctorres345.hemisphere

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.ctorres345.hemisphere.core.model.StorageAccessState
import io.github.ctorres345.hemisphere.feature.browser.BrowserRoute
import io.github.ctorres345.hemisphere.feature.browser.access.AccessRoute
import io.github.ctorres345.hemisphere.feature.browser.access.AccessViewModel

/**
 * The app back stack (architecture §7). It is saved across rotation and process death. Access is
 * the gate: the stack starts on the access screen and moves to Browser once storage access is
 * granted. Storage access is read again every time the app comes to the foreground, so revoking
 * it in the system settings puts the access screen back. Each entry gets its own saved state and
 * ViewModel store, so a screen's ViewModel lives while its key is on the stack and is cleared when
 * it is popped. The gate reads the access state through its own [AccessViewModel].
 */
@Composable
fun HemisphereNavHost(
    modifier: Modifier = Modifier,
    accessViewModel: AccessViewModel = hiltViewModel(),
) {
    val access by accessViewModel.state.collectAsStateWithLifecycle()
    val gate: NavKey = if (access.access == StorageAccessState.Granted) BrowserKey else AccessKey
    val backStack = rememberNavBackStack(gate)
    LaunchedEffect(gate) {
        if (backStack.lastOrNull() != gate) {
            backStack.clear()
            backStack.add(gate)
        }
    }
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
                entry<AccessKey> { AccessRoute(viewModel = hiltViewModel()) }
                entry<BrowserKey> { BrowserRoute(viewModel = hiltViewModel()) }
            },
    )
}
