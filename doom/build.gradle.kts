plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "net.nullsum.freedoom"
    compileSdk = 34

    defaultConfig {
        applicationId = "net.nullsum.freedoom"
        minSdk = 21
        targetSdk = 34
        versionCode = 19
        versionName = "0.4.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    /*
    signingConfigs {
        create("release") {
            // Replace with your actual keystore information
            keyAlias = "key0"
            keyPassword = ""
            storeFile = file("C:/Users/matth/Dropbox/Keystore/mkrupczak3.jks")
            storePassword = ""
        }
    }
    */

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
    implementation(libs.androidx.viewpager2)
    implementation(libs.material)
    implementation(libs.kotlinx.coroutines.android)
}
