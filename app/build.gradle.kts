plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "pt.utad.refresh"
    compileSdk = 35

    defaultConfig {
        applicationId = "pt.utad.refresh"
        minSdk = 23
        targetSdk = 35

        val commitCount = "git rev-list --count HEAD".runCommand()?.trim()?.toIntOrNull() ?: 1
        versionCode = commitCount

        val gitHash = "git rev-parse --short HEAD".runCommand()?.trim() ?: "unknown"
        buildConfigField("String", "GIT_HASH", "\"$gitHash\"")
        versionName = "$commitCount ($gitHash)"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("RELEASE_STORE_FILE") ?: project.property("RELEASE_STORE_FILE").toString())
            storePassword = (System.getenv("RELEASE_STORE_PASSWORD") ?: project.property("RELEASE_STORE_PASSWORD")).toString()
            keyAlias = (System.getenv("RELEASE_KEY_ALIAS") ?: project.property("RELEASE_KEY_ALIAS")).toString()
            keyPassword = (System.getenv("RELEASE_KEY_PASSWORD") ?: project.property("RELEASE_KEY_PASSWORD")).toString()
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += "-opt-in=androidx.camera.core.ExperimentalGetImage"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation (libs.circleimageview)
    implementation (libs.retrofit)
    implementation (libs.converter.gson)
    implementation (libs.kotlinx.coroutines.android.v164)
    implementation (libs.androidx.lifecycle.runtime.ktx)
    implementation (libs.logging.interceptor)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation (libs.glide)
    annotationProcessor (libs.compiler)
    implementation(libs.barcode.scanning)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
}

fun String.runCommand(): String? =
    try {
        ProcessBuilder(*split(" ").toTypedArray())
            .redirectErrorStream(true)
            .start()
            .inputStream
            .bufferedReader()
            .readText()
    } catch (e: Exception) {
        null
    }