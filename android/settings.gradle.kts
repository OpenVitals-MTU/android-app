pluginManagement {
    val flutterSdkPath =
        run {
            val properties = java.util.Properties()
            file("local.properties").inputStream().use { properties.load(it) }
            val flutterSdkPath = properties.getProperty("flutter.sdk")
            require(flutterSdkPath != null) { "flutter.sdk not set in local.properties" }
            flutterSdkPath
        }

    includeBuild("$flutterSdkPath/packages/flutter_tools/gradle")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.flutter.flutter-plugin-loader") version "1.0.0"
    // AGP 9.2.1 (latest stable 9.x): connect-client 1.2.0-alpha04 mandates
    // Android Gradle plugin 9.1.0+, so 9.2.x satisfies the floor.
    id("com.android.application") version "9.2.1" apply false
    id("org.jetbrains.kotlin.android") version "2.3.20" apply false
    // Glance (Jetpack Compose for app widgets) needs the Compose compiler plugin.
    // Version-matched to the Kotlin above — the Compose compiler ships in lockstep
    // with Kotlin from 2.0 on.
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
}

include(":app")
