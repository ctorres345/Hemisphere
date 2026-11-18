plugins {
    alias(libs.plugins.hemisphere.android.library)
}

dependencies {
    api(project(":core:model"))
    api(project(":core:fs"))
    // The contract suite lives in main so other modules can subclass it.
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
