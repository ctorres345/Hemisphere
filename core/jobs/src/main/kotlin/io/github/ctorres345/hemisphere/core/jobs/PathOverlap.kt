package io.github.ctorres345.hemisphere.core.jobs

import io.github.ctorres345.hemisphere.core.model.EntryKey
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.JobRequest

/**
 * The overlap rule of [JobQueue.enqueue]. A job claims its roots and the entries it creates in the
 * destination (exclusive), and the destination folder (shared). Two exclusive claims clash when
 * one path is the other or an ancestor of it. A shared claim clashes only with an exclusive claim
 * on that folder or an ancestor; two shared claims never clash. The entries a job creates in a
 * shared folder are its own exclusive claims, so they still clash with another job's. A rename
 * also claims its new name next to the root, exclusively.
 */
internal object PathOverlap {
    private class Claim(
        val path: FsPath,
        val shared: Boolean,
    )

    fun clashes(
        new: JobRequest,
        existing: JobRequest,
    ): Boolean {
        val mine = claims(new)
        val theirs = claims(existing)
        return mine.any { a -> theirs.any { b -> clash(a, b) } }
    }

    private fun claims(request: JobRequest): List<Claim> {
        val renamed =
            request.newName?.let { name ->
                request.roots.map { Claim(it.parent().child(name), shared = false) }
            }
        val roots = request.roots.map { Claim(it, shared = false) } + renamed.orEmpty()
        val destination = request.destination ?: return roots
        val created =
            request.roots.mapNotNull { root ->
                root.name?.let { Claim(destination.child(it), shared = false) }
            }
        return roots + created + Claim(destination, shared = true)
    }

    private fun clash(
        a: Claim,
        b: Claim,
    ): Boolean =
        when {
            a.shared && b.shared -> false
            a.shared -> covers(b.path, a.path)
            b.shared -> covers(a.path, b.path)
            else -> covers(a.path, b.path) || covers(b.path, a.path)
        }

    /** Whether [outer] is [inner] or an ancestor of it: by name-based key, or by equal id-based keys. */
    private fun covers(
        outer: FsPath,
        inner: FsPath,
    ): Boolean {
        val sameKey = outer.key == inner.key
        val o = nameKey(outer)
        val i = nameKey(inner)
        val byName =
            o.volume == i.volume && i.segments.size >= o.segments.size &&
                i.segments.subList(0, o.segments.size) == o.segments
        return sameKey || byName
    }

    private fun nameKey(path: FsPath): EntryKey.ByPath =
        EntryKey.of(path.volume, path.segments, path.caseInsensitive) as EntryKey.ByPath
}
