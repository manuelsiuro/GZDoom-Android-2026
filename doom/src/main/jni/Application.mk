APP_ABI      := armeabi-v7a arm64-v8a

APP_PLATFORM := android-21

APP_STL :=  c++_static

# This engine is pre-C++17 code (uses the 'register' keyword etc.); NDK r27's clang
# defaults to C++17 where those are hard errors. Pin to C++14.
APP_CPPFLAGS += -std=c++14 -Wno-register -Wno-deprecated-register -fpermissive \
                -Wno-error=vla-cxx-extension -Wno-vla-cxx-extension \
                -Wno-error=deprecated-declarations -Wno-error=writable-strings

# NDK r27's clang promotes several legacy-C/C++ diagnostics to hard errors that older
# toolchains (which emileb built with) treated as warnings. Downgrade them.
APP_CFLAGS += -Wno-error=incompatible-function-pointer-types \
              -Wno-error=incompatible-pointer-types \
              -Wno-error=implicit-function-declaration \
              -Wno-error=int-conversion \
              -Wno-error=implicit-int \
              -Wno-error=return-type

# Align shared-library LOAD segments to 16 KB so the app supports Android 15+ devices
# that use 16 KB memory pages (required by Google Play for modern target SDKs).
APP_LDFLAGS += -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384
