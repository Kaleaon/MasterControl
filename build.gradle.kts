// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    val kotlinVersion: String by project
    val androidGradlePluginVersion: String by project
    
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:$androidGradlePluginVersion")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
