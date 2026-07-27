plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    kotlin("plugin.compose") version "2.1.0"
}

android {
    namespace = "com.boxpay.checkout.sdk"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField("String", "SDK_VERSION", "\"2.0.0-beta3\"")

        android.buildFeatures.buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    signingConfigs {
        create("release") {
            val keystorePath: String? = System.getenv("KEYSTORE_PATH")
            val storePassword: String? = System.getenv("STORE_PASSWORD")
            val keyAlias: String? = System.getenv("KEY_ALIAS")
            val keyPassword: String? = System.getenv("KEY_PASSWORD")

            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword?.let { this.storePassword = it }
                keyAlias?.let { this.keyAlias = it }
                keyPassword?.let { this.keyPassword = it }
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures{
        viewBinding = true
        compose = true
    }
}


dependencies {
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation ("com.google.android.material:material:1.3.0-alpha04")
    implementation ("androidx.fragment:fragment-ktx:1.2.0")
    testImplementation("androidx.fragment:fragment-testing:1.5.7")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.9.3")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.9.3")
    testImplementation("org.mockito:mockito-core:5.7.0")
    implementation("androidx.activity:activity-compose:1.5.1")
    api("com.github.BoxPay-SDKs.cross-platform-sdk:cross-platform-sdk:1.0.2-beta4")
}


publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.example.AndroidCheckOutSDK"
            artifactId = "AndroidCheckOutSDK"
            version = "1.0.1"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
allprojects{
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://www.jitpack.io") }
    }
}