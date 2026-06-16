package com.opentouchgaming.androidcore;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Environment;

import androidx.core.util.Pair;

import com.opentouchgaming.androidcore.ui.OptionsDialogKt;
import com.opentouchgaming.androidcore.ui.ScopedStorageDialog;
import com.opentouchgaming.androidcore.ui.StorageConfigDialog;
import com.opentouchgaming.androidcore.ui.UserFilesDialog;
import com.opentouchgaming.androidcore.ui.tutorial.Tutorial;
import com.opentouchgaming.androidcore.ui.widgets.SwitchWidget;
import com.opentouchgaming.saffal.FileSAF;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Emile on 03/08/2017.
 */

public class AppInfo
{
    private static final int SCOPED_VERSION = 30;
    public static Apps app;
    public static String title;
    public static String internalFiles;
    public static String cacheFiles;
    public static String directory;
    public static String flashRoot; // Root of internal flash
    public static String sdcardRoot; // Root of the SD card
    public static String sdcardWritable; // WRITABLE area on the SD dard
    public static String packageId;
    public static String emailAddress;
    public static String key;
    public static int sidePanelImage;
    public static boolean isAndroidTv;
    public static GameEngine[] gameEngines;
    public static GameEngine currentEngine;
    public static ArrayList<Tutorial> tutorials = new ArrayList<>();
    public static List<StorageConfigDialog.StorageExamples> storageExamples;
    public static ScopedStorageDialog.Tutorial scopedTutorial;
    public static String website = null;
    public static boolean hideModWads = false;
    public static boolean groupSimilarEngines = false;
    public static UserFilesDialog.UserFileEntryDescription[] userFilesEntries = null;

    public static int defaultAppImage;

    public static boolean showRateButton = true;
    public static boolean canDisabledScopedStorage = true;

    static DebugLog log;
    private static Context context;

    static
    {
        log = new DebugLog(DebugLog.Module.CORE, "AppInfo");
    }

    static public void setApp(Apps app)
    {
        AppInfo.app = app;
    }

