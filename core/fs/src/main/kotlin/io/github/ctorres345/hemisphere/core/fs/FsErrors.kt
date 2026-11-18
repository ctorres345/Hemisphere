package io.github.ctorres345.hemisphere.core.fs

import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.FsResult
import java.io.IOException
import java.nio.file.AccessDeniedException
import java.nio.file.DirectoryIteratorException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileSystemException
import java.nio.file.InvalidPathException
import java.nio.file.NoSuchFileException
import java.nio.file.NotDirectoryException

/**
 * Runs [block] and turns the I/O failures it throws into an [FsError]; no exception leaves `core:fs`
 * except `CancellationException`, which is not caught here.
 */
@Suppress("SwallowedException") // the mapped type is the whole message
internal inline fun <T> catching(block: () -> T): FsResult<T> =
    try {
        FsResult.Ok(block())
    } catch (e: InvalidPathException) {
        FsResult.Err(FsError.InvalidName)
    } catch (e: DirectoryIteratorException) {
        FsResult.Err(fsErrorOf(e.cause))
    } catch (e: IOException) {
        FsResult.Err(fsErrorOf(e))
    } catch (e: SecurityException) {
        FsResult.Err(FsError.AccessDenied)
    }

/**
 * The one error mapper for local file I/O. The errno decides first, wherever the cause chain names
 * one: a `FileSystemException`'s reason (the JDK's `strerror` text), Android's libcore form
 * `open failed: ENOSPC (No space left on device)`, `java.io`'s `path (No space left on device)`, or
 * a bare `No space left on device`, as a failed channel write throws it.
 * Only then does the exception type decide. File paths in messages are never matched on their own,
 * so a file named `ENOSPC` is not taken for an errno.
 */
internal fun fsErrorOf(error: Throwable?): FsError =
    errnoOf(error)
        ?: when (error) {
            is FileAlreadyExistsException -> FsError.AlreadyExists
            is NoSuchFileException -> FsError.NotFound
            is NotDirectoryException -> FsError.Unsupported
            is AccessDeniedException -> FsError.AccessDenied
            else -> FsError.Io(error?.message)
        }

/** The [FsError] for the first errno named anywhere in [error]'s cause chain, or null. */
private fun errnoOf(error: Throwable?): FsError? =
    generateSequence(error) { it.cause.takeIf { cause -> cause !== it } }
        .take(MAX_CAUSES)
        .firstNotNullOfOrNull { cause -> ERRNOS.firstOrNull { it.matches(cause) }?.error }

/** One errno: its symbol, as Android names it, and the `strerror` text the JDK uses. */
private class Errno(
    symbol: String,
    private val text: String,
    val error: FsError,
) {
    private val inMessage = Regex("""\b$symbol \(|\(${Regex.escape(text)}\)$""")

    /**
     * A `FileSystemException` is judged by its reason alone, since its message holds the path. Any
     * other exception matches by its whole message being the `strerror` text (what `FileChannel.write`
     * throws on a full disk) or by the errno forms above.
     */
    fun matches(cause: Throwable): Boolean {
        val said = if (cause is FileSystemException) cause.reason else cause.message
        return said != null && (said.equals(text, ignoreCase = true) || inMessage.containsMatchIn(said))
    }
}

private const val MAX_CAUSES = 8

private val ERRNOS =
    listOf(
        Errno("ENOSPC", "No space left on device", FsError.NoSpace),
        Errno("EDQUOT", "Disk quota exceeded", FsError.NoSpace),
        Errno("EFBIG", "File too large", FsError.FileTooLarge),
        Errno("EROFS", "Read-only file system", FsError.ReadOnly),
        Errno("ENAMETOOLONG", "File name too long", FsError.InvalidName),
        Errno("EEXIST", "File exists", FsError.AlreadyExists),
        Errno("ENOENT", "No such file or directory", FsError.NotFound),
        Errno("EACCES", "Permission denied", FsError.AccessDenied),
        Errno("EPERM", "Operation not permitted", FsError.AccessDenied),
    )
