package com.msa.freedoom.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.msa.freedoom.BuildConfig
import com.msa.freedoom.R
import com.msa.freedoom.ui.DoomIcons

private const val URL_GITHUB_PROFILE = "https://github.com/manuelsiuro"
private const val URL_PROJECT_REPO = "https://github.com/manuelsiuro/GZDoom-Android-2026"
private const val URL_ENGINE = "https://github.com/emileb/gzdoom"
private const val URL_FREEDOOM = "https://freedoom.github.io/"

/** About screen: app/engine details, the developer, and credit links. */
@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // --- header ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.launch),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.about_app_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(
                    R.string.about_version_value,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                BuildConfig.APPLICATION_ID,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        HorizontalDivider()

        // --- app / engine ---
        SectionHeader(stringResource(R.string.about_section_app))
        LinkRow(
            icon = DoomIcons.Gamepad,
            title = stringResource(R.string.about_engine),
            subtitle = stringResource(R.string.about_engine_version) + " · " +
                stringResource(R.string.about_engine_credit_sub),
            onClick = { uriHandler.openUri(URL_ENGINE) },
        )
        HorizontalDivider()

        // --- developer ---
        SectionHeader(stringResource(R.string.about_section_developer))
        ListItem(
            leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
            headlineContent = { Text(stringResource(R.string.about_developer_name)) },
        )
        LinkRow(
            icon = DoomIcons.Star,
            title = stringResource(R.string.about_github_profile),
            subtitle = URL_GITHUB_PROFILE,
            onClick = { uriHandler.openUri(URL_GITHUB_PROFILE) },
        )
        LinkRow(
            icon = DoomIcons.Download,
            title = stringResource(R.string.about_project_repo),
            subtitle = URL_PROJECT_REPO,
            onClick = { uriHandler.openUri(URL_PROJECT_REPO) },
        )
        HorizontalDivider()

        // --- credits ---
        SectionHeader(stringResource(R.string.about_section_credits))
        LinkRow(
            icon = DoomIcons.Extension,
            title = stringResource(R.string.about_freedoom),
            subtitle = stringResource(R.string.about_freedoom_sub),
            onClick = { uriHandler.openUri(URL_FREEDOOM) },
        )
        HorizontalDivider()
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.tertiary,
    )
}

@Composable
private fun LinkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = {
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
    )
}