    static public void setAppInfo(Context ctx, Apps app, String title, String directory, String pkg, String email, boolean isAndroidTv, int defaultAppImage,
                                  boolean hideModWads, boolean groupSimilarDefault)
    {
        AppInfo.context = ctx;
        AppInfo.app = app;
        AppInfo.title = title;
        AppInfo.directory = directory;
        AppInfo.internalFiles = ctx.getFilesDir().getAbsolutePath();
        AppInfo.cacheFiles = ctx.getCacheDir().toString();

        AppInfo.packageId = pkg;
        AppInfo.emailAddress = email;

        AppInfo.isAndroidTv = isAndroidTv;
        AppInfo.defaultAppImage = defaultAppImage;
        AppInfo.hideModWads = hideModWads;

        AppInfo.flashRoot = Environment.getExternalStorageDirectory().toString();

        AppInfo.groupSimilarEngines = SwitchWidget.fetchValue(ctx, OptionsDialogKt.GROUP_SIMILAR_ENGINES, groupSimilarDefault);

        // NOW DEFAULT TO SCOPED STORAGE ON NEW INSTALL!!
        if (app != Apps.RAZE_TOUCH)
        {
            String appDir = AppSettings.getStringOption(context, "app_dir", null);
            if (appDir == null && isScopedAllowed())
            {
                setScoped(true);
                if (Utils.getTargetAPI() > 29) // On new installs built with API 30+ there is no way to disabled scoped storage on Android 11+.
                    AppSettings.setBoolOption(AppInfo.getContext(), "scoped_storage_permanent", true);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            File[] files = context.getExternalFilesDirs(null);
            if (files != null && files.length > 1 && files[1] != null)
            {
                if (!files[1].exists())
                    files[1].mkdirs();

                AppInfo.sdcardWritable = files[1].getAbsolutePath();
                AppInfo.sdcardRoot = AppInfo.sdcardWritable.substring(0, AppInfo.sdcardWritable.indexOf("/Android/data"));
            }
        }

        log.log(DebugLog.Level.D, "Build.VERSION.SDK_INT = " + Build.VERSION.SDK_INT);
        log.log(DebugLog.Level.D, "flashRoot = " + flashRoot);
        log.log(DebugLog.Level.D, "sdcardRoot = " + sdcardRoot);
        log.log(DebugLog.Level.D, "sdcardWritable = " + sdcardWritable);
    }

    static public GameEngine getGameEngine(GameEngine.Engine type)
    {
        GameEngine ret = null;
        for (GameEngine e : gameEngines)
        {
            if (e.engine == type)
            {
                ret = e;
                break;
            }
        }
        return ret;
    }

    static public void setScoped(boolean enabled)
    {
        AppSettings.setBoolOption(AppInfo.getContext(), "scoped_storage_enabled", enabled);
        setAppDirectory(getDefaultAppDirectory());
        setAppSecDirectory(getDefaultAppSecDirectory());
    }

    static public boolean isScopedPermanent() // Handles the strange case where Android 11 can upgrade the app but due to preserveLegacyExternalStorage
    // Scoped storage is optional until new install
    {
        return AppSettings.getBoolOption(AppInfo.getContext(), "scoped_storage_permanent", false);
    }

    static public boolean isScopedAllowed()
    {
        return (Build.VERSION.SDK_INT >= SCOPED_VERSION);
    }

    static public boolean isScopedEnabled()
    {
        if (isScopedAllowed())
        {
            return AppSettings.getBoolOption(AppInfo.getContext(), "scoped_storage_enabled", false);
        }
        else
            return false;
    }

    // PRIMARY DEFAULT
    @SuppressLint("NewApi")
    static public String getDefaultAppDirectory()
    {
        if (!isScopedEnabled())
        {
            return flashRoot + "/OpenTouch/" + directory;
        }
        else // Android R!!!! FUCKK
        {
            File[] files = context.getExternalFilesDirs(null);

            if (!files[0].exists())
                files[0].mkdirs();

            return files[0].getAbsolutePath();
        }
    }

    // SECONDARY DEFAULT
    static public String getDefaultAppSecDirectory()
    {
        if (sdcardRoot != null)
        {
            if (!isScopedEnabled())
            {
                return sdcardRoot + "/OpenTouch/" + directory;
            }
            else
            {
                return null;
            }
        }
        else
        {
            return null;
        }
    }

    static public String getAppDirectory(String fileInfo)
    {
        String appDir = AppSettings.getStringOption(context, "app_dir", null);
        if (appDir == null)
        {
            appDir = getDefaultAppDirectory();
            AppSettings.setStringOption(context, "app_dir", appDir);
        }

        Utils.mkdirs(AppInfo.getContext(), appDir, fileInfo);

        return appDir;
    }

    static public String getAppDirectory()
    {
        return getAppDirectory(null);
    }

    static public void setAppDirectory(String appDir)
    {
        AppSettings.setStringOption(context, "app_dir", appDir);
    }

    static public String getAppSecDirectory()
    {
        String appDir = AppSettings.getStringOption(context, "app_sec_dir", null);

        if (appDir == null)
        {
            appDir = getDefaultAppSecDirectory();
            AppSettings.setStringOption(context, "app_sec_dir", appDir);
        }

        return appDir;
    }

    static public void setAppSecDirectory(String appDir)
    {
        AppSettings.setStringOption(context, "app_sec_dir", appDir);
    }

    static public void setUserFileInSecondary(boolean yes)
    {
        AppSettings.setBoolOption(context, "user_files_in_secondary", yes);
    }

    // User files (engine config, savegames, touch control saves)
    static public String getUserFiles()
    {
        String userFiles;

        // Option to put user_files on secondary storage
        if (AppSettings.getBoolOption(context, "user_files_in_secondary", false) && getAppSecDirectory() != null)
            userFiles = getAppSecDirectory() + "/user_files";
        else
            userFiles = getAppDirectory() + "/user_files";

        new FileSAF(userFiles).mkdirs();

        // Also create touch_layouts folder
        new FileSAF(userFiles + "/touch_layouts").mkdirs();

        return userFiles;
    }

    static public String getResFiles()
    {
        String files = getAppDirectory() + "/res/";

        new File(files).mkdirs();

        return files;
    }

    static public String[] getBackupPaths()
    {
        String b1 = getAppDirectory() + "/backup";

        String b2 = null;
        if (isScopedEnabled() && getAppSecDirectory() != null)
        {
            b2 = getAppSecDirectory() + "/backup";
            return new String[]{b2, b1};
        }
        else
        {
            return new String[]{b1};
        }
    }

    static public String getQuickCommandsPath()
    {
        String qcBasePath = getUserFiles() + "/QC";

        new FileSAF(qcBasePath).mkdirs();

        return qcBasePath;
    }


    static public String replaceRootPaths(String path)
    {
        if (path != null)
        {
            String ret = path.replace(flashRoot, "<Internal>");
            if (sdcardRoot != null)
            {
                ret = ret.replace(sdcardRoot, "<SD-Card>");
            }
            return ret;
        }
        else
        {
            return "Not set";
        }
    }

    static public String hidePaths(String path, String hide1, String hide2)
    {
        String ret = path.replace(hide1 + "/", "");

        if (hide2 != null)
        {
            ret = ret.replace(hide2 + "/", "");
        }
        return ret;
    }

    public static Pair<String, Integer> getDisplayPathAndImage(String path)
    {
        String newPath = path;
        Integer image = R.drawable.ic_baseline_phone_android_black;

        if (path != null)
        {
            if (path.contains("/[internal]"))  // SAF paths
            {
                newPath = path.replace("/[internal]", "");
                image = R.drawable.ic_baseline_phone_android_black;
            }
            else if (path.contains("/[SD-Card]"))  // SAF paths
            {
                newPath = path.replace("/[SD-Card]", "");
                image = R.drawable.ic_baseline_sd_card_black;
            }
            else if (path.contains(flashRoot))
            {
                newPath = path.replace(flashRoot, "");
                image = R.drawable.ic_baseline_phone_android_black;
            }
            else if ((sdcardRoot != null) && (path.contains(sdcardRoot)))
            {
                newPath = path.replace(sdcardRoot, "");
                image = R.drawable.ic_baseline_sd_card_black;
            }
        }
        else
        {
            if (AppInfo.isScopedEnabled())
            {
                newPath = "Set Data path ----------->";
            }
            else
            {
                newPath = " -- Not Set --";
            }
            image = R.drawable.ic_baseline_error_outline_black;
        }

        return new androidx.core.util.Pair(newPath, image);
    }

    static public String hideAppPaths(String path)
    {
        String appPath = getAppDirectory();
        String appSecPath = getAppSecDirectory();

        String ret = path.replace(appPath + "/", "");

        if (appSecPath != null)
        {
            ret = ret.replace(appSecPath + "/", "");
        }
        return ret;
    }

    static public String getGamepadDirectory()
    {
        String path = getUserFiles() + "/gamepad";

        new FileSAF(path).mkdirs();

        return path;
    }

    static public String getSuperModFile()
    {
        FileSAF file = new FileSAF(AppInfo.getUserFiles() + "/loadouts/loadout.dat");
        file.getParentFile().mkdirs();
        return file.getAbsolutePath();
    }

    // JNI
    static public Context getContext()
    {
        return context;
    }

    static public void setContext(Context ctx)
    {
        context = ctx;
    }

    // JNI
    static public String getFilesDir()
    {
        return context.getFilesDir().getAbsolutePath();
    }

    public enum Apps
    {
        MOD_ENGINE, DELTA_TOUCH, ALPHA_TOUCH, QUAD_TOUCH, RAZE_TOUCH, PSI_TOUCH
    }
}
