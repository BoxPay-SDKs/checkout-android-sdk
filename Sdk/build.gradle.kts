plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}




android {
    namespace = "com.boxpay.checkout.sdk"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField("String", "SDK_VERSION", "\"1.1.19\"")

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
    testImplementation("androidx.fragment:fragment-testing:1.5.7")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.9.3")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.9.3")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")
    implementation("com.microsoft.clarity:clarity:2.5.1")
    testImplementation("org.mockito:mockito-core:5.7.0") // Replace with the latest version
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    // gson converter
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.10.0")
    implementation ("androidx.lifecycle:lifecycle-extensions:2.2.0")
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