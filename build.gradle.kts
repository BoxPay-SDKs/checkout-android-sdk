// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.4.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0-RC1" apply false
    id("com.android.library") version "8.4.1" apply false
}

buildscript {
    dependencies {
        classpath("org.jacoco:org.jacoco.core:0.8.7")// Add JaCoCo classpath here
    }
}