pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    dependencyResolutionManagement {
        repositories {
            google()
            mavenCentral()
            maven { url = uri("https://www.jitpack.io") }
        }
    }
}


rootProject.name = "AndroidCheckOutSDK"
include(":app")
include(":Sdk")
