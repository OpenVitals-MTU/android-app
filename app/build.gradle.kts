import org.gradle.api.tasks.testing.Test

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


android {
    namespace = "tech.mmarca.openvitals"
    compileSdk = 37

    defaultConfig {
        applicationId = "tech.mmarca.openvitals"
        minSdk = 26
        targetSdk = 36
        versionCode = 14001
        versionName = "1.4.1"
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
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Activity + Navigation
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Lifecycle + ViewModel
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Background work
    implementation(libs.androidx.work.runtime.ktx)

    // Health Connect
    implementation(libs.health.connect.client)

    // Dependency injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Testing
    testImplementation(libs.junit4)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}

tasks.withType<Test>().configureEach {
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}
