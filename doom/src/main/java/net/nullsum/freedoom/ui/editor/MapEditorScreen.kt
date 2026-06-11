@file:OptIn(ExperimentalMaterial3Api::class)

package net.nullsum.freedoom.ui.editor

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.nullsum.freedoom.ui.editor.data.ProjectSummary
import net.nullsum.freedoom.ui.editor.model.MapTheme
import net.nullsum.freedoom.ui.editor.model.TileType
import net.nullsum.freedoom.ui.editor.model.WadFormat
import net.nullsum.freedoom.ui.launch.WadEntry

/**
 * The in-app map studio: a paint canvas plus tools, a tile palette, per-map management,
 * generation tuning, a test-IWAD picker and named projects. All durable state lives in the
 * hoisted [MapEditorState]; this is just the Material 3 shell.
 */
@Composable
fun MapEditorScreen(state: MapEditorState, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        state.loadCurrentOrNew()
        state.loadIwads()
    }

    var showSize by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    var showMaps by remember { mutableStateOf(false) }
    var showTuning by remember { mutableStateOf(false) }
    var showProjects by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        EditorHeader(
            state = state,
            onOpenProjects = { showProjects = true },
        )

        // Canvas hero.
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            MapCanvas(state = state, modifier = Modifier.fillMaxSize())
            if (state.isBusy) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xAA000000)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        state.genStatus?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = Color.White)
                        }
                    }
                }
            }
        }

        TilePaletteRow(state)

        EditorToolbar(
            state = state,
            onSize = { showSize = true },
            onTheme = { showTheme = true },
            onMaps = { showMaps = true },
            onTuning = { showTuning = true },
        )

        ActionRow(
            state = state,
            onResult = { ok, msg ->
                if (!ok) Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                else if (msg.isNotEmpty()) Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            },
        )
    }

    if (showSize) SizeDialog(state) { showSize = false }
    if (showTheme) ThemeDialog(state) { showTheme = false }
    if (showMaps) MapsSheet(state) { showMaps = false }
    if (showTuning) TuningSheet(state) { showTuning = false }
    if (showProjects) ProjectsSheet(state) { showProjects = false }
}

// ---------------------------------------------------------------------------- header

@Composable
private fun EditorHeader(state: MapEditorState, onOpenProjects: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    var iwadMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = state.project.name,
            onValueChange = { state.rename(it) },
            label = { Text("Level name") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )

        // Test IWAD picker.
        Box {
            OutlinedButton(onClick = { iwadMenuOpen = true }) {
                Text(state.project.iwadFile.removeSuffix(".wad"), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            DropdownMenu(expanded = iwadMenuOpen, onDismissRequest = { iwadMenuOpen = false }) {
                if (state.availableIwads.isEmpty()) {
                    DropdownMenuItem(text = { Text("No IWADs found") }, onClick = { iwadMenuOpen = false })
                }
                state.availableIwads.forEach { wad: WadEntry ->
                    DropdownMenuItem(
                        text = { Text(wad.file) },
                        onClick = { state.setIwad(wad.file); iwadMenuOpen = false },
                    )
                }
            }
        }

        Box {
            TextButton(onClick = { menuOpen = true }) { Text("⋮") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Projects…") },
                    onClick = { menuOpen = false; onOpenProjects() },
                )
            }
        }
    }
}

// ----------------------------------------------------------------------------- palette

