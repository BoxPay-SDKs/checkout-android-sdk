plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.example.tray"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures{
        viewBinding = true

    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.media3:media3-common:1.2.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation ("com.google.android.material:material:1.3.0-alpha04")
    implementation ("androidx.fragment:fragment-ktx:1.3.6")
    implementation ("com.android.volley:volley:1.2.1")
    implementation ("com.mikhaellopez:circularprogressbar:3.1.0")
    implementation ("com.airbnb.android:lottie:4.2.0")
    implementation ("com.google.code.gson:gson:2.10.1")
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
        maven { url = uri("https://www.jitpack.io")
            credentials { username = "jp_l40tne0p9r7vrirj2rus5l3tmo" }
        }
    }
}