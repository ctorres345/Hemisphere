package io.github.ctorres345.hemisphere.core.fs.testing

import io.github.ctorres345.hemisphere.core.fs.FileSystemProvider
import io.github.ctorres345.hemisphere.core.fs.VolumeRepository
import io.github.ctorres345.hemisphere.core.model.Capabilities
import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.Volume
import io.github.ctorres345.hemisphere.core.model.VolumeId
import io.github.ctorres345.hemisphere.core.model.VolumeLimits
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * A [VolumeRepository] for tests. It starts with one volume backed by [provider], with [home] as
 * the home folder; [mount] and [unmount] add and remove volumes, and [observeVolumes] emits on each.
 */
class FakeVolumeRepository(
    provider: FileSystemProvider = InMemoryProvider(),
    private val home: FsPath = FsPath.root(TEST_VOLUME),
    limits: VolumeLimits = VolumeLimits.NONE,
) : VolumeRepository {
    private val mounted =
        MutableStateFlow(listOf(Mounted(volumeOf(home.volume, limits), provider, followsProvider = true)))

    /** Makes [volume] reachable through [provider], replacing a volume mounted with the same id. */
    fun mount(
        volume: Volume,
        provider: FileSystemProvider,
    ) = mounted.update { list -> list.filterNot { it.volume.id == volume.id } + Mounted(volume, provider) }

    /** Makes [volume] unreachable: `providerFor` and `limitsFor` return `VolumeUnavailable` for it. */
    fun unmount(volume: VolumeId) = mounted.update { list -> list.filterNot { it.volume.id == volume } }

    override fun homeFolder(): FsPath = home

    override fun providerFor(volume: VolumeId): FsResult<FileSystemProvider> =
        mounted.value.firstOrNull { it.volume.id == volume }?.let { FsResult.Ok(it.provider) }
            ?: FsResult.Err(FsError.VolumeUnavailable)

    /**
     * The starting volume reads its capabilities from its provider each time the list is emitted (on
     * collection, mount and unmount), not when they change; a volume given to [mount] keeps the
     * capabilities it was given.
     */
    override fun observeVolumes(): Flow<List<Volume>> = mounted.map { list -> list.map { it.current() } }

    override fun limitsFor(volume: VolumeId): FsResult<VolumeLimits> =
        mounted.value.firstOrNull { it.volume.id == volume }?.let { FsResult.Ok(it.volume.limits) }
            ?: FsResult.Err(FsError.VolumeUnavailable)

    private class Mounted(
        val volume: Volume,
        val provider: FileSystemProvider,
        val followsProvider: Boolean = false,
    ) {
        fun current(): Volume = if (followsProvider) volume.copy(capabilities = provider.capabilities) else volume
    }

    companion object {
        val TEST_VOLUME = VolumeId("test")

        private fun volumeOf(
            id: VolumeId,
            limits: VolumeLimits,
        ) = Volume(id, "Test volume", Volume.Kind.Internal, limits, Capabilities())
    }
}
