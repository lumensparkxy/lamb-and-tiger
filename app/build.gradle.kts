plugins { alias(libs.plugins.android.application); alias(libs.plugins.compose.compiler) }
val liveAds = providers.gradleProperty("lamb.liveAds").orNull == "true"
val liveAppId = providers.gradleProperty("lamb.admobAppId").orNull.orEmpty()
val liveBannerId = providers.gradleProperty("lamb.admobBannerId").orNull.orEmpty()
val liveInterstitialId = providers.gradleProperty("lamb.admobInterstitialId").orNull.orEmpty()
val testAppId = "ca-app-pub-3940256099942544~3347511713"
if (liveAds) {
    require(liveAppId.matches(Regex("ca-app-pub-[0-9]{16}~[0-9]{10}")) && !liveAppId.contains("3940256099942544")) { "Owned AdMob app ID required" }
    for (id in listOf(liveBannerId, liveInterstitialId)) {
        require(id.matches(Regex("ca-app-pub-[0-9]{16}/[0-9]{10}")) && !id.contains("3940256099942544")) { "Owned banner and interstitial ad-unit IDs required" }
    }
}
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
        manifestPlaceholders["admobAppId"] = testAppId
        buildConfigField("boolean", "ADS_ENABLED", "false")
        buildConfigField("String", "BANNER_AD_UNIT_ID", "\"\"")
        buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"\"")
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug" }
        release {
            manifestPlaceholders["admobAppId"] = if (liveAds) liveAppId else testAppId
            buildConfigField("boolean", "ADS_ENABLED", liveAds.toString())
            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"${if (liveAds) liveBannerId else ""}\"")
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"${if (liveAds) liveInterstitialId else ""}\"")
        }
        create("adsQa") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".adsqa"
            matchingFallbacks += "debug"
            // Optional owned-app consent check; QA NEVER uses live ad-unit IDs.
            manifestPlaceholders["admobAppId"] = if (providers.gradleProperty("lamb.qaOwnedApp").orNull == "true") liveAppId else testAppId
            buildConfigField("boolean", "ADS_ENABLED", "true")
            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    testOptions { animationsDisabled = true }
    testBuildType = providers.gradleProperty("lamb.testBuildType").orElse("debug").get()
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
    implementation(libs.google.ads)
    implementation(libs.google.ump)
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
