package io.github.ctorres345.hemisphere.core.fs

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import java.io.IOException
import java.nio.file.ClosedWatchServiceException
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import kotlin.concurrent.thread

/**
 * A [DirectoryWatcher] on `java.nio.file.WatchService`, for JVM tests where `FileObserver` does not
 * fire. Linux uses inotify; macOS polls, so the JDK's high-sensitivity modifier is asked for when the
 * JDK has it (a 2-second poll instead of 10).
 */
internal object WatchServiceWatcher : DirectoryWatcher {
    private val kinds =
        arrayOf<WatchEvent.Kind<*>>(
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY,
        )

    private val highSensitivity: Array<WatchEvent.Modifier> =
        runCatching {
            val modifier = Class.forName("com.sun.nio.file.SensitivityWatchEventModifier").getField("HIGH").get(null)
            arrayOf(modifier as WatchEvent.Modifier)
        }.getOrDefault(emptyArray())

    override fun watch(folder: File): Flow<Unit> =
        callbackFlow {
            val path = folder.toPath()
            val service = path.fileSystem.newWatchService()
            try {
                path.register(service, kinds, *highSensitivity)
            } catch (e: IOException) {
                service.close()
                throw e
            }
            val poller =
                thread(isDaemon = true, name = "watch-${folder.name}") {
                    try {
                        while (true) {
                            val key = service.take()
                            key.pollEvents()
                            trySend(Unit)
                            key.reset()
                        }
                    } catch (
                        @Suppress("SwallowedException") e: ClosedWatchServiceException,
                    ) {
                        // The collector stopped.
                    } catch (
                        @Suppress("SwallowedException") e: InterruptedException,
                    ) {
                        // The test ended.
                    }
                }
            awaitClose {
                service.close()
                poller.interrupt()
            }
        }.buffer(Channel.CONFLATED)
}
