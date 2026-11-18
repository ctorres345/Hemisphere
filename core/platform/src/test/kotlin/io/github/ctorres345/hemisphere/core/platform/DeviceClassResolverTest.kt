package io.github.ctorres345.hemisphere.core.platform

import io.github.ctorres345.hemisphere.core.model.DeviceClass
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceClassResolverTest {
    private fun pad(
        external: Boolean?,
        vendor: Int = 1234,
        name: String = "pad",
    ) = InputDeviceInfo(hasGamepadSource = true, isExternal = external, vendorId = vendor, name = name)

    @Test
    fun `built-in gamepad is a handheld`() {
        assertEquals(DeviceClass.Handheld, DeviceClassResolver.resolve(listOf(pad(external = false)), 400))
    }

    @Test
    fun `a virtual gamepad does not make a handheld`() {
        val virtual = InputDeviceInfo(true, false, 0, "uinput-pad", isVirtual = true)
        assertEquals(DeviceClass.Phone, DeviceClassResolver.resolve(listOf(virtual), 400))
    }

    @Test
    fun `external gamepad on a small screen is a phone`() {
        assertEquals(DeviceClass.Phone, DeviceClassResolver.resolve(listOf(pad(external = true)), 400))
    }

    @Test
    fun `external gamepad on a big screen is a tablet`() {
        assertEquals(DeviceClass.Tablet, DeviceClassResolver.resolve(listOf(pad(external = true)), 600))
    }

    @Test
    fun `no gamepad is a tablet at 600 dp and a phone at 599`() {
        assertEquals(DeviceClass.Tablet, DeviceClassResolver.resolve(emptyList(), 600))
        assertEquals(DeviceClass.Phone, DeviceClassResolver.resolve(emptyList(), 599))
    }

    @Test
    fun `a non-gamepad built-in device does not make a handheld`() {
        val keys = InputDeviceInfo(false, false, 0, "gpio-keys")
        assertEquals(DeviceClass.Phone, DeviceClassResolver.resolve(listOf(keys), 400))
    }

    @Test
    fun `before API 29 vendor id 0 is built in`() {
        assertEquals(DeviceClass.Handheld, DeviceClassResolver.resolve(listOf(pad(null, vendor = 0)), 400))
    }

    @Test
    fun `before API 29 a known built-in name is built in and an unknown one is not`() {
        assertEquals(DeviceClass.Handheld, DeviceClassResolver.resolve(listOf(pad(null, name = "Retroid Pad")), 400))
        assertEquals(DeviceClass.Phone, DeviceClassResolver.resolve(listOf(pad(null, name = "Xbox Wireless")), 400))
    }
}
