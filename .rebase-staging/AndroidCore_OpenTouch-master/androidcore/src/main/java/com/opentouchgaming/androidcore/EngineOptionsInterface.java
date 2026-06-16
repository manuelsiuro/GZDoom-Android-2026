package com.opentouchgaming.androidcore;

import android.app.Activity;

import androidx.arch.core.util.Function;


/**
 * Created by Emile on 10/12/2017.
 */

public interface EngineOptionsInterface
{
    void showDialog(final Activity act, GameEngine engine, int version, Function<Integer, Void> update);

    RunInfo getRunInfo(int version);

    boolean hasMultiplayer();

    void launchMultiplayer(Activity ac, GameEngine engine, int version, String mainArgs, MultiplayerCallback callback);

    default int audioOverrideFreq()
    {
        return 0;
    }

    default int audioOverrideSamples()
    {
        return 0;
    }

    default int audioOverrideBackend()
    {
        return -1;
    }

    default int SDLMidiBackend()
    {
        return 0;
    } // 0 = Timidity, 1 = Fluidsynth

    interface MultiplayerCallback
    {
        void launch(String mpArgs);
    }

    class RunInfo
    {
        public String args;
        public int glesVersion; //1 = GLES1, 2 = GLES 2, 3 = GLES 3, 4 = Vulkan
        public boolean useGL4ES;
        public boolean touchSurfaceview;

        public boolean maintainAspect;
        public String frameBufferWidth;
        public String frameBufferHeight;

        /** SDL major version to use for this engine. 2 = app2012 (SDL2), 3 = app3000 (SDL3). */
        public int sdlVersion = 2;

        /** Override gamepad config filename for this engine. Null means use the global setting. */
        public String gamepadConfig = null;
    }
}
