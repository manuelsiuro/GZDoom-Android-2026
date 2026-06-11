package net.nullsum.freedoom.ui.editor.generate

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import net.nullsum.freedoom.ui.editor.model.MapDoc
import net.nullsum.freedoom.ui.editor.model.TileType
import java.io.File
import java.io.FileOutputStream

/**
 * Renders one [MapDoc] to a width×height ARGB bitmap where each pixel is the tile's
 * canonical RGB, and pixel (0,0) carries the theme colour (the converter reads the theme
 * from the top-left pixel and overwrites it to white on load).
 */
fun renderMapBitmap(map: MapDoc): Bitmap {
    val bmp = createBitmap(map.width, map.height)
    for (y in 0 until map.height) {
        for (x in 0 until map.width) {
            val rgb = if (x == 0 && y == 0) {
                map.theme.pixelRgb
            } else {
                TileType.fromOrdinal(map.tiles[y * map.width + x]).rgb
            }
            bmp[x, y] = (0xFF000000.toInt()) or rgb
        }
    }
    return bmp
}

/** Renders [map] to [outFile] as a PNG. */
fun renderMapPng(map: MapDoc, outFile: File) {
    val bmp = renderMapBitmap(map)
    FileOutputStream(outFile).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bmp.recycle()
}
