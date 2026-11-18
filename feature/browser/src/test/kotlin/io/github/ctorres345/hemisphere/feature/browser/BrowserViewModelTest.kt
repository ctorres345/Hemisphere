package io.github.ctorres345.hemisphere.feature.browser

import io.github.ctorres345.hemisphere.core.domain.files.GetHomeFolderUseCase
import io.github.ctorres345.hemisphere.core.domain.files.ObserveFolderUseCase
import io.github.ctorres345.hemisphere.core.fs.testing.FakeVolumeRepository
import io.github.ctorres345.hemisphere.core.fs.testing.InMemoryProvider
import io.github.ctorres345.hemisphere.core.fs.testing.file
import io.github.ctorres345.hemisphere.core.fs.testing.folder
import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Real use cases over the in-memory provider: send intents, read the state. No mocks. */
class BrowserViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val provider = InMemoryProvider()
    private val volumes = FakeVolumeRepository(provider)
    private val home: FsPath = volumes.homeFolder()

    private fun viewModel() =
        BrowserViewModel(GetHomeFolderUseCase(volumes), ObserveFolderUseCase(volumes, main.dispatcher))

    @Test
    fun `opens on the home folder, sorted`() =
        runTest(main.dispatcher) {
            provider.addFolder(home, file("notes.md"), folder("Music"))

            val state = viewModel().state.value

            assertEquals(home, state.folder)
            assertEquals(listOf("Music", "notes.md"), state.entries.map { it.name })
            assertEquals(false, state.loading)
            assertEquals(null, state.error)
        }

    @Test
    fun `an error shows, and Retry lists again`() =
        runTest(main.dispatcher) {
            provider.addFolder(home, folder("Music"))
            provider.failNext(home, FsError.AccessDenied)
            val viewModel = viewModel()
            assertEquals(FsError.AccessDenied, viewModel.state.value.error)

            viewModel.onIntent(BrowserIntent.Retry)

            assertEquals(2, provider.listCalls)
            assertEquals(null, viewModel.state.value.error)
            assertEquals(
                listOf("Music"),
                viewModel.state.value.entries
                    .map { it.name },
            )
        }
}
