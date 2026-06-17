package net.nullsum.freedoom.ui.launch

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shared compatibility-badge helpers, used by both the launch-side mod picker and the
 * Browse screen so a WAD's game/slot reads the same everywhere. The pure [badgeText]/
 * [verdictRank] live here too (next to [GameCompat]) so non-Compose callers can reuse them.
 */

/** Short human descriptor, e.g. "Doom · MAPxx" or "Heretic"; empty when the family is unknown. */
fun badgeText(info: AddonInfo): String {
    val family = when (info.family) {
        GameFamily.DOOM -> "Doom"
        GameFamily.HERETIC -> "Heretic"
        GameFamily.HEXEN -> "Hexen"
        GameFamily.STRIFE -> "Strife"
        GameFamily.CHEX -> "Chex"
        GameFamily.UNKNOWN -> return ""
    }
    val slot = when (info.slot) {
        MapSlot.EPISODIC -> "ExMy"
        MapSlot.MAPXX -> "MAPxx"
        MapSlot.BOTH -> "ExMy+MAPxx"
        MapSlot.NONE -> ""
    }
    return if (slot.isEmpty()) family else "$family · $slot"
}

/** Sort key: most-compatible first, incompatible last. */
fun verdictRank(v: Verdict): Int = when (v) {
    Verdict.COMPATIBLE -> 0
    Verdict.MINOR -> 1
    Verdict.UNKNOWN -> 2
    Verdict.INCOMPATIBLE -> 3
}

@Composable
fun verdictColor(v: Verdict): Color = when (v) {
    Verdict.COMPATIBLE -> MaterialTheme.colorScheme.tertiary
    Verdict.INCOMPATIBLE -> MaterialTheme.colorScheme.error
    Verdict.MINOR, Verdict.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
}

/**
 * A small rounded compatibility pill. In the mod picker [verdict] colors it green/red
 * against the selected IWAD; in Browse there is no selected IWAD so callers pass
 * [Verdict.UNKNOWN] for a neutral, descriptive badge. [likely] marks a pre-download
 * guess (dir/filename) versus a definitive post-install lump scan.
 */
@Composable
fun CompatBadge(
    text: String,
    verdict: Verdict,
    likely: Boolean,
    modifier: Modifier = Modifier,
) {
    if (text.isEmpty()) return
    val color = verdictColor(verdict)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f),
        contentColor = color,
    ) {
        Text(
            text = if (likely) "~ $text" else text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
