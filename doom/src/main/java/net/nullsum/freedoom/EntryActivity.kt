package net.nullsum.freedoom

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
import com.beloko.touchcontrols.GamePadFragment
import net.nullsum.freedoom.ui.MainScreen
import net.nullsum.freedoom.ui.theme.DoomTheme

class EntryActivity : AppCompatActivity() {

    // An inbound idgames:// VIEW intent, surfaced to Compose. The activity is
    // singleInstance, so re-launches arrive via onNewIntent, not a fresh onCreate.
    private var deeplink by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Transparent system bars with light (white) icons — the app is always dark-themed.
        // The bottom NavigationBar paints its own surface behind the navigation bar so the
        // two read as one (see MainScreen's Scaffold).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        AppSettings.reloadSettings(application)
        // App-specific external storage needs no runtime permission on modern Android.
        AppSettings.createDirectories(this)

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
