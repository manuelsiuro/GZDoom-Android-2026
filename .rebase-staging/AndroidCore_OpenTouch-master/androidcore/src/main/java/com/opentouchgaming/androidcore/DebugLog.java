package com.opentouchgaming.androidcore;

import android.util.Log;

/**
 * Created by Emile on 06/08/2017.
 */

public class DebugLog
{

    final String tag;
    final Module module;

    public DebugLog(Module module, String tag)
    {
        this.module = module;
        this.tag = tag;
    }

    public void log(Level level, String msg)
    {
        switch (level)
        {
            case I:
                Log.i(tag, msg);
                break;
            case D:
                Log.d(tag, msg);
                break;
            case W:
                Log.w(tag, msg);
                break;
            case E:
                Log.e(tag, msg);
                break;
        }
    }

    public enum Module
    {
        CONTROLS, GAMEFRAGMENT, APP, LICENSE, CORE,
    }

    public enum Level
    {
        D, I, W, E
    }
}
