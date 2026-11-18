package io.github.ctorres345.hemisphere.core.domain.files

import io.github.ctorres345.hemisphere.core.fs.LockedFolders
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.VolumeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetFolderLockUseCaseTest {
    private val root = FsPath.root(VolumeId("primary"))
    private val android = root.child("Android")

    @Test
    fun `on Android 11 and up Android data and obb are locked, in any case`() {
        val lock = GetFolderLockUseCase(LockedFolders(sdkInt = 30))

        assertTrue(lock(android.child("data")).locked)
        assertTrue(lock(android.child("OBB")).locked)
        assertTrue(lock(android).isChildLocked("Data"))
        assertFalse(lock(android).isChildLocked("media"))
        assertFalse(lock(android.child("media")).locked)
    }

    @Test
    fun `below Android 11 nothing is locked`() {
        val lock = GetFolderLockUseCase(LockedFolders(sdkInt = 29))

        assertEquals(FolderLock.NONE, lock(android.child("data")))
        assertEquals(FolderLock.NONE, lock(android))
    }

    @Test
    fun `only the folders at the volume root count`() {
        val lock = GetFolderLockUseCase(LockedFolders(sdkInt = 34))

        assertFalse(lock(root.child("Games").child("Android").child("data")).locked)
        assertEquals(emptySet<String>(), lock(root.child("Games").child("Android")).lockedChildren)
        assertEquals(FolderLock.NONE, lock(root))
    }
}
