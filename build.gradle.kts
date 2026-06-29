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
    .orElse("1.7.0-SNAPSHOT")

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

tasks.register("verifyAndroidTest") {
    group = "verification"
    description = "Runs connectedDebugAndroidTest when ANDROID_SERIAL is set."
    onlyIf {
        providers.environmentVariable("ANDROID_SERIAL").isPresent
    }
    dependsOn(":app:connectedDebugAndroidTest")
}

tasks.register("verifyLocalApp") {
    group = "verification"
    description = "Runs CI verification for the local internet-free OpenVitals app."
    dependsOn(
        ":app:testDebugUnitTest",
        ":app:lintDebug",
        ":app:assembleDebug",
        ":app:compileDebugAndroidTestKotlin",
    )
}

tasks.register("verifyLocalReleaseChecks") {
    group = "verification"
    description = "Runs release preflight checks for the local internet-free OpenVitals app."
    dependsOn(
        ":app:assembleDebug",
        ":app:testDebugUnitTest",
        ":app:lintDebug",
    )
}

project(":app").tasks.configureEach {
    if (name == "testDebugUnitTest" || name == "lintDebug") {
        mustRunAfter("assembleDebug")
    }
}
