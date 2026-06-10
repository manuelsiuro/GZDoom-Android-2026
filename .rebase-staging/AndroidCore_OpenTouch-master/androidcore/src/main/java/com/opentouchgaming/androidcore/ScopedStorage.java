package com.opentouchgaming.androidcore;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import androidx.annotation.RequiresApi;

import com.opentouchgaming.saffal.FileSAF;
import com.opentouchgaming.saffal.UtilsSAF;

import java.util.function.Function;

public class ScopedStorage
{

    public static final int OPENDOCUMENT_TREE_RESULT = 5005;
    // The string will be null on OK, or contain an error message
    public static Function<String, Void> openDocumentCallback;
    static DebugLog log;
    // This is so we can ignore the SAF test in onResume if we JUST set it, the permission is not ready to use yet
    static boolean justGotNewSaf = false;

    static
    {
        log = new DebugLog(DebugLog.Module.GAMEFRAGMENT, "ScopedStorage");
    }

    // Check if storage is setup, true if OK
    static public boolean checkStorageOK(Activity activity)
    {
        // Always set context, this also loads the saffal library, needed if even not used
        UtilsSAF.setContext(activity, false);

        if (!AppInfo.isScopedEnabled())
        {
            //Thank fuck scoped storage not necessary
            return true;
        }
        else
        {
            UtilsSAF.loadTreeRoot(activity);

            // Check if the first time are checking this
            if (!AppSettings.getBoolOption(activity, "storage_checked", false))
            {
                // First reset the primary folder, do this in call cases even an upgrade
                // Example will be set to: /Android/data/com.opentouchgaming.deltatouch/files
                AppInfo.setAppDirectory(AppInfo.getDefaultAppDirectory());
                AppSettings.setBoolOption(activity, "storage_checked", true);
            }

            if (!justGotNewSaf)
            {
                // Check we can still access the SAF folder OK, to avoid security exceptions later on
                if (AppInfo.getAppSecDirectory() != null)
                {
                    if (UtilsSAF.isInSAFRoot(AppInfo.getAppSecDirectory()))
                    {
                        FileSAF root = new FileSAF(AppInfo.getAppSecDirectory());
                        try
                        {
                            // Do a quick test to see if it create exceptions
                            if (root.exists())
                            {
                                root.listFiles();
                            }
                        }
                        catch (Exception e)
                        {
                            log.log(DebugLog.Level.D, "Can not read root from SAF, resetting to default");
                            AppInfo.setAppSecDirectory(AppInfo.getDefaultAppSecDirectory());
                        }
                    }
                }
            }

            justGotNewSaf = false;

            return false;
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    static public void activityResult(Activity activity, int requestCode, int resultCode, Intent data)
    {

        if (requestCode == OPENDOCUMENT_TREE_RESULT)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                Uri treeUri = data.getData();

                Uri docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri));
                String rootDocumentId = DocumentsContract.getDocumentId(docUri);

                log.log(DebugLog.Level.D, "rootDocumentId = " + rootDocumentId);
                log.log(DebugLog.Level.D, "getAuthority = " + treeUri.getAuthority());

                if (!treeUri.getAuthority().contentEquals("com.android.externalstorage.documents"))
                {
                    openDocumentCallback.apply("Did not choose a valid folder.");
                    return;
                }

                String root;

                // Work out if internal memory or SD card was selected
                if (rootDocumentId != null)
                {
                    String[] split = rootDocumentId.split(":", -1);

                    if (split[0].contentEquals("primary"))
                        root = "/[internal]";
                    else
                        root = "/[SD-Card]";

                    if (split.length > 1 && split[1].length() > 0)
                    {
                        root = root + "/" + split[1];
                    }
                }
                else
                {
                    openDocumentCallback.apply("Return path is null.");
                    return;
                }

                // Setup the UtilsSAF static data
                UtilsSAF.setTreeRoot(new UtilsSAF.TreeRoot(treeUri, root, rootDocumentId));

                // Save URI for next launch
                UtilsSAF.saveTreeRoot(activity);

                // Take permissions for ever
                final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                activity.getContentResolver().takePersistableUriPermission(treeUri, takeFlags);

                justGotNewSaf = true;

                AppInfo.setAppSecDirectory(UtilsSAF.getTreeRoot().rootPath);

                if (openDocumentCallback != null)
                    openDocumentCallback.apply(null);
            }
            else
            {
                if (openDocumentCallback != null)
                    openDocumentCallback.apply("Folder selection canceled");
            }
        }
    }
}
