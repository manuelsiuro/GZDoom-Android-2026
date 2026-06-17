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

    /**
     * Like [generateWad], but also injects hand-placed things.
     *
     * @param perMapThings parallel to [pngPaths]; each entry is a compact
     *   descriptor of placed things, "type,cellX,cellY,angle,flags;..." in
     *   editor grid-cell coordinates (empty string = no placed things for that map).
     * @param suppressAutoThings when true, disables the procedural monster/item
     *   scatter so only hand-placed things appear (the grid Start tile still
     *   yields a player start).
     */
    external fun generateWadWithThings(
        pngPaths: Array<String>,
        perMapThings: Array<String>,
        suppressAutoThings: Boolean,
        outWadPath: String,
        configPath: String
    ): Boolean
}
