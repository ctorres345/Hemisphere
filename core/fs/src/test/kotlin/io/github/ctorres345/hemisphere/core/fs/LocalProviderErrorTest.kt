package io.github.ctorres345.hemisphere.core.fs

import io.github.ctorres345.hemisphere.core.model.FsError
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.AccessDeniedException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileSystemException
import java.nio.file.NoSuchFileException

/** The one error mapper, on the exceptions the JDK and Android's libcore throw. */
class LocalProviderErrorTest {
    @Test
    fun `out of space is NoSpace, from the JDK and from Android`() {
        assertEquals(FsError.NoSpace, fsErrorOf(FileSystemException("/v/a", null, "No space left on device")))
        assertEquals(FsError.NoSpace, fsErrorOf(IOException("write failed: ENOSPC (No space left on device)")))
        assertEquals(FsError.NoSpace, fsErrorOf(IOException("write failed: EDQUOT (Disk quota exceeded)")))
    }

    @Test
    fun `a failed channel write, a bare strerror message, is mapped too`() {
        assertEquals(FsError.NoSpace, fsErrorOf(IOException("No space left on device")))
        assertEquals(FsError.FileTooLarge, fsErrorOf(IOException("File too large")))
    }

    @Test
    fun `a missing parent is NotFound`() {
        assertEquals(FsError.NotFound, fsErrorOf(NoSuchFileException("/v/missing/a.txt")))
        assertEquals(FsError.NotFound, fsErrorOf(FileNotFoundException("/v/missing/a.txt (No such file or directory)")))
        assertEquals(FsError.NotFound, fsErrorOf(IOException("open failed: ENOENT (No such file or directory)")))
    }

    @Test
    fun `EFBIG is FileTooLarge`() {
        assertEquals(FsError.FileTooLarge, fsErrorOf(IOException("write failed: EFBIG (File too large)")))
        assertEquals(FsError.FileTooLarge, fsErrorOf(FileSystemException("/v/a", null, "File too large")))
    }

    @Test
    fun `the other errnos map to their FsError`() {
        assertEquals(FsError.ReadOnly, fsErrorOf(FileSystemException("/v/a", null, "Read-only file system")))
        assertEquals(FsError.AccessDenied, fsErrorOf(FileNotFoundException("/v/a (Permission denied)")))
        assertEquals(FsError.AccessDenied, fsErrorOf(IOException("open failed: EPERM (Operation not permitted)")))
        assertEquals(FsError.InvalidName, fsErrorOf(FileSystemException("/v/a", null, "File name too long")))
        assertEquals(FsError.AlreadyExists, fsErrorOf(IOException("open failed: EEXIST (File exists)")))
    }

    @Test
    fun `the exception type decides when no errno is named`() {
        assertEquals(FsError.AlreadyExists, fsErrorOf(FileAlreadyExistsException("/v/a")))
        assertEquals(FsError.AccessDenied, fsErrorOf(AccessDeniedException("/v/a")))
        assertEquals(FsError.Io("boom"), fsErrorOf(IOException("boom")))
    }

    @Test
    fun `an errno in a cause counts`() {
        val cause = IOException("write failed: ENOSPC (No space left on device)")

        assertEquals(FsError.NoSpace, fsErrorOf(IOException("copy failed", cause)))
    }

    @Test
    fun `a file whose name looks like an errno is not taken for one`() {
        assertEquals(FsError.NotFound, fsErrorOf(NoSuchFileException("/v/ENOSPC (No space left on device)/a")))
        assertEquals(FsError.AlreadyExists, fsErrorOf(FileAlreadyExistsException("/v/EFBIG")))
    }
}
