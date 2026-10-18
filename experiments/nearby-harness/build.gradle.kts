buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Nearby 19.5.1 contains Kotlin 2.4 metadata. Keep AGP built-in Kotlin,
        // but override its default compiler as documented by Android.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
    }
}

plugins {
    id("com.android.application") version "9.4.1" apply false
}
