package io.github.ctorres345.hemisphere

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** The Hilt root (architecture §9): the only `@HiltAndroidApp`. */
@HiltAndroidApp
class HemisphereApplication : Application()
