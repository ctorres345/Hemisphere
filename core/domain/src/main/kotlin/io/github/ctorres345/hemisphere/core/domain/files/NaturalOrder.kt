package io.github.ctorres345.hemisphere.core.domain.files

import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FolderSort

/**
 * Names the way people expect: runs of ASCII digits compare as numbers (`file2` before `file10`),
 * the rest compares one lower-cased character at a time. Names that still tie compare by their raw
 * text, so this is a total order and `sortedWith` never sees a contradiction. It allocates nothing,
 * as it runs for every pair a folder sort compares.
 */
internal object NaturalOrder : Comparator<String> {
    override fun compare(
        a: String,
        b: String,
    ): Int {
        var i = 0
        var j = 0
        var c = 0
        while (c == 0 && i < a.length && j < b.length) {
            if (a[i].isAsciiDigit() && b[j].isAsciiDigit()) {
                val endA = digitsEnd(a, i)
                val endB = digitsEnd(b, j)
                c = compareNumbers(a, i, endA, b, j, endB)
                i = endA
                j = endB
            } else {
                c = a[i].lowercaseChar().compareTo(b[j].lowercaseChar())
                i++
                j++
            }
        }
        if (c == 0) c = (a.length - i).compareTo(b.length - j)
        return if (c != 0) c else a.compareTo(b)
    }

    private fun digitsEnd(
        s: String,
        from: Int,
    ): Int {
        var end = from
        while (end < s.length && s[end].isAsciiDigit()) end++
        return end
    }

    /** Two digit runs by value: leading zeros skipped, then the longer run is larger, then digit by digit. */
    @Suppress("LongParameterList")
    private fun compareNumbers(
        a: String,
        startA: Int,
        endA: Int,
        b: String,
        startB: Int,
        endB: Int,
    ): Int {
        var i = startA
        var j = startB
        while (i < endA && a[i] == '0') i++
        while (j < endB && b[j] == '0') j++
        var c = (endA - i).compareTo(endB - j)
        while (c == 0 && i < endA) {
            c = a[i].compareTo(b[j])
            i++
            j++
        }
        return c
    }

    private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'
}

private val byName: Comparator<Entry> = compareBy(NaturalOrder) { it.name }

/**
 * The order [sort] puts entries in: folders first, then [FolderSort.field] in its direction. Entries
 * without a size or date count as smallest: first when ascending, last when descending. Equal sizes or
 * dates fall back to the natural name order, A to Z.
 */
internal fun FolderSort.comparator(): Comparator<Entry> {
    val byField: Comparator<Entry> =
        when (field) {
            FolderSort.Field.Name -> byName
            FolderSort.Field.Size -> compareBy(nullsFirst()) { it.size }
            FolderSort.Field.Date -> compareBy(nullsFirst()) { it.modified }
        }
    val ordered =
        compareBy<Entry> { it.kind != Entry.Kind.Folder }.then(if (descending) byField.reversed() else byField)
    // Equal sizes or dates fall back to A-to-Z names; the name order is total, so it needs no fallback.
    return if (field == FolderSort.Field.Name) ordered else ordered.then(byName)
}
