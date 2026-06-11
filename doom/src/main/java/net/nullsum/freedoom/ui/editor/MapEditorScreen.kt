package net.nullsum.freedoom.ui.editor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.doomandroid.png2wad.Png2WadConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.nullsum.freedoom.AppSettings
import net.nullsum.freedoom.Game
import net.nullsum.freedoom.Utils
import net.nullsum.freedoom.ui.launch.buildLaunchArgs
import java.io.File
import java.io.FileOutputStream

// Tile colors (ARGB) — must match the png2wad GetTileTypeFromPixel() table.
private val COLOR_WALL = Color(0xFFFFFFFF)
private val COLOR_ROOM = Color(0xFF000000)
private val COLOR_SP_FLOOR = Color(0xFFFF0000)
private val COLOR_SP_CEILING = Color(0xFF008000)
private val COLOR_SKY = Color(0xFF0000FF)
private val COLOR_DOOR = Color(0xFF808000)
private val COLOR_SECRET = Color(0xFFFF00FF)
private val COLOR_START = Color(0xFFFFFF00)
private val COLOR_EXIT = Color(0xFF00FF00)

// Theme colors — selected via the top-left pixel; must match [Themes] in DEFAULT_PREFERENCES.
private val THEME_TECH = Color(0xFFFFFFFF) // Default theme = white per [Themes] Default=255,255,255
private val THEME_CAVE = Color(0xFF808080)
private val THEME_HELL = Color(0xFFFF0000)
private val THEME_CITY = Color(0xFF8080FF)

private val TILES = listOf(
    "Wall" to COLOR_WALL,
    "Room" to COLOR_ROOM,
    "Sp. Floor" to COLOR_SP_FLOOR,
    "Sp. Ceiling" to COLOR_SP_CEILING,
    "Sky" to COLOR_SKY,
    "Door" to COLOR_DOOR,
    "Secret" to COLOR_SECRET,
    "Start" to COLOR_START,
    "Exit" to COLOR_EXIT,
)

private val THEMES = listOf(
    "Tech" to THEME_TECH,
    "Cave" to THEME_CAVE,
    "Hell" to THEME_HELL,
    "City" to THEME_CITY,
)

private const val GENERATED_WAD_NAME = "generated.wad"

@Composable
fun MapEditorScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val coroutineScope = rememberCoroutineScope()

    val gridSize = 16
    var pixels by remember { mutableStateOf(Array(gridSize * gridSize) { COLOR_ROOM }) }
    var selectedColor by remember { mutableStateOf(COLOR_WALL) }
    var selectedTheme by remember { mutableStateOf(THEME_TECH) }
    var isBusy by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Theme (top-left pixel)", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            THEMES.forEach { (name, color) ->
                PaletteItem(name, color, selectedTheme == color) { selectedTheme = color }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Tile palette", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TILES.take(5).forEach { (name, color) ->
                PaletteItem(name, color, selectedColor == color) { selectedColor = color }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TILES.drop(5).forEach { (name, color) ->
                PaletteItem(name, color, selectedColor == color) { selectedColor = color }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .border(2.dp, Color.Gray),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val cell = size.width / gridSize
                            val col = (offset.x / cell).toInt().coerceIn(0, gridSize - 1)
                            val row = (offset.y / cell).toInt().coerceIn(0, gridSize - 1)
                            if (col != 0 || row != 0) {
                                val np = pixels.clone(); np[row * gridSize + col] = selectedColor; pixels = np
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val cell = size.width / gridSize
                            val col = (change.position.x / cell).toInt().coerceIn(0, gridSize - 1)
                            val row = (change.position.y / cell).toInt().coerceIn(0, gridSize - 1)
                            if (col != 0 || row != 0) {
                                val np = pixels.clone(); np[row * gridSize + col] = selectedColor; pixels = np
                            }
                        }
                    },
            ) {
                val cell = size.width / gridSize
                for (row in 0 until gridSize) {
                    for (col in 0 until gridSize) {
                        val color = if (row == 0 && col == 0) selectedTheme else pixels[row * gridSize + col]
                        drawRect(color, Offset(col * cell, row * cell), Size(cell, cell))
                    }
                }
                for (i in 0..gridSize) {
                    drawLine(Color.DarkGray, Offset(0f, i * cell), Offset(size.width, i * cell), 1f)
                    drawLine(Color.DarkGray, Offset(i * cell, 0f), Offset(i * cell, size.height), 1f)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (activity == null) return@Button
                isBusy = true
                coroutineScope.launch {
                    val wad = generateWad(context, gridSize, pixels, selectedTheme)
                    if (wad != null) {
                        launchGeneratedMap(activity, wad)
                    } else {
                        Toast.makeText(context, "Failed to generate WAD", Toast.LENGTH_LONG).show()
                    }
                    isBusy = false
                }
            },
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            Text(if (isBusy) "Working…" else "Generate & Play")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                isBusy = true
                coroutineScope.launch {
                    val wad = generateWad(context, gridSize, pixels, selectedTheme)
                    Toast.makeText(
                        context,
                        if (wad != null) "WAD generated: ${wad.absolutePath}" else "Failed to generate WAD",
                        Toast.LENGTH_LONG,
                    ).show()
                    isBusy = false
                }
            },
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            Text("Generate only")
        }
    }
}

