
buildscript {
    dependencies {
        classpath(kotlin("gradle-plugin", version = "2.0.0"))
    }
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    kotlin("jvm") version "2.0.0" apply false

    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    id("com.google.devtools.ksp") version "2.0.0-1.0.21" apply false
    id("com.google.gms.google-services") version "4.4.1" apply false

    alias(libs.plugins.compose.compiler) apply false

    //id("androidx.room") version "$2.6.0" apply false
}