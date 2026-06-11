plugins {
    id("com.android.library")
}

android {
    namespace = "com.doomandroid.png2wad"
    // Matches the consuming GZDoom app (compileSdk 36) — newer AGP rejects very old
    // compileSdk values, and a library should not compile against a newer SDK than its app.
    compileSdk = 36

    defaultConfig {
        // Must be <= the consuming app's minSdk. The GZDoom-for-Android app (:doom)
        // uses minSdk 23, so keep this at 23 to avoid a manifest-merge failure.
        minSdk = 23

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
            }
        }

        // Match the GZDoom engine's prebuilt ABIs so libpng2wad.so is only built
        // for ABIs the app actually ships (avoids packaging an x86 png2wad with no
        // matching engine lib).
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
}
