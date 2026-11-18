package io.github.ctorres345.hemisphere

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Top-level screens on the app back stack (architecture §7). Settings and Trash join later. */
@Serializable
data object BrowserKey : NavKey

/** Asks for storage access (ADR 0002). Shown until access is granted, and again if it is revoked. */
@Serializable
data object AccessKey : NavKey
