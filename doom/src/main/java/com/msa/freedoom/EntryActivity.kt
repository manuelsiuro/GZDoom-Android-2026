package com.msa.freedoom

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import android.content.res.Configuration
import com.beloko.touchcontrols.GamePadFragment
import com.msa.freedoom.ui.MainScreen
import com.msa.freedoom.ui.theme.DoomTheme
import com.msa.freedoom.ui.theme.ThemeController
import com.msa.freedoom.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EntryActivity : AppCompatActivity() {

    // An inbound idgames:// VIEW intent, surfaced to Compose. The activity is
    // singleInstance, so re-launches arrive via onNewIntent, not a fresh onCreate.
    private var deeplink by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppSettings.reloadSettings(application)
        ThemeController.init(application)

        // Transparent system bars whose icon colour follows the resolved theme: light
        // (white) icons on dark, dark icons on light, so they stay legible either way.
        // The bottom NavigationBar paints its own surface behind the navigation bar so
        // the two read as one (see MainScreen's Scaffold).
        if (resolvedDark()) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            )
        } else {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            )
        }

        // App-specific external storage needs no runtime permission on modern Android.
        // Off the main thread so a slow/full SD card can't ANR cold start; the launch tab
        // re-runs createDirectories on its own IO path before it needs the dirs.
        lifecycleScope.launch(Dispatchers.IO) { AppSettings.createDirectories(application) }

        GamePadFragment.gamepadActions = Utils.getGameGamepadConfig(resources)

        deeplink = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data

        setContent {
            DoomTheme {
                MainScreen(
                    deeplink = deeplink,
                    onDeeplinkConsumed = { deeplink = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_VIEW) deeplink = intent.data
    }


    /** Whether the chosen theme resolves to a dark scheme at startup (drives system-bar icons). */
    private fun resolvedDark(): Boolean {
        val systemDark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        return when (ThemeController.mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            // DYNAMIC and SYSTEM both pick their dark/light variant from the OS setting.
            ThemeMode.SYSTEM, ThemeMode.DYNAMIC -> systemDark
        }
    }

    private fun gamePadFragment(): GamePadFragment? =
        supportFragmentManager.fragments.filterIsInstance<GamePadFragment>().firstOrNull()

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val frag = gamePadFragment()
        return frag?.onGenericMotionEvent(event) ?: super.onGenericMotionEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val frag = gamePadFragment()
        return if (frag != null && frag.onKeyDown(keyCode, event)) true else super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val frag = gamePadFragment()
        return if (frag != null && frag.onKeyUp(keyCode, event)) true else super.onKeyUp(keyCode, event)
    }
}
