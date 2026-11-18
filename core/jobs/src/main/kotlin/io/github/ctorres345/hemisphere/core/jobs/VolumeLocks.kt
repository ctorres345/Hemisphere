package io.github.ctorres345.hemisphere.core.jobs

import io.github.ctorres345.hemisphere.core.model.VolumeId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The per-volume job lock. Runners and the mount scan (HEM-47, HEM-50) hold it while they touch a
 * volume's temp or Trash files. Not reentrant.
 */
@Singleton
class VolumeLocks
    @Inject
    constructor() {
        private val locks = ConcurrentHashMap<VolumeId, Mutex>()

        /** Runs [block] holding every lock in [volumes], taken in a fixed order so two callers never deadlock. */
        suspend fun <T> withVolumes(
            volumes: Set<VolumeId>,
            block: suspend () -> T,
        ): T = lockAll(volumes.sortedBy { it.value }.map(::lockFor), block)

        private fun lockFor(volume: VolumeId): Mutex = locks.getOrPut(volume) { Mutex() }

        private suspend fun <T> lockAll(
            mutexes: List<Mutex>,
            block: suspend () -> T,
        ): T =
            if (mutexes.isEmpty()) {
                block()
            } else {
                mutexes.first().withLock { lockAll(mutexes.drop(1), block) }
            }
    }
