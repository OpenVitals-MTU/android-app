import org.gradle.buildconfiguration.tasks.UpdateDaemonJvm
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
}

val openVitalsGroup = providers.gradleProperty("openVitalsGroup")
    .orElse("tech.mmarca.openvitals")
val openVitalsArtifactVersion = providers.gradleProperty("openVitalsArtifactVersion")
    .orElse(providers.environmentVariable("OPENVITALS_ARTIFACT_VERSION"))
    .orElse("2.15.1-SNAPSHOT")

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
val isCiEnvironment = providers.environmentVariable("CI").isPresent

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
    description = "Runs the phone app unit tests against the CI build type, and the domain and link modules'."
    dependsOn(":wearlink:test", ":domain:testCiUnitTest", ":app:testCiUnitTest")
}

tasks.register<Exec>("verifyTranslations") {
    group = "verification"
    description = "Validates Android translation resources for completeness and placeholder safety."
    commandLine("python3", "scripts/verify-translations.py")
}

tasks.register("verifyCiPreflight") {
    group = "verification"
    description = "Runs the translation, duplicate-class and android-test compile checks for CI."
    dependsOn(
        "verifyTranslations",
        // Not assembleCi: nothing used that APK, and dexing it was a minute and a half.
        // The release pipeline builds and packages the real one after this gate.
        ":app:checkCiDuplicateClasses",
        ":app:compileCiAndroidTestKotlin",
    )
}

// Lint is half of the gate's task time. CI runs it on pushes to main and in
// the release build, not on pull requests (.github/workflows/test.yml), so it
// is not part of verifyCiPreflight.
tasks.register("verifyCiLint") {
    group = "verification"
    description = "Runs phone app and domain module lint for CI."
    dependsOn(":app:lintCi", ":domain:lintCi")
}

// Phone app only. The wear module has its own gate (verifyWearCi) and its own
// pipeline (.github/workflows/wear-test.yml), so a watch failure never blocks a phone PR.
// The whole gate in one invocation, for a local run and the release build.
tasks.register("verifyCi") {
    group = "verification"
    description = "Runs phone app CI verification without connected-device instrumentation tests."
    dependsOn(
        "verifyCiUnitTest",
        "verifyCiPreflight",
        "verifyCiLint",
    )
}

// Wear OS module only. Same shape as verifyCi: unit tests, lint, build,
// android-test compile. No translation gate yet; the module has no locales.
tasks.register("verifyWearCi") {
    group = "verification"
    description = "Runs Wear OS module unit tests, lint, build, and android-test compile checks for CI."
    dependsOn(
        ":wearlink:test",
        ":wear:testDebugUnitTest",
        ":wear:lintDebug",
        ":wear:assembleDebug",
        ":wear:compileDebugAndroidTestKotlin",
    )
}

