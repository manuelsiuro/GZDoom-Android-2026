package com.opentouchgaming.androidcore;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import it.gmariotti.changelibs.library.view.ChangeLogRecyclerView;

public class AboutDialog
{
    public static int aboutRes;

    public static void show(final Context ctx, boolean includeRateButton)
    {
        final Dialog dialog;

        dialog = new Dialog(ctx);
        dialog.setContentView(R.layout.about_dialog_view);
        dialog.setTitle("Changes");
        dialog.setCancelable(true);
        //there are a lot of settings, for dialog, check them all out!

        //set up text
        final TextView text = dialog.findViewById(R.id.about_text_textview);
        text.setVisibility(View.INVISIBLE);

        final ChangeLogRecyclerView changeLogView = dialog.findViewById(R.id.changeLogView);

        //set up button
        Button button = dialog.findViewById(R.id.ok_button);
        button.setOnClickListener(v ->
                                  {
                                      dialog.dismiss();
                                  });

        button = dialog.findViewById(R.id.about_changes_button);
        button.setOnClickListener(v ->
                                  {
                                      dialog.setTitle("Changes");
                                      text.setVisibility(View.INVISIBLE);
                                      changeLogView.setVisibility(View.VISIBLE);
                                  });

        button = dialog.findViewById(R.id.about_license_button);
        button.setOnClickListener(v ->
                                  {
                                      dialog.setTitle("About");
                                      text.setText(readTxt(ctx, aboutRes));
                                      text.setVisibility(View.VISIBLE);
                                      changeLogView.setVisibility(View.INVISIBLE);
                                  });


        button = dialog.findViewById(R.id.about_rate_button);
        if (includeRateButton)
        {
            Animation mAnimation = new AlphaAnimation(1, 0);
            mAnimation.setDuration(500);
            mAnimation.setInterpolator(new LinearInterpolator());
            mAnimation.setRepeatCount(Animation.INFINITE);
            mAnimation.setRepeatMode(Animation.REVERSE);
            button.startAnimation(mAnimation);

            button.setOnClickListener(v ->
                                      {
                                          Intent marketIntent = new Intent(Intent.ACTION_VIEW,
                                                                           Uri.parse("http://play.google.com/store/apps/details?id=" + ctx.getPackageName()));
                                          ctx.startActivity(marketIntent);
                                      });
        }
        else
        {
            button.setVisibility(View.GONE);
        }

        //now that the dialog is set up, it's time to show it

        DisplayMetrics metrics = new DisplayMetrics();
        dialog.getWindow().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int screenHeight = metrics.heightPixels;
        int scrreenWidth = metrics.widthPixels;
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.height = screenHeight;
        params.width = (int) (scrreenWidth * 90.0 / 100);
        dialog.getWindow().setAttributes(params);

        dialog.show();
    }

    private static String readTxt(Context ctx, int id)
    {
        InputStream inputStream = ctx.getResources().openRawResource(id);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        int i;
        try
        {
            i = inputStream.read();
            while (i != -1)
            {
                byteArrayOutputStream.write(i);
                i = inputStream.read();
            }
            inputStream.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return byteArrayOutputStream.toString();
    }

    public static boolean showAbout(Context ctx)
    {
        try
        {
            int versionCode = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionCode;
            int last_ver = AppSettings.getIntOption(ctx, "last_opened_version", -1);
            //Log.d("test"," ver = " +  versionCode + " last =" + last_ver);
            if (versionCode != last_ver)
            {
                AppSettings.setIntOption(ctx, "last_opened_version", versionCode);

                // If first run DO NOT show the dialog, in case the scoped storage dialog needs to show
                return last_ver != -1;
            }
            else
                return false;

        }
        catch (NameNotFoundException e)
        {
            return false;
        }
    }
}
