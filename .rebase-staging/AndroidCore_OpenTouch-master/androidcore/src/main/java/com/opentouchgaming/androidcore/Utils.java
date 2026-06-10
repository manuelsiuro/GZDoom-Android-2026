package com.opentouchgaming.androidcore;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;
import com.opentouchgaming.androidcore.license.LicenseCheck;
import com.opentouchgaming.androidcore.ui.OptionsDialogKt;
import com.opentouchgaming.androidcore.ui.widgets.SwitchWidget;
import com.opentouchgaming.saffal.FileSAF;
import com.opentouchgaming.saffal.UtilsSAF;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

public class Utils
{
    static final int BUFFER_SIZE = 1024;
    // Used to represent a quotation mark
    static public String QOT = "^''";
    static String LOG = "Utils";
    static DebugLog log;
    static String NAME_PREFIX = "class ";

    static
    {
        log = new DebugLog(DebugLog.Module.CONTROLS, "Utils");
    }

    static public int dpToPx(Resources r, int dp)
    {
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
        return px;
    }

    static public void mkdirs(Context context, String path, String infoFile)
    {
        File file = new FileSAF(path);

        if (!file.exists())
        {
            if (!file.mkdirs())
            {
                log.log(DebugLog.Level.E, "ERROR, could not create folder: " + path);
            }
        }

        if (infoFile == null)
            infoFile = ".tmp";

        File info = new FileSAF(path, infoFile);
        if (!info.exists())
        {
            try
            {
                info.createNewFile();
                // 2 ways to try to make the folder visible over USB
                AppInfo.getContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + info.getAbsolutePath())));
                new SingleMediaScanner(context, false, info.getAbsolutePath());
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    static public void copyFile(File in, File out) throws IOException
    {
        InputStream isIn = new FileInputStream(in);
        OutputStream inOut = new FileOutputStream(out);
        copyFile(isIn, inOut);
    }

    static public void copyFile(InputStream in, OutputStream out) throws IOException
    {
        copyFile(in, out, null);
    }

    static public void copyFile(InputStream in, OutputStream out, ProgressDialog pb) throws IOException
    {
        byte[] buffer = new byte[1024 * 4];
        int read;
        while ((read = in.read(buffer)) != -1)
        {
            out.write(buffer, 0, read);
            if (pb != null)
                pb.setProgress(pb.getProgress() + read);
        }
        out.close();
    }

