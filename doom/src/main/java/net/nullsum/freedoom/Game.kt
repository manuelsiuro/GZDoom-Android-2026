@file:Suppress("DEPRECATION", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")

/*
 * Copyright (C) 2009 jeyries@yahoo.fr
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.nullsum.freedoom

import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Window
import android.view.WindowManager
import com.bda.controller.Controller
import com.bda.controller.ControllerListener
import com.bda.controller.StateEvent
import com.beloko.libsdl.SDLLib
import com.beloko.touchcontrols.ControlInterpreter
import com.beloko.touchcontrols.MogaHack
import com.beloko.touchcontrols.ShowKeyboard
import com.beloko.touchcontrols.TouchControlsEditing
import com.beloko.touchcontrols.TouchControlsSettings
import com.beloko.touchcontrols.TouchSettings
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Game : Activity(), Handler.Callback {
    private val mogaListener = MogaControllerListener()
    private val LOG = "Game"
    private var mogaController: Controller? = null
    private lateinit var act: Activity
    private var surfaceWidth = -1
    private var surfaceHeight = 0
    private var resDiv = 1
    private lateinit var controlInterp: ControlInterpreter
    private var args: String? = null
    private var gamePath: String? = null
    private var setupLaunch = false //True if the native setup program launched this
    private var mGLSurfaceView: GameView? = null
    private val mRenderer = QuakeRenderer()
    private lateinit var handlerUI: Handler

    /**
     * Called when the activity is first created.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        act = this

        handlerUI = Handler(this)

        AppSettings.reloadSettings(application)

        args = intent.getStringExtra("args")
        gamePath = intent.getStringExtra("game_path")
        setupLaunch = intent.getBooleanExtra("setup_launch", false)
        resDiv = intent.getIntExtra("res_div", 1)

        val controller = Controller.getInstance(this)
        mogaController = controller
        MogaHack.init(controller, this)
        controller.setListener(mogaListener, Handler())

        // fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // keep screen on
        window.setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        Utils.setImmersionMode(this)

        startGame()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Utils.onWindowFocusChanged(this, hasFocus)
    }

    fun startGame() {
        NativeLib.loadLibraries()

        val engine = NativeLib()

        controlInterp = ControlInterpreter(
            engine,
            Utils.getGameGamepadConfig(this.resources),
            TouchSettings.gamePadControlsFile,
            TouchSettings.gamePadEnabled
        )

        TouchControlsSettings.setup(act, engine)
        TouchControlsSettings.loadSettings(act)
        TouchControlsSettings.sendToQuake()

        TouchControlsEditing.setup(act)

        val glView = GameView(this)
        mGLSurfaceView = glView

        NativeLib.gv = glView

        ShowKeyboard.setup(act, glView)

        glView.setEGLConfigChooser(BestEglChooser(applicationContext))

        glView.setRenderer(mRenderer)

        // This will keep the screen on, while your view is visible.
        glView.keepScreenOn = true

        setContentView(glView)
        glView.requestFocus()
        glView.isFocusableInTouchMode = true
    }

    override fun onPause() {
        Log.i(LOG, "onPause")
        SDLLib.nativePause()
        SDLLib.onPause()
        mogaController?.onPause()
        super.onPause()
    }

    override fun onResume() {
        Log.i(LOG, "onResume")
        SDLLib.nativeResume()
        SDLLib.onResume()
        mogaController?.onResume()
        super.onResume()
        mGLSurfaceView?.onResume()
    }

    override fun onDestroy() {
        Log.i(LOG, "onDestroy")
        super.onDestroy()
        mogaController?.exit()
        System.exit(0)
    }

    override fun handleMessage(msg: Message): Boolean {
        // TODO Auto-generated method stub
        return false
    }

    inner class MogaControllerListener : ControllerListener {
        override fun onKeyEvent(event: com.bda.controller.KeyEvent) {
            //Log.d(LOG, "onKeyEvent " + event.getKeyCode());
            controlInterp.onMogaKeyEvent(
                event,
                mogaController!!.getState(Controller.STATE_CURRENT_PRODUCT_VERSION)
            )
        }

        override fun onMotionEvent(event: com.bda.controller.MotionEvent) {
            controlInterp.onGenericMotionEvent(event)
        }

        override fun onStateEvent(event: StateEvent) {
            Log.d(LOG, "onStateEvent " + event.state)
        }
    }

    ///////////// GLSurfaceView.Renderer implementation ///////////

    inner class GameView(context: Context) : MyGLSurfaceView(context) {

        /*--------------------
         * Event handling
         *--------------------*/

        override fun onGenericMotionEvent(event: MotionEvent): Boolean {
            return controlInterp.onGenericMotionEvent(event)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            return controlInterp.onTouchEvent(event)
        }

        override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
            return controlInterp.onKeyDown(keyCode, event)
        }

        override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
            return controlInterp.onKeyUp(keyCode, event)
        }
    } // end of QuakeView

    inner class QuakeRenderer : MyGLSurfaceView.Renderer {

        private var divDone = false

        //// new Renderer interface
        private var notifiedflags = 0
        private var SDLinited = false

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            Log.d("Renderer", "onSurfaceCreated")
        }

        private fun init(width: Int, height: Int) {
            Log.i(LOG, "screen size : " + width + "x" + height)

            NativeLib.setScreenSize(width, height)

            Utils.copyPNGAssets(applicationContext, AppSettings.graphicsDir)

            Log.i(LOG, "Quake2Init start")

            //args = "-width 1280 -height 736 +set vid_renderer 1 -iwad tnt.wad -file brutal19.pk3 +set fluid_patchset /sdcard/WeedsGM3.sf2";
            //args = "+set vid_renderer 1 ";
            val gzdoomArgs =
                "-width " + surfaceWidth / resDiv + " -height " + surfaceHeight / resDiv + " +set vid_renderer 1 "
            val argsArray = Utils.createArgs(args + gzdoomArgs)

            var audioSample = AudioTrack.getNativeOutputSampleRate(AudioTrack.MODE_STREAM)
            Log.d(LOG, "audioSample = $audioSample")

            if (audioSample != 48000 && audioSample != 44100) //Just in case
                audioSample = 48000

            val ret = NativeLib.init(AppSettings.graphicsDir, audioSample, argsArray, 0, gamePath!!)

            Log.i(LOG, "Quake2Init done")
        }

        override fun onDrawFrame(gl: GL10?) {
            Log.d("Renderer", "onDrawFrame")

            if (!divDone) {
                handlerUI.post {
                    mGLSurfaceView!!.holder.setFixedSize(surfaceWidth / resDiv, surfaceHeight / resDiv)
                    divDone = true
                }
            }

            if (divDone) {
                init(surfaceWidth / resDiv, surfaceHeight / resDiv)
            } else {
                try {
                    Thread.sleep(200)
                } catch (e: InterruptedException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                }
            }

            Log.d("Renderer", "onDrawFrame END")
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            Log.d("Renderer", String.format("onSurfaceChanged %dx%d", width, height))

            if (surfaceWidth == -1) {
                surfaceWidth = width
                surfaceHeight = height
            }

            if (!SDLinited) {
                SDLLib.nativeInit(false)
                SDLLib.surfaceChanged(PixelFormat.RGBA_8888, surfaceWidth / resDiv, surfaceHeight / resDiv)
                SDLinited = true
            }

            //Display display = ((WindowManager) act.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            //Point size = new Point();
            //display.getSize(size);
            controlInterp.setScreenSize(surfaceWidth, surfaceHeight)

            //controlInterp.setScreenSize(width, height);
        }
    } // end of QuakeRenderer
}
