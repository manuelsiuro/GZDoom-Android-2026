package com.msa.freedoom.ui.stats

import android.content.Context
import com.msa.freedoom.AppSettings

/** Aggregate, account-free play statistics shown on the Stats screen. */
data class PlayStats(
    val totalPlayMillis: Long,
    val launchCount: Int,
    val lastPlayed: Long,
)

/**
 * Local, no-account play tracking. A session is bracketed by [recordLaunch] (when the
 * engine activity starts) and [finalizeSession] (when the user returns to the launcher).
 * Playtime is approximate — the engine runs in a separate process we can't precisely
 * observe — so a single session is capped to keep a backgrounded/abandoned game from
 * inflating the total.
 */
object StatsStore {
    private const val KEY_TOTAL = "stats_total_play_ms"
    private const val KEY_COUNT = "stats_launch_count"
    private const val KEY_LAST = "stats_last_played"
    private const val KEY_PENDING_START = "stats_pending_start"

    /** Upper bound credited for one session (12h); guards against process-kill skew. */
    private const val SESSION_CAP_MS = 12L * 60 * 60 * 1000

    fun recordLaunch(ctx: Context) {
        val now = System.currentTimeMillis()
        AppSettings.setIntOption(ctx, KEY_COUNT, AppSettings.getIntOption(ctx, KEY_COUNT, 0) + 1)
        AppSettings.setLongOption(ctx, KEY_LAST, now)
        AppSettings.setLongOption(ctx, KEY_PENDING_START, now)
    }

    /** Credits the elapsed time of an open session (if any) to the total. Idempotent. */
    fun finalizeSession(ctx: Context) {
        val start = AppSettings.getLongOption(ctx, KEY_PENDING_START, 0L)
        if (start <= 0L) return
        val elapsed = (System.currentTimeMillis() - start).coerceIn(0L, SESSION_CAP_MS)
        AppSettings.setLongOption(ctx, KEY_TOTAL, AppSettings.getLongOption(ctx, KEY_TOTAL, 0L) + elapsed)
        AppSettings.setLongOption(ctx, KEY_PENDING_START, 0L)
    }

    fun read(ctx: Context): PlayStats = PlayStats(
        totalPlayMillis = AppSettings.getLongOption(ctx, KEY_TOTAL, 0L),
        launchCount = AppSettings.getIntOption(ctx, KEY_COUNT, 0),
        lastPlayed = AppSettings.getLongOption(ctx, KEY_LAST, 0L),
    )
}
