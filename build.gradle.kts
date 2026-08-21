buildscript {
    dependencies {
        // AGP 9 uses built-in Kotlin. Pin Kotlin so the Compose compiler plugin matches it.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
        classpath("org.jetbrains.kotlin.plugin.compose:org.jetbrains.kotlin.plugin.compose.gradle.plugin:2.3.21")
    }
}

plugins {
    id("com.android.application") version "9.3.0" apply false
}
