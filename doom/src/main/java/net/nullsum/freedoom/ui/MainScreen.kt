@file:OptIn(ExperimentalMaterial3Api::class)

package net.nullsum.freedoom.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.fragment.compose.AndroidFragment
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.beloko.touchcontrols.GamePadFragment
import net.nullsum.freedoom.R
import net.nullsum.freedoom.ui.browse.BrowseMode
import net.nullsum.freedoom.ui.browse.BrowseScreen
import net.nullsum.freedoom.ui.browse.BrowseState
import net.nullsum.freedoom.ui.editor.MapEditorScreen
import net.nullsum.freedoom.ui.editor.MapEditorState
import net.nullsum.freedoom.ui.launch.LaunchScreen
import net.nullsum.freedoom.ui.launch.LaunchState
import net.nullsum.freedoom.ui.options.OptionsScreen
import net.nullsum.freedoom.ui.settings.AboutScreen
import net.nullsum.freedoom.ui.settings.SettingsScreen

private object Routes {
    const val PLAY = "play"
    const val BROWSE = "browse"
    const val EDITOR = "editor"
    const val SETTINGS = "settings"
    const val SETTINGS_GAMEPAD = "settings/gamepad"
    const val SETTINGS_OPTIONS = "settings/options"
    const val SETTINGS_ABOUT = "settings/about"
}

private data class BottomDestination(
    val route: String,
    val icon: ImageVector,
    val labelRes: Int,
)

@Composable
fun MainScreen(
    deeplink: android.net.Uri? = null,
    onDeeplinkConsumed: () -> Unit = {},
) {
    val activity = requireNotNull(LocalActivity.current)
    val launchState = remember { LaunchState(activity) }
    val scope = rememberCoroutineScope()
    // Hoisted here (above the NavHost) so in-flight downloads survive navigating away from
    // the browse destination, which the NavHost disposes when it leaves the screen.
    val browseState = remember { BrowseState(activity, scope) }
    // Hoisted likewise so the in-progress drawing + project survive leaving the editor
    // destination. Process-death recovery is handled by the screen's own disk restore.
    val editorState = remember { MapEditorState(activity, scope) }

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    // Re-scan when returning to the PLAY destination (e.g. after the base dir changed in
    // Options). LaunchScreen's own one-shot initialize() won't re-run on a kept back-stack
    // entry, so this MainScreen-level effect restores the legacy rescan-on-return behavior.
    LaunchedEffect(currentRoute) {
        if (currentRoute == Routes.PLAY && launchState.initialized) {
            launchState.refreshGames()
        }
    }

    // An inbound idgames:// link: hand it to BrowseState and switch to the Browse tab.
    // BrowseScreen resolves it once its catalogs/API are ready and opens the detail sheet.
    LaunchedEffect(deeplink) {
        val uri = deeplink ?: return@LaunchedEffect
        browseState.onDeeplink(uri)
        browseState.mode = BrowseMode.SEARCH
        navController.navigate(Routes.BROWSE) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        onDeeplinkConsumed()
    }

    val bottomDestinations = listOf(
        BottomDestination(Routes.PLAY, Icons.Filled.PlayArrow, R.string.play_tab),
        BottomDestination(Routes.BROWSE, Icons.Filled.Search, R.string.browse_tab),
        BottomDestination(Routes.EDITOR, Icons.Filled.Edit, R.string.editor_tab),
        BottomDestination(Routes.SETTINGS, Icons.Filled.Settings, R.string.settings_tab),
    )

    Scaffold(
        // Edge-to-edge: content respects the safe area, but the NavigationBar paints its
        // surface behind the system navigation bar so the two share one color.
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar {
                bottomDestinations.forEach { dest ->
                    // The Settings tab stays selected while drilled into its sub-pages.
                    val selected = currentRoute == dest.route ||
                        (dest.route == Routes.SETTINGS && currentRoute?.startsWith(Routes.SETTINGS) == true)
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != dest.route) {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = null) },
                        label = { Text(stringResource(dest.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.PLAY,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(Routes.PLAY) {
                LaunchScreen(
                    state = launchState,
                    onOpenOptions = { navController.navigate(Routes.SETTINGS_OPTIONS) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(Routes.BROWSE) {
                BrowseScreen(state = browseState, modifier = Modifier.fillMaxSize())
            }
            composable(Routes.EDITOR) {
                MapEditorScreen(state = editorState, modifier = Modifier.fillMaxSize())
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenGamepad = { navController.navigate(Routes.SETTINGS_GAMEPAD) },
                    onOpenOptions = { navController.navigate(Routes.SETTINGS_OPTIONS) },
                    onOpenAbout = { navController.navigate(Routes.SETTINGS_ABOUT) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(Routes.SETTINGS_GAMEPAD) {
                SubPage(
                    title = stringResource(R.string.gamepad_tab),
                    onBack = { navController.popBackStack() },
                ) { padding ->
                    AndroidFragment<GamePadFragment>(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    )
                }
            }
            composable(Routes.SETTINGS_OPTIONS) {
                SubPage(
                    title = stringResource(R.string.options_tab),
                    onBack = { navController.popBackStack() },
                ) { padding ->
                    OptionsScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    )
                }
            }
            composable(Routes.SETTINGS_ABOUT) {
                SubPage(
                    title = stringResource(R.string.about_tab),
                    onBack = { navController.popBackStack() },
                ) { padding ->
                    AboutScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    )
                }
            }
        }
    }
}

/** Full-screen Settings sub-page chrome: a top bar with a back arrow over [content]. */
@Composable
private fun SubPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(
        // The parent Scaffold already insets this for the status bar via its innerPadding,
        // so this nested Scaffold must not add it again.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        content = content,
    )
}
