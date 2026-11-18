package io.github.ctorres345.hemisphere.core.fs

import kotlinx.coroutines.Job
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.coroutines.cancellation.CancellationException

/**
 * Stops a stream once the coroutine that opened it is cancelled: the next read or write closes the
 * file and throws `CancellationException`, so a cancelled copy loop never keeps a file open. Only a
 * cancelled job stops it; one that completed normally (an opener's `withContext` block) does not.
 */
private inline fun <T> FileChannel.whileActive(
    job: Job?,
    block: () -> T,
): T {
    if (job?.isCancelled == true) {
        closeQuietly()
        throw CancellationException("The coroutine that opened this file was cancelled")
    }
    return block()
}

private fun FileChannel.closeQuietly() {
    try {
        close()
    } catch (
        @Suppress("SwallowedException") e: IOException,
    ) {
        // Nothing was written that a caller could rely on: the job was cancelled.
    }
}

/** A local file read from its channel's position; see [whileActive] for cancellation. */
internal class LocalSource(
    private val channel: FileChannel,
    private val job: Job?,
) : InputStream() {
    override fun read(): Int {
        val one = ByteArray(1)
        return if (read(one, 0, 1) == -1) -1 else one[0].toInt() and BYTE_MASK
    }

    override fun read(
        bytes: ByteArray,
        offset: Int,
        length: Int,
    ): Int =
        channel.whileActive(job) {
            if (length == 0) 0 else channel.read(ByteBuffer.wrap(bytes, offset, length))
        }

    override fun skip(count: Long): Long =
        channel.whileActive(job) {
            val skipped = count.coerceIn(0, maxOf(0, channel.size() - channel.position()))
            channel.position(channel.position() + skipped)
            skipped
        }

    override fun available(): Int =
        channel.whileActive(job) {
            (channel.size() - channel.position()).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
        }

    override fun close() = channel.close()

    /** For tests: false once the file is closed. */
    internal val isOpen: Boolean get() = channel.isOpen
}

/**
 * A local file written through its channel. `close()` syncs the bytes to storage
 * (`FileChannel.force(true)`) before it returns, as [Sink] promises, then runs [onClosed]; if the sync
 * fails it throws and [onClosed] does not run. Once the opener's job is cancelled, `close()` only
 * closes the file. A write after `close()` throws `ClosedChannelException`.
 */
internal class LocalSink(
    private val channel: FileChannel,
    private val job: Job?,
    private val onClosed: () -> Unit,
) : OutputStream() {
    override fun write(byte: Int) = write(byteArrayOf(byte.toByte()), 0, 1)

    override fun write(
        bytes: ByteArray,
        offset: Int,
        length: Int,
    ) = channel.whileActive(job) {
        val buffer = ByteBuffer.wrap(bytes, offset, length)
        while (buffer.hasRemaining()) channel.write(buffer)
    }

    override fun close() {
        // Already closed, by a cancelled write too: that file was never meant to be kept.
        if (!channel.isOpen) return
        if (job?.isCancelled == true) {
            // A cancelled copy is not a finished file: no sync, no scan, and nothing thrown over the
            // cancellation.
            channel.closeQuietly()
            return
        }
        channel.use { it.force(true) }
        onClosed()
    }

    /** For tests: false once the file is closed. */
    internal val isOpen: Boolean get() = channel.isOpen
}

private const val BYTE_MASK = 0xFF
