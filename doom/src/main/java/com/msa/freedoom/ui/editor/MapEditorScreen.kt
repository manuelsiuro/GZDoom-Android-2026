@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.msa.freedoom.ui.editor

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.msa.freedoom.R
import com.msa.freedoom.ui.DoomIcons
import com.msa.freedoom.ui.editor.data.ProjectSummary
import com.msa.freedoom.ui.editor.generate.renderMapBitmap
import com.msa.freedoom.ui.editor.launch.shareWad
import com.msa.freedoom.ui.editor.model.MapTheme
import com.msa.freedoom.ui.editor.model.TileType
import com.msa.freedoom.ui.editor.model.WadFormat
import com.msa.freedoom.ui.launch.WadEntry

/**
 * The in-app map studio: a paint canvas plus tools, a tile palette, per-map management,
 * generation tuning, a test-IWAD picker and named projects. All durable state lives in the
 * hoisted [MapEditorState]; this is just the Material 3 shell.
 *
 * Mobile-first layout: a left tool rail of icon toggles + canvas + bottom palette in portrait;
 * a side panel in landscape (≥600dp). Project/management actions live in a left navigation
 * drawer; only Test stays a prominent top-bar action.
 */
@Composable
fun MapEditorScreen(state: MapEditorState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        state.loadCurrentOrNew()
        state.loadIwads()
    }

    var showSize by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    var showMaps by remember { mutableStateOf(false) }
    var showTuning by remember { mutableStateOf(false) }
    var showProjects by remember { mutableStateOf(false) }
    var showReplace by remember { mutableStateOf(false) }
    var showTemplates by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var testWarnings by remember { mutableStateOf<List<String>?>(null) }
    var pendingClear by remember { mutableStateOf<TileType?>(null) }

    fun toast(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    val startTest = {
        scope.launch {
            if (!state.generateAndLaunch()) {
                toast(context.getString(R.string.editor_failed_generate))
            }
        }
        Unit
    }
    val onTest = {
        val map = state.project.maps[state.project.testMapIndex.coerceIn(0, state.project.maps.lastIndex)]
        val warnings = validateMap(map)
        if (warnings.isEmpty()) startTest() else testWarnings = warnings
    }
    val onGenerate = {
        scope.launch {
            val r = state.generate()
            toast(
                if (r != null) context.getString(R.string.editor_wad_generated, r.wadFile.name)
                else context.getString(R.string.editor_failed_generate),
            )
        }
        Unit
    }
    val onShare = {
        scope.launch {
            val wad = state.generateForShare()
            if (wad != null) shareWad(context, wad)
            else toast(context.getString(R.string.editor_failed_generate))
        }
        Unit
    }
    val closeDrawer = { scope.launch { drawerState.close() }; Unit }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            EditorDrawer(
                state = state,
                onSize = { closeDrawer(); showSize = true },
                onTheme = { closeDrawer(); showTheme = true },
                onMaps = { closeDrawer(); showMaps = true },
                onTuning = { closeDrawer(); showTuning = true },
                onProjects = { closeDrawer(); showProjects = true },
                onReplaceTile = { closeDrawer(); showReplace = true },
                onTemplates = { closeDrawer(); showTemplates = true },
                onClearMap = { closeDrawer(); pendingClear = it },
                onGenerate = { closeDrawer(); onGenerate() },
                onShare = { closeDrawer(); onShare() },
            )
        },
    ) {
        Scaffold(
            topBar = {
                EditorTopBar(
                    state = state,
                    onMenu = { scope.launch { drawerState.open() } },
                    onRename = { showRename = true },
                    onTest = onTest,
                    onGenerate = onGenerate,
                    onShare = onShare,
                    onFit = { state.resetView() },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            // Host already insets this subtree; zero here to avoid double status-bar inset.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            modifier = modifier,
        ) { innerPadding ->
            BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (maxWidth >= WIDE_BREAKPOINT) LandscapeBody(state) else PortraitBody(state)
            }
        }
    }

    if (showSize) SizeDialog(state) { showSize = false }
    if (showTheme) ThemeDialog(state) { showTheme = false }
    if (showMaps) MapsSheet(state) { showMaps = false }
    if (showTuning) TuningSheet(state) { showTuning = false }
    if (showProjects) ProjectsSheet(state, onMessage = ::toast) { showProjects = false }
    if (showReplace) ReplaceTileDialog(state) { showReplace = false }
    if (showTemplates) TemplatesDialog(state) { showTemplates = false }
    if (showRename) RenameDialog(state) { showRename = false }
    testWarnings?.let { warnings ->
        ValidationDialog(
            warnings = warnings,
            onTestAnyway = { testWarnings = null; startTest() },
            onDismiss = { testWarnings = null },
        )
    }
    pendingClear?.let { tile ->
        AlertDialog(
            onDismissRequest = { pendingClear = null },
            title = { Text(stringResource(R.string.editor_confirm_clear_title)) },
            text = { Text(stringResource(R.string.editor_confirm_clear_text)) },
            confirmButton = {
                TextButton(onClick = { state.clearMap(tile); pendingClear = null }) {
                    Text(stringResource(R.string.editor_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingClear = null }) { Text(stringResource(R.string.cancel_button)) }
            },
        )
    }
}

/** Width at/above which the editor switches to the side-panel landscape layout. */
private val WIDE_BREAKPOINT = 600.dp

// -------------------------------------------------------------------------- responsive bodies

/** Portrait / narrow: left tool rail, canvas fills, modifiers + palette stacked below. */
@Composable
private fun PortraitBody(state: MapEditorState) {
    Row(modifier = Modifier.fillMaxSize()) {
        EditorToolRail(state)
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            CanvasArea(state, modifier = Modifier.weight(1f).fillMaxWidth())
            ModifiersBar(state)
            TilePaletteStrip(state)
        }
    }
}

