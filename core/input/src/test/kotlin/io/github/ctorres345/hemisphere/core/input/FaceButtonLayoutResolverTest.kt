package io.github.ctorres345.hemisphere.core.input

import io.github.ctorres345.hemisphere.core.designsystem.FaceButtonLayout
import io.github.ctorres345.hemisphere.core.model.FaceButtons
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FaceButtonLayoutResolverTest {
    private val nintendo = 0x057e

    @Test
    fun explicitSettingsWinOverTheDevice() {
        assertEquals(FaceButtonLayout.Xbox, FaceButtonLayoutResolver.resolve(FaceButtons.Xbox, nintendo))
        assertEquals(FaceButtonLayout.Nintendo, FaceButtonLayoutResolver.resolve(FaceButtons.Nintendo, null))
    }

    @Test
    fun autoUsesTheDeviceTableAndFallsBackToXbox() {
        assertEquals(FaceButtonLayout.Nintendo, FaceButtonLayoutResolver.resolve(FaceButtons.Auto, nintendo))
        assertEquals(FaceButtonLayout.Xbox, FaceButtonLayoutResolver.resolve(FaceButtons.Auto, 0x045e))
        assertEquals(FaceButtonLayout.Xbox, FaceButtonLayoutResolver.resolve(FaceButtons.Auto, null))
    }

    @Test
    fun layoutFlowFollowsTheSettingAndTheDevice() =
        runTest {
            val setting = MutableStateFlow(FaceButtons.Auto)
            val vendor = MutableStateFlow<Int?>(null)
            val resolver = FaceButtonLayoutResolver(setting, vendor)
            assertEquals(FaceButtonLayout.Xbox, resolver.layout.first())
            vendor.value = nintendo
            assertEquals(FaceButtonLayout.Nintendo, resolver.layout.first())
            setting.value = FaceButtons.Xbox
            assertEquals(FaceButtonLayout.Xbox, resolver.layout.first())
        }
}
