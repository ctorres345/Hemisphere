package io.github.ctorres345.hemisphere.core.fs

import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.Volume
import io.github.ctorres345.hemisphere.core.model.VolumeId
import io.github.ctorres345.hemisphere.core.model.VolumeLimits
import kotlinx.coroutines.flow.Flow

/**
 * The volumes the app can reach and the one provider that owns each (architecture §3).
 */
interface VolumeRepository {
    /** The folder the app opens on: the root of the internal storage, [PrimaryVolume]. */
    fun homeFolder(): FsPath

    /** The provider that owns [volume], or [io.github.ctorres345.hemisphere.core.model.FsError.VolumeUnavailable]. */
    fun providerFor(volume: VolumeId): FsResult<FileSystemProvider>

    /** The volumes reachable now. Emits the current list on collect, then again when one mounts or goes away. */
    fun observeVolumes(): Flow<List<Volume>>

    /** What [volume]'s file system refuses, or `VolumeUnavailable` when it is not mounted. */
    fun limitsFor(volume: VolumeId): FsResult<VolumeLimits>
}
