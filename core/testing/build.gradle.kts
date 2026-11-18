plugins {
    alias(libs.plugins.hemisphere.android.library)
}

dependencies {
    api(project(":core:fs"))
    api(project(":core:fs-testing"))
    api(project(":core:jobs"))
    api(project(":core:data"))
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
