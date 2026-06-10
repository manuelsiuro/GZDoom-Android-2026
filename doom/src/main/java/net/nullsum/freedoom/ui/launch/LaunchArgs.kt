package net.nullsum.freedoom.ui.launch

/** A selectable IWAD in the game-data directory. */
data class WadEntry(val file: String, val sizeBytes: Long) {
    val iwadArgs: String get() = "-iwad $file"
}

/** An add-on selected in the mod picker, as a path relative to the game-data directory. */
data class ModEntry(val relPath: String) {
    val name: String get() = relPath.substringAfterLast('/')
    val isFolder: Boolean get() = !relPath.substringAfterLast('/').contains('.')
    // Same case-sensitive check as the legacy ModSelectDialog.getResult().
    val isPatch: Boolean get() = relPath.endsWith(".deh") || relPath.endsWith(".bex")
}

/** Builds the add-on portion of the command line, matching ModSelectDialog.getResult(). */
fun buildModArgs(mods: List<ModEntry>): String = buildString {
    for (mod in mods) {
        append(if (mod.isPatch) "-deh " else "-file ")
        append(mod.relPath)
        append(" ")
    }
}

/**
 * Assembles the full engine command line. Must stay byte-identical to the legacy
 * LaunchFragmentGZdoom.startGame() concatenation, where the args EditText held
 * [modArgs][extraArgs] back to back.
 */
fun buildLaunchArgs(iwadArgs: String, modArgs: String, extraArgs: String, baseDir: String): String =
    "$iwadArgs $modArgs$extraArgs -savedir $baseDir/gzdoom_saves +set fluid_patchset gzdoom.sf2 +set midi_dmxgus 0 "
