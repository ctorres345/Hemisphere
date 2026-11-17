package io.github.ctorres345.hemisphere.core.model

/** The result of a file-system call. Errors are values; no exception crosses `core:fs` (architecture §3). */
sealed interface FsResult<out T> {
    data class Ok<out T>(
        val value: T,
    ) : FsResult<T>

    data class Err(
        val error: FsError,
    ) : FsResult<Nothing>
}

/** Why a file-system call failed. The UI turns each one into a message and, where it can, an action. */
sealed interface FsError {
    data object NotFound : FsError

    data object AccessDenied : FsError

    data object NoSpace : FsError

    data object AlreadyExists : FsError

    data object ReadOnly : FsError

    data object Unsupported : FsError

    data object VolumeUnavailable : FsError

    data object FileTooLarge : FsError

    data object InvalidName : FsError

    data object AuthRequired : FsError

    data object Offline : FsError

    data object Timeout : FsError

    /** Any other I/O failure. [message] is for logs only, never shown to the user. */
    data class Io(
        val message: String?,
    ) : FsError
}
