import com.android.build.api.variant.HostTestBuilder

plugins {
    alias(libs.plugins.hemisphere.android.library)
}

android {
    buildFeatures.buildConfig = true
    // Sample files are for development only: a release install starts with an empty home (HEM-9).
    // The app's release build uses this module's release variant, so it gets the release value.
    // testDebug and testRelease list a new home with these values, so a wrong one fails `./gradlew check`.
    buildTypes {
        debug { buildConfigField("boolean", "SEED_SAMPLES", "true") }
        release { buildConfigField("boolean", "SEED_SAMPLES", "false") }
    }
}

// AGP 9 makes unit tests for the debug build only; the release ones guard SEED_SAMPLES.
androidComponents {
    beforeVariants(selector().withBuildType("release")) { variant ->
        variant.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = true
    }
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)
    implementation(project(":core:platform"))
}
