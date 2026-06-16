package net.nullsum.freedoom

import android.content.res.Configuration
import android.os.Bundle
import org.libsdl.app2012.SDLActivity

/**
 * The gameplay activity. Since the rebase onto emileb's GZDoom 4.15 mobile
 * port this is a thin subclass of the vendored SDL2 SDLActivity: SDL owns the
 * surface and EGL context, and the engine main loop runs on the SDL thread
 * (SDLOpenTouch.RunApplication -> NativeLib.init, which never returns).
 *
 * Launch contract from the Compose launcher (unchanged): Intent extras
 * "args" (engine command line), "game_path" (Freedoom base dir),
 * "res_div" (resolution divider), "game" (selected IWAD index).
 */
class Game : SDLActivity() {

    override fun getLibraries(): Array<String> = arrayOf(
        "hidapi",
        "saffal",
        "openal",
        "zmusic_uz",
        "touchcontrols",
        "SDL2",
        "uzdoom",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Utils.setImmersionMode(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Utils.onWindowFocusChanged(this, hasFocus)
    }

    override fun onDestroy() {
        super.onDestroy()
        // The engine cannot re-initialise in the same process (static state,
        // and NativeLib.init never returns); kill the process so the next
        // launch starts clean.
        System.exit(0)
    }
}
