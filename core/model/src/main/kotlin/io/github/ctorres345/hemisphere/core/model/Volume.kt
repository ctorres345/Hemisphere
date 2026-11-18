package io.github.ctorres345.hemisphere.core.model

/** One storage root with what it allows and what it can do. */
data class Volume(
    val id: VolumeId,
    val label: String,
    val kind: Kind,
    val limits: VolumeLimits = VolumeLimits.NONE,
    val capabilities: Capabilities = Capabilities.NONE,
) {
    enum class Kind { Internal, SdCard, Usb, GrantedFolder }
}

/** The volume's root path, carrying its case and id flags, so every path on it keys the same way. */
fun Volume.root(): FsPath = FsPath.root(id, limits.caseInsensitive, capabilities.idAddressed)
