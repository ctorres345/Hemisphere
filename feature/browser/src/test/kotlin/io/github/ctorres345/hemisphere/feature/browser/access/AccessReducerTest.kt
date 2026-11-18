package io.github.ctorres345.hemisphere.feature.browser.access

import io.github.ctorres345.hemisphere.core.model.StorageAccessState
import org.junit.Assert.assertEquals
import org.junit.Test

class AccessReducerTest {
    @Test
    fun `access changes replace the access`() {
        val next = reduce(AccessUiState(), AccessResult.AccessChanged(StorageAccessState.NeedsSettings))

        assertEquals(StorageAccessState.NeedsSettings, next.access)
    }

    @Test
    fun `granting asks for the grant flow when the system can still ask`() {
        val next = reduce(AccessUiState(access = StorageAccessState.Denied), AccessResult.GrantRequested)

        assertEquals(AccessLaunch.Grant, next.launch)
    }

    @Test
    fun `granting opens the app settings once the system will not ask again`() {
        val next = reduce(AccessUiState(access = StorageAccessState.NeedsSettings), AccessResult.GrantRequested)

        assertEquals(AccessLaunch.AppSettings, next.launch)
    }

    @Test
    fun `a handled launch is cleared`() {
        val next = reduce(AccessUiState(launch = AccessLaunch.Grant), AccessResult.LaunchCleared)

        assertEquals(null, next.launch)
    }

    @Test
    fun `becoming granted drops a pending launch`() {
        val next =
            reduce(AccessUiState(launch = AccessLaunch.Grant), AccessResult.AccessChanged(StorageAccessState.Granted))

        assertEquals(AccessUiState(access = StorageAccessState.Granted, launch = null), next)
    }
}
