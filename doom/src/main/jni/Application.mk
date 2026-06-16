APP_ABI      := armeabi-v7a arm64-v8a

APP_PLATFORM := android-23

# Multiple C++ shared libs (gzdoom, touchcontrols, saffal, SDL2) -> shared STL.
APP_STL := c++_shared

# Disable the Delta Touch Google Play licence check paths in Clibs_OpenTouch.
APP_CFLAGS   += -DNO_SEC
APP_CPPFLAGS += -DNO_SEC

# NDK r27 clang promotes several legacy diagnostics to errors; the vendored
# 2010s-era C code (openal, sndfile, mpg123, jpeg, ...) trips them.
APP_CFLAGS += -Wno-error=incompatible-function-pointer-types \
              -Wno-error=incompatible-pointer-types \
              -Wno-error=implicit-function-declaration \
              -Wno-error=int-conversion \
              -Wno-error=implicit-int \
              -Wno-error=return-type

# Align shared-library LOAD segments to 16 KB for Android 15+ 16K-page devices.
APP_LDFLAGS += -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384

# GZDoom registers CVARs/classes/action functions in custom ELF sections
# (areg/creg/vreg/...) walked via __start_*/__stop_* symbols. Modern lld no
# longer treats those symbols as keeping sections alive under the NDK's
# default --gc-sections, which silently strips every registration entry and
# crashes the engine on the first CVAR access. Disable that GC behaviour.
APP_LDFLAGS += -Wl,-z,nostart-stop-gc
