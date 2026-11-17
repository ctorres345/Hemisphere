package io.github.ctorres345.hemisphere.core.testing

import io.github.ctorres345.hemisphere.core.fs.FileSystemProvider
import io.github.ctorres345.hemisphere.core.fs.VolumeRepository
import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.VolumeId

/** One volume backed by [provider], with [home] as the home folder. */
class FakeVolumeRepository(
    private val provider: FileSystemProvider = InMemoryProvider(),
    private val home: FsPath = FsPath.root(TEST_VOLUME),
) : VolumeRepository {
    override fun homeFolder(): FsPath = home

    override fun providerFor(volume: VolumeId): FsResult<FileSystemProvider> =
        if (volume == home.volume) FsResult.Ok(provider) else FsResult.Err(FsError.VolumeUnavailable)

    companion object {
        val TEST_VOLUME = VolumeId("test")
    }
}
