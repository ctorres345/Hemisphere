package io.github.ctorres345.hemisphere.core.model

// The one versioned string form of [FsPath] and [EntryKey]. Saved jobs, Trash rows and saved state
// keep these strings, so the v1 text never changes and [decodeFsPath] and [decodeEntryKey] read v1 forever.
//
// Fields are joined by `|`; each field escapes `%` as `%25`, `|` as `%7C`, `/` as `%2F`.
//
// - Path: `v1|<flags>|<volume>|<ref, empty for none, else ~ then ref>|` then `/<segment>` per segment.
//   Flags is one digit, 0 to 3: add 1 for case-insensitive and 2 for id-addressed.
// - Key by path: `v1|k|<volume>|` then `/<segment>` per segment.
// - Key by id: `v1|i|<volume>|<ref>`.
//
// Example: `v1|1|primary|~doc%7C1|/Music/A.txt`.
//
// Save paths, not keys: a by-path key holds folded names, and the fold uses the runtime's case tables,
// which can change with an OS update. Store the path form and rebuild the key after loading.

private const val VERSION = "v1"
private const val PATH_FIELDS = 5
private const val KEY_FIELDS = 4
private const val ESCAPE_LENGTH = 3
private const val IDX_REF = 3
private const val IDX_SEGMENTS = 4
private const val FLAG_CI = 1
private const val FLAG_ID = 2

/** Internal signal for bad text; always caught in the two public decoders. */
private class BadText(
    why: String,
) : Exception(why)

private fun esc(s: String): String = s.replace("%", "%25").replace("|", "%7C").replace("/", "%2F")

private fun unesc(s: String): String {
    val out = StringBuilder()
    var i = 0
    while (i < s.length) {
        val c = s[i]
        if (c == '%') {
            val code = s.substring(i + 1, minOf(i + ESCAPE_LENGTH, s.length))
            out.append(
                when (code) {
                    "25" -> '%'
                    "7C" -> '|'
                    "2F" -> '/'
                    else -> throw BadText("escape")
                },
            )
            i += ESCAPE_LENGTH
        } else {
            out.append(c)
            i++
        }
    }
    return out.toString()
}

private fun encodeSegments(segments: List<String>): String = segments.joinToString("") { "/" + esc(it) }

private fun decodeSegments(s: String): List<String> {
    if (s.isEmpty()) return emptyList()
    if (s[0] != '/') throw BadText("segments")
    return s.substring(1).split('/').map { unesc(it) }
}

private fun fields(
    text: String,
    count: Int,
): List<String> {
    val parts = text.split('|')
    if (parts.firstOrNull() != VERSION) throw BadText("unknown version")
    if (parts.size != count) throw BadText("field count")
    return parts
}

private fun <T> decoding(block: () -> T): FsResult<T> =
    try {
        FsResult.Ok(block())
    } catch (e: BadText) {
        FsResult.Err(FsError.Io("bad path text: ${e.message}"))
    }

/** The v1 string form of this path. */
fun FsPath.encode(): String =
    listOf(
        VERSION,
        ((if (caseInsensitive) FLAG_CI else 0) or (if (idAddressed) FLAG_ID else 0)).toString(),
        esc(volume.value),
        ref?.let { "~" + esc(it.value) }.orEmpty(),
        encodeSegments(segments),
    ).joinToString("|")

/** Reads [text] back into a path. Never throws; unknown version or bad text is an [FsResult.Err]. */
fun decodeFsPath(text: String): FsResult<FsPath> =
    decoding {
        val f = fields(text, PATH_FIELDS)
        val flags = if (f[1].length == 1) f[1][0] - '0' else -1
        if (flags !in 0..(FLAG_CI or FLAG_ID)) throw BadText("flag")
        val volume = f[2]
        val ref = f[IDX_REF]
        val segments = f[IDX_SEGMENTS]
        FsPath(
            VolumeId(unesc(volume)),
            decodeSegments(segments),
            when {
                ref.isEmpty() -> null
                ref[0] == '~' -> ProviderRef(unesc(ref.substring(1)))
                else -> throw BadText("ref")
            },
            caseInsensitive = flags and FLAG_CI != 0,
            idAddressed = flags and FLAG_ID != 0,
        )
    }

/** The v1 string form of this key. */
fun EntryKey.encode(): String =
    when (this) {
        is EntryKey.ByPath -> "$VERSION|k|${esc(volume.value)}|${encodeSegments(segments)}"
        is EntryKey.ById -> "$VERSION|i|${esc(volume.value)}|${esc(ref.value)}"
    }

/** Reads [text] back into a key. Never throws; unknown version or bad text is an [FsResult.Err]. */
fun decodeEntryKey(text: String): FsResult<EntryKey> =
    decoding {
        val f = fields(text, KEY_FIELDS)
        val kind = f[1]
        val volume = f[2]
        val rest = f[IDX_REF]
        when (kind) {
            "k" -> EntryKey.ByPath(VolumeId(unesc(volume)), decodeSegments(rest))
            "i" -> EntryKey.ById(VolumeId(unesc(volume)), ProviderRef(unesc(rest)))
            else -> throw BadText("key kind")
        }
    }
