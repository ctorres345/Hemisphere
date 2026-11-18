package io.github.ctorres345.hemisphere.core.input

import io.github.ctorres345.hemisphere.core.data.settings.SettingsRepository
import io.github.ctorres345.hemisphere.core.designsystem.FaceButtonLayout
import io.github.ctorres345.hemisphere.core.model.FaceButtons
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Resolves the Face buttons setting (Auto, Xbox, Nintendo) to a [FaceButtonLayout].
 *
 * Android does not report the layout, so Auto is a heuristic (docs/design-system.md §3): a small table
 * of vendor ids that ship Nintendo-style pads, falling back to Xbox. Handhelds the design names
 * (Retroid Pocket, AYN Odin) use the Xbox layout, so they resolve through the fallback.
 *
 * @param faceButtons the setting.
 * @param vendorId the connected gamepad's `InputDevice.vendorId`, or null when none is known.
 */
class FaceButtonLayoutResolver(
    faceButtons: Flow<FaceButtons>,
    vendorId: Flow<Int?>,
) {
    /** Reads the setting from [settings] (core:data). */
    constructor(settings: SettingsRepository, vendorId: Flow<Int?>) :
        this(settings.settings.map { it.faceButtons }, vendorId)

    /** The layout to use now; changes when the setting or the connected pad changes. */
    val layout: Flow<FaceButtonLayout> =
        combine(faceButtons, vendorId, ::resolve).distinctUntilChanged()

    companion object {
        /** Vendor ids whose pads use the Nintendo layout. */
        val NINTENDO_VENDOR_IDS: Set<Int> = setOf(0x057e)

        /** The pure rule: an explicit setting wins; Auto looks [vendorId] up in the device table. */
        fun resolve(
            setting: FaceButtons,
            vendorId: Int?,
        ): FaceButtonLayout =
            when (setting) {
                FaceButtons.Xbox -> {
                    FaceButtonLayout.Xbox
                }

                FaceButtons.Nintendo -> {
                    FaceButtonLayout.Nintendo
                }

                FaceButtons.Auto -> {
                    if (vendorId in NINTENDO_VENDOR_IDS) FaceButtonLayout.Nintendo else FaceButtonLayout.Xbox
                }
            }
    }
}
