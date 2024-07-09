plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}




android {
    namespace = "com.example.tray"
    compileSdk = 33

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField("String", "SDK_VERSION", "\"1.1.8-beta\"")

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
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.4.0")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.media3:media3-common:1.1.0")
    implementation("com.google.android.gms:play-services-auth-api-phone:18.0.2")
    implementation("androidx.work:work-runtime-ktx:2.8.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation ("com.google.android.material:material:1.3.0-alpha04")
    implementation ("androidx.fragment:fragment-ktx:1.2.0")
    implementation ("com.android.volley:volley:1.2.1")
    implementation ("com.mikhaellopez:circularprogressbar:3.1.0")
    implementation ("com.airbnb.android:lottie:4.2.0")
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation ("io.supercharge:shimmerlayout:2.1.0")
    implementation ("com.github.skydoves:balloon:1.4.7")
    implementation ("com.squareup.picasso:picasso:2.71828")
    implementation("io.coil-kt:coil:2.4.0")
    implementation("io.coil-kt:coil-svg:2.4.0")
    implementation ("jp.wasabeef:glide-transformations:4.3.0")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.3")
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