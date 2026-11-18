package io.github.ctorres345.hemisphere.core.jobs

import io.github.ctorres345.hemisphere.core.model.JobId
import io.github.ctorres345.hemisphere.core.model.JobRequest
import io.github.ctorres345.hemisphere.core.model.VolumeId

/**
 * Which queued jobs start now (architecture §4). Short jobs always start. A long job starts when
 * fewer than [MAX_LONG] long jobs run and no long job runs on its volume (the destination's, else
 * its first root's). Queued jobs are taken in queue order.
 */
internal object Scheduler {
    const val MAX_LONG = 2

    data class Slot(
        val id: JobId,
        val isShort: Boolean,
        val volume: VolumeId,
    )

    /** The slot [request] needs: its destination's volume, else its first root's. */
    fun slot(
        id: JobId,
        request: JobRequest,
    ) = Slot(id, request.kind.isShort, (request.destination ?: request.roots.first()).volume)

    fun toStart(
        queued: List<Slot>,
        running: List<Slot>,
    ): List<JobId> {
        val longs = running.filterNot { it.isShort }
        val busy = longs.mapTo(mutableSetOf()) { it.volume }
        var longCount = longs.size
        val start = mutableListOf<JobId>()
        for (job in queued) {
            if (job.isShort) {
                start += job.id
            } else if (longCount < MAX_LONG && job.volume !in busy) {
                start += job.id
                busy += job.volume
                longCount++
            }
        }
        return start
    }
}
