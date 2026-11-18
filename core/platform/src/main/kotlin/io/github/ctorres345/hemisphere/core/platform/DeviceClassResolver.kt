package io.github.ctorres345.hemisphere.core.platform

import io.github.ctorres345.hemisphere.core.model.DeviceClass

/**
 * What the rule needs to know about one input device. [isExternal] is null below API 29. A virtual
 * device (a key mapper or streaming app's pad) never counts.
 */
data class InputDeviceInfo(
    val hasGamepadSource: Boolean,
    val isExternal: Boolean?,
    val vendorId: Int,
    val name: String,
    val isVirtual: Boolean = false,
)

/** The device-class rule (HEM-32), free of Android types so a unit test can pin it. */
object DeviceClassResolver {
    /**
     * Names of known handheld built-in pads, lowercase. A heuristic used only below API 29, where
     * `InputDevice.isExternal` does not exist.
     */
    val BUILT_IN_NAMES = listOf("gpio-keys", "retroid", "odin", "ayn", "anbernic")

    const val TABLET_MIN_SMALLEST_WIDTH_DP = 600

    fun resolve(
        devices: List<InputDeviceInfo>,
        smallestScreenWidthDp: Int,
    ): DeviceClass =
        when {
            devices.any { it.hasGamepadSource && !it.isVirtual && isBuiltIn(it) } -> DeviceClass.Handheld
            smallestScreenWidthDp >= TABLET_MIN_SMALLEST_WIDTH_DP -> DeviceClass.Tablet
            else -> DeviceClass.Phone
        }

    private fun isBuiltIn(device: InputDeviceInfo): Boolean =
        device.isExternal?.not()
            ?: (device.vendorId == 0 || BUILT_IN_NAMES.any { device.name.lowercase().contains(it) })
}
