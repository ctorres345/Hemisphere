package io.github.ctorres345.hemisphere.core.model

/** Whether the app may read and write shared storage (ADR 0002). */
enum class StorageAccessState {
    /** Shared storage can be used. */
    Granted,

    /** Not granted yet, or revoked. The Grant button can still ask. */
    Denied,

    /** The system will not ask again: the user has to turn access on in the app's settings page. */
    NeedsSettings,
}
