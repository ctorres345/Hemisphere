package io.github.ctorres345.hemisphere.core.domain.settings

import io.github.ctorres345.hemisphere.core.model.LayoutPin
import io.github.ctorres345.hemisphere.core.model.Orientation
import io.github.ctorres345.hemisphere.core.testing.FakeSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SetLayoutPinUseCaseTest {
    private val repo = FakeSettingsRepository()

    @Test
    fun `passes the pin to the repository`() =
        runTest {
            SetLayoutPinUseCase(repo)(Orientation.Landscape, LayoutPin.Dual)
            val s = repo.settings.first()
            assertEquals(LayoutPin.Dual, s.layoutPinLandscape)
            assertEquals(LayoutPin.Auto, s.layoutPinPortrait)
        }
}
