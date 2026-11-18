package io.github.ctorres345.hemisphere.core.fs

import java.io.InputStream
import java.io.OutputStream

/**
 * Bytes read from a provider, as a plain `java.io.InputStream`.
 *
 * Why `java.io` and not okio: every consumer we plan (Commons Compress, `ContentResolver`
 * streams, `FileChannel`, `Files.newInputStream`) already speaks `InputStream` and `OutputStream`,
 * so a provider hands its stream out without a wrapper, and `core:fs` gains no dependency.
 * okio's buffering and `Timeout` are nice, but not needed while the job runner owns the copy loop
 * and its buffer.
 *
 * Streams are blocking: callers read on an IO dispatcher. A read that fails throws `IOException`
 * (the one place an exception leaves `core:fs`, because `java.io` has no other way to fail);
 * callers map it with their own `FsError.Io`. Close the stream when done.
 */
typealias Source = InputStream

/**
 * Bytes written to a provider, as a plain `java.io.OutputStream`. See [Source] for why `java.io`.
 *
 * The contract of [OutputStream.close] is stronger than the JDK's: **`close()` returns only after
 * the bytes are synced to storage** (`FileChannel.force`, `FileDescriptor.sync`, or the provider's
 * equivalent), so a job may rename the temp file to its final name right after `close()` and a
 * power cut cannot leave an empty file under that name. If the sync fails, `close()` throws
 * `IOException` and the data must not be treated as written. [OutputStream.flush] only hands bytes
 * to the provider and does not sync. A provider whose sink cannot be synced (a pipe or socket from
 * a `ContentResolver`, where `sync` fails with `EINVAL`) treats a clean close as synced and says so
 * in its own docs.
 *
 * What a reader sees under the name before `close()` returns is unspecified; after it returns, the
 * whole content is there. A sink that is never closed (the job was cancelled) leaves the entry as
 * it was at open, or a partial file: callers write to a temp name and delete it on failure.
 * `close()` is idempotent; a write after `close()` throws `IOException`.
 */
typealias Sink = OutputStream

/** How [FileSystemProvider.openWrite] treats an entry that is already there. */
enum class WriteMode {
    /** The entry must not exist, else `FsError.AlreadyExists`. Nothing is changed in that case. */
    Create,

    /**
     * Create the entry, or replace its whole content. The old bytes are gone as soon as the sink is
     * opened (the file is truncated at open, as `O_TRUNC` does), not when it is closed. To replace a
     * file safely, write a temp name with `Create` and `rename` it over once `close()` returns.
     */
    Overwrite,

    /**
     * Add to the end of an existing entry, to resume a copy. Only when `capabilities.resume`; else
     * `Unsupported`. `sizeHint` is then the number of bytes to add.
     */
    Append,
}
