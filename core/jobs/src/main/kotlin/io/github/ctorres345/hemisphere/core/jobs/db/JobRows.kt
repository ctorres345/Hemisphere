package io.github.ctorres345.hemisphere.core.jobs.db

import io.github.ctorres345.hemisphere.core.jobs.JobCursor
import io.github.ctorres345.hemisphere.core.jobs.JobRecord
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.JobId
import io.github.ctorres345.hemisphere.core.model.JobKind
import io.github.ctorres345.hemisphere.core.model.JobProgress
import io.github.ctorres345.hemisphere.core.model.JobRequest
import io.github.ctorres345.hemisphere.core.model.JobState
import io.github.ctorres345.hemisphere.core.model.JobStatus
import io.github.ctorres345.hemisphere.core.model.decodeFsPath
import io.github.ctorres345.hemisphere.core.model.encode

// Mapping between `jobs` rows and the queue's types.

internal fun JobRequest.toEntity(): JobEntity =
    JobEntity(
        kind = kind.name,
        status = JobStatus.Queued.name,
        roots = PathList.encode(roots),
        destination = destination?.encode(),
        options = options,
        newName = newName,
    )

/** The request this row was saved from, or `null` when the row is damaged. */
internal fun JobEntity.savedRequest(): JobRequest? {
    val savedKind = JobKind.entries.firstOrNull { it.name == kind }
    val savedRoots = PathList.decode(roots)?.takeIf { it.isNotEmpty() }
    val savedDestination = destination?.let(::pathOrNull)
    val destinationOk = destination == null || savedDestination != null
    return if (savedKind != null && savedRoots != null && destinationOk) {
        JobRequest(savedKind, savedRoots, savedDestination, options, newName)
    } else {
        null
    }
}

internal fun JobEntity.savedStatus(): JobStatus =
    JobStatus.entries.firstOrNull { it.name == status } ?: JobStatus.Failed

internal fun JobEntity.savedProgress(): JobProgress = JobProgress(itemsDone, itemsTotal, bytesDone, bytesTotal)

internal fun JobEntity.savedCursor(): JobCursor =
    JobCursor(cursorLastItem, cursorBytes, tempCounter, tempPath?.let(::pathOrNull))

internal fun JobEntity.toState(kind: JobKind): JobState =
    JobState(JobId(id), kind, savedStatus(), savedProgress(), currentItem?.let(::pathOrNull), error)

internal fun JobRecord.toEntity(job: JobId): RecordEntity =
    RecordEntity(jobId = job.value, type = type.name, path = path.encode(), detail = detail)

private fun pathOrNull(text: String): FsPath? = (decodeFsPath(text) as? FsResult.Ok)?.value
