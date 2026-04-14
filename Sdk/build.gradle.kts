plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.boxpay.checkout.sdk"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField("String", "SDK_VERSION", "\"1.2.14-beta1\"")

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures{
        viewBinding = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15" // Use the appropriate version
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
    implementation ("com.mixpanel.android:mixpanel-android:7.5.4")
    testImplementation("org.mockito:mockito-core:5.7.0") // Replace with the latest version
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    // gson converter
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.10.0")
    implementation ("androidx.lifecycle:lifecycle-extensions:2.2.0")
    // compose dependencies
    implementation("androidx.compose.ui:ui:1.7.4")
    implementation("androidx.compose.material:material:1.7.4")
    implementation("androidx.compose.ui:ui-tooling:1.7.4")
    implementation("androidx.compose.material:material-icons-core:1.5.1")
    implementation("androidx.compose.material3:material3:1.1.1")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("io.coil-kt:coil-svg:2.2.2")
    implementation("androidx.activity:activity-compose:1.5.1")
    implementation("com.github.simformsolutions:SSCustomEditTextOutLineBorder:1.0.16")
    implementation("com.github.BoxPay-SDKs.cross-platform-sdk:cross-platform-sdk:1.0.0-beta19")
    implementation("com.hbb20:ccp:2.7.3")
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