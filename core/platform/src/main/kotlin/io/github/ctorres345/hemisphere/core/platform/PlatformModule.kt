package io.github.ctorres345.hemisphere.core.platform

import android.content.Context
import android.os.Build
import android.view.InputDevice
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.ctorres345.hemisphere.core.model.DeviceClass
import javax.inject.Singleton

/** Computes the [DeviceClass] once per process; `core:data` receives it by injection. */
@Module
@InstallIn(SingletonComponent::class)
object PlatformModule {
    @Provides
    @Singleton
    fun deviceClass(
        @ApplicationContext context: Context,
    ): DeviceClass {
        val devices =
            InputDevice
                .getDeviceIds()
                .toList()
                .mapNotNull { InputDevice.getDevice(it) }
                .map { it.toInfo() }
        return DeviceClassResolver.resolve(devices, context.resources.configuration.smallestScreenWidthDp)
    }

    private fun InputDevice.toInfo() =
        InputDeviceInfo(
            hasGamepadSource = sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD,
            isExternal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) isExternal else null,
            vendorId = vendorId,
            name = name,
            isVirtual = isVirtual,
        )
}
