package io.github.ctorres345.hemisphere.core.fs

import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.VolumeId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.IOException
import java.time.Instant

/**
 * Direct file access by path, for volumes the app can read by path (architecture §3). Each volume
 * maps to a root folder; paths never leave it.
 */
internal class LocalProvider(
    private val roots: Map<VolumeId, File>,
    private val ioDispatcher: CoroutineDispatcher,
    private val batchSize: Int = DEFAULT_BATCH_SIZE,
) : FileSystemProvider {
    init {
        require(batchSize > 0) { "batchSize must be positive" }
    }

    override fun list(dir: FsPath): Flow<FsResult<List<Entry>>> =
        flow {
            when (val folder = resolve(dir)) {
                is FsResult.Err -> {
                    emit(folder)
                }

                is FsResult.Ok -> {
                    val result =
                        try {
                            readChildren(folder.value)
                        } catch (e: IOException) {
                            FsResult.Err(FsError.Io(e.message))
                        } catch (
                            @Suppress("SwallowedException") e: SecurityException,
                        ) {
                            // The type is the whole message: the app lacks access to this folder.
                            FsResult.Err(FsError.AccessDenied)
                        }
                    when (result) {
                        is FsResult.Err -> emit(result)
                        is FsResult.Ok -> for (batch in result.value.chunked(batchSize)) emit(FsResult.Ok(batch))
                    }
                }
            }
        }.flowOn(ioDispatcher)

    private fun resolve(path: FsPath): FsResult<File> {
        val root = roots[path.volume]
        return when {
            root == null -> FsResult.Err(FsError.VolumeUnavailable)
            path.segments.any(::isUnsafeSegment) -> FsResult.Err(FsError.InvalidName)
            else -> FsResult.Ok(path.segments.fold(root) { parent, name -> File(parent, name) })
        }
    }

    /** A segment that would name another folder than the one asked for, or escape the volume root. */
    private fun isUnsafeSegment(segment: String): Boolean = segment in setOf("", ".", "..") || '/' in segment

    private fun readChildren(folder: File): FsResult<List<Entry>> {
        val children = folder.listFiles()
        return when {
            children != null -> FsResult.Ok(children.map { it.toEntry() })
            !folder.exists() -> FsResult.Err(FsError.NotFound)
            !folder.isDirectory -> FsResult.Err(FsError.Unsupported)
            else -> FsResult.Err(FsError.AccessDenied)
        }
    }

    private fun File.toEntry(): Entry {
        val isFolder = isDirectory
        return Entry(
            name = name,
            kind = if (isFolder) Entry.Kind.Folder else Entry.Kind.File,
            size = if (isFolder) null else length(),
            modified = lastModified().takeIf { it > 0 }?.let { Instant.ofEpochMilli(it) },
        )
    }

    private companion object {
        const val DEFAULT_BATCH_SIZE = 500
    }
}
