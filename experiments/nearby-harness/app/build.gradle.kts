plugins {
    id("com.android.application")
}

android {
    namespace = "com.lumensparkxy.lambandtiger.nearbyharness"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.lumensparkxy.lambandtiger.nearbyharness"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1-diagnostic"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-nearby:19.5.1")
    implementation("com.google.android.gms:play-services-base:18.5.0")
    testImplementation("junit:junit:4.13.2")
}
