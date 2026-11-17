package io.github.ctorres345.hemisphere

import android.app.Application

/** Holds the [AppContainer] for the whole process. Becomes the `@HiltAndroidApp` class later. */
class HemisphereApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
