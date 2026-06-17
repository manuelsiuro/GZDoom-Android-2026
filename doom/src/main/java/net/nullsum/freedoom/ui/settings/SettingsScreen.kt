package net.nullsum.freedoom.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import net.nullsum.freedoom.R
import net.nullsum.freedoom.ui.DoomIcons

/**
 * The Settings destination: a tappable list of sub-screens. Each row drills into a
 * full-screen sub-page (see the nested settings routes in MainScreen). Add more rows here as
 * settings categories grow.
 */
@Composable
fun SettingsScreen(
    onOpenGamepad: () -> Unit,
    onOpenOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsRow(
            icon = DoomIcons.Gamepad,
            title = stringResource(R.string.gamepad_tab),
            onClick = onOpenGamepad,
        )
        HorizontalDivider()
        SettingsRow(
            icon = Icons.Filled.Settings,
            title = stringResource(R.string.options_tab),
            onClick = onOpenOptions,
        )
        HorizontalDivider()
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        trailingContent = {
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null)
        },
    )
}
