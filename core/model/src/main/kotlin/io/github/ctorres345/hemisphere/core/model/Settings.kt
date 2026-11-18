package io.github.ctorres345.hemisphere.core.model

/** The theme the user picked. [FollowSystem] is the default until they choose. */
enum class ThemeSetting { Light, Dark, AMOLED, FollowSystem }

/** The 8 curated accents, named as in the design tokens (docs/design-system.md). */
enum class Accent { Blue, Teal, Green, Amber, Orange, Red, Pink, Violet }

/** Which face-button labels and confirm/cancel layout gamepad input uses. */
enum class FaceButtons { Auto, Xbox, Nintendo }

/** The pinned layout: [Auto] lets the layout shell decide from the window size. */
enum class LayoutPin { Auto, Single, Dual }

/** What kind of device this is; it keys the layout pin so one backup restores right everywhere. */
enum class DeviceClass { Phone, Handheld, Tablet }

enum class Orientation { Portrait, Landscape }

/** Every saved setting, with its default. */
data class Settings(
    val theme: ThemeSetting = ThemeSetting.FollowSystem,
    val accent: Accent = Accent.Blue,
    val faceButtons: FaceButtons = FaceButtons.Auto,
    val trashRetentionDays: Int = DEFAULT_TRASH_RETENTION_DAYS,
    val layoutPinPortrait: LayoutPin = LayoutPin.Auto,
    val layoutPinLandscape: LayoutPin = LayoutPin.Auto,
) {
    /** The user's pin for [orientation] on this device class. */
    fun pinFor(orientation: Orientation): LayoutPin =
        when (orientation) {
            Orientation.Portrait -> layoutPinPortrait
            Orientation.Landscape -> layoutPinLandscape
        }

    companion object {
        const val DEFAULT_TRASH_RETENTION_DAYS = 30
        val DEFAULT = Settings()
    }
}
