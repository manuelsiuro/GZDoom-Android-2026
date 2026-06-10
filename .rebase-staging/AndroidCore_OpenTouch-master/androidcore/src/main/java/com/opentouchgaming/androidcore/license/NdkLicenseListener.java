package com.opentouchgaming.androidcore.license;


import static com.opentouchgaming.androidcore.DebugLog.Level.D;

import android.os.Parcel;

import com.opentouchgaming.androidcore.AppInfo;
import com.opentouchgaming.androidcore.DebugLog;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public class NdkLicenseListener extends android.os.Binder
{

    static final String LISTENER = "com.android.vending.licensing.ILicenseResultListener";
    static DebugLog log;

    static
    {
        log = new DebugLog(DebugLog.Module.LICENSE, "NdkLicenseListener");
    }

    NdkLicenseCallback cb;
    String key;

    public NdkLicenseListener(NdkLicenseCallback callback, String key)
    {
        cb = callback;
        this.key = key;
    }

    public boolean onTransact(int op, Parcel in, Parcel reply, int flags)
    {

        if (op == 1)
        {
            NdkLicenseCallback.LicStatus ret = new NdkLicenseCallback.LicStatus();

            in.enforceInterface(LISTENER);
            int code = in.readInt();
            String data = in.readString();
            String signature = in.readString();

            log.log(D, "code = " + code + " data = " + data);
            log.log(D, "sig = " + signature);
            log.log(D, "key = " + key);

            if (code == 0 || code == 2)
            {
                //byte[] sig_test;
                //sig_test = Base64.decode(signature);
                int verif = NdkLvlInterface.doCheck(key.getBytes(), data.getBytes(), signature.getBytes());
                if (verif == 0)
                {
                    //Log.e(LOG, "Signature verification failed.");
                    ret.code = NdkLicenseCallback.NO_GOOD;
                    ret.desc = "Signature verification failed.";
                    cb.status(ret);
                    return true;
                }


                if ((data.startsWith("0|")) || (data.startsWith("2|")))
                {
                    ret.code = NdkLicenseCallback.GOOD;
                    ret.desc = "GOOD";
                    cb.status(ret);

                    // License OK, write out file
                    try
                    {
                        PrintWriter writer = null;
                        writer = new PrintWriter(AppInfo.internalFiles + "/l.dat", "UTF-8");
                        writer.println(data);
                        writer.println(signature);
                        writer.flush();
                        writer.close();
                    }
                    catch (FileNotFoundException e)
                    {
                        e.printStackTrace();
                    }
                    catch (UnsupportedEncodingException e)
                    {
                        e.printStackTrace();
                    }

                    return true;
                }
                else
                {
                    ret.code = NdkLicenseCallback.NO_GOOD;
                    ret.desc = "Bad DATA code: " + data;
                    cb.status(ret);
                    return true;
                }


            }
            else if (code == 1)
            {
                ret.code = NdkLicenseCallback.NO_GOOD;
                ret.desc = "Bad code: " + code;
                cb.status(ret);
                return true;
            }
            else
            {
                ret.code = NdkLicenseCallback.ERROR;
                ret.desc = "Bad code: " + code;
                cb.status(ret);
                return true;
            }
        }

        return true;
    }
}