    static public void showDownloadDialog(final Activity act, String title, final String directory, final String file, final int size,
                                          final ServerAPI.Callback cb)
    {
        boolean ok = LicenseCheck.checkLicenseFile(act, AppInfo.key);
        if (!ok)
        {
            LicenseCheck.fetchLicense(act, true, AppInfo.key);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        builder.setMessage(title).setCancelable(true).setPositiveButton("OK", (dialog, id) -> ServerAPI.downloadFile(act, file, directory, size, cb));
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog alert = builder.create();
        alert.show();
    }

    public static String checkFiles(String basePath, String[] files_to_ceck)
    {
        File[] files = new File(basePath).listFiles();
        boolean ok = true;

        String filesNotFound = "";

        String[] expected;
        expected = files_to_ceck;

        if (files == null)
            files = new File[0];

        if (files != null)
        {
            for (File f : files)
            {
                Log.d(LOG, "FILES: " + f.toString());
            }

            for (String e : expected)
            {
                boolean found = false;
                for (File f : files)
                {
                    if (f.toString().toLowerCase().endsWith(e.toLowerCase()))
                        found = true;
                }
                if (!found)
                {
                    Log.d(LOG, "Didnt find " + e);
                    filesNotFound += e + "\n";
                    ok = false;
                }
            }
        }

        if (filesNotFound.contentEquals(""))
            return null;
        else
            return filesNotFound;

    }

    static public boolean checkFilesInPath(String path1, String[] files)
    {
        boolean filesPresent = true;

        for (String file : files)
        {
            File f = new File(path1 + "/" + file);
            String filePath = f.getParent();
            String fileName = f.getName();
            boolean foundInList = false;
            if (filePath != null && fileName != null)
            {
                FileSAF[] filesFound = new FileSAF(filePath).listFiles();
                if (filesFound != null)
                {
                    for (FileSAF ff : filesFound)
                    {
                        if (ff.getName().compareToIgnoreCase(fileName) == 0)
                        {
                            foundInList = true;
                            break;
                        }
                    }
                }
            }

            if (!foundInList)
            {
                filesPresent = false;
                break;
            }
        }
        return filesPresent;
    }

    static public String checkFilesInPaths(String path1, String path2, String[] files)
    {
        if ((path1 != null) && (checkFilesInPath(path1, files)))
            return path1;
        else if ((path2 != null) && (checkFilesInPath(path2, files)))
            return path2;
        else
            return null;
    }

    static public void copyPNGAssets(Context ctx, String dir)
    {
        copyPNGAssets(ctx, dir, "");
    }

    static public void copyPNGAssets(Context ctx, String dir, String prefix)
    {

        if (prefix == null)
            prefix = "";

        File d = new File(dir);
        if (!d.exists())
            d.mkdirs();

        AssetManager assetManager = ctx.getAssets();
        String[] files = null;
        try
        {
            files = assetManager.list("");
        }
        catch (IOException e)
        {
            Log.e("tag", "Failed to get asset file list.", e);
        }
        for (String filename : files)
        {
            if ((filename.endsWith("png") || filename.endsWith("txt")) && filename.startsWith(prefix))
            {
                try
                {
                    InputStream in = assetManager.open(filename);
                    OutputStream out = new FileOutputStream(dir + "/" + filename.substring(prefix.length()));
                    copyFile(in, out);
                    in.close();
                    out.flush();
                    out.close();
                }
                catch (IOException e)
                {
                    Log.e("tag", "Failed to copy asset file: " + filename, e);
                }
            }
        }
    }

    static public ArrayList<File> listFiles(String[] paths)
    {
        ArrayList<File> files = new ArrayList<>();
        for (String path : paths)
        {
            if (path != null)
            {
                if (UtilsSAF.ready() && UtilsSAF.isInSAFRoot(path))
                {
                    // SAF files
                    FileSAF[] safFiles = new FileSAF(path).listFiles();

                    if (safFiles != null)
                        Collections.addAll(files, safFiles);
                }
                else
                { // Normal file
                    File[] f = new File(path).listFiles();
                    if (f != null)
                        files.addAll(Arrays.asList(f));
                }
            }
        }

        return files;
    }

    static public String[] creatArgs(String appArgs)
    {
        //ArrayList<String> a = new ArrayList<String>(Arrays.asList(appArgs.split(" ")));
        ArrayList<String> a = new ArrayList<String>(Arrays.asList(appArgs.split("\"?( |$)(?=(([^\"]*\"){2})*[^\"]*$)\"?")));
        Iterator<String> iter = a.iterator();
        while (iter.hasNext())
        {
            if (iter.next().contentEquals(""))
            {
                iter.remove();
            }
        }

        // Replace QOT with a quotation mark
        for (int index = 0; index < a.size(); index++)
        {
            a.set(index, a.get(index).replace(QOT, "\""));
        }

        return a.toArray(new String[a.size()]);
    }

    public static void expand(final View v)
    {
        v.measure(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        final int targtetHeight = v.getMeasuredHeight();

        v.getLayoutParams().height = 0;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t)
            {
                v.getLayoutParams().height = interpolatedTime == 1 ? LayoutParams.WRAP_CONTENT : (int) (targtetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds()
            {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int) (targtetHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    public static void collapse(final View v)
    {
        final int initialHeight = v.getMeasuredHeight();

        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t)
            {
                if (interpolatedTime == 1)
                {
                    v.setVisibility(View.GONE);
                }
                else
                {
                    v.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds()
            {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int) (initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    static public StringBuilder getLogCat()
    {
        String[] logcatArgs = new String[]{"logcat", "-d", "-v", "time"};
        StringBuilder sb = new StringBuilder();
        Process logcatProc = null;
        try
        {
            logcatProc = Runtime.getRuntime().exec(logcatArgs);
        }
        catch (IOException e)
        {
            return null;
        }

        BufferedReader reader = null;
        try
        {
            String separator = System.getProperty("line.separator");

            reader = new BufferedReader(new InputStreamReader(logcatProc.getInputStream()), BUFFER_SIZE);
            String line;
            while ((line = reader.readLine()) != null)
            {
                sb.append(line);
                sb.append(separator);
            }
        }
        catch (IOException e)
        {
        }
        finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                }
                catch (IOException e)
                {
                }
            }
        }

        return sb;
    }

    static public void SendDebugEmail(final Activity activity, final String emailAddress, final String appId, final String logFile)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage("Are you sure you want to email the debug log?\nIf yes, please give good information about the problem.\n").setCancelable(true)
                .setPositiveButton("SEND EMAIL", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        // TODO Auto-generated method stub
                        PrintWriter printWriter = null;
                        try
                        {
                            String filename = AppInfo.cacheFiles + "/" + AppInfo.app.toString() + "_logcat.txt";
                            printWriter = new PrintWriter(new FileWriter(filename), true);

                            StringBuilder log = Utils.getLogCat();

                            printWriter.print(log);

                            printWriter.close();

                            final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
                            emailIntent.setType("plain/text");

                            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, AppInfo.app.toString() + "_" + GD.version + " Logging file");
                            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{emailAddress});
                            emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Enter description of issue:  ");

                            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            //Uri uri = Uri.parse("file://" + filename);
                            ArrayList<Uri> uris = new ArrayList<Uri>();
                            Uri uri = FileProvider.getUriForFile(activity, appId + ".provider", new File(filename));
                            uris.add(uri);
                            if (logFile != null)
                            {
                                // Log file might be in SAF, need to copy to cache first
                                FileSAF logFileSAF = new FileSAF((logFile));
                                if (logFileSAF.exists())
                                {
                                    FileSAF logCache = new FileSAF(AppInfo.cacheFiles + "/" + logFileSAF.getName());
                                    OutputStream logCacheOS = logCache.getOutputStream();
                                    Utils.copyFile(logFileSAF.getInputStream(), logCacheOS);
                                    logCacheOS.close();

                                    uri = FileProvider.getUriForFile(activity, appId + ".provider", logCache);
                                    uris.add(uri);
                                }
                            }
                            emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

                            activity.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
                        }
                        catch (IOException e)
                        {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    }
                });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    static public void copyAsset(Context ctx, String file, String destdir, String destFilename)
    {
        AssetManager assetManager = ctx.getAssets();
        new File(destdir).mkdirs();
        try
        {
            InputStream in = assetManager.open(file);
            OutputStream out = new FileOutputStream(destdir + "/" + destFilename);
            copyFile(in, out);
            in.close();
            out.flush();
            out.close();
        }
        catch (IOException e)
        {
            Log.e("tag", "Failed to copy asset file: " + file + " error = " + e);
        }
    }

    static public void copyAsset(Context ctx, String file, String destdir)
    {
        copyAsset(ctx, file, destdir, file);
    }

    static public void copyToClipboard(Context ctx, String lable, String text)
    {
        ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(lable, text);
        clipboard.setPrimaryClip(clip);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
            Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show();
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
    {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth)
        {
            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight)
    {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public static void setImmersionMode(final Context ctx, Window window, String key)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            if (SwitchWidget.fetchValue(ctx, key, false))
            {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                                                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                                            // hide nav bar
                                                            |
                                                            View.SYSTEM_UI_FLAG_FULLSCREEN
                                                            // hide status bar
                                                            |
                                                            View.SYSTEM_UI_FLAG_IMMERSIVE |
                                                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

                View decorView = window.getDecorView();
                decorView.setOnSystemUiVisibilityChangeListener(visibility ->
                                                                {
                                                                    Log.d(LOG, "onSystemUiVisibilityChange");

                                                                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                                                                                                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                                                                                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                                                                                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                                                                                                // hide nav bar
                                                                                                                |
                                                                                                                View.SYSTEM_UI_FLAG_FULLSCREEN
                                                                                                                // hide status bar
                                                                                                                |
                                                                                                                View.SYSTEM_UI_FLAG_IMMERSIVE |
                                                                                                                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                                                                });
            }
        }
    }

    public static void expandToCutout(final Context ctx, Window window, String key)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        {
            if (SwitchWidget.fetchValue(ctx, key, false))
            {
                WindowManager.LayoutParams attributes = window.getAttributes();
                attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setAttributes(attributes);
            }
        }
    }

    public static String humanReadableByteCount(long bytes, boolean si)
    {
        int unit = si ? 1000 : 1024;
        if (bytes < unit)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = ("KMGTPE").charAt(exp - 1) + ("");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static long getSecureID(Context ctx)
    {
        BigInteger b = new BigInteger(Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID), 16);
        // Log.d("TEST","long = " + b.longValue());
        return b.longValue();
    }

    public static float convertDpToPixel(float dp, Context context)
    {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

    public static String

    filesInfoString(String path, String ext, int maxFiles)
    {
        String pakFiles = "[ ";
        int nbrFiles = 0;
        int nbrDirs = 0;
        long totalSize = 0;

        File[] files = new FileSAF(path).listFiles();

        if (files != null)
        {
            for (File file : files)
            {
                if (file.isDirectory())
                {
                    nbrDirs++;
                }
                else if (ext == null || file.getName().toLowerCase().endsWith(ext))
                {
                    if (nbrFiles < maxFiles)
                    {
                        pakFiles += file.getName() + ", ";
                    }
                    totalSize += file.length();
                    nbrFiles++;
                }
            }
        }
        pakFiles += "]";

        String ret = nbrFiles + " files";
        if (nbrDirs != 0)
            ret += " and " + nbrDirs + " folders";
        ret += " (" + Utils.humanReadableByteCount(totalSize, false) + ")";
        return ret;
    }

    static public ArrayList<String> findFiles(FileSAF root, String name, ArrayList<String> files)
    {
        FileSAF[] list = root.listFiles();
        if (list != null)
        {
            for (FileSAF fil : list)
            {
                if (fil.isDirectory())
                {
                    findFiles(fil, name, files);
                }
                else if (name.equalsIgnoreCase(fil.getName()))
                {
                    files.add(fil.getAbsolutePath());
                }
            }
        }
        return files;
    }

    static int getTargetAPI()
    {
        int targetSdkVersion = 0;
        try
        {
            PackageInfo packageInfo = AppInfo.getContext().getPackageManager().getPackageInfo(AppInfo.getContext().getPackageName(), 0);
            targetSdkVersion = packageInfo.applicationInfo.targetSdkVersion;
        }
        catch (PackageManager.NameNotFoundException e)
        {

        }
        return targetSdkVersion;
    }

    private static String getClassName(Type type)
    {
        String fullName = type.toString();
        if (fullName.startsWith(NAME_PREFIX))
            return fullName.substring(NAME_PREFIX.length());
        return fullName;
    }

    public static boolean toJson(String outputFile, Object object)
    {
        boolean error = false;
        try
        {
            String jsonInString = new Gson().toJson(object);
            JSONArray jsonObject = new JSONArray(jsonInString);
            FileSAF fileOut = new FileSAF(outputFile);

            if (!fileOut.exists())
                fileOut.createNewFile();

            BufferedWriter output = new BufferedWriter(new OutputStreamWriter(fileOut.getOutputStream()));
            output.write(jsonObject.toString(4));
            output.close();
        }
        catch (JSONException | IOException e)
        {
            e.printStackTrace();
            error = true;
        }
        return error;
    }

    public static <T> T fromJson(String inputFile, Type type, boolean createNew)
    {
        T t = null;

        try
        {
            Gson gson = new Gson();
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileSAF(inputFile).getInputStream()));
            // Type type = new TypeToken<clazz>() {  }.getType();
            t = gson.fromJson(br, type);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        return t;
    }

    public static String quoteString(String s)
    {
        if (s.contains(" "))
        {
            return "\"" + s + "\"";
        }
        else
            return s;
    }

    public static class SpinnerValues
    {
        private float value;
        private String name;

        public SpinnerValues(float value, String name)
        {
            this.value = value;
            this.name = name;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public float getValue()
        {
            return value;
        }

        public void setValue(float value)
        {
            this.value = value;
        }

        public int getIntValue()
        {
            return (int) value;
        }

        //to display object as a string in spinner
        @Override
        public String toString()
        {
            return name;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof SpinnerValues)
            {
                SpinnerValues c = (SpinnerValues) obj;
                return c.getName().equals(name) && c.getValue() == value;
            }

            return false;
        }
    }

    public static void setInsets(Context ctx, Dialog dialog)
    {
        View decorView = dialog.getWindow() != null ? dialog.getWindow().getDecorView() : null;

        if (decorView != null)
        {
            setInsets(ctx, decorView, false);
        }
    }

    public static void setInsets(Context ctx, View view, boolean isGame)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        {
            ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) ->
            {
                // Always system bars
                int type = WindowInsetsCompat.Type.systemBars();

                // Camera cut, note by default it expands for API35+ now
                if (!SwitchWidget.fetchValue(ctx, isGame ? OptionsDialogKt.EXPAND_INTO_NOTCH : OptionsDialogKt.LAUNCHER_EXPAND_INTO_NOTCH, false))
                {
                    type |= WindowInsetsCompat.Type.displayCutout();
                }

                Insets insets = windowInsets.getInsets(type);

                v.setPadding(insets.left, insets.top, insets.right, insets.bottom);

                return windowInsets;
            });
        }
    }
}
