plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
    alias(libs.plugins.compose.compiler)

    //id("androidx.room")
}

android {
    namespace = "com.example.myqrapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myqrapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildFeatures {
        viewBinding = true
    }

    // Used for Unit testing Android dependent elements in /test folder
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {

    implementation(libs.play.services.location)
    val multidex_version = "2.0.1"
    implementation("androidx.multidex:multidex:$multidex_version")


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.junit.ktx)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.androidx.room.common)
    testImplementation(libs.junit)
    testImplementation("org.testng:testng:6.9.6")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("androidx.multidex:multidex:2.0.1")

    implementation("io.github.g00fy2.quickie:quickie-bundled:1.10.0")

    implementation("com.google.zxing:core:3.4.1")
    implementation("me.dm7.barcodescanner:zxing:1.8.4")
    implementation("com.github.androidmads:QRGenerator:1.0.1")

    testImplementation("junit:junit:4.13.2")

    testImplementation("com.google.truth:truth:1.4.2")
    androidTestImplementation("com.google.truth:truth:1.4.2")

    androidTestImplementation("org.mockito:mockito-android:2.28.2")

    testImplementation("org.mockito:mockito-core:3.11.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")

    testImplementation("androidx.arch.core:core-testing:2.1.0")

    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("org.mockito:mockito-inline:2.13.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.2")

    implementation("androidx.activity:activity-ktx:1.3.0")
    // retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    // coroutine
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")

    implementation("com.google.firebase:firebase-auth:21.0.1")

    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))

    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Add the dependencies for the Firebase Cloud Messaging and Analytics libraries
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-messaging")

    // Add the dependency for the Firebase SDK for Google Analytics
    implementation("com.google.firebase:firebase-analytics")

    // TODO: Add the dependencies for any other Firebase products you want to use
    // See https://firebase.google.com/docs/android/setup#available-libraries
    // For example, add the dependencies for Firebase Authentication and Cloud Firestore
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    implementation("com.squareup.okio:okio:1.17.2")

    val room_version = "2.6.1"
    implementation(kotlin("stdlib-jdk8"))
    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")
    // To use Kotlin Symbol Processing (KSP)
    ksp("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")

    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("androidx.camera:camera-core:1.1.0-alpha09")
    implementation("androidx.camera:camera-camera2:1.1.0-alpha09")
    implementation("androidx.camera:camera-lifecycle:1.1.0-alpha09")
    implementation("androidx.camera:camera-view:1.0.0-alpha32")
}