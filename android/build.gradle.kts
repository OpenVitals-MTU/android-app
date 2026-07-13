allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

val newBuildDir: Directory =
    rootProject.layout.buildDirectory
        .dir("../../build")
        .get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
}
subprojects {
    project.evaluationDependsOn(":app")
}

// Lift plugin modules that compile against an older SDK than the app up to the app's.
//
// Plugins are supposed to track `flutter.compileSdkVersion`, but some hardcode a number
// and go stale: file_picker 9.x pins `compileSdk 34`, while its own transitive dependency
// (flutter_plugin_android_lifecycle) now demands 36+. That combination fails the build in
// :file_picker:checkDebugAarMetadata, and there is nothing to fix inside our own code.
//
// Compiling against a NEWER SDK is always safe -- it only widens the APIs a module may
// call, and is the action AGP's own error message recommends. It changes neither minSdk
// (which devices install) nor targetSdk (which runtime behaviours we opt into).
//
// Read the target off :app rather than repeating a literal, so bumping the app's
// compileSdk carries the plugins along instead of quietly desyncing them. `evaluationDependsOn`
// above guarantees :app is configured before this runs.
subprojects {
    // :app is both the source of the target and -- thanks to the evaluationDependsOn above
    // -- already evaluated by now, so adding an afterEvaluate hook to it would throw.
    if (project.name == "app") return@subprojects
    afterEvaluate {
        val appCompileSdk =
            project(":app").extensions
                .findByType(com.android.build.api.dsl.ApplicationExtension::class.java)
                ?.compileSdk ?: return@afterEvaluate
        val library =
            extensions.findByType(com.android.build.api.dsl.LibraryExtension::class.java)
                ?: return@afterEvaluate
        val current = library.compileSdk
        if (current == null || current < appCompileSdk) {
            logger.info("Raising ${project.name} compileSdk from $current to $appCompileSdk")
            library.compileSdk = appCompileSdk
        }
    }
}

// Strip the GNU build-id from :jni's libdartjni.so, so the APK is reproducible.
//
// F-Droid builds OpenVitals from source and byte-compares the result against the APK
// published on the Codeberg release; it only ships our signed APK if they match (see
// the fdroiddata recipe's `Binaries:` + `AllowedAPKSigningKeys`). Everything reproduced
// except libdartjni.so, which differed in exactly 20 bytes: its `.note.gnu.build-id`.
//
// The NDK links with `-Wl,--build-id=sha1`, and that hash is taken over the linked
// output BEFORE it is stripped -- so it fingerprints the DWARF debug info, which carries
// absolute paths (the NDK, the pub cache, and AGP's `.cxx/<config-hash>` directory, whose
// hash is itself computed from absolute paths and the CMake version). The compiled code
// is identical on every machine; only that fingerprint is not. Chasing it by making two
// build environments agree on every path and tool version is a race that has to be re-won
// on every release, so drop the fingerprint instead: with no build-id there is nothing
// left to differ.
//
// libdartjni.so is not ours and cannot be avoided -- the `jni` package arrives
// transitively through `path_provider_android` and is compiled from source on every
// build -- so the flag is injected from here. The NDK toolchain puts ANDROID_LINKER_FLAGS
// (which is where its own `--build-id=sha1` lives) BEFORE CMAKE_SHARED_LINKER_FLAGS, and
// lld honours the last `--build-id` on the command line, so ours wins.
subprojects {
    if (project.name != "jni") return@subprojects
    afterEvaluate {
        val library =
            extensions.findByType(com.android.build.api.dsl.LibraryExtension::class.java)
                ?: return@afterEvaluate
        library.defaultConfig.externalNativeBuild.cmake.arguments.add(
            "-DCMAKE_SHARED_LINKER_FLAGS=-Wl,--build-id=none",
        )
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
