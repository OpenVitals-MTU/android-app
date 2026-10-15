// App-local code with no dependency on the rest of the phone app: the pure
// models, insight calculations, preference enums and medical-document parsing
// under `domain`, and the period, FIT, geo, stats and export primitives under
// `core`. Packages keep their names; :app depends on this module.
//
// An Android library rather than plain Kotlin: a handful of files use Health
// Connect record constants and the platform's locale data, and Android lint
// then still checks the module, on its own task beside the app's. The module
// exists for build time: lint and the unit tests run here in parallel with
// the app's, and a change elsewhere leaves this module's results cached.
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "tech.mmarca.openvitals.domain"
    compileSdk = 37
    buildToolsVersion = "37.0.0"
    // Same build types as the app, so the app's ci variant takes this module's
    // ci variant and the tests and lint here run on the one compile. A fallback
    // to debug would instead change how a multiplatform dependency resolves,
    // against the app's lockfile.
    testBuildType = "ci"

    defaultConfig {
        minSdk = 26
    }

    buildTypes {
        create("ci") {
            initWith(getByName("debug"))
        }
        create("nightly") {
            initWith(getByName("release"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // The FHIR, C-CDA and SMART Health Card fixtures, used by this module's
    // tests and by the app's import tests.
    testFixtures {
        enable = true
    }

    lint {
        ignoreTestSources = true
    }
}

dependencies {
    // Public types of this module reference both.
    api(project(":wearlink"))
    api(libs.health.connect.client)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.zxing.core)
    implementation(libs.androidx.core.ktx)

    // The fixtures build FHIR JSON and assert on what they parse.
    testFixturesImplementation(libs.kotlinx.serialization.json)
    testFixturesImplementation(libs.truth)
    // The SMART Health Card fixture encodes a QR code.
    testFixturesImplementation(libs.zxing.core)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.org.json)
}

tasks.withType<Test>().configureEach {
    // Same sizing as the app's tests: half the cores, every core on a
    // tests-only CI job.
    maxParallelForks = providers.gradleProperty("openvitals.testForks").orNull?.toInt()
        ?: (Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, 4)
    maxHeapSize = "1g"
}
