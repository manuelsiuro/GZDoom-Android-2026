package com.opentouchgaming.androidcore.license;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import com.opentouchgaming.androidcore.DebugLog;
import com.opentouchgaming.androidcore.Utils;

public class NdkLicense
{
    static final String SERVICE = "com.android.vending.licensing.ILicensingService";
    static DebugLog log;

    static
    {
        log = new DebugLog(DebugLog.Module.LICENSE, "NdkLicense");
    }

    public static void check(final Context context, final String key, final NdkLicenseCallback callback)
    {
        Intent intent = new Intent(SERVICE);
        intent.setPackage("com.android.vending");

        log.log(DebugLog.Level.D, "Binding service...");

        boolean res = context.bindService(intent, new ServiceConnection()
        {
            public void onServiceConnected(ComponentName name, IBinder binder)
            {
                log.log(DebugLog.Level.D, "onServiceConnected");

                Parcel d = Parcel.obtain();

                long id = Utils.getSecureID(context);

                try
                {
                    d.writeInterfaceToken(SERVICE);
                    d.writeLong(id);
                    d.writeString(context.getPackageName());
                    d.writeStrongBinder(new NdkLicenseListener(callback, key));
                    binder.transact(1, d, null, IBinder.FLAG_ONEWAY);
                }
                catch (RemoteException e)
                {
                    log.log(DebugLog.Level.E, "Error connecting to l server:" + e);
                }
                d.recycle();
            }

            public void onServiceDisconnected(ComponentName name)
            {
                log.log(DebugLog.Level.D, "onServiceDisconnected");
            }
        }, Context.BIND_AUTO_CREATE);

        if (res)
            log.log(DebugLog.Level.D, "Bound OK");
        else
            log.log(DebugLog.Level.D, "NOT BOUND");
    }
}