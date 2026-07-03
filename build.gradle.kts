import org.gradle.buildconfiguration.tasks.UpdateDaemonJvm
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
}

val openVitalsGroup = providers.gradleProperty("openVitalsGroup")
    .orElse("tech.mmarca.openvitals")
val openVitalsArtifactVersion = providers.gradleProperty("openVitalsArtifactVersion")
    .orElse(providers.environmentVariable("OPENVITALS_ARTIFACT_VERSION"))
    .orElse("1.7.4-SNAPSHOT")

allprojects {
    group = openVitalsGroup.get()
    version = openVitalsArtifactVersion.get()

    dependencyLocking {
        lockAllConfigurations()
    }
}

tasks.named<UpdateDaemonJvm>("updateDaemonJvm") {
    // AGP 9.1 defaults to JDK 17; keep daemon generation aligned with that,
    // and avoid pinning a specific vendor for CI portability.
    languageVersion = JavaLanguageVersion.of(17)
}

val hasAndroidSerial = providers.environmentVariable("ANDROID_SERIAL").isPresent
val isCiEnvironment = providers.environmentVariable("CI").isPresent ||
    providers.environmentVariable("WOODPECKER").isPresent

tasks.register("verifyAndroidTest") {
    group = "verification"
    description = "Runs connectedDebugAndroidTest for local connected-device checks."
    enabled = hasAndroidSerial && !isCiEnvironment
    if (enabled) {
        dependsOn(":app:connectedDebugAndroidTest")
    }
}

tasks.register("verifyLocalApp") {
    group = "verification"
    description = "Runs CI verification without connected-device instrumentation tests."
    dependsOn(
        ":app:testDebugUnitTest",
        "verifyLocalAppPreflight",
    )
}

tasks.register("verifyLocalAppPreflight") {
    group = "verification"
    description = "Runs Android app build and lint checks without unit or connected-device tests."
    dependsOn(
        ":app:lintDebug",
        ":app:assembleDebug",
        ":app:compileDebugAndroidTestKotlin",
    )
}

tasks.register("verifyLocalReleaseChecks") {
    group = "verification"
    description = "Runs release preflight checks without connected-device instrumentation tests."
    dependsOn(
        ":app:testDebugUnitTest",
        "verifyLocalReleasePreflight",
    )
}

tasks.register("verifyLocalReleasePreflight") {
    group = "verification"
    description = "Runs release app build and lint checks without unit or connected-device tests."
    dependsOn(
        ":app:assembleDebug",
        ":app:lintDebug",
    )
}

project(":app").tasks.configureEach {
    if (name == "lintDebug") {
        mustRunAfter("assembleDebug")
    }
}
