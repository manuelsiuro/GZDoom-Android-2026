package com.opentouchgaming.androidcore;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Emile on 26/03/2016.
 */
public class AssetFileAccess
{
    static String LOG = "AssetFileAccess";

    static Context ctx;
    static ArrayList<AssetFileDescriptor> openFiles = new ArrayList<>();

    public static native void setAssetManager(AssetManager mng);

    public static void init(Context c)
    {
        ctx = c;
    }

    public static int fopen(String filename, String mode)
    {
        Log.d(LOG, "filename = " + filename);

        try
        {
            AssetFileDescriptor fd = ctx.getAssets().openFd(filename);
            openFiles.add(fd);

            return openFiles.indexOf(fd);
        }
        catch (IOException e)
        {
            Log.e(LOG, "fopen: No file found with name: " + filename);
            e.printStackTrace();
            return -1;
        }
    }

    public static int flen(int handle)
    {
        AssetFileDescriptor fd = getFd(handle);
        if (fd != null)
        {
            return (int) fd.getLength();
        }
        else
        {
            Log.e(LOG, "flen: No file found with handle: " + handle);
            return 0;
        }
    }

    private static AssetFileDescriptor getFd(int handle)
    {
        if (handle - 1 < openFiles.size())
        {
            return openFiles.get(handle - 1);
        }
        else
        {
            Log.e(LOG, "getFd: No file found with handle: " + handle);
            return null;
        }
    }
}
