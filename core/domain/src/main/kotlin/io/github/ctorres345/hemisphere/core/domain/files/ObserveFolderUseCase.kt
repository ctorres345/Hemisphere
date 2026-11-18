package io.github.ctorres345.hemisphere.core.domain.files

import io.github.ctorres345.hemisphere.core.data.user.FolderViewRepository
import io.github.ctorres345.hemisphere.core.fs.DefaultDispatcher
import io.github.ctorres345.hemisphere.core.fs.VolumeRepository
import io.github.ctorres345.hemisphere.core.jobs.InternalEntries
import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FolderSort
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.Listing
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Streams a folder as [Listing]s (architecture §3): every entry read so far, in the folder's saved
 * sort (folders first), with `complete = false` until the last batch. The app's own entries (Trash,
 * temp files) are hidden. An error part-way keeps what was read and ends the reading.
 *
 * When the folder's sort changes, the last listing is sent again in the new order, also after it is
 * complete, so the flow ends only when the sort flow does. A sort that cannot be read uses the default.
 * Change signals come with HEM-69.
 */
class ObserveFolderUseCase
    @Inject
    constructor(
        private val volumes: VolumeRepository,
        private val folderViews: FolderViewRepository,
        @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    ) {
        operator fun invoke(dir: FsPath): Flow<Listing> =
            flow { emitAll(sorted(dir, SortAsRead())) }.flowOn(defaultDispatcher)

        /** Each listing as read, in the latest saved order; a new order re-sorts the last listing. */
        private fun sorted(
            dir: FsPath,
            sorter: SortAsRead,
        ): Flow<Listing> = combine(read(dir), sortOrder(dir), sorter::sorted)

        /** The folder's saved order, one comparator per change. A sort that cannot be read uses the default. */
        private fun sortOrder(dir: FsPath): Flow<Comparator<Entry>> =
            folderViews
                .observe(dir)
                .catch { emit(FolderSort.DEFAULT) }
                .map { it.comparator() }

        /** The folder's entries as read so far, unsorted. */
        private fun read(dir: FsPath): Flow<Listing> =
            flow {
                val provider =
                    when (val found = volumes.providerFor(dir.volume)) {
                        is FsResult.Err -> return@flow emit(Listing(emptyList(), complete = true, error = found.error))
                        is FsResult.Ok -> found.value
                    }
                var entries = emptyList<Entry>()
                var failed = false
                // The provider ends its flow after an error, so nothing arrives once `failed` is set.
                provider.list(dir).collect { batch ->
                    when (batch) {
                        is FsResult.Ok -> {
                            entries = entries + batch.value.filterNot { InternalEntries.isInternal(it.name) }
                            emit(Listing(entries, complete = false))
                        }

                        is FsResult.Err -> {
                            failed = true
                            emit(Listing(entries, complete = true, error = batch.error))
                        }
                    }
                }
                if (!failed) emit(Listing(entries, complete = true))
            }
    }
