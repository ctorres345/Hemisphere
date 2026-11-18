package io.github.ctorres345.hemisphere.core.domain.settings

import io.github.ctorres345.hemisphere.core.model.FaceButtons
import io.github.ctorres345.hemisphere.core.testing.FakeSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SetFaceButtonsUseCaseTest {
    private val repo = FakeSettingsRepository()

    @Test
    fun `passes the Face buttons to the repository`() =
        runTest {
            SetFaceButtonsUseCase(repo)(FaceButtons.Nintendo)
            assertEquals(FaceButtons.Nintendo, repo.settings.first().faceButtons)
        }
}
