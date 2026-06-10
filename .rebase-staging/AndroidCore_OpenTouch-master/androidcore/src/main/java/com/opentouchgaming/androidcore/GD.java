package com.opentouchgaming.androidcore;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;


public class GD
{
    public static boolean DEBUG = true;
    public static String qc_fn = "quick_cmd.dat";

    public static int version;

    public static void init(Context ctx)
    {
        PackageInfo pInfo;
        try
        {
            pInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            version = pInfo.versionCode;
        }
        catch (NameNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
