package io.github.ctorres345.hemisphere.core.model

/** What a volume's file system refuses: file size, name characters, and name case rules. */
data class VolumeLimits(
    /** Largest file in bytes, or `null` for no limit. */
    val maxFileSize: Long? = null,
    /** Characters this file system refuses on top of [ALWAYS_FORBIDDEN]. */
    val forbiddenNameChars: Set<Char> = emptySet(),
    val caseInsensitive: Boolean = false,
    /** FAT drops a trailing dot or space, so such a name would be saved under another name. */
    val rejectsTrailingDotOrSpace: Boolean = false,
    /** Longest name in UTF-16 units, or `null` for no limit here. */
    val maxNameLength: Int? = null,
) {
    /** `null` when [name] and [size] are allowed, else [FsError.InvalidName] or [FsError.FileTooLarge]. */
    fun check(
        name: String,
        size: Long = 0L,
    ): FsError? =
        when {
            name.isEmpty() || name == "." || name == ".." -> FsError.InvalidName
            name.any { it in ALWAYS_FORBIDDEN || it in forbiddenNameChars } -> FsError.InvalidName
            maxNameLength != null && name.length > maxNameLength -> FsError.InvalidName
            rejectsTrailingDotOrSpace && (name.endsWith('.') || name.endsWith(' ')) -> FsError.InvalidName
            maxFileSize != null && size > maxFileSize -> FsError.FileTooLarge
            else -> null
        }

    companion object {
        /** No file system allows these in a name: `/` separates segments and NUL ends the name. */
        val ALWAYS_FORBIDDEN = setOf('/', '\u0000')

        val NONE = VolumeLimits()

        private val CONTROL_CHARS = (0 until 0x20).map { it.toChar() }.toSet()

        /**
         * FAT32: a file is at most 4 GiB minus one byte; names may not hold `/ : ? " * | < > \` or
         * control characters, nor end in a dot or a space, nor be longer than 255 UTF-16 units.
         */
        val FAT32 =
            VolumeLimits(
                maxFileSize = 4L * 1024 * 1024 * 1024 - 1,
                forbiddenNameChars = setOf('/', ':', '?', '"', '*', '|', '<', '>', '\\') + CONTROL_CHARS,
                caseInsensitive = true,
                rejectsTrailingDotOrSpace = true,
                maxNameLength = FAT_MAX_NAME,
            )

        private const val FAT_MAX_NAME = 255
    }
}
