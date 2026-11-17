plugins {
    `kotlin-dsl`
}

group = "io.github.ctorres345.hemisphere.buildlogic"

kotlin {
    jvmToolchain(libs.versions.jdk.get().toInt())
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)
    testImplementation(libs.junit)
}

gradlePlugin {
    plugins {
        register("jvmLibrary") {
            id = "hemisphere.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "hemisphere.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "hemisphere.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "hemisphere.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidApplication") {
            id = "hemisphere.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("moduleGraph") {
            id = "hemisphere.module.graph"
            implementationClass = "ModuleGraphConventionPlugin"
        }
    }
}
