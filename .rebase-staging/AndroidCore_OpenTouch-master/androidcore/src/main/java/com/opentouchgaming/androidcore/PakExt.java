package com.opentouchgaming.androidcore;

/**
 * Created by Emile on 04/02/2018.
 */

public class PakExt
{
    public static final int GAME_QUAKE1 = 0;
    public static final int GAME_QUAKE2 = 1;
    public static final int GAME_HEXEN2 = 2;

    public static native int extract(String pakFile, String file, int options, String outFilename);

    public static void load()
    {
        System.loadLibrary("qpakman");
    }
}
