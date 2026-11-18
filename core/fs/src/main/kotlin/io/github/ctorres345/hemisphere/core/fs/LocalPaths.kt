package io.github.ctorres345.hemisphere.core.fs

import android.annotation.SuppressLint
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

// Path rules and file moves behind LocalProvider.

private val UNSAFE_SEGMENTS = setOf("", ".", "..")

/** A segment that would name another folder than the one asked for, or escape the volume root. */
internal fun isUnsafeSegment(segment: String): Boolean =
    segment in UNSAFE_SEGMENTS || '/' in segment || '\u0000' in segment

/**
 * Free bytes the app may write to [folder]'s volume, 0 when it is full, null when the platform cannot
 * tell. The file store's figure comes first because it throws when it cannot read the volume, where
 * `File.usableSpace` answers 0 for both "full" and "unknown"; that one is only the fallback, with 0
 * read as unknown. Not `StorageManager.getAllocatableBytes`: it needs a storage UUID, which paths on
 * removable and app-home volumes do not map to, and clearing other apps' caches is not a file
 * manager's call.
 */
@SuppressLint("UsableSpace")
internal fun usableBytes(folder: File): Long? =
    try {
        Files.getFileStore(folder.toPath()).usableSpace
    } catch (
        @Suppress("SwallowedException", "TooGenericExceptionCaught") e: RuntimeException,
    ) {
        folder.usableSpace.takeIf { it > 0 } // UnsupportedOperationException, SecurityException
    } catch (
        @Suppress("SwallowedException") e: IOException,
    ) {
        folder.usableSpace.takeIf { it > 0 }
    }

/** True unless the folder's free space plus [freed] bytes is known to be less than [sizeHint]. */
internal fun fits(
    folder: Path,
    sizeHint: Long?,
    freed: Long,
): Boolean {
    val free = sizeHint?.let { usableBytes(folder.toFile()) } ?: return true // no hint, or unknown space
    return sizeHint <= free + freed
}

/** True when [target] would sit in [folder] or under it: a folder cannot move into itself. */
internal fun isInside(
    target: Path,
    folder: Path,
): Boolean {
    val parent = target.parent
    return parent != null &&
        Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS) &&
        Files.isDirectory(parent) &&
        parent.toRealPath().startsWith(folder.toRealPath())
}

/**
 * `Files.move` without `REPLACE_EXISTING` refuses a taken name. On a case-folding file system a
 * case-only rename looks taken, by the entry itself: `Files.move` would then do nothing, so that
 * one case goes through `rename(2)` directly.
 */
internal fun moveWithoutReplacing(
    source: Path,
    target: Path,
    attributes: AttributeReader,
) {
    if (isCaseOnlyRename(source, target, attributes)) {
        if (!source.toFile().renameTo(target.toFile())) throw IOException("rename to ${target.fileName} failed")
    } else {
        Files.move(source, target)
    }
}

private fun isCaseOnlyRename(
    source: Path,
    target: Path,
    attributes: AttributeReader,
): Boolean {
    val sourceName = source.fileName.toString()
    val targetName = target.fileName.toString()
    val sameEntryNewCase =
        sourceName != targetName &&
            sourceName.equals(targetName, ignoreCase = true) &&
            source.parent == target.parent &&
            Files.exists(target, LinkOption.NOFOLLOW_LINKS)
    if (!sameEntryNewCase) return false
    val key = attributes.read(source, followLinks = false).fileKey()
    return key != null && key == attributes.read(target, followLinks = false).fileKey()
}
