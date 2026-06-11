package com.doomandroid.png2wad

class Png2WadConverter {
    
    companion object {
        init {
            System.loadLibrary("png2wad")
        }
    }

    /**
     * Generates a Doom WAD from an array of PNG file paths.
     * @param pngPaths Array of absolute paths to PNG files.
     * @param outWadPath Absolute path where the resulting .wad file will be saved.
     * @param configPath Absolute path to the Preferences.ini file.
     * @return true if successful, false otherwise.
     */
    external fun generateWad(pngPaths: Array<String>, outWadPath: String, configPath: String): Boolean
}
