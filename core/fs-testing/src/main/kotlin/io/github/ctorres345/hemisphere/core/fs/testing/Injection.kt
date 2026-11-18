package io.github.ctorres345.hemisphere.core.fs.testing

import kotlinx.coroutines.CompletableDeferred

/** The calls of `FileSystemProvider` that [InMemoryProvider] can fail or stall. */
enum class FsCall {
    List,
    Stat,
    OpenRead,
    OpenWrite,
    CreateDirectory,
    Rename,
    Move,
    Copy,
    SetTimes,
    FreeSpace,
    Delete,
    Observe,
}

/** Where an injected stream failure hits, for [InMemoryProvider.failNextStream]. */
enum class StreamFault {
    /** Every read of the next `openRead` stream throws `IOException`. */
    Read,

    /** Every write to the next `openWrite` sink throws `IOException`, and its close commits nothing. */
    Write,

    /** Closing the next `openWrite` sink throws `IOException`, so the new bytes never land. */
    Close,
}

/**
 * A call held in place until [release], made by [InMemoryProvider.stallNext]. Cancelling the
 * caller while it waits works as for any suspend call.
 */
class Stall internal constructor() {
    private val reached = CompletableDeferred<Unit>()
    private val gate = CompletableDeferred<Unit>()

    /** Waits until a call has reached the stall, so a test knows the call is really suspended. */
    suspend fun awaitReached() = reached.await()

    /** Lets the stalled call go on. */
    fun release() {
        gate.complete(Unit)
    }

    internal suspend fun pass() {
        reached.complete(Unit)
        gate.await()
    }
}
