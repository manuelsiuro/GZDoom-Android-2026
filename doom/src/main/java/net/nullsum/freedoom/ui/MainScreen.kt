package net.nullsum.freedoom.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.fragment.compose.AndroidFragment
import com.beloko.touchcontrols.GamePadFragment
import kotlinx.coroutines.launch
import net.nullsum.freedoom.R
import net.nullsum.freedoom.ui.browse.BrowseScreen
import net.nullsum.freedoom.ui.browse.BrowseState
import net.nullsum.freedoom.ui.editor.MapEditorScreen
import net.nullsum.freedoom.ui.launch.LaunchScreen
import net.nullsum.freedoom.ui.launch.LaunchState
import net.nullsum.freedoom.ui.options.OptionsScreen

@Composable
fun MainScreen() {
    val activity = requireNotNull(LocalActivity.current)
    val launchState = remember { LaunchState(activity) }
    val pagerState = rememberPagerState(pageCount = { 5 })
    val scope = rememberCoroutineScope()
    // Hoisted here (with MainScreen's scope) so in-flight downloads survive tab swipes
    // even when the pager disposes the browse page.
    val browseState = remember { BrowseState(activity, scope) }
    val titles = listOf(
        stringResource(R.string.app_name),
        stringResource(R.string.gamepad_tab),
        stringResource(R.string.options_tab),
        stringResource(R.string.browse_tab),
        stringResource(R.string.editor_tab),
    )

    // Re-scan when returning to the launch tab (e.g. after the base dir changed in Options).
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 0 && launchState.initialized) {
            launchState.refreshGames()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                titles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title) },
                    )
                }
            }
        },
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            // Keep the gamepad fragment resident so input forwarding works from any tab:
            // page 1 is at most 2 pages away from any of the 4 pages
            // (replaces ViewPager2's offscreenPageLimit = 2).
            beyondViewportPageCount = 2,
        ) { page ->
            when (page) {
                0 -> LaunchScreen(
                    state = launchState,
                    onOpenOptions = { scope.launch { pagerState.animateScrollToPage(2) } },
                    modifier = Modifier.fillMaxSize(),
                )
                1 -> AndroidFragment<GamePadFragment>(modifier = Modifier.fillMaxSize())
                2 -> OptionsScreen(modifier = Modifier.fillMaxSize())
                3 -> BrowseScreen(state = browseState, modifier = Modifier.fillMaxSize())
                else -> MapEditorScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