/** Landscape / wide (≥600dp): tool rail, canvas fills, a right side panel holds modifiers + palette. */
@Composable
private fun LandscapeBody(state: MapEditorState) {
    Row(modifier = Modifier.fillMaxSize()) {
        EditorToolRail(state)
        CanvasArea(state, modifier = Modifier.weight(1f).fillMaxHeight())
        Column(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ModifiersBar(state, wrap = true)
            TilePaletteStrip(state, wrap = true)
        }
    }
}

// --------------------------------------------------------------------------------- top bar

@Composable
private fun EditorTopBar(
    state: MapEditorState,
    onMenu: () -> Unit,
    onRename: () -> Unit,
    onTest: () -> Unit,
    onGenerate: () -> Unit,
    onShare: () -> Unit,
    onFit: () -> Unit,
) {
    var overflowOpen by remember { mutableStateOf(false) }
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onMenu) {
                Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.editor_open_menu))
            }
        },
        title = {
            Text(
                state.project.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onRename),
            )
        },
        actions = {
            IconButton(onClick = { state.undo() }, enabled = state.canUndo) {
                Icon(DoomIcons.Undo, contentDescription = stringResource(R.string.editor_undo))
            }
            IconButton(onClick = { state.redo() }, enabled = state.canRedo) {
                Icon(DoomIcons.Redo, contentDescription = stringResource(R.string.editor_redo))
            }
            IconButton(onClick = onTest, enabled = !state.isBusy) {
                Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.editor_test_map))
            }
            Box {
                IconButton(onClick = { overflowOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.editor_more))
                }
                DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.editor_generate_wad)) },
                        enabled = !state.isBusy,
                        onClick = { overflowOpen = false; onGenerate() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.editor_share_wad)) },
                        enabled = !state.isBusy,
                        onClick = { overflowOpen = false; onShare() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.editor_fit_to_screen)) },
                        onClick = { overflowOpen = false; onFit() },
                    )
                }
            }
        },
    )
}

// --------------------------------------------------------------------------------- tool rail

