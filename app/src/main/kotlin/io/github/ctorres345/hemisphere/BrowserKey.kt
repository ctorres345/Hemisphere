package io.github.ctorres345.hemisphere

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Top-level screens on the app back stack (architecture §7). Trash joins later. */
@Serializable
data object BrowserKey : NavKey

/** Asks for storage access (ADR 0002). Shown until access is granted, and again if it is revoked. */
@Serializable
data object AccessKey : NavKey

/** The Settings screen, opened from Browser's top bar until Places (HEM-56) holds the way in. */
@Serializable
data object SettingsKey : NavKey
