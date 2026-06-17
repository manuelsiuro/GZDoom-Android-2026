plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.msa.freedoom"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.msa.freedoom"
        minSdk = 33
        targetSdk = 36
        versionCode = 50
        versionName = "0.50.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            // signingConfig = signingConfigs.getByName("release")
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/libs")
        }
    }

    packaging {
        jniLibs {
            // Store .so uncompressed and page-aligned so they can be mmap'd directly on
            // 16 KB-page devices (Android 15+). Combined with the 16 KB ELF LOAD alignment
            // (see jni/Application.mk), this makes the app 16 KB-compatible.
            useLegacyPackaging = false
        }
    }

    buildFeatures {
        resValues = true
        compose = true
        buildConfig = true // BuildConfig.VERSION_NAME feeds the idgames API User-Agent
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "../libs", "include" to listOf("*.jar"))))
    implementation(project(":touchcontrols"))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.material)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.compose)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // PNG->WAD generator (native libpng2wad.so via CMake) used by the map editor.
    implementation(project(":png2wad-sdk"))

    testImplementation(libs.junit)
}
