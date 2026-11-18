package io.github.ctorres345.hemisphere.feature.browser.access

import io.github.ctorres345.hemisphere.core.domain.files.ObserveStorageAccessUseCase
import io.github.ctorres345.hemisphere.core.domain.files.RecordStoragePermissionResultUseCase
import io.github.ctorres345.hemisphere.core.model.StorageAccessState
import io.github.ctorres345.hemisphere.core.testing.FakeStorageAccess
import io.github.ctorres345.hemisphere.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Real use cases over a fake storage access: send intents, read the state. No mocks. */
class AccessViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private fun viewModel(access: FakeStorageAccess) =
        AccessViewModel(ObserveStorageAccessUseCase(access), RecordStoragePermissionResultUseCase(access))

    @Test
    fun `denied access shows the access screen state`() =
        runTest(main.dispatcher) {
            val viewModel = viewModel(FakeStorageAccess(StorageAccessState.Denied))

            assertEquals(AccessUiState(access = StorageAccessState.Denied), viewModel.state.value)
        }

    @Test
    fun `Grant while denied asks for the grant flow`() =
        runTest(main.dispatcher) {
            val viewModel = viewModel(FakeStorageAccess(StorageAccessState.Denied))

            viewModel.onIntent(AccessIntent.GrantClicked)

            assertEquals(AccessLaunch.Grant, viewModel.state.value.launch)
            viewModel.onIntent(AccessIntent.LaunchHandled)
            assertEquals(null, viewModel.state.value.launch)
        }

    @Test
    fun `Grant while access needs settings opens the app settings`() =
        runTest(main.dispatcher) {
            val viewModel = viewModel(FakeStorageAccess(StorageAccessState.NeedsSettings))

            viewModel.onIntent(AccessIntent.GrantClicked)

            assertEquals(StorageAccessState.NeedsSettings, viewModel.state.value.access)
            assertEquals(AccessLaunch.AppSettings, viewModel.state.value.launch)
        }

    @Test
    fun `a second denial the system will not ask about again needs settings`() =
        runTest(main.dispatcher) {
            val viewModel = viewModel(FakeStorageAccess(StorageAccessState.Denied))

            viewModel.onIntent(AccessIntent.PermissionResult(granted = false, canAskAgain = true))
            assertEquals(StorageAccessState.Denied, viewModel.state.value.access)
            viewModel.onIntent(AccessIntent.PermissionResult(granted = false, canAskAgain = false))

            assertEquals(StorageAccessState.NeedsSettings, viewModel.state.value.access)
        }

    @Test
    fun `granted access shows the browser, and a later revoke shows the access screen again`() =
        runTest(main.dispatcher) {
            val access = FakeStorageAccess(StorageAccessState.Granted)
            val viewModel = viewModel(access)
            assertEquals(StorageAccessState.Granted, viewModel.state.value.access)

            access.set(StorageAccessState.Denied)

            assertEquals(StorageAccessState.Denied, viewModel.state.value.access)
        }

    @Test
    fun `Pick a folder is a placeholder that changes nothing`() =
        runTest(main.dispatcher) {
            val viewModel = viewModel(FakeStorageAccess(StorageAccessState.Denied))
            val before = viewModel.state.value

            viewModel.onIntent(AccessIntent.PickFolderClicked)

            assertEquals(before, viewModel.state.value)
        }
}
