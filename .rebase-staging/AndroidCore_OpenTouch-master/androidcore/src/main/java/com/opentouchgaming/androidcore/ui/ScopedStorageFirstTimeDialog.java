package com.opentouchgaming.androidcore.ui;

import android.app.Activity;
import android.app.Dialog;
import android.os.Build;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.core.util.Pair;

import com.opentouchgaming.androidcore.AppInfo;
import com.opentouchgaming.androidcore.DebugLog;
import com.opentouchgaming.androidcore.R;


public class ScopedStorageFirstTimeDialog
{
    static DebugLog log;

    static
    {
        log = new DebugLog(DebugLog.Module.GAMEFRAGMENT, "ScopedStorageFirstTimeDialog");
    }

    Activity activity;
    TextView appDirTextView;
    ImageView appDirIcon;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public ScopedStorageFirstTimeDialog(final Activity activity)
    {
        this.activity = activity;

        final Dialog dialog = new Dialog(activity);
        //dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_scoped_storage_first_time);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        appDirTextView = dialog.findViewById(R.id.app_dir_textview);
        appDirIcon = dialog.findViewById(R.id.app_dir_image);

        Pair<String, Integer> pathApp = AppInfo.getDisplayPathAndImage(AppInfo.getAppDirectory());
        appDirTextView.setText(pathApp.first);
        appDirIcon.setImageResource(pathApp.second);


        Button setNow = dialog.findViewById(R.id.set_now_button);
        setNow.setOnClickListener(v -> new ScopedStorageDialog(activity, AppInfo.scopedTutorial, () ->
        {
            dialog.dismiss();
        }));

        dialog.show();

        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
