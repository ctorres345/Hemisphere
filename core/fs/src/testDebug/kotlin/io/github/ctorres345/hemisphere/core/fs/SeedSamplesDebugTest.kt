package io.github.ctorres345.hemisphere.core.fs

import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsResult
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Lists a new home through the public builder, as the debug build makes it (HEM-9). */
class SeedSamplesDebugTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `debug builds seed sample files into a new home`() =
        runTest(StandardTestDispatcher()) {
            val repo = appHomeVolumes(temp.root, StandardTestDispatcher(testScheduler))
            val provider = (repo.providerFor(AppHomeVolume) as FsResult.Ok).value

            val names =
                provider
                    .list(repo.homeFolder())
                    .toList()
                    .filterIsInstance<FsResult.Ok<List<Entry>>>()
                    .flatMap { it.value }
                    .map { it.name }
            assertTrue(names.contains("Readme.txt"))
        }
}
