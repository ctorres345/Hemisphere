package io.github.ctorres345.hemisphere.core.jobs

/**
 * The app's own entries on a volume: the Trash folder and the temp names that copy and rename use
 * (architecture §3). Listings hide them and the job walker skips them.
 */
object InternalEntries {
    private const val TRASH_FOLDER = ".Trash-hemisphere"
    private val tempPrefixes = listOf(".hemisphere-part-", ".hemisphere-rename-")

    fun isInternal(name: String): Boolean = name == TRASH_FOLDER || tempPrefixes.any(name::startsWith)
}
