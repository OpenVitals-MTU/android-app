import org.gradle.buildconfiguration.tasks.UpdateDaemonJvm
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
}

val openVitalsGroup = providers.gradleProperty("openVitalsGroup")
    .orElse("tech.mmarca.openvitals")
val openVitalsArtifactVersion = providers.gradleProperty("openVitalsArtifactVersion")
    .orElse(providers.environmentVariable("OPENVITALS_ARTIFACT_VERSION"))
    .orElse("1.2.0-SNAPSHOT")

allprojects {
    group = openVitalsGroup.get()
    version = openVitalsArtifactVersion.get()
}

tasks.named<UpdateDaemonJvm>("updateDaemonJvm") {
    // AGP 9.1 defaults to JDK 17; keep daemon generation aligned with that,
    // and avoid pinning a specific vendor for CI portability.
    languageVersion = JavaLanguageVersion.of(17)
}

tasks.register("verifyLocalApp") {
    group = "verification"
    description = "Runs CI verification for the local internet-free OpenVitals app and shared artifacts."
    dependsOn(
        ":openvitals-core-period:test",
        ":app:testDebugUnitTest",
        ":app:lintDebug",
        ":app:assembleDebug",
    )
}

tasks.register("verifyLocalReleaseChecks") {
    group = "verification"
    description = "Runs release preflight checks for the local internet-free OpenVitals app and shared artifacts."
    dependsOn(
        ":openvitals-core-period:test",
        ":app:testDebugUnitTest",
        ":app:lintDebug",
    )
}

tasks.register("publishOpenVitalsArtifactsToMavenLocal") {
    group = "publishing"
    description = "Publishes OpenVitals shared artifacts to the local Maven cache."
    dependsOn(":openvitals-core-period:publishToMavenLocal")
}

tasks.register("publishOpenVitalsArtifactsToLocalRepository") {
    group = "publishing"
    description = "Publishes OpenVitals shared artifacts to build/openvitals-maven-repository."
    dependsOn(":openvitals-core-period:publishAllPublicationsToOpenVitalsLocalRepository")
}
