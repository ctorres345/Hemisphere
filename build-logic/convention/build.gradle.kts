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
    compileOnly(libs.hilt.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
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
        register("androidApplication") {
            id = "hemisphere.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("hilt") {
            id = "hemisphere.hilt"
            implementationClass = "HiltConventionPlugin"
        }
        register("designTokens") {
            id = "hemisphere.design.tokens"
            implementationClass = "DesignTokensConventionPlugin"
        }
        register("screenshot") {
            id = "hemisphere.screenshot"
            implementationClass = "ScreenshotConventionPlugin"
        }
        register("moduleGraph") {
            id = "hemisphere.module.graph"
            implementationClass = "ModuleGraphConventionPlugin"
        }
    }
}

// ThemeTokensCodegenTest reads the repo's tokens/ files, so a token edit must rerun it.
tasks.test {
    inputs
        .dir(rootDir.resolve("../tokens"))
        .withPropertyName("designTokens")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
