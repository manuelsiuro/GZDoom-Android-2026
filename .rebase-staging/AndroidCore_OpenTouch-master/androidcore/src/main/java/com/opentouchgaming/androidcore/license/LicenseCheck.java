package com.opentouchgaming.androidcore.license;

import static com.opentouchgaming.androidcore.DebugLog.Level.D;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.widget.Toast;

import com.opentouchgaming.androidcore.AppInfo;
import com.opentouchgaming.androidcore.DebugLog;
import com.opentouchgaming.androidcore.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by Emile on 29/10/2017.
 */

public class LicenseCheck
{
    static DebugLog log;
    // Yes yes yes very bad don't care for this.
    static Activity activity;
    static ProgressDialog progressDialog;

    static
    {
        log = new DebugLog(DebugLog.Module.LICENSE, "LicenseCheck");
    }

    public static boolean checkLicenseFile(Context context, String key)
    {
        boolean ok = false;//BuildConfig.DEBUG;

        try
        {
            File f = new File(AppInfo.internalFiles + "/l.dat");

            BufferedReader b = new BufferedReader(new FileReader(f));
            String data = b.readLine();
            String sig = b.readLine();

            if ((data != null) && (sig != null))
            {
                log.log(D, "data = " + data);
                log.log(D, "sig = " + sig);

                String[] fields = data.split("\\|");
                //0|123456789|com.opentouchgaming.deltatouch|1|ANlOHQPQPDt8bkZGksjaQL4mXgwweeZhA==|1509278207744
                if (fields.length >= 6)
                {
                    if (fields[0].contentEquals("0")) // Check licensed (Should always be)
                    {
                        long thisId = Utils.getSecureID(context);
                        long fileId = Long.parseLong(fields[1]);

                        if (thisId == fileId) // Check sec ID
                        {
                            try
                            {
                                PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                                int lId = Integer.parseInt(fields[3]);

                                lId = pInfo.versionCode; // FIX THIS! TEMP UNTIL GOOGLE FIXES ISSUE

                                if (lId == pInfo.versionCode) // Check correct version of app
                                {
                                    ok = true;
                                }
                                else
                                {
                                    log.log(D, "Id failed " + lId + " != " + pInfo.versionCode);
                                }
                            }
                            catch (PackageManager.NameNotFoundException e)
                            {
                                e.printStackTrace();
                            }

                        }
                        else
                        {
                            log.log(D, "Sec id failed: " + thisId + " != " + fileId);
                        }
                    }
                    else
                    {
                        log.log(D, "Not licensed: " + fields[0]);
                    }
                }
                else
                {
                    log.log(D, "Wrong number of fields");
                }
            }

        }
        catch (FileNotFoundException e)
        {
            log.log(D, "l.dat file not found");

        }
        catch (IOException e)
        {
            log.log(D, "l.dat file not opened: " + e);
        }

        // Delete the file if failed
        if (!ok)
        {
            File f = new File(AppInfo.internalFiles + "/l.dat");
            f.delete();
        }

        return ok;
    }

    public static void fetchLicense(final Activity activity_, boolean showDialog, final String key)
    {
        if (showDialog)
        {
            activity = activity_;

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage("License not yet found. Press OK to fetch license from Google").setCancelable(true)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            NdkLicense.check(activity, key, new LCallback());


                            progressDialog = new ProgressDialog(activity);
                            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                            progressDialog.setMessage("Please wait...");
                            progressDialog.setIndeterminate(true);
                            progressDialog.setCanceledOnTouchOutside(false);
                            progressDialog.show();

                        }
                    });

            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    static class LCallback implements NdkLicenseCallback
    {

        @Override
        public void status(final LicStatus ret)
        {
            if (ret.code == NdkLicenseCallback.GOOD)
            {
                log.log(D, "Google says it is licensed");

            }
            else if (ret.code == NdkLicenseCallback.NO_GOOD)
            {
                log.log(D, "Google says it is unlicensed");
                if (activity != null)
                {
                    activity.runOnUiThread(new Runnable()
                    {
                        public void run()
                        {

                            Toast.makeText(activity, "Unlicensed, reason: " + ret.desc, Toast.LENGTH_LONG).show();

                            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                            builder.setMessage("Unlicensed, reason: " + ret.desc).setCancelable(true)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener()
                                    {
                                        public void onClick(DialogInterface dialog, int id)
                                        {
                                            dialog.cancel();
                                        }
                                    });

                            AlertDialog alert = builder.create();
                            alert.show();
                        }
                    });
                }

            }
            else
            {
                log.log(D, "Got responce " + ret.code + " Desc = " + ret.desc);
                if (activity != null)
                {
                    activity.runOnUiThread(new Runnable()
                    {
                        public void run()
                        {

                            Toast.makeText(activity, "Can not check license: " + ret.desc, Toast.LENGTH_LONG).show();

                            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                            builder.setMessage("Can not check license. Please make sure you have working internet. " + ret.desc).setCancelable(true)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener()
                                    {
                                        public void onClick(DialogInterface dialog, int id)
                                        {
                                            dialog.cancel();
                                        }
                                    });

                            AlertDialog alert = builder.create();
                            alert.show();
                        }
                    });
                }
            }

            if (progressDialog != null)
            {
                progressDialog.dismiss();
                progressDialog = null;
            }
        }
    }


}