/** The drawing tools, paired with their icons and a label string-res, in rail order. */
private val TOOL_SPECS: List<Triple<EditorTool, ImageVector, Int>> = listOf(
    Triple(EditorTool.Brush, DoomIcons.Brush, R.string.editor_tool_brush),
    Triple(EditorTool.Eraser, DoomIcons.Eraser, R.string.editor_tool_erase),
    Triple(EditorTool.Bucket, DoomIcons.Fill, R.string.editor_tool_fill),
    Triple(EditorTool.Line, DoomIcons.Line, R.string.editor_tool_line),
    Triple(EditorTool.Rect, DoomIcons.Rect, R.string.editor_tool_rect),
    Triple(EditorTool.Eyedropper, DoomIcons.Colorize, R.string.editor_tool_pick),
    Triple(EditorTool.Pan, DoomIcons.Pan, R.string.editor_tool_pan),
)

/**
 * A persistent left rail of icon toggles for the drawing tools. A Row sibling of the canvas
 * (never an overlay) so it shrinks — not covers — the canvas region; [MapCanvas]'s cell mapping
 * divides by its own node size, which only stays correct if nothing floats over it.
 */
@Composable
private fun EditorToolRail(state: MapEditorState) {
    Column(
        modifier = Modifier
            .width(56.dp)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TOOL_SPECS.forEach { (tool, icon, labelRes) ->
            FilledIconToggleButton(
                checked = state.activeTool == tool,
                onCheckedChange = { state.activeTool = tool },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(icon, contentDescription = stringResource(labelRes))
            }
        }
    }
}

// ------------------------------------------------------------------------------- canvas area

@Composable
private fun CanvasArea(state: MapEditorState, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
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
}

// ----------------------------------------------------------------------------- modifiers bar

/**
 * Contextual drawing modifiers: symmetry, brush tip, and (Rect only) Filled. Scrolls horizontally
 * in portrait; wraps in the landscape side panel.
 */
@Composable
private fun ModifiersBar(state: MapEditorState, wrap: Boolean = false) {
    val content: @Composable () -> Unit = {
        if (state.activeTool == EditorTool.Rect) {
            FilterChip(
                selected = state.rectFilled,
                onClick = { state.rectFilled = !state.rectFilled },
                label = { Text(stringResource(R.string.editor_filled)) },
            )
        }
        // Symmetry (cycles Off → mirror-LR → mirror-UD → 4-way) and brush tip size.
        FilterChip(
            selected = state.symmetry != MapGridOps.SymmetryMode.None,
            onClick = { state.symmetry = nextSymmetry(state.symmetry) },
            label = { Text(stringResource(symmetryLabel(state.symmetry))) },
        )
        FilterChip(
            selected = state.brushSize > 1,
            onClick = { state.brushSize = if (state.brushSize >= 3) 1 else state.brushSize + 1 },
            label = { Text(stringResource(R.string.editor_tip, state.brushSize)) },
        )
    }
    if (wrap) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) { content() }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) { content() }
    }
}

// ----------------------------------------------------------------------------- palette

@Composable
private fun TilePaletteStrip(state: MapEditorState, wrap: Boolean = false) {
    val swatch: @Composable (TileType) -> Unit = { tile ->
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
                    .size(40.dp)
                    .background(tile.composeColor, RoundedCornerShape(6.dp))
                    .border(
                        width = if (selected) 3.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(6.dp),
                    ),
            )
            Text(tile.displayName, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
    if (wrap) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) { TileType.entries.forEach { swatch(it) } }
    } else {
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(TileType.entries.toList()) { swatch(it) }
        }
    }
}

// ----------------------------------------------------------------------------- drawer