@Composable
private fun TilePaletteRow(state: MapEditorState) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(TileType.entries.toList()) { tile ->
            val selected = state.selectedTile == tile && state.activeTool != EditorTool.Eraser
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable {
                        state.selectedTile = tile
                        if (state.activeTool == EditorTool.Eraser || state.activeTool == EditorTool.Pan) {
                            state.activeTool = EditorTool.Brush
                        }
                    }
                    .padding(2.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(tile.composeColor, RoundedCornerShape(6.dp))
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF555555),
                            shape = RoundedCornerShape(6.dp),
                        ),
                )
                Text(tile.displayName, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

// ----------------------------------------------------------------------------- toolbar

@Composable
private fun EditorToolbar(
    state: MapEditorState,
    onSize: () -> Unit,
    onTheme: () -> Unit,
    onMaps: () -> Unit,
    onTuning: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolChip("Brush", state.activeTool == EditorTool.Brush) { state.activeTool = EditorTool.Brush }
            ToolChip("Erase", state.activeTool == EditorTool.Eraser) { state.activeTool = EditorTool.Eraser }
            ToolChip("Fill", state.activeTool == EditorTool.Bucket) { state.activeTool = EditorTool.Bucket }
            ToolChip("Pan", state.activeTool == EditorTool.Pan) { state.activeTool = EditorTool.Pan }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { state.undo() }, enabled = state.canUndo) { Text("Undo") }
            TextButton(onClick = { state.redo() }, enabled = state.canRedo) { Text("Redo") }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AssistChip(onClick = onSize, label = { Text("Size ${state.gridWidth}×${state.gridHeight}") })
            AssistChip(onClick = onTheme, label = { Text("Theme: ${state.theme.displayName}") })
            AssistChip(onClick = onMaps, label = { Text("Maps (${state.project.maps.size})") })
            AssistChip(onClick = onTuning, label = { Text("Tuning") })
        }
    }
}

@Composable
private fun ToolChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

// ----------------------------------------------------------------------------- actions

@Composable
private fun ActionRow(state: MapEditorState, onResult: (Boolean, String) -> Unit) {
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = {
                scope.launch {
                    val r = state.generate()
                    onResult(r != null, if (r != null) "WAD generated: ${r.wadFile.name}" else "Failed to generate WAD")
                }
            },
            enabled = !state.isBusy,
            modifier = Modifier.weight(1f).height(48.dp),
        ) { Text("Generate") }

        Button(
            onClick = {
                scope.launch {
                    val ok = state.generateAndLaunch()
                    if (!ok) onResult(false, "Failed to generate WAD")
                }
            },
            enabled = !state.isBusy,
            modifier = Modifier.weight(1f).height(48.dp),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Test")
        }
    }
}

// ------------------------------------------------------------------------- size dialog

