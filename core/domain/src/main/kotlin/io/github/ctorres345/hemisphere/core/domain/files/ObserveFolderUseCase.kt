package io.github.ctorres345.hemisphere.core.domain.files

import io.github.ctorres345.hemisphere.core.fs.DefaultDispatcher
import io.github.ctorres345.hemisphere.core.fs.VolumeRepository
import io.github.ctorres345.hemisphere.core.jobs.InternalEntries
import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.Listing
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Streams a folder as [Listing]s (architecture §3): every entry read so far, sorted folders first
 * and then by name, with `complete = false` until the last batch. The app's own entries (Trash, temp
 * files) are hidden. An error part-way keeps what was read and ends the stream.
 *
 * Saved per-folder sort and change signals come with the tickets that add them.
 */
class ObserveFolderUseCase
    @Inject
    constructor(
        private val volumes: VolumeRepository,
        @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    ) {
        operator fun invoke(dir: FsPath): Flow<Listing> =
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
                            entries =
                                (
                                    entries +
                                        batch.value.filterNot {
                                            InternalEntries.isInternal(
                                                it.name,
                                            )
                                        }
                                ).sortedWith(order)
                            emit(Listing(entries, complete = false))
                        }

                        is FsResult.Err -> {
                            failed = true
                            emit(Listing(entries, complete = true, error = batch.error))
                        }
                    }
                }
                if (!failed) emit(Listing(entries, complete = true))
            }.flowOn(defaultDispatcher)

        private companion object {
            val order: Comparator<Entry> =
                compareBy<Entry> { it.kind != Entry.Kind.Folder }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    .thenBy { it.name }
        }
    }
