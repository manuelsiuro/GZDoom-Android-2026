cmake_minimum_required(VERSION 3.22.1)

project("png2wad")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Define zdbsp specific flags (Disable SSE for Android ARM, mostly handled by compiler)
add_definitions(-DDISABLE_SSE)
add_definitions(-Dstricmp=strcasecmp)
add_definitions(-Dstrnicmp=strncasecmp)
add_definitions(-DNO_MAP_VIEWER)

# Suppress warnings for legacy C code
add_compile_options(-Wno-deprecated-non-prototype)

# Find Android libraries
find_library(log-lib log)
find_library(z-lib z)

# Add source files
add_library(
        png2wad
        SHARED
        png2wad-jni.cpp
        textures-jni.cpp
        Toolbox.cpp
        INIFile.cpp
        Config.cpp
        WadFile.cpp
        DoomMap.cpp
        Generator.cpp
        PNGToWad.cpp
        TextureReader.cpp

        # ZDBSP sources
        zdbsp/blockmapbuilder.cpp
        zdbsp/getopt.c
        zdbsp/getopt1.c
        zdbsp/main.cpp
        zdbsp/nodebuild_classify_nosse2.cpp
        zdbsp/nodebuild_classify_sse1.cpp
        zdbsp/nodebuild_classify_sse2.cpp
        zdbsp/nodebuild_classify_sse2_vect.cpp
        zdbsp/nodebuild_events.cpp
        zdbsp/nodebuild_extract.cpp
        zdbsp/nodebuild_gl.cpp
        zdbsp/nodebuild_utility.cpp
        zdbsp/nodebuild.cpp
        zdbsp/processor_udmf.cpp
        zdbsp/processor.cpp
        zdbsp/sc_man.cpp
        zdbsp/view.cpp
        zdbsp/wad.cpp
)

# Link libraries
target_link_libraries(
        png2wad

        ${log-lib}
        ${z-lib}
)