@Composable
private fun EditorDrawer(
    state: MapEditorState,
    onSize: () -> Unit,
    onTheme: () -> Unit,
    onMaps: () -> Unit,
    onTuning: () -> Unit,
    onProjects: () -> Unit,
    onReplaceTile: () -> Unit,
    onTemplates: () -> Unit,
    onClearMap: (TileType) -> Unit,
    onGenerate: () -> Unit,
    onShare: () -> Unit,
) {
    var iwadMenuOpen by remember { mutableStateOf(false) }
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 16.dp),
        ) {
            Text(stringResource(R.string.editor_map_studio), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            // Test IWAD picker.
            Text(stringResource(R.string.editor_test_iwad), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Box {
                OutlinedButton(onClick = { iwadMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(state.project.iwadFile.removeSuffix(".wad"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DropdownMenu(expanded = iwadMenuOpen, onDismissRequest = { iwadMenuOpen = false }) {
                    if (state.availableIwads.isEmpty()) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.editor_no_iwads)) }, onClick = { iwadMenuOpen = false })
                    }
                    state.availableIwads.forEach { wad: WadEntry ->
                        DropdownMenuItem(
                            text = { Text(wad.file) },
                            onClick = { state.setIwad(wad.file); iwadMenuOpen = false },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            DrawerItem(stringResource(R.string.editor_grid_size_label, state.gridWidth, state.gridHeight), DoomIcons.Rect, onSize)
            DrawerItem(stringResource(R.string.editor_theme_label, state.theme.displayName), DoomIcons.Palette, onTheme)
            DrawerItem(stringResource(R.string.editor_maps_label, state.project.maps.size), DoomIcons.Layers, onMaps)
            DrawerItem(stringResource(R.string.editor_new_from_template), Icons.Filled.Add, onTemplates)
            DrawerItem(stringResource(R.string.editor_generation_tuning), DoomIcons.Tune, onTuning)
            DrawerItem(stringResource(R.string.editor_replace_tile_menu), DoomIcons.Fill, onReplaceTile)

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            DrawerItem(stringResource(R.string.editor_clear_map_room), DoomIcons.Brush) { onClearMap(TileType.Room) }
            DrawerItem(stringResource(R.string.editor_fill_with_walls), DoomIcons.Fill) { onClearMap(TileType.Wall) }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            DrawerItem(stringResource(R.string.editor_projects_menu), DoomIcons.Folder, onProjects)
            DrawerItem(stringResource(R.string.editor_generate_wad), DoomIcons.Download, onGenerate)
            DrawerItem(stringResource(R.string.editor_share_wad), Icons.Filled.Share, onShare)
        }
    }
}

@Composable
private fun DrawerItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        icon = { Icon(icon, contentDescription = null) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}

// ----------------------------------------------------------------------------- rename dialog

@Composable
private fun RenameDialog(state: MapEditorState, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(state.project.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_level_name)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.editor_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { state.rename(name); onDismiss() }) { Text(stringResource(R.string.editor_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) } },
    )
}

private fun nextSymmetry(m: MapGridOps.SymmetryMode): MapGridOps.SymmetryMode = when (m) {
    MapGridOps.SymmetryMode.None -> MapGridOps.SymmetryMode.Horizontal
    MapGridOps.SymmetryMode.Horizontal -> MapGridOps.SymmetryMode.Vertical
    MapGridOps.SymmetryMode.Vertical -> MapGridOps.SymmetryMode.Both
    MapGridOps.SymmetryMode.Both -> MapGridOps.SymmetryMode.None
}

@StringRes
private fun symmetryLabel(m: MapGridOps.SymmetryMode): Int = when (m) {
    MapGridOps.SymmetryMode.None -> R.string.editor_sym_off
    MapGridOps.SymmetryMode.Horizontal -> R.string.editor_sym_lr
    MapGridOps.SymmetryMode.Vertical -> R.string.editor_sym_ud
    MapGridOps.SymmetryMode.Both -> R.string.editor_sym_4way
}

/** A space-frugal text button for the cramped map-row actions (Dup / move / delete). */
@Composable
private fun CompactAction(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.widthIn(min = 36.dp),
    ) { Text(label) }
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
        title = { Text(stringResource(R.string.editor_grid_size_title)) },
        text = {
            Column {
                Text(stringResource(R.string.editor_presets), style = MaterialTheme.typography.labelLarge)
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
                Text(stringResource(R.string.editor_custom_wh, MIN_GRID, MAX_GRID), style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = customW,
                        onValueChange = { customW = it.filter(Char::isDigit).take(3) },
                        label = { Text(stringResource(R.string.editor_w)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Text("×")
                    OutlinedTextField(
                        value = customH,
                        onValueChange = { customH = it.filter(Char::isDigit).take(3) },
                        label = { Text(stringResource(R.string.editor_h)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = keepContent, onClick = { keepContent = true })
                    Text(stringResource(R.string.editor_keep_drawing))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !keepContent, onClick = { keepContent = false })
                    Text(stringResource(R.string.editor_clear))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val w = customW.toIntOrNull() ?: state.gridWidth
                val h = customH.toIntOrNull() ?: state.gridHeight
                apply(w, h)
            }) { Text(stringResource(R.string.editor_apply)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) } },
    )
}

// ------------------------------------------------------------------------ theme dialog

@Composable
private fun ThemeDialog(state: MapEditorState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_theme_title)) },
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
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.editor_close)) } },
    )
}

