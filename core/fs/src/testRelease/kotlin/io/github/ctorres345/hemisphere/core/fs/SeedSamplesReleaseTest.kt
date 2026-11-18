package io.github.ctorres345.hemisphere.core.fs

import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Lists a new home through the public builder, as the release build makes it (HEM-9). */
class SeedSamplesReleaseTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `release builds leave a new home empty and write no seed marker`() =
        runTest(StandardTestDispatcher()) {
            val repo = appHomeVolumes(temp.root, temp.root, GrantedAccess, StandardTestDispatcher(testScheduler))
            val provider = (repo.providerFor(AppHomeVolume) as FsResult.Ok).value

            val names =
                provider
                    .list(FsPath.root(AppHomeVolume))
                    .toList()
                    .filterIsInstance<FsResult.Ok<List<Entry>>>()
                    .flatMap { it.value }
                    .map { it.name }
            assertEquals(emptyList<String>(), names)
            assertEquals(listOf("home"), temp.root.list()!!.toList())
        }
}
