package io.github.ctorres345.hemisphere.core.fs

import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.StorageAccessState
import io.github.ctorres345.hemisphere.core.model.VolumeId
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class VolumeRepositoryTest {
    @get:Rule
    val temp = TemporaryFolder()

    private fun TestScope.repository(access: StorageAccess): VolumeRepository =
        appHomeVolumes(
            appFilesDir = temp.newFolder("files"),
            primaryRoot = File(temp.root, "storage").also { it.mkdirs() },
            storageAccess = access,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

    @Test
    fun `home is the root of the primary volume`() =
        runTest {
            val repo = repository(TestStorageAccess(StorageAccessState.Granted))

            assertEquals(FsPath.root(VolumeId("primary")), repo.homeFolder())
        }

    @Test
    fun `once granted the primary volume lists the storage root`() =
        runTest {
            val repo = repository(TestStorageAccess(StorageAccessState.Granted))
            File(temp.root, "storage/Download").mkdirs()

            val provider = (repo.providerFor(PrimaryVolume) as FsResult.Ok).value
            val names =
                provider
                    .list(repo.homeFolder())
                    .toList()
                    .filterIsInstance<FsResult.Ok<List<Entry>>>()
                    .flatMap { it.value }
                    .map { it.name }

            assertEquals(listOf("Download"), names)
        }

    @Test
    fun `the primary volume is closed until access is granted, then opens`() =
        runTest {
            val access = TestStorageAccess(StorageAccessState.Denied)
            val repo = repository(access)

            assertEquals(FsResult.Err(FsError.AccessDenied), repo.providerFor(PrimaryVolume))

            access.set(StorageAccessState.NeedsSettings)
            assertEquals(FsResult.Err(FsError.AccessDenied), repo.providerFor(PrimaryVolume))

            access.set(StorageAccessState.Granted)
            assert(repo.providerFor(PrimaryVolume) is FsResult.Ok)
        }

    @Test
    fun `an unknown volume is unavailable`() =
        runTest {
            val repo = repository(TestStorageAccess(StorageAccessState.Granted))

            assertEquals(FsResult.Err(FsError.VolumeUnavailable), repo.providerFor(VolumeId("sd")))
        }
}