// -------------------------------------------------------------------------- maps sheet

@Composable
private fun MapsSheet(state: MapEditorState, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.editor_maps_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.editor_maps_help),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            state.project.maps.forEachIndexed { i, mapDoc ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val isCurrent = i == state.currentMapIndex
                    val thumb = remember(mapDoc) { renderMapBitmap(mapDoc).asImageBitmap() }
                    Image(
                        bitmap = thumb,
                        contentDescription = null,
                        filterQuality = FilterQuality.None,
                        modifier = Modifier
                            .size(40.dp)
                            .border(1.dp, Color(0xFF555555)),
                    )
                    Text(
                        "MAP%02d".format(i + 1),
                        maxLines = 1,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable { state.selectMap(i) },
                    )
                    Spacer(Modifier.weight(1f))
                    // Test-target radio (compact; the label is the section help text above).
                    RadioButton(selected = state.project.testMapIndex == i, onClick = { state.setTestMap(i) })
                    CompactAction(stringResource(R.string.editor_dup)) { state.duplicateMap(i) }
                    CompactAction("↑", enabled = i > 0) { state.moveMap(i, i - 1) }
                    CompactAction("↓", enabled = i < state.project.maps.lastIndex) { state.moveMap(i, i + 1) }
                    CompactAction("✕", enabled = state.project.maps.size > 1) { state.deleteMap(i) }
                }
                HorizontalDivider()
            }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(
                onClick = { state.addMap() },
                enabled = state.project.maps.size < com.msa.freedoom.ui.editor.model.MapProject.MAX_MAPS,
            ) { Text(stringResource(R.string.editor_add_map)) }
        }
    }
}

// ------------------------------------------------------------------------ tuning sheet

