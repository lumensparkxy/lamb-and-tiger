plugins { alias(libs.plugins.kotlin.jvm) }
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
java { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
dependencies { testImplementation(libs.junit) }
tasks.test { maxHeapSize = "1g" }

tasks.register<JavaExec>("evaluateAi") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.maswadkar.lambandtiger.game.AiEvaluationKt")
    maxHeapSize = "2g"
    args(providers.gradleProperty("seeds").getOrElse("200"),providers.gradleProperty("workers").getOrElse("2"),providers.gradleProperty("pairs").getOrElse("0,1,2,3"))
}
