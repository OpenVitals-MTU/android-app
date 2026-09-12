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
    .orElse("2.7.3-SNAPSHOT")

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
    description = "Runs connectedCiAndroidTest for local connected-device checks."
    enabled = hasAndroidSerial && !isCiEnvironment
    if (enabled) {
        dependsOn(":app:connectedCiAndroidTest")
    }
}

tasks.register("verifyCiUnitTest") {
    group = "verification"
    description = "Runs the phone app unit tests against the CI build type."
    dependsOn(":app:testCiUnitTest")
}

tasks.register<Exec>("verifyTranslations") {
    group = "verification"
    description = "Validates Android translation resources for completeness and placeholder safety."
    commandLine("python3", "scripts/verify-translations.py")
}

tasks.register("verifyCiPreflight") {
    group = "verification"
    description = "Runs phone app build, lint, and android-test compile checks for CI."
    dependsOn(
        "verifyTranslations",
        ":app:lintCi",
        ":app:assembleCi",
        ":app:compileCiAndroidTestKotlin",
    )
}

// Phone app only. The wear module has its own gate (verifyWearCi) and its own
// pipeline (.woodpecker/wear-test.yml), so a watch failure never blocks a phone PR.
tasks.register("verifyCi") {
    group = "verification"
    description = "Runs phone app CI verification without connected-device instrumentation tests."
    dependsOn(
        "verifyCiUnitTest",
        "verifyCiPreflight",
    )
}

// Wear OS module only. Same shape as verifyCi: unit tests, lint, build,
// android-test compile. No translation gate yet; the module has no locales.
tasks.register("verifyWearCi") {
    group = "verification"
    description = "Runs Wear OS module unit tests, lint, build, and android-test compile checks for CI."
    dependsOn(
        ":wear:testDebugUnitTest",
        ":wear:lintDebug",
        ":wear:assembleDebug",
        ":wear:compileDebugAndroidTestKotlin",
    )
}

project(":app").tasks.configureEach {
    if (name == "lintCi") {
        mustRunAfter("assembleCi")
    }
}
