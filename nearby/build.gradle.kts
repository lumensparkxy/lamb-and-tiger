plugins { alias(libs.plugins.android.library) }
android {
    namespace = "com.maswadkar.lambandtiger.nearby"
    compileSdk = 37
    defaultConfig { minSdk = 29 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    implementation("com.google.android.gms:play-services-nearby:19.5.1")
    implementation("com.google.android.gms:play-services-base:18.5.0")
    testImplementation(libs.junit)
}
