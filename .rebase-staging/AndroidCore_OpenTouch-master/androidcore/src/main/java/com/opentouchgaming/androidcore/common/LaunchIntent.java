package com.opentouchgaming.androidcore.common;

import android.content.Context;
import android.content.Intent;

import androidx.core.util.Pair;

import com.opentouchgaming.androidcore.AppInfo;
import com.opentouchgaming.androidcore.EngineOptionsInterface;
import com.opentouchgaming.androidcore.ui.OptionsDialogKt;
import com.opentouchgaming.androidcore.ui.widgets.SpinnerWidget;
import com.opentouchgaming.androidcore.ui.widgets.SwitchWidget;

public class LaunchIntent
{

    public static void populateIntent(Context ctx, Intent intent, EngineOptionsInterface engineOptions, EngineOptionsInterface.RunInfo runInfo, int gameType,
                                      int weaponWheel, Pair<String, String> quickCommandPaths)
    {
        // Paths
        intent.putExtra("app", AppInfo.app.name());
        intent.putExtra("user_files", AppInfo.getUserFiles());
        intent.putExtra("res_files", AppInfo.getResFiles());
        intent.putExtra("secondary_path", AppInfo.getAppSecDirectory());

        // Graphics
        intent.putExtra("res_div_float", OptionsDialogKt.Companion.GetResolutionScale(ctx));
        intent.putExtra("gles_version", runInfo.glesVersion);
        intent.putExtra("use_gl4es", runInfo.useGL4ES);
        intent.putExtra("framebuffer_width", runInfo.frameBufferWidth);
        intent.putExtra("framebuffer_height", runInfo.frameBufferHeight);
        intent.putExtra("framebuffer_maintain_aspect", runInfo.maintainAspect);
        intent.putExtra("touch_surfaceview", runInfo.touchSurfaceview);

        // Audio
        int audioDefault = SpinnerWidget.fetchValue(ctx, OptionsDialogKt.SDL_AUDIO_BACKEND, 0); // For SDL
        int audioFreq = engineOptions.audioOverrideFreq();
        int audioSamples = engineOptions.audioOverrideSamples();
        int SDLMidiPlayer = engineOptions.SDLMidiBackend();
        int openALBackend = SpinnerWidget.fetchValue(ctx, OptionsDialogKt.OPENAL_AUDIO_BACKEND, 0); // Default OpenSL

        intent.putExtra("audio_backend", audioDefault);
        intent.putExtra("audio_freq", audioFreq);
        intent.putExtra("audio_samples", audioSamples);
        intent.putExtra("sdl_midi_player", SDLMidiPlayer);
        intent.putExtra("openal_audio_backend", openALBackend);

        // Gamepad config override (null means use the global setting)
        if (runInfo.gamepadConfig != null)
            intent.putExtra("engine_gamepad_config", runInfo.gamepadConfig);

        // Other
        intent.putExtra("game_type", gameType);
        intent.putExtra("wheel_nbr", weaponWheel);
        intent.putExtra("quick_command_main_path", quickCommandPaths.first);
        intent.putExtra("quick_command_mod_path", quickCommandPaths.second);

        // Mouse axis options
        boolean swapMouseXY = SwitchWidget.fetchValue(ctx, OptionsDialogKt.SWAP_MOUSE_XY, false);
        intent.putExtra("swap_mouse_xy", swapMouseXY);
        intent.putExtra("invert_mouse_x", swapMouseXY && SwitchWidget.fetchValue(ctx, OptionsDialogKt.INVERT_MOUSE_X, false));
        intent.putExtra("invert_mouse_y", swapMouseXY && SwitchWidget.fetchValue(ctx, OptionsDialogKt.INVERT_MOUSE_Y, false));
    }
}
