package com.doomandroid.png2wad

import android.graphics.Bitmap

/**
 * Reads real Doom textures and flats out of an IWAD (plus optional override
 * PWADs) and decodes them to [Bitmap]s for the editor's texture browser.
 *
 * Backed by the native [TextureReader] in libpng2wad. Holds a native handle;
 * call [open] before use and [close] (or use as an [AutoCloseable]) when done.
 * Not thread-safe: confine a single instance to one worker (decode off the main
 * thread — the IWAD is parsed fully into native memory on [open]).
 */
class WadTextures : AutoCloseable {

    private var handle: Long = 0L

    val isOpen: Boolean get() = handle != 0L

    /** Opens [iwadPath]; returns true if a palette + textures were found. */
    fun open(iwadPath: String, extraWadPaths: List<String> = emptyList()): Boolean {
        close()
        handle = nativeOpen(iwadPath, extraWadPaths.toTypedArray())
        return handle != 0L
    }

    /** Composite wall texture names (TEXTURE1/TEXTURE2), in WAD order. */
    fun wallNames(): List<String> =
        if (handle != 0L) nativeListWalls(handle).asList() else emptyList()

    /** Flat (floor/ceiling) names between F_START/F_END, in WAD order. */
    fun flatNames(): List<String> =
        if (handle != 0L) nativeListFlats(handle).asList() else emptyList()

    /**
     * Decodes [name] (a wall texture, flat, or raw patch/sprite lump) into an
     * ARGB_8888 bitmap, or null if it isn't present / can't be decoded.
     */
    fun decode(name: String): Bitmap? {
        if (handle == 0L) return null
        val raw = nativeGetRgba(handle, name) ?: return null
        if (raw.size < 2) return null
        val w = raw[0]
        val h = raw[1]
        if (w <= 0 || h <= 0 || raw.size < 2 + w * h) return null
        return Bitmap.createBitmap(raw, 2, w, w, h, Bitmap.Config.ARGB_8888)
    }

    override fun close() {
        if (handle != 0L) {
            nativeClose(handle)
            handle = 0L
        }
    }

    private external fun nativeOpen(iwadPath: String, extraWadPaths: Array<String>): Long
    private external fun nativeListWalls(handle: Long): Array<String>
    private external fun nativeListFlats(handle: Long): Array<String>
    private external fun nativeGetRgba(handle: Long, name: String): IntArray?
    private external fun nativeClose(handle: Long)

    companion object {
        init { System.loadLibrary("png2wad") }
    }
}
