package com.msa.freedoom.ui.editor.things

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.msa.freedoom.ui.editor.MapEditorState
import com.msa.freedoom.ui.editor.model.ThingCatalog
import com.msa.freedoom.ui.editor.model.ThingCategory
import com.msa.freedoom.ui.editor.model.ThingType
import com.msa.freedoom.ui.editor.texture.TextureCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * The thing palette shown below the canvas while the Thing tool is active: category chips and a
 * row of selectable thing types with real sprite thumbnails from the IWAD (falling back to a
 * coloured placeholder). The chosen type is placed by tapping the canvas.
 */
@Composable
fun ThingPaletteStrip(state: MapEditorState, cache: TextureCache, modifier: Modifier = Modifier) {
    val iwadPath = remember(state.project.iwadFile) { state.iwadAbsolutePath() }
    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(ThingCategory.entries.toList()) { cat ->
                FilterChip(
                    selected = state.thingCategory == cat,
                    onClick = { state.thingCategory = cat },
                    label = { Text(cat.displayName) },
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        val types = ThingCatalog.byCategory[state.thingCategory].orEmpty()
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(types, key = { it.id }) { type ->
                ThingSwatch(
                    type = type,
                    selected = state.selectedThingType.id == type.id,
                    cache = cache,
                    iwadPath = iwadPath,
                    onClick = { state.pickThingType(type) },
                )
            }
        }
    }
}

@Composable
private fun ThingSwatch(
    type: ThingType,
    selected: Boolean,
    cache: TextureCache,
    iwadPath: String,
    onClick: () -> Unit,
) {
    val bmp by produceState<ImageBitmap?>(initialValue = null, iwadPath, type.id) {
        value = withContext(Dispatchers.Default) {
            if (type.spriteCandidates.isEmpty()) null else cache.decodeFirst(iwadPath, type.spriteCandidates)
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(56.dp).clickable(onClick = onClick).padding(2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(6.dp),
                )
                .background(Color(0xFF1A1A1A), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            val b = bmp
            if (b != null) {
                Image(bitmap = b, contentDescription = type.displayName, filterQuality = FilterQuality.None)
            } else {
                Text(
                    type.displayName.take(2).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                )
            }
        }
        Text(
            type.displayName,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// Doom thing flag bits.
private const val FLAG_EASY = 1   // skills 1-2
private const val FLAG_MED = 2    // skill 3
private const val FLAG_HARD = 4   // skills 4-5
private const val FLAG_AMBUSH = 8 // deaf until it sees the player

/**
 * Inspector for the currently-selected placed thing: its name, an angle slider (Doom degrees,
 * 45° steps), skill + ambush flag chips, and a delete button. Shown only when a thing is selected.
 */
@Composable
fun ThingInspector(state: MapEditorState, modifier: Modifier = Modifier) {
    val idx = state.selectedThingIndex ?: return
    val thing = state.currentMap.things.getOrNull(idx) ?: return
    val name = ThingCatalog.byId(thing.type)?.displayName ?: "Thing ${thing.type}"

    fun toggle(bit: Int) = state.setSelectedThingFlags(thing.flags xor bit)

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f), maxLines = 1)
            AssistChip(onClick = { state.clearThingSelection() }, label = { Text("Done") })
            TextButton(onClick = { state.deleteSelectedThing() }) { Text("Delete") }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Angle ${thing.angle}°", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(84.dp))
            Slider(
                value = thing.angle.toFloat(),
                onValueChange = { state.setSelectedThingAngle((it / 45f).roundToInt() * 45) },
                valueRange = 0f..315f,
                steps = 7, // 0,45,…,315
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(thing.flags and FLAG_EASY != 0, { toggle(FLAG_EASY) }, label = { Text("Easy") })
            FilterChip(thing.flags and FLAG_MED != 0, { toggle(FLAG_MED) }, label = { Text("Med") })
            FilterChip(thing.flags and FLAG_HARD != 0, { toggle(FLAG_HARD) }, label = { Text("Hard") })
            FilterChip(thing.flags and FLAG_AMBUSH != 0, { toggle(FLAG_AMBUSH) }, label = { Text("Ambush") })
        }
    }
}
