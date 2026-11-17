package io.github.ctorres345.hemisphere.core.fs

import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.VolumeId

/**
 * The volumes the app can reach and the one provider that owns each (architecture §3). The skeleton
 * has a single app-owned volume; listing mounted volumes and emitting on mount comes with storage access.
 */
interface VolumeRepository {
    /** The folder the app opens on. */
    fun homeFolder(): FsPath

    /** The provider that owns [volume], or [io.github.ctorres345.hemisphere.core.model.FsError.VolumeUnavailable]. */
    fun providerFor(volume: VolumeId): FsResult<FileSystemProvider>
}
