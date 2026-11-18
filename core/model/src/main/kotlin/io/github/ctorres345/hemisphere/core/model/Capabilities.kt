package io.github.ctorres345.hemisphere.core.model

/** What a volume's provider can do (architecture §3). Everything defaults to off. */
data class Capabilities(
    val idAddressed: Boolean = false,
    val move: Boolean = false,
    val randomAccess: Boolean = false,
    val resume: Boolean = false,
    val serverCopy: Boolean = false,
    val setTimes: Boolean = false,
    val watch: Boolean = false,
) {
    companion object {
        val NONE = Capabilities()
    }
}
