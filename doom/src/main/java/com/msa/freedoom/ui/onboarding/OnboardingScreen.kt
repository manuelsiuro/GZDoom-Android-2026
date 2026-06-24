package com.msa.freedoom.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import kotlinx.coroutines.launch
import com.msa.freedoom.R
import com.msa.freedoom.ui.DoomIcons

private data class OnboardingPane(
    val icon: ImageVector,
    val titleRes: Int,
    val bodyRes: Int,
)

/**
 * First-run wizard shown once (gated by the `onboarding_done` pref in MainScreen).
 * Explains what Freedoom is, how to get more content, and how the controls work, so a
 * fresh install converts into an engaged player instead of a silent first launch.
 *
 * @param onFinish called when the user finishes/skips; [browse] is true when they tapped
 *   the "browse add-ons" CTA, so the host can deep-link straight to the Browse tab.
 */
@Composable
fun OnboardingScreen(onFinish: (browse: Boolean) -> Unit) {
    val panes = listOf(
        OnboardingPane(Icons.Filled.PlayArrow, R.string.onboarding_welcome_title, R.string.onboarding_welcome_body),
        OnboardingPane(Icons.Filled.Search, R.string.onboarding_content_title, R.string.onboarding_content_body),
        OnboardingPane(DoomIcons.Gamepad, R.string.onboarding_controls_title, R.string.onboarding_controls_body),
    )
    val pagerState = rememberPagerState(pageCount = { panes.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == panes.lastIndex

    Surface(Modifier.fillMaxSize()) {
        // Shown before the main Scaffold, so it must inset itself for the status/navigation
        // bars (the activity is edge-to-edge) — otherwise the bottom button hides under them.
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onFinish(false) }) {
                    Text(stringResource(R.string.onboarding_skip))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { page ->
                val pane = panes[page]
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        pane.icon,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        stringResource(pane.titleRes),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(pane.bodyRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                panes.indices.forEach { i ->
                    val selected = i == pagerState.currentPage
                    Box(
                        Modifier
                            .padding(4.dp)
                            .size(if (selected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                    )
                }
            }

            Button(
                onClick = {
                    when {
                        // Middle pane's CTA jumps straight to Browse; otherwise advance/finish.
                        isLast -> onFinish(false)
                        pagerState.currentPage == 1 -> onFinish(true)
                        else -> scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(
                    stringResource(
                        when {
                            isLast -> R.string.onboarding_start
                            pagerState.currentPage == 1 -> R.string.onboarding_browse
                            else -> R.string.onboarding_next
                        }
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