@Composable
private fun PaletteItem(name: String, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color)
                .border(3.dp, if (isSelected) Color.White else Color.DarkGray)
                .clickable { onClick() },
        )
        Text(name, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}

/**
 * Renders the grid to a PNG, writes Preferences.ini, and runs the native png2wad
 * converter. The output WAD is written into the Freedoom mods dir so it can be
 * loaded with "-file mods/generated.wad". Returns the WAD file on success.
 */
private suspend fun generateWad(
    context: Context,
    gridSize: Int,
    pixels: Array<Color>,
    theme: Color,
): File? = withContext(Dispatchers.IO) {
    try {
        val bitmap = Bitmap.createBitmap(gridSize, gridSize, Bitmap.Config.ARGB_8888)
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val c = if (row == 0 && col == 0) theme else pixels[row * gridSize + col]
                bitmap.setPixel(
                    col, row,
                    android.graphics.Color.argb(
                        (c.alpha * 255).toInt(),
                        (c.red * 255).toInt(),
                        (c.green * 255).toInt(),
                        (c.blue * 255).toInt(),
                    ),
                )
            }
        }

        val pngFile = File(context.cacheDir, "temp_map.png")
        FileOutputStream(pngFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val configFile = File(context.cacheDir, "Preferences.ini")
        configFile.writeText(DEFAULT_PREFERENCES)

        val modsDir = File(AppSettings.getQuakeFullDir(), "mods").apply { mkdirs() }
        val wadFile = File(modsDir, GENERATED_WAD_NAME)

        val ok = Png2WadConverter().generateWad(
            arrayOf(pngFile.absolutePath),
            wadFile.absolutePath,
            configFile.absolutePath,
        )
        if (ok && wadFile.exists()) wadFile else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Launches the GZDoom engine on the freshly generated map. Mirrors the Intent
 * contract of [net.nullsum.freedoom.ui.launch.LaunchState.launchGame] and jumps
 * straight into MAP01 via "+map MAP01".
 */
private suspend fun launchGeneratedMap(activity: Activity, wadFile: File) {
    val base = AppSettings.getQuakeFullDir()
    withContext(Dispatchers.IO) {
        AppSettings.createDirectories(activity)
        // Ensure the IWAD + engine resources exist (the editor tab may be used
        // before the launch tab has unpacked them).
        if (!File(base, "freedoom2.wad").exists()) {
            Utils.copyFreedoomFilesToSD(activity)
        }
        Utils.copyAsset(activity, "gzdoom.pk3", base)
        Utils.copyAsset(activity, "gzdoom.sf2", base)
    }

    // Path relative to the engine working dir (game_path == base).
    val modArgs = "-file mods/${wadFile.name} "
    val intent = Intent(activity, Game::class.java).apply {
        action = Intent.ACTION_MAIN
        addCategory(Intent.CATEGORY_LAUNCHER)
        putExtra("res_div", AppSettings.getIntOption(activity, "gzdoom_res_div", 1))
        putExtra("game_path", base)
        putExtra("game", "net.nullsum.freedoom")
        putExtra("args", buildLaunchArgs("-iwad freedoom2.wad", modArgs, "+map MAP01", base))
    }
    activity.startActivity(intent)
}

private val DEFAULT_PREFERENCES = """[Options]
BuildNodes=false
Doom1Format=false
Episode=1
GenerateEntranceAndExit=true
GenerateThings=true

[Things]
Types.AmmoSmall=2008,2007,2047,2010
Types.AmmoLarge=2048,2046,2049,17
Types.Armor=2018,2019
Types.Health=2012,2011
Types.MonstersVeryHard=64,69,3003
Types.MonstersHard=3005,69
Types.MonstersMedium=3002,3006,58,65
Types.MonstersEasy=3004,9,3001
Types.PowerUps=8,2023,2022,2024,2013
Types.WeaponsLow=2002,2005,2001,82
Types.WeaponsHigh=2006,2004,2003

Count.AmmoLarge=4,8
Count.AmmoSmall=8,12
Count.Armor=2,4
Count.Health=8,10
Count.MonstersAverage=15,25
Count.MonstersEasy=15,25
Count.MonstersHard=5,10
Count.MonstersVeryHard=2,5
Count.PowerUps=0,2
Count.WeaponsHigh=1,3
Count.WeaponsLow=2,4

[Themes]
Default=255,255,255
Cave=128,128,128
City=128,128,255
Hell=255,0,0

[Theme.Default]
Height.Default=0,64
Height.DoorSide=0,64
Height.Entrance=4,64
Height.Exit=4,64
Height.Exterior=0,128
Height.SpecialCeiling=0,60
Height.SpecialFloor=-4,64

LightLevel.Default=192
LightLevel.DoorSide=192
LightLevel.Entrance=192
LightLevel.Exit=255
LightLevel.Exterior=255
LightLevel.SpecialCeiling=255
LightLevel.SpecialFloor=192

SectorSpecial.Default=0
SectorSpecial.DoorSide=0
SectorSpecial.Entrance=0
SectorSpecial.Exit=8
SectorSpecial.Exterior=0
SectorSpecial.SpecialCeiling=17
SectorSpecial.SpecialFloor=7

Textures.Ceiling=CEIL3_1,CEIL3_3,FLAT18,FLAT20,FLAT4,FLAT5_5
Textures.CeilingSpecial=CEIL3_4,FLAT17,FLAT2,FLOOR1_7,GRNLITE1,TLITE6_1,TLITE6_4,TLITE6_5,TLITE6_6
Textures.Door=DOOR1,DOOR3
Textures.DoorSide=LITE5
Textures.Floor=FLAT1_1,FLAT1_2,FLAT5,FLAT5_5,FLOOR0_1,FLOOR0_2,FLOOR1_1,FLOOR3_3,FLOOR4_1,FLOOR5_3,FLOOR5_4
Textures.FloorEntrance=CEIL4_3
Textures.FloorExit=FLAT22
Textures.FloorExterior=FLOOR6_2,FLAT10
Textures.FloorSpecial=NUKAGE1
Textures.Wall=STARTAN2,CEMENT6,GRAY1,ICKWALL1,SLADWALL,BIGBRIK3,BRONZE4,BROVINE2,SPACEW2,SPACEW4,TEKGREN2
Textures.WallExterior=

[Theme.Cave]
Height.Default=0,64
Height.DoorSide=0,64
Height.Entrance=4,64
Height.Exit=4,64
Height.Exterior=0,128
Height.SpecialCeiling=0,64
Height.SpecialFloor=-4,64

LightLevel.Default=164
LightLevel.DoorSide=164
LightLevel.Entrance=164
LightLevel.Exit=255
LightLevel.Exterior=192
LightLevel.SpecialCeiling=164
LightLevel.SpecialFloor=192

SectorSpecial.Default=0
SectorSpecial.DoorSide=0
SectorSpecial.Entrance=0
SectorSpecial.Exit=8
SectorSpecial.Exterior=0
SectorSpecial.SpecialCeiling=0
SectorSpecial.SpecialFloor=0

Textures.Ceiling=FLAT5_7,FLAT5_8,FLOOR6_2,MFLR8_2
Textures.CeilingSpecial=FLAT5_2
Textures.Door=BIGDOOR5
Textures.DoorSide=METAL,SUPPORT3
Textures.Floor=FLAT10,FLAT5_7,FLAT5_8,FLOOR6_2,MFLR8_2,MFLR8_4
Textures.FloorEntrance=CEIL4_3
Textures.FloorExit=FLAT22
Textures.FloorExterior=
Textures.FloorSpecial=FWATER1
Textures.Wall=ASHWALL2,ROCK1,STONE4,STONE6
Textures.WallExterior=

[Theme.City]
Height.Default=0,64
Height.DoorSide=0,64
Height.Entrance=4,64
Height.Exit=4,64
Height.Exterior=0,256
Height.SpecialCeiling=0,64
Height.SpecialFloor=-4,64

LightLevel.Default=192
LightLevel.DoorSide=192
LightLevel.Entrance=192
LightLevel.Exit=255
LightLevel.Exterior=220
LightLevel.SpecialCeiling=255
LightLevel.SpecialFloor=192

SectorSpecial.Default=0
SectorSpecial.DoorSide=0
SectorSpecial.Entrance=0
SectorSpecial.Exit=8
SectorSpecial.Exterior=0
SectorSpecial.SpecialCeiling=0
SectorSpecial.SpecialFloor=0

Textures.Ceiling=CEIL3_1,FLAT5_4,FLAT9
Textures.CeilingSpecial=CEIL3_4
Textures.Door=DOOR1,DOOR3
Textures.DoorSide=LITE5
Textures.Floor=FLAT3,FLAT5,FLOOR0_2,FLOOR0_6,FLOOR0_7,FLOOR3_3
Textures.FloorEntrance=CEIL4_3
Textures.FloorExit=FLAT22
Textures.FloorExterior=FLAT1,RROCK03
Textures.FloorSpecial=FLAT14,FLOOR1_1,FLOOR1_6
Textures.Wall=STARTAN2,CEMENT6,GRAY1,ICKWALL1,SLADWALL,BIGBRIK3,BRONZE4,BROVINE2,SPACEW2,SPACEW4,TEKGREN2
Textures.WallExterior=BIGBRIK2,BLAKWAL2,BRICK1,BRICK5,BRICK11

[Theme.Hell]
Height.Default=0,64
Height.DoorSide=0,64
Height.Entrance=4,64
Height.Exit=4,64
Height.Exterior=0,128
Height.SpecialCeiling=0,64
Height.SpecialFloor=-4,64

LightLevel.Default=164
LightLevel.DoorSide=164
LightLevel.Entrance=164
LightLevel.Exit=255
LightLevel.Exterior=192
LightLevel.SpecialCeiling=164
LightLevel.SpecialFloor=192

SectorSpecial.Default=0
SectorSpecial.DoorSide=0
SectorSpecial.Entrance=0
SectorSpecial.Exit=8
SectorSpecial.Exterior=0
SectorSpecial.SpecialCeiling=0
SectorSpecial.SpecialFloor=5

Textures.Ceiling=CEIL1_1,FLAT5_1,FLAT5_2,FLAT5_3
Textures.CeilingSpecial=FLAT5_6,SFLR6_1,SFLR6_4
Textures.Door=BIGDOOR5
Textures.DoorSide=METAL,SUPPORT3
Textures.Floor=FLAT1_1,FLAT1_2,FLAT5_1,FLAT5_2,FLAT5_3
Textures.FloorEntrance=GATE4
Textures.FloorExit=GATE1,GATE2,GATE3
Textures.FloorExterior=FLAT5_7,FLAT5_8,FLOOR6_1,FLOOR6_2,MFLR8_2,MFLR8_3
Textures.FloorSpecial=LAVA1
Textures.Wall=GSTONE1,GSTVINE1,MARBGRAY,MARBLE2,SKINEDGE,SKSNAKE1
Textures.WallExterior=
"""
