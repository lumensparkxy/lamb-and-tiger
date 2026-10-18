buildscript {
    repositories { google(); mavenCentral() }
    dependencies { classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20") }
}
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
}
allprojects {
    // Keep generated classes outside cloud-synced Documents folders. The path is
    // local, deterministic per checkout, and contains no shared machine paths.
    layout.buildDirectory.set(File(gradle.gradleUserHomeDir, "lamb-and-tiger-builds/${rootDir.absolutePath.hashCode().toUInt().toString(16)}/${project.name}"))
    dependencyLocking { lockAllConfigurations() }
}
tasks.register("printBuildLocations") {
    doLast { allprojects.forEach { println("${it.path}: ${it.layout.buildDirectory.get().asFile}") } }
}
