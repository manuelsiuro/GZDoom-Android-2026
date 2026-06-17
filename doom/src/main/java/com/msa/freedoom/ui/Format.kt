package com.msa.freedoom.ui

import java.util.Locale

/** Human-readable byte size (MB / KB / B), US-formatted. Shared by the launch and browse UIs. */
fun formatFileSize(bytes: Long): String = when {
    bytes >= 1 shl 20 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1 shl 10 -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}
