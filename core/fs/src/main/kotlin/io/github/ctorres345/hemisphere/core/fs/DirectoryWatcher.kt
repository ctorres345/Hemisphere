// detekt 1.23.8's IgnoredReturnValue crashes on this file (an NPE resolving a call's package), so
// it is skipped here; nothing in the file ignores a meaningful return value.
@file:Suppress("IgnoredReturnValue")

package io.github.ctorres345.hemisphere.core.fs

import android.os.FileObserver
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Signals that something directly in a folder changed, for [LocalProvider.observe]. A seam: the app
 * uses [FileObserverWatcher]; JVM tests inject one built on `java.nio.file.WatchService`, because
 * `FileObserver` may not fire under Robolectric.
 */
internal fun interface DirectoryWatcher {
    /** Emits once per change, conflated; ends or stays silent when the folder cannot be watched. */
    fun watch(folder: File): Flow<Unit>
}

/**
 * Watches with Android's `FileObserver` (inotify). All observers in a process share one inotify fd,
 * keyed by watch descriptor, and a folder has one descriptor: a second observer on the same folder
 * would replace the first, and stopping either would stop both. So each folder gets one shared
 * observer, started by its first collector and stopped when its last one stops.
 */
internal object FileObserverWatcher : DirectoryWatcher {
    private val observers = HashMap<String, SharedObserver>()

    override fun watch(folder: File): Flow<Unit> =
        callbackFlow {
            val key = folder.canonicalPath
            val listener = ChangeListener { trySend(Unit) }
            synchronized(observers) {
                observers.getOrPut(key) { SharedObserver(key).also { it.startWatching() } }.listeners += listener
            }
            awaitClose {
                synchronized(observers) { release(key, listener) }
            }
        }.buffer(Channel.CONFLATED)

    private fun release(
        key: String,
        listener: ChangeListener,
    ) {
        val shared = observers[key]
        shared?.listeners?.remove(listener)
        if (shared != null && shared.listeners.isEmpty()) {
            shared.stopWatching()
            observers.remove(key)
        }
    }
}

private fun interface ChangeListener {
    fun changed()
}

/**
 * One folder's observer; it calls every listener that watches the folder. Built with the String
 * constructor, which works on every API level (the File one needs API 29).
 */
@Suppress("DEPRECATION")
private class SharedObserver(
    path: String,
) : FileObserver(path, EVENTS) {
    val listeners = CopyOnWriteArraySet<ChangeListener>()

    private companion object {
        const val EVENTS =
            FileObserver.CREATE or FileObserver.DELETE or FileObserver.MOVED_FROM or FileObserver.MOVED_TO or
                FileObserver.CLOSE_WRITE or FileObserver.ATTRIB or FileObserver.DELETE_SELF or FileObserver.MOVE_SELF
    }

    override fun onEvent(
        event: Int,
        path: String?,
    ) = listeners.forEach { it.changed() }
}
