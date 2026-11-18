package io.github.ctorres345.hemisphere.feature.browser.pane

import android.os.Bundle

/** Where [writePanes] puts each field: a [Bundle], or a byte count. */
internal interface SavedSink {
    fun putInt(
        key: String,
        value: Int,
    )

    fun putString(
        key: String,
        value: String,
    )

    fun putStrings(
        key: String,
        values: List<String>,
    )

    fun putInts(
        key: String,
        values: IntArray,
    )
}

internal class BundleSink(
    private val bundle: Bundle,
) : SavedSink {
    override fun putInt(
        key: String,
        value: Int,
    ) = bundle.putInt(key, value)

    override fun putString(
        key: String,
        value: String,
    ) = bundle.putString(key, value)

    override fun putStrings(
        key: String,
        values: List<String>,
    ) = bundle.putStringArrayList(key, ArrayList(values))

    override fun putInts(
        key: String,
        values: IntArray,
    ) = bundle.putIntArray(key, values)
}

/** Counts UTF-16 bytes of every key and string, and four bytes per int. */
internal class SizeCounter : SavedSink {
    var bytes = 0
        private set

    override fun putInt(
        key: String,
        value: Int,
    ) {
        bytes += key.length * UTF16 + INT
    }

    override fun putString(
        key: String,
        value: String,
    ) {
        bytes += (key.length + value.length) * UTF16
    }

    override fun putStrings(
        key: String,
        values: List<String>,
    ) {
        bytes += key.length * UTF16 + values.sumOf { it.length * UTF16 }
    }

    override fun putInts(
        key: String,
        values: IntArray,
    ) {
        bytes += key.length * UTF16 + values.size * INT
    }
}

private const val UTF16 = 2
private const val INT = 4
