plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false

    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

// OneDrive sync locks files under build/, so Gradle cannot delete stale intermediates
val localAppData: String? = System.getenv("LOCALAPPDATA")
if (localAppData != null && rootDir.absolutePath.contains("OneDrive")) {
    allprojects {
        layout.buildDirectory.set(file("$localAppData/gradle-builds/${rootProject.name}/${project.name}"))
    }
}
