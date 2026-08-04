import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.PathSensitivity

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kover)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

val releaseStoreFilePath = System.getenv("OPENVITALS_RELEASE_STORE_FILE")
val releaseStorePassword = System.getenv("OPENVITALS_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = System.getenv("OPENVITALS_RELEASE_KEY_ALIAS")
val releaseKeyPassword = System.getenv("OPENVITALS_RELEASE_KEY_PASSWORD")
val isPkcs12ReleaseStore = releaseStoreFilePath
    ?.lowercase()
    ?.let { it.endsWith(".p12") || it.endsWith(".pfx") || it.endsWith(".pkcs12") }
    ?: false
val effectiveReleaseKeyPassword = if (isPkcs12ReleaseStore) {
    releaseStorePassword
} else {
    releaseKeyPassword
}

val hasReleaseSigning = listOf(
    releaseStoreFilePath,
    releaseStorePassword,
    releaseKeyAlias,
    effectiveReleaseKeyPassword,
).all { !it.isNullOrBlank() }

val signDebugWithReleaseKey = System.getenv("OPENVITALS_SIGN_DEBUG_WITH_RELEASE_KEY") == "true"
val minifyDebugForCi = System.getenv("OPENVITALS_MINIFY_DEBUG_FOR_CI") == "true"
val apkAbiFilters = System.getenv("OPENVITALS_APK_ABI_FILTERS")
    ?.split(',')
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() }
    ?.toSet()
    ?: emptySet()
val versionCodeOverride = providers.environmentVariable("OPENVITALS_VERSION_CODE")
    .map { it.toInt() }
val versionNameOverride = providers.environmentVariable("OPENVITALS_VERSION_NAME")
val nightlyVersionNameSuffix = versionNameOverride
    .map { "" }
    .orElse("-nightly")
val localAppleHealthExportPath = providers.gradleProperty("appleHealthExport")
    .orElse(providers.systemProperty("appleHealthExport"))
    .orElse(providers.environmentVariable("APPLE_HEALTH_EXPORT"))
// versionCode is a monotonic release counter, independent of versionName.
//
// The Flutter era multiplied the then-base by 10 and added a per-ABI digit
// (armeabi-v7a 1, arm64-v8a 2), while its Play AABs took the bare x10. Its
// Codeberg release markers recorded the 9-digit BASE, and the nightly line kept
// running after 2.4.1 (base 107030440): the append-only refs/version-code/*
// refs on origin show bases up to 107030443. So Google Play open testing
// serves AAB code 1070304430, and the last Flutter nightly's Codeberg/F-Droid
// APKs shipped 1070304431/1070304432. None of that is visible to the marker
// survey in scripts/version-code.sh — Play-only codes have no Codeberg marker,
// and the 9-digit base markers sit below any 10-digit floor — so THIS floor is
// the defense and must clear every code any channel has ever served. Anything
// at or below those codes is a DOWNGRADE: Play rejects the rollout ("does not
// allow any existing users to upgrade"), Android refuses the update, and
// F-Droid rejects the build. This value continues that number line — old base
// x10, last digit free for a per-ABI split if one is ever needed again. The
// first nightly on this floor ships versionCode 1070304441.
val baseVersionCode = 1070304442
val baseVersionName = "2.5.0"
val translationCoverageResDir = layout.buildDirectory.dir("generated/res/translationCoverage").get().asFile
val generateTranslationCoverage by tasks.registering(Exec::class) {
    inputs.files(
        fileTree(rootProject.file("app/src/main/res")) {
            include("values*/strings.xml")
        },
    )
    inputs.file(rootProject.file("scripts/generate-translation-coverage.py"))
    outputs.dir(translationCoverageResDir)
    workingDir(rootProject.projectDir)
    commandLine(
        "python3",
        rootProject.file("scripts/generate-translation-coverage.py"),
        translationCoverageResDir,
    )
}

android {
    namespace = "tech.mmarca.openvitals"
    compileSdk = 37
    buildToolsVersion = "37.0.0"
    testBuildType = "ci"

    defaultConfig {
        applicationId = "tech.mmarca.openvitals"
        minSdk = 26
        targetSdk = 36
        versionCode = versionCodeOverride.orElse(baseVersionCode).get()
        versionName = versionNameOverride.orElse(baseVersionName).get()
        buildConfigField("boolean", "OPENVITALS_DIAGNOSTICS", "false")
        if (apkAbiFilters.isNotEmpty()) {
            ndk {
                abiFilters.addAll(apkAbiFilters)
            }
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(checkNotNull(releaseStoreFilePath))
                storePassword = checkNotNull(releaseStorePassword)
                keyAlias = checkNotNull(releaseKeyAlias)
                keyPassword = checkNotNull(effectiveReleaseKeyPassword)
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("boolean", "OPENVITALS_DIAGNOSTICS", "true")
            if (signDebugWithReleaseKey && hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            if (minifyDebugForCi) {
                isDebuggable = false
                isMinifyEnabled = true
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
        }

        create("ci") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".ci"
            versionNameSuffix = "-ci"
        }

        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        create("nightly") {
            initWith(getByName("release"))
            versionNameSuffix = nightlyVersionNameSuffix.get()
            buildConfigField("boolean", "OPENVITALS_DIAGNOSTICS", "true")
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    sourceSets {
        getByName("main") {
            res.srcDir(translationCoverageResDir)
        }
    }

    lint {
        disable += setOf(
            "LogNotTimber",
            // Partial Weblate languages are allowed once scripts/verify-translations.py
            // confirms at least 70% coverage and placeholder safety.
            "MissingTranslation",
        )
    }
}

tasks.configureEach {
    if (
        name != "generateTranslationCoverage" &&
        (
            name.contains("Resources") ||
                name.contains("SourceSetPaths") ||
                name.startsWith("extractDeepLinks")
        )
    ) {
        dependsOn(generateTranslationCoverage)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // Compose
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.reorderable)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    // Instrumentation runs against the `ci` variant, not `debug`. ui-test-manifest
    // is what declares the ComponentActivity that createComposeRule launches into,
    // so without it on this variant every Compose test fails identically at
    // startup with "Unable to resolve activity for MAIN/LAUNCHER" — before any
    // assertion runs, and regardless of what the test does.
    "ciImplementation"(libs.androidx.compose.ui.test.manifest)

    // Activity + Navigation
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Lifecycle + ViewModel
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Background work
    implementation(libs.androidx.work.runtime.ktx)

    // Local metric storage
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Home screen widgets
    implementation(libs.androidx.glance.appwidget)

    // Health Connect
    implementation(libs.health.connect.client)

    // Offline maps
    implementation(libs.maplibre.android.sdk)
    implementation(libs.mapsforge.map.android)
    implementation(libs.mapsforge.themes)

    // Dependency injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Testing
    testImplementation(libs.junit4)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.health.connect.testing)
    testImplementation(libs.truth)
    testImplementation(libs.gson)
    // Real org.json for local tests (the android.jar copy is a throwing stub);
    // used by the Flutter-migration BLE registry tolerance test.
    testImplementation(libs.org.json)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

tasks.withType<Test>().configureEach {
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    localAppleHealthExportPath.orNull?.let { path ->
        systemProperty("appleHealthExport", path)
    }
    // StringFormatSpecifierTest reads the source strings off disk rather than
    // through R, so Gradle cannot infer the dependency and would call the task
    // up to date after an edit that broke a format specifier. Declaring it means
    // a strings change reruns the suite instead of silently skipping its guard.
    inputs.files(fileTree("src/main/res") { include("values*/strings.xml") })
        .withPropertyName("localisedStrings")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
