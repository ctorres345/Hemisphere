package io.github.ctorres345.hemisphere.core.domain.files

import io.github.ctorres345.hemisphere.core.fs.VolumeRepository
import io.github.ctorres345.hemisphere.core.model.FsPath

/** The folder a new pane opens on. */
class GetHomeFolderUseCase(
    private val volumes: VolumeRepository,
) {
    operator fun invoke(): FsPath = volumes.homeFolder()
}
