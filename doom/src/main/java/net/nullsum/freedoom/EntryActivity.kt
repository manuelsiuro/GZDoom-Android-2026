package net.nullsum.freedoom

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.beloko.touchcontrols.GamePadFragment
import net.nullsum.freedoom.ui.MainScreen
import net.nullsum.freedoom.ui.theme.DoomTheme

class EntryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        AppSettings.reloadSettings(application)
        // App-specific external storage needs no runtime permission on modern Android.
        AppSettings.createDirectories(this)

        GamePadFragment.gamepadActions = Utils.getGameGamepadConfig(resources)

        setContent {
            DoomTheme {
                MainScreen()
            }
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
