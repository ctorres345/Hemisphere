package io.github.ctorres345.hemisphere.core.fs

import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.VolumeId

/**
 * The volumes the app can reach and the one provider that owns each (architecture §3). Listing
 * mounted volumes (SD cards) and emitting on mount come with later tickets.
 */
interface VolumeRepository {
    /** The folder the app opens on: the root of the internal storage, [PrimaryVolume]. */
    fun homeFolder(): FsPath

    /** The provider that owns [volume], or [io.github.ctorres345.hemisphere.core.model.FsError.VolumeUnavailable]. */
    fun providerFor(volume: VolumeId): FsResult<FileSystemProvider>
}
