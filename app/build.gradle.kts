plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1"
    id("com.google.gms.google-services")
    id ("kotlin-kapt")
    id ("com.google.dagger.hilt.android")
    id("androidx.navigation.safeargs")
    id("kotlin-parcelize")
    alias(libs.plugins.google.firebase.crashlytics)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.rahuljoshi.uttarakhandswabhimanmorcha"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rahuljoshi.uttarakhandswabhimanmorcha"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
    }
    viewBinding{
        enable = true
    }
    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/DEPENDENCIES",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1"
            )
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.ui.text.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //size
    implementation (libs.sdp.android)
    implementation (libs.ssp.android)

    //circle image
    implementation (libs.circleimageview)

    //hilt dependency
    implementation (libs.hilt.android)
    kapt (libs.hilt.android.compiler)


    //firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.crashlytics)

    //google drive
    implementation(libs.google.auth.library.oauth2.http) // or latest
    implementation(libs.google.api.client) // or latest
    implementation(libs.google.api.client.gson) // or latest
    implementation(libs.play.services.auth)
    implementation(libs.google.api.services.drive)
    implementation(libs.google.http.client.android)
    implementation(libs.play.services.gcm)

    implementation(libs.grpc.stub)
    implementation(libs.grpc.protobuf.lite)
    implementation(libs.grpc.okhttp)
    implementation(libs.grpc.android)
    compileOnly(libs.annotations.api)

    //smtp mail
    implementation (libs.android.mail)
    implementation (libs.android.activation)

    //room
    ksp(libs.room.compiler)
    implementation(libs.androidx.room.runtime)
    ksp(libs.room.compiler)

    //qr
    implementation (libs.zxing.android.embedded)

    //image
    implementation(libs.coil)

}

kapt {
    correctErrorTypes = true
}