@file:OptIn(ExperimentalMaterial3Api::class)

package com.msa.freedoom.ui.editor.texture

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.msa.freedoom.ui.editor.MapEditorState
import com.msa.freedoom.ui.editor.model.TextureRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A mobile bottom sheet for assigning real IWAD textures/flats to each [TextureRole]. Role
 * chips along the top (with an override-count badge); below, a searchable grid of real
 * thumbnails decoded from the project's IWAD. Tapping a thumbnail toggles it into the role's
 * override list (multi-select). An empty override falls back to the theme default.
 */
@Composable
fun TextureBrowserSheet(state: MapEditorState, cache: TextureCache, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val iwadPath = remember(state.project.iwadFile) { state.iwadAbsolutePath() }
    var role by remember { mutableStateOf(TextureRole.Wall) }
    var query by remember { mutableStateOf("") }

    val available by produceState(initialValue = true, iwadPath) {
        value = withContext(Dispatchers.IO) {
            state.ensureIwadAvailable() // unpack a bundled IWAD if it isn't on disk yet
            cache.isAvailable(iwadPath)
        }
    }
    val names by produceState(initialValue = emptyList<String>(), iwadPath, role, available) {
        value = withContext(Dispatchers.IO) {
            if (!available) emptyList()
            else if (role.isFlat) cache.flatNames(iwadPath) else cache.wallNames(iwadPath)
        }
    }
    val selected = state.textureOverride(role).toSet()
    val filtered = remember(names, query) {
        if (query.isBlank()) names else names.filter { it.contains(query, ignoreCase = true) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("Textures", style = MaterialTheme.typography.titleLarge)
            Text(
                "Pick real textures from ${state.project.iwadFile}. Selected ones replace the " +
                    "theme default for that surface across the whole project.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))

            if (!available) {
                Text(
                    "Couldn't read ${state.project.iwadFile}. Test-launch a map once so the IWAD is " +
                        "unpacked, then reopen this.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // Role chips with override counts.
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TextureRole.entries.forEach { r ->
                    val count = state.textureOverride(r).size
                    FilterChip(
                        selected = role == r,
                        onClick = { role = r; query = "" },
                        label = { Text(if (count > 0) "${r.displayName} ($count)" else r.displayName) },
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    if (selected.isEmpty()) "Using theme default"
                    else "${selected.size} selected",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                )
                if (selected.isNotEmpty()) {
                    TextButton(onClick = { state.setTextureOverride(role, emptyList()) }) {
                        Text("Use theme default")
                    }
                }
            }

            // Search by name.
            androidx.compose.material3.OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(76.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it }) { name ->
                    TextureSwatch(
                        cache = cache,
                        iwadPath = iwadPath,
                        name = name,
                        selected = name in selected,
                        onClick = {
                            val cur = state.textureOverride(role).toMutableList()
                            if (name in cur) cur.remove(name) else cur.add(name)
                            state.setTextureOverride(role, cur)
                        },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onDismiss) { Text("Done") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TextureSwatch(
    cache: TextureCache,
    iwadPath: String,
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bmp by produceState<ImageBitmap?>(initialValue = null, iwadPath, name) {
        value = withContext(Dispatchers.Default) { cache.decode(iwadPath, name) }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(4.dp),
                )
                .background(Color(0xFF1A1A1A), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            val b = bmp
            if (b != null) {
                Image(
                    bitmap = b,
                    contentDescription = name,
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )
            }
        }
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
