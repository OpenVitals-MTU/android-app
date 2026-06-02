plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

java {
    withJavadocJar()
    withSourcesJar()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit4)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("OpenVitals Core Period")
                description.set("Period range, date window, and navigation primitives shared by OpenVitals apps.")
                url.set("https://codeberg.org/OpenVitals/android-app")
                licenses {
                    license {
                        name.set("GNU Affero General Public License v3.0 or later")
                        url.set("https://www.gnu.org/licenses/agpl-3.0.en.html")
                    }
                }
                developers {
                    developer {
                        id.set("mmarca")
                        name.set("OpenVitals maintainers")
                    }
                }
                scm {
                    connection.set("scm:git:ssh://git@codeberg.org/OpenVitals/android-app.git")
                    developerConnection.set("scm:git:ssh://git@codeberg.org/OpenVitals/android-app.git")
                    url.set("https://codeberg.org/OpenVitals/android-app")
                }
            }
        }
    }

    repositories {
        maven {
            name = "OpenVitalsLocal"
            url = rootProject.layout.buildDirectory.dir("openvitals-maven-repository").get().asFile.toURI()
        }

        val releaseRepositoryUrl = providers.gradleProperty("openVitalsMavenRepositoryUrl")
            .orElse(providers.environmentVariable("OPENVITALS_MAVEN_REPOSITORY_URL"))
            .orNull
        if (!releaseRepositoryUrl.isNullOrBlank()) {
            maven {
                name = "OpenVitalsRelease"
                url = uri(releaseRepositoryUrl)
                credentials {
                    username = providers.gradleProperty("openVitalsMavenUsername")
                        .orElse(providers.environmentVariable("OPENVITALS_MAVEN_USERNAME"))
                        .orNull
                    password = providers.gradleProperty("openVitalsMavenPassword")
                        .orElse(providers.environmentVariable("OPENVITALS_MAVEN_PASSWORD"))
                        .orNull
                }
            }
        }
    }
}
