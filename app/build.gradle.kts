plugins { alias(libs.plugins.android.application); alias(libs.plugins.compose.compiler) }
android {
    namespace = "com.maswadkar.lambandtiger"
    compileSdk = 37
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "com.maswadkar.lambandtiger"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1-solo"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes { debug { applicationIdSuffix = ".debug" } }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    testOptions { animationsDisabled = true }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    implementation(project(":game"))
    implementation(project(":nearby"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.activity)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime)
    implementation(libs.coroutines)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.android.runner)
    androidTestImplementation(libs.android.junit)
    // Compose's transitive Espresso 3.5 uses InputManager reflection removed in API 37.
    androidTestImplementation(libs.espresso.core)
    debugImplementation(libs.compose.test.manifest)
}
