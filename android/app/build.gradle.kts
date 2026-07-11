plugins {
    id("com.android.application")
    // Home-screen widgets are Glance (Compose) composables, ported from the Kotlin
    // app, so this module needs the Compose compiler. Kotlin itself is applied by
    // Flutter's Built-in Kotlin (see the comment on the `kotlin` block below).
    id("org.jetbrains.kotlin.plugin.compose")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

// Release signing, ported from the Kotlin app. All four values come from the
// process environment (Woodpecker secrets in CI); nothing is read from
// gradle.properties or local.properties, and no keystore is ever committed.
val releaseStoreFilePath: String? = System.getenv("OPENVITALS_RELEASE_STORE_FILE")
val releaseStorePassword: String? = System.getenv("OPENVITALS_RELEASE_STORE_PASSWORD")
val releaseKeyAlias: String? = System.getenv("OPENVITALS_RELEASE_KEY_ALIAS")
val releaseKeyPassword: String? = System.getenv("OPENVITALS_RELEASE_KEY_PASSWORD")

// A PKCS12 store has a single password: the key password IS the store password.
// The real OpenVitals keystore is a .p12, so dropping this yields the misleading
// "keystore password was incorrect" failure in CI.
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

android {
    namespace = "tech.mmarca.openvitals"
    // compileSdk 37 matches the reference Kotlin app. connect-client 1.2.0-alpha04
    // (pulled by the health_connect_native plugin, on AGP 9.1.1) resolves its newer
    // record/permission mappings against API 37, so the app module compiles against
    // the same SDK. The android-37.0 platform must be installed.
    compileSdk = 37
    ndkVersion = flutter.ndkVersion

    compileOptions {
        // flutter_local_notifications' AAR metadata mandates Java 8+ core-library
        // desugaring (it relies on desugared java.time APIs), so enable it here.
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "tech.mmarca.openvitals"
        // minSdk 26: required by Health Connect (androidx.health) and matches the
        // Kotlin source. This is above the Flutter default (24).
        minSdk = 26
        // targetSdk 36 matches the Kotlin source (and the current Flutter default).
        targetSdk = 36
        // versionName / versionCode come from pubspec (`version: <name>+<code>`),
        // which is what `scripts/release.sh` bumps and what CI overrides per build
        // via `flutter build --build-name/--build-number`. The version code is a
        // monotonic counter whose source of truth is the Codeberg release notes
        // (see scripts/version-code.sh), not this file.
        versionCode = flutter.versionCode
        versionName = flutter.versionName

        // The launcher (and the widget picker) label. Overridden per build type so a
        // debug install is never mistaken for the real app sitting next to it.
        manifestPlaceholders["appLabel"] = "OpenVitals"
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
            // Mirrors the Kotlin source's debug build type. Without the suffix a
            // debug build shares `tech.mmarca.openvitals` with the installed
            // release app: the install fails on the signature mismatch, and
            // uninstalling to force it would destroy that app's health data.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // The suffix already separates the packages, but both showed up as plain
            // "OpenVitals" in the launcher and, worse, as two identical entries in the
            // widget picker.
            manifestPlaceholders["appLabel"] = "OpenVitals Debug"
        }
        release {
            // Signed with the SAME key as the published Kotlin app: this build
            // replaces it in place on the existing Play listing / Codeberg APK
            // channel, and a different certificate would make the update fail to
            // install (INSTALL_FAILED_UPDATE_INCOMPATIBLE) for every current user.
            //
            // Deliberately NOT falling back to the debug key when the signing env
            // is absent: an unsigned release is a loud, obvious failure, whereas a
            // debug-signed one looks fine right up until it reaches a real device.
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                null
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    // Required by flutter_local_notifications (see coreLibraryDesugaring above).
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
    // Home-screen widgets. The `home_widget` plugin depends on Glance but only as
    // `implementation`, so it is not on our compile classpath — declare it here.
    // Pinned to the same 1.1.1 the plugin resolves, to avoid two Glance versions.
    implementation("androidx.glance:glance-appwidget:1.1.1")
}

flutter {
    source = "../.."
}
