package com.msa.freedoom.ui.editor.texture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.doomandroid.png2wad.WadTextures

/**
 * Opens a [WadTextures] for the current IWAD and caches decoded thumbnails. Shared by the
 * texture browser and the thing palette so the (multi-MB) IWAD is parsed once.
 *
 * The native handle is not thread-safe, so all native access is serialised here; decode it
 * from a background coroutine (decoding a single thumbnail is fast). Bounded LRU so scrolling
 * a large texture set never grows memory without limit.
 */
class TextureCache {

    private class Holder(val bitmap: ImageBitmap?)

    private val lock = Any()
    private var wad: WadTextures? = null
    private var openedPath: String? = null

    private val lru = object : LinkedHashMap<String, Holder>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Holder>) = size > MAX_ENTRIES
    }

    private fun ensureOpen(iwadPath: String): WadTextures? {
        if (openedPath == iwadPath && wad?.isOpen == true) return wad
        wad?.close()
        lru.clear()
        val w = WadTextures()
        wad = if (w.open(iwadPath)) w else { w.close(); null }
        openedPath = iwadPath
        return wad
    }

    fun isAvailable(iwadPath: String): Boolean = synchronized(lock) { ensureOpen(iwadPath) != null }

    fun wallNames(iwadPath: String): List<String> = synchronized(lock) { ensureOpen(iwadPath)?.wallNames().orEmpty() }
    fun flatNames(iwadPath: String): List<String> = synchronized(lock) { ensureOpen(iwadPath)?.flatNames().orEmpty() }

    /** Decode [name] (texture/flat/sprite) to a thumbnail, cached. null if it can't be decoded. */
    fun decode(iwadPath: String, name: String): ImageBitmap? = synchronized(lock) {
        val key = "$iwadPath|$name"
        lru[key]?.let { return it.bitmap }
        val bmp = ensureOpen(iwadPath)?.decode(name)?.asImageBitmap()
        lru[key] = Holder(bmp)
        bmp
    }

    /** Decode the first decodable candidate name (used for thing sprite frames). */
    fun decodeFirst(iwadPath: String, candidates: List<String>): ImageBitmap? {
        for (c in candidates) decode(iwadPath, c)?.let { return it }
        return null
    }

    fun close() = synchronized(lock) {
        wad?.close()
        wad = null
        openedPath = null
        lru.clear()
    }

    private companion object {
        const val MAX_ENTRIES = 400
    }
}

/** A [TextureCache] scoped to the composition; closed (releasing native memory) on dispose. */
@Composable
fun rememberTextureCache(): TextureCache {
    val cache = remember { TextureCache() }
    DisposableEffect(cache) { onDispose { cache.close() } }
    return cache
}
