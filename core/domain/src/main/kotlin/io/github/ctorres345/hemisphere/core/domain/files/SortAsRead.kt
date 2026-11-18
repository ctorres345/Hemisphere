package io.github.ctorres345.hemisphere.core.domain.files

import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.Listing

/**
 * Sorts each listing of one folder read. While the order stays the same, the listing only grows at
 * its end, so the new tail joins the list sorted last time and TimSort merges the two runs.
 */
internal class SortAsRead {
    private var order: Comparator<Entry>? = null
    private var sorted = emptyList<Entry>()

    fun sorted(
        listing: Listing,
        next: Comparator<Entry>,
    ): Listing {
        val entries = listing.entries
        sorted =
            if (next === order && entries.size >= sorted.size) {
                (sorted + entries.subList(sorted.size, entries.size)).sortedWith(next)
            } else {
                entries.sortedWith(next)
            }
        order = next
        return listing.copy(entries = sorted)
    }
}