@Composable
private fun TuningSheet(state: MapEditorState, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.editor_generation_tuning), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.editor_tuning_help),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = state.project.generateThings,
                    onCheckedChange = { state.setGenerateThings(it) },
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.editor_auto_populate))
            }
            Spacer(Modifier.height(8.dp))

            DensitySlider(stringResource(R.string.editor_density_monsters), state.project.tuning.monsterDensity) { state.setMonsterDensity(it) }
            DensitySlider(stringResource(R.string.editor_density_items), state.project.tuning.itemDensity) { state.setItemDensity(it) }
            DensitySlider(stringResource(R.string.editor_density_ammo), state.project.tuning.ammoDensity) { state.setAmmoDensity(it) }

            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.editor_wad_format), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.project.format == WadFormat.DOOM2,
                    onClick = { state.setFormat(WadFormat.DOOM2) },
                    label = { Text(stringResource(R.string.editor_doom2_label)) },
                )
                FilterChip(
                    selected = state.project.format == WadFormat.DOOM1,
                    onClick = { state.setFormat(WadFormat.DOOM1) },
                    label = { Text(stringResource(R.string.editor_doom1_label)) },
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.editor_test_skill), style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SKILL_NAMES.forEachIndexed { idx, name ->
                    val level = idx + 1
                    FilterChip(
                        selected = state.project.skill == level,
                        onClick = { state.setSkill(level) },
                        label = { Text(name) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.editor_close)) }
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
private fun ProjectsSheet(state: MapEditorState, onMessage: (String) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val savedMsg = stringResource(R.string.editor_saved)
    var saveName by remember { mutableStateOf(state.project.name) }
    var projects by remember { mutableStateOf<List<ProjectSummary>>(emptyList()) }
    var pendingDelete by remember { mutableStateOf<ProjectSummary?>(null) }

    LaunchedEffect(Unit) { projects = state.listProjects() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.editor_projects_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = saveName,
                    onValueChange = { saveName = it },
                    label = { Text(stringResource(R.string.editor_save_as)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = {
                    scope.launch {
                        state.saveNamedAs(saveName)
                        projects = state.listProjects()
                        onMessage(savedMsg)
                    }
                }) { Text(stringResource(R.string.editor_save)) }
            }

            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.editor_saved_header), style = MaterialTheme.typography.labelLarge)
            if (projects.isEmpty()) {
                Text(stringResource(R.string.editor_no_saved_projects), style = MaterialTheme.typography.bodySmall)
            }
            projects.forEach { p ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.name)
                        Text(stringResource(R.string.editor_map_count, p.mapCount), style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = {
                        scope.launch { state.openProject(p.file); onDismiss() }
                    }) { Text(stringResource(R.string.editor_open)) }
                    TextButton(onClick = { pendingDelete = p }) { Text(stringResource(R.string.editor_delete)) }
                }
                HorizontalDivider()
            }
        }
    }

    pendingDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.editor_confirm_delete_project_title)) },
            text = { Text(stringResource(R.string.editor_confirm_delete_project_text, p.name)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    scope.launch { state.deleteProject(p.file); projects = state.listProjects() }
                }) { Text(stringResource(R.string.editor_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel_button)) }
            },
        )
    }
}

// Doom skill abbreviations, index 0..4 → skill 1..5.
private val SKILL_NAMES = listOf("ITYTD", "HNTR", "HMP", "UV", "NM")

// -------------------------------------------------------------------- replace-tile dialog

@Composable
private fun ReplaceTileDialog(state: MapEditorState, onDismiss: () -> Unit) {
    var from by remember { mutableStateOf(TileType.Wall) }
    var to by remember { mutableStateOf(TileType.Door) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_replace_tile_title)) },
        text = {
            Column {
                Text(stringResource(R.string.editor_replace_every), style = MaterialTheme.typography.labelLarge)
                TileSwatchRow(selected = from) { from = it }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.editor_replace_with), style = MaterialTheme.typography.labelLarge)
                TileSwatchRow(selected = to) { to = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { state.replaceTile(from, to); onDismiss() }) { Text(stringResource(R.string.editor_replace)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) } },
    )
}

@Composable
private fun TileSwatchRow(selected: TileType, onPick: (TileType) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TileType.entries.forEach { tile ->
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(tile.composeColor, RoundedCornerShape(6.dp))
                    .border(
                        width = if (tile == selected) 3.dp else 1.dp,
                        color = if (tile == selected) MaterialTheme.colorScheme.primary else Color(0xFF555555),
                        shape = RoundedCornerShape(6.dp),
                    )
                    .clickable { onPick(tile) },
            )
        }
    }
}

// ----------------------------------------------------------------------- templates dialog

@Composable
private fun TemplatesDialog(state: MapEditorState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_new_from_template_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.editor_template_help), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                MapTemplates.all.forEach { tpl ->
                    val thumb = remember(tpl.name) { renderMapBitmap(tpl.build()).asImageBitmap() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { state.addMapFromDoc(tpl.build()); onDismiss() }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Image(
                            bitmap = thumb,
                            contentDescription = null,
                            filterQuality = FilterQuality.None,
                            modifier = Modifier.size(44.dp).border(1.dp, Color(0xFF555555)),
                        )
                        Text(tpl.name)
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.editor_close)) } },
    )
}

// ---------------------------------------------------------------------- validation dialog

@Composable
private fun ValidationDialog(warnings: List<String>, onTestAnyway: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_heads_up)) },
        text = {
            Column {
                warnings.forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = { TextButton(onClick = onTestAnyway) { Text(stringResource(R.string.editor_test_anyway)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) } },
    )
}
