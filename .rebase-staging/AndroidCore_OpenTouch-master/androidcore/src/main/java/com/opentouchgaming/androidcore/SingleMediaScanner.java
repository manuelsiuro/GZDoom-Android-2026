package com.opentouchgaming.androidcore;


import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.util.Log;

import java.io.File;

public class SingleMediaScanner implements MediaScannerConnectionClient
{

    private final MediaScannerConnection mMs;
    private final String mFile;
    private boolean path;

    public SingleMediaScanner(Context context, boolean path, String f)
    {
        if (GD.DEBUG)
            Log.d("SingleMediaScanner", "path = " + path + ", f = " + f);
        mFile = f;
        mMs = new MediaScannerConnection(context, this);
        mMs.connect();
    }

    @Override
    public void onMediaScannerConnected()
    {
        if (path)
        {
            File p = new File(mFile);
            File[] files = p.listFiles();
            if (files != null)
            {
                for (File f : files)
                {
                    mMs.scanFile(f.getAbsolutePath(), null);
                }
            }
        }
        else
            mMs.scanFile(mFile, null);
    }

    @Override
    public void onScanCompleted(String path, Uri uri)
    {
        mMs.disconnect();
    }

}
