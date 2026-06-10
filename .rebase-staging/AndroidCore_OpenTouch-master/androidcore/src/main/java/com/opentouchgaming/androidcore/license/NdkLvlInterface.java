package com.opentouchgaming.androidcore.license;

import android.util.Log;

public class NdkLvlInterface
{

    static native int checkRSASig(byte[] key, byte[] message, byte[] sig);


    public static int doCheck(byte[] key, byte[] message, byte[] sig)
    {
        try
        {
            System.loadLibrary("ndk_lvl");
        }
        catch (UnsatisfiedLinkError ule)
        {
            Log.e("JNI", "WARNING: Could not load shared library: " + ule);
        }

        return checkRSASig(key, message, sig);
    }
}