@Composable
private fun SizeDialog(state: MapEditorState, onDismiss: () -> Unit) {
    var customW by remember { mutableStateOf(state.gridWidth.toString()) }
    var customH by remember { mutableStateOf(state.gridHeight.toString()) }
    var keepContent by remember { mutableStateOf(true) }

    fun apply(w: Int, h: Int) {
        state.setSize(w, h, if (keepContent) ResizeMode.CropPad else ResizeMode.Clear)
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Grid size") },
        text = {
            Column {
                Text("Presets", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(GRID_PRESETS) { p ->
                        FilterChip(
                            selected = state.gridWidth == p && state.gridHeight == p,
                            onClick = { customW = p.toString(); customH = p.toString() },
                            label = { Text("$p²") },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Custom (W×H, $MIN_GRID–$MAX_GRID)", style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = customW,
                        onValueChange = { customW = it.filter(Char::isDigit).take(3) },
                        label = { Text("W") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Text("×")
                    OutlinedTextField(
                        value = customH,
                        onValueChange = { customH = it.filter(Char::isDigit).take(3) },
                        label = { Text("H") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = keepContent, onClick = { keepContent = true })
                    Text("Keep drawing (crop/pad)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !keepContent, onClick = { keepContent = false })
                    Text("Clear")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val w = customW.toIntOrNull() ?: state.gridWidth
                val h = customH.toIntOrNull() ?: state.gridHeight
                apply(w, h)
            }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ------------------------------------------------------------------------ theme dialog

@Composable
private fun ThemeDialog(state: MapEditorState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Theme") },
        text = {
            Column {
                MapTheme.entries.forEach { t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { state.setTheme(t); onDismiss() }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = state.theme == t, onClick = { state.setTheme(t); onDismiss() })
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(20.dp).background(t.composeColor, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(t.displayName)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

// -------------------------------------------------------------------------- maps sheet

@Composable
private fun MapsSheet(state: MapEditorState, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Maps", style = MaterialTheme.typography.titleLarge)
            Text(
                "Each map becomes MAP01, MAP02… in the WAD. Pick one to edit; set the test target.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            state.project.maps.forEachIndexed { i, _ ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val isCurrent = i == state.currentMapIndex
                    Text(
                        "MAP%02d".format(i + 1),
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable { state.selectMap(i) }.weight(1f),
                    )
                    // Test-target radio.
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { state.setTestMap(i) }) {
                        RadioButton(selected = state.project.testMapIndex == i, onClick = { state.setTestMap(i) })
                        Text("test", style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = { state.duplicateMap(i) }) { Text("Dup") }
                    TextButton(onClick = { state.moveMap(i, i - 1) }, enabled = i > 0) { Text("↑") }
                    TextButton(onClick = { state.moveMap(i, i + 1) }, enabled = i < state.project.maps.lastIndex) { Text("↓") }
                    TextButton(
                        onClick = { state.deleteMap(i) },
                        enabled = state.project.maps.size > 1,
                    ) { Text("Del") }
                }
                HorizontalDivider()
            }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(
                onClick = { state.addMap() },
                enabled = state.project.maps.size < net.nullsum.freedoom.ui.editor.model.MapProject.MAX_MAPS,
            ) { Text("Add map") }
        }
    }
}

// ------------------------------------------------------------------------ tuning sheet

@Composable
private fun TuningSheet(state: MapEditorState, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Generation tuning", style = MaterialTheme.typography.titleLarge)
            Text(
                "Relative densities. The engine also scales by map area, so bigger maps get more.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            DensitySlider("Monsters", state.project.tuning.monsterDensity) { state.setMonsterDensity(it) }
            DensitySlider("Items", state.project.tuning.itemDensity) { state.setItemDensity(it) }
            DensitySlider("Ammo", state.project.tuning.ammoDensity) { state.setAmmoDensity(it) }

            Spacer(Modifier.height(12.dp))
            Text("WAD format", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.project.format == WadFormat.DOOM2,
                    onClick = { state.setFormat(WadFormat.DOOM2) },
                    label = { Text("Doom II (MAP01)") },
                )
                FilterChip(
                    selected = state.project.format == WadFormat.DOOM1,
                    onClick = { state.setFormat(WadFormat.DOOM1) },
                    label = { Text("Doom I (ExMy)") },
                )
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    }
}

@Composable
private fun DensitySlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("$label  ${(value * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
        Slider(value = value, onValueChange = onChange, valueRange = 0f..1f)
    }
}

// ---------------------------------------------------------------------- projects sheet

@Composable
private fun ProjectsSheet(state: MapEditorState, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var saveName by remember { mutableStateOf(state.project.name) }
    var projects by remember { mutableStateOf<List<ProjectSummary>>(emptyList()) }

    LaunchedEffect(Unit) { projects = state.listProjects() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Projects", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = saveName,
                    onValueChange = { saveName = it },
                    label = { Text("Save as") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = {
                    scope.launch {
                        state.saveNamedAs(saveName)
                        projects = state.listProjects()
                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Save") }
            }

            Spacer(Modifier.height(12.dp))
            Text("Saved", style = MaterialTheme.typography.labelLarge)
            if (projects.isEmpty()) {
                Text("No saved projects yet.", style = MaterialTheme.typography.bodySmall)
            }
            projects.forEach { p ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.name)
                        Text("${p.mapCount} map(s)", style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = {
                        scope.launch { state.openProject(p.file); onDismiss() }
                    }) { Text("Open") }
                    TextButton(onClick = {
                        scope.launch { state.deleteProject(p.file); projects = state.listProjects() }
                    }) { Text("Delete") }
                }
                HorizontalDivider()
            }
        }
    }
}
