package com.msa.freedoom.ui.stats

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.util.concurrent.TimeUnit
import com.msa.freedoom.R

/** Local play statistics (no account, no network). Read once when the screen opens. */
@Composable
fun StatsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val stats = remember { StatsStore.read(context) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatRow(stringResource(R.string.stat_total_playtime), formatDuration(context, stats.totalPlayMillis))
        StatRow(stringResource(R.string.stat_launch_count), stats.launchCount.toString())
        StatRow(
            stringResource(R.string.stat_last_played),
            if (stats.lastPlayed <= 0L) stringResource(R.string.stat_never)
            else DateUtils.getRelativeTimeSpanString(stats.lastPlayed).toString(),
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun formatDuration(context: android.content.Context, millis: Long): String {
    if (millis <= 0L) return context.getString(R.string.stat_no_time)
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    return when {
        hours > 0 -> context.getString(R.string.stat_hours_minutes, hours, minutes)
        else -> context.getString(R.string.stat_minutes, minutes)
    }
}
