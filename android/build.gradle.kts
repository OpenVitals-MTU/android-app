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

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
