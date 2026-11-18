package io.github.ctorres345.hemisphere.core.jobs.db

import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.FsResult
import io.github.ctorres345.hemisphere.core.model.decodeFsPath
import io.github.ctorres345.hemisphere.core.model.encode

/**
 * A job's roots as one column: each path's v1 text with its length in front (`<length>:<text>`),
 * so no character in a name can split the list.
 */
internal object PathList {
    fun encode(paths: List<FsPath>): String =
        paths.joinToString("") { it.encode().let { text -> "${text.length}:$text" } }

    /** The paths in [text], or `null` when the text is damaged. */
    fun decode(text: String): List<FsPath>? {
        val out = mutableListOf<FsPath>()
        var i = 0
        while (i < text.length) {
            val colon = text.indexOf(':', i)
            val length = if (colon < 0) null else text.substring(i, colon).toIntOrNull()
            val end = if (length == null) -1 else colon + 1 + length
            val path = if (end in (colon + 1)..text.length) decodeFsPath(text.substring(colon + 1, end)) else null
            if (path !is FsResult.Ok) return null
            out += path.value
            i = end
        }
        return out
    }
}
