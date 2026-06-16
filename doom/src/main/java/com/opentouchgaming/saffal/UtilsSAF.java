package com.opentouchgaming.saffal;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.system.Os;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class UtilsSAF
{
    static String TAG = "UtilsSAF";

    static Context context;

    static int cacheNativeFs = 0;
    static boolean CASE_INSENSITIVE = true;

    static List<TreeRoot> treeRoots = new ArrayList<>();

    /**
     * Set a Context so all operations don't need to pass in a new one
     *
     * @param ctx the Context.
     */
    public static void setContext(@NonNull Context ctx, boolean cacheNativeFs)
    {
        UtilsSAF.context = ctx;
        UtilsSAF.cacheNativeFs = cacheNativeFs ? 1 : 0;
        // Load C library
        System.loadLibrary("saffal");
    }

    /**
     * Get the first tree root (for backward compatibility).
     *
     * @return first TreeRoot, or null if none set.
     */
    public static TreeRoot getTreeRoot()
    {
        return treeRoots.isEmpty() ? null : treeRoots.get(0);
    }

    /**
     * Get all tree roots.
     */
    public static List<TreeRoot> getTreeRoots()
    {
        return treeRoots;
    }

    /**
     * Replace all roots with a single root (backward-compatible equivalent of the old setTreeRoot).
     *
     * @param treeRoot the uri and root path.
     */
    public static void setTreeRoot(@NonNull TreeRoot treeRoot)
    {
        treeRoots.clear();
        addTreeRoot(treeRoot);
    }

    /**
     * Add a new root alongside any existing roots.
     *
     * @param treeRoot the uri and root path to add.
     */
    public static void addTreeRoot(@NonNull TreeRoot treeRoot)
    {
        DocumentNode documentRoot = new DocumentNode();
        documentRoot.name = "root";
        documentRoot.isDirectory = true;
        documentRoot.documentId = treeRoot.rootDocumentId;
        documentRoot.treeUri = treeRoot.uri;
        treeRoot.documentRoot = documentRoot;

        treeRoots.add(treeRoot);

        rebuildNativePaths();
    }

    /**
     * Remove the root whose rootPath matches the given path.
     */
    public static void removeTreeRoot(@NonNull String rootPath)
    {
        treeRoots.removeIf(r -> r.rootPath.equals(rootPath));
        rebuildNativePaths();
    }

    /**
     * Remove all tree roots.
     */
    public static void clearTreeRoots()
    {
        treeRoots.clear();
        rebuildNativePaths();
    }

    /**
     * Rebuild the native SAF path list to match the current treeRoots.
     */
    private static void rebuildNativePaths()
    {
        String[] paths = new String[treeRoots.size()];
        for (int i = 0; i < treeRoots.size(); i++)
        {
            paths[i] = treeRoots.get(i).rootPath;
        }
        FileJNI.initSAFPaths(paths, cacheNativeFs);
    }

    /**
     * Return the TreeRoot whose rootPath is a prefix of the given path, or null.
     */
    public static TreeRoot getTreeRootForPath(String path)
    {
        for (TreeRoot root : treeRoots)
        {
            if (path.startsWith(root.rootPath))
                return root;
        }
        return null;
    }

    /**
     * Find the DocumentNode for a full filesystem path across all registered roots.
     *
     * @return DocumentNode if found, otherwise null.
     */
    public static DocumentNode findDocumentNode(String fullPath)
    {
        TreeRoot root = getTreeRootForPath(fullPath);
        if (root == null)
            return null;
        String documentPath = getDocumentPath(fullPath, root);
        return DocumentNode.findDocumentNode(root.documentRoot, documentPath);
    }

    /**
     * Get ContentResolver
     *
     * @return ContentResolver
     */
    public static ContentResolver getContentResolver()
    {
        return context.getContentResolver();
    }

    /**
     * Launch the Select Document screen. You should give some pictures about how to select the internal storage
     *
     * @param activity Your Activity
     * @param code     Code return on onActivityResult
     */
    public static void openDocumentTree(@NonNull Activity activity, int code)
    {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);

        activity.startActivityForResult(intent, code);
    }

    /**
     * Save all registered roots to SharedPreferences so loadTreeRoots can restore them.
     *
     * @param ctx A Context
     */
    public static void saveTreeRoots(Context ctx)
    {
        SharedPreferences prefs = ctx.getSharedPreferences("utilsSAF", 0);
        SharedPreferences.Editor prefsEdit = prefs.edit();

        int count = 0;
        for (TreeRoot root : treeRoots)
        {
            if (root.uri != null && root.rootPath != null && root.rootDocumentId != null)
            {
                prefsEdit.putString("uri_" + count, root.uri.toString());
                prefsEdit.putString("rootPath_" + count, root.rootPath);
                prefsEdit.putString("rootDocumentId_" + count, root.rootDocumentId);
                count++;
            }
        }
        prefsEdit.putInt("count", count);
        prefsEdit.apply();
    }

    /** @deprecated Use {@link #saveTreeRoots(Context)} */
    public static void saveTreeRoot(Context ctx) { saveTreeRoots(ctx); }

    /**
     * Load all previously saved roots. Restores the full set of roots from SharedPreferences.
     * Falls back to the legacy single-root format if no multi-root data is found.
     *
     * @param ctx A Context
     * @return true if at least one root was loaded successfully.
     */
    public static boolean loadTreeRoots(Context ctx)
    {
        try
        {
            SharedPreferences prefs = ctx.getSharedPreferences("utilsSAF", 0);

            int count = prefs.getInt("count", 0);

            if (count > 0)
            {
                treeRoots.clear();
                for (int i = 0; i < count; i++)
                {
                    String url = prefs.getString("uri_" + i, null);
                    String rootPath = prefs.getString("rootPath_" + i, null);
                    String rootDocumentId = prefs.getString("rootDocumentId_" + i, null);
                    if (url != null && rootPath != null && rootDocumentId != null)
                    {
                        addTreeRoot(new TreeRoot(Uri.parse(url), rootPath, rootDocumentId));
                    }
                }
                return !treeRoots.isEmpty();
            }
            else
            {
                // Fall back to legacy single-root format
                String url = prefs.getString("uri", null);
                if (url != null)
                {
                    String rootPath = prefs.getString("rootPath", null);
                    String rootDocumentId = prefs.getString("rootDocumentId", null);
                    if (rootPath != null && rootDocumentId != null)
                    {
                        setTreeRoot(new TreeRoot(Uri.parse(url), rootPath, rootDocumentId));
                        return true;
                    }
                }
            }
        }
        catch (IllegalArgumentException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    /** @deprecated Use {@link #loadTreeRoots(Context)} */
    public static boolean loadTreeRoot(Context ctx) { return loadTreeRoots(ctx); }

    /**
     * Returns true when at least one SAF root is configured and a context is set.
     *
     * @return True if ready
     */
    public static boolean ready()
    {
        return !treeRoots.isEmpty() && context != null;
    }

    /**
     * Returns true if the path falls under any registered SAF root.
     *
     * @return True if in SAF space
     */
    public static boolean isInSAFRoot(String path)
    {
        return getTreeRootForPath(path) != null;
    }

    /**
     * Returns true if the path is a prefix of (or equal to) any registered SAF root path.
     *
     * @return True if path leads into a SAF root
     */
    public static boolean isRootOfSAFRoot(String path)
    {
        if (ready())
        {
            String[] inputPathParts = path.split("/");
            for (TreeRoot root : treeRoots)
            {
                String[] startStringParts = root.rootPath.split("/");
                if (inputPathParts.length > startStringParts.length)
                    continue;
                boolean match = true;
                for (int i = 0; i < inputPathParts.length; i++)
                {
                    if (!inputPathParts[i].equals(startStringParts[i]))
                    {
                        match = false;
                        break;
                    }
                }
                if (match)
                    return true;
            }
        }
        return false;
    }

    // Found at: https://stackoverflow.com/questions/30546441/android-open-file-with-intent-chooser-from-uri-obtained-by-storage-access-frame
    public static String getFdPath(int fd)
    {
        final String resolved;

        try
        {
            final File procfsFdFile = new File("/proc/self/fd/" + fd);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            {
                // Returned name may be empty or "pipe:", "socket:", "(deleted)" etc.
                resolved = Os.readlink(procfsFdFile.getAbsolutePath());
            }
            else
            {
                // Returned name is usually valid or empty, but may start from
                // funny prefix if the file does not have a name
                resolved = procfsFdFile.getCanonicalPath();
            }

            if (TextUtils.isEmpty(resolved) || resolved.charAt(0) != '/' || resolved.startsWith("/proc/") || resolved.startsWith("/fd/"))
                return null;
        }
        catch (IOException ioe)
        {
            // This exception means, that given file DID have some name, but it is
            // too long, some of symlinks in the path were broken or, most
            // likely, one of it's directories is inaccessible for reading.
            // Either way, it is almost certainly not a pipe.
            return "";
        }
        catch (Exception errnoe)
        {
            // Actually ErrnoException, but base type avoids VerifyError on old versions
            // This exception should be VERY rare and means, that the descriptor
            // was made unavailable by some Unix magic.
            return null;
        }

        return resolved;
    }

    /**
     * Returns a REAL File, even for files in SAF! NOTE, the file name and path of the file in SAF may be incorrect
     *
     * @return File or null
     */
    //@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static File getRealFile(String path)
    {
        DBG("getRealFile: " + path);
        File file = null;
        if (ready() && isInSAFRoot(path))
        {
            FileSAF fileSAF = new FileSAF(path);
            // fileskeep.add(fileSAF);
            if (fileSAF.exists())
            {
                file = fileSAF.getRealFile(path);
            }
        }
        else
        {
            file = new File(path);
        }

        return file;
    }


    // public static ArrayList<FileSAF> fileskeep = new ArrayList<>();

    static InputStream getInputStream(DocumentFile docFile) throws FileNotFoundException
    {
        return context.getContentResolver().openInputStream(docFile.getUri());
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    static ParcelFileDescriptor getParcelDescriptor(String documentId, Uri treeUri, boolean write) throws IOException
    {
        Uri uri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId);

        // NOTE! If we are writing we ALWAYS truncate the file (rwt), this means append won't work, will fix if needed
        ParcelFileDescriptor filePfd = context.getContentResolver().openFileDescriptor(uri, write ? "rwt" : "r");

        return filePfd;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    static boolean renameDocument(String documentId, Uri treeUri, String newName) throws IOException
    {
        Uri uri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId);

        DocumentsContract.renameDocument(context.getContentResolver(), uri, newName);

        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    static boolean deleteDocument(String documentId, Uri treeUri) throws IOException
    {
        Uri uri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId);

        return DocumentsContract.deleteDocument(context.getContentResolver(), uri);
    }

    public static String getDocumentPath(String fullPath)
    {
        TreeRoot root = getTreeRootForPath(fullPath);
        if (root == null)
        {
            DBG("getDocumentPath: ERROR, no SAF root matches path: " + fullPath);
            return null;
        }
        return getDocumentPath(fullPath, root);
    }

    private static String getDocumentPath(String fullPath, TreeRoot root)
    {
        if (fullPath.length() > root.rootPath.length())
        {
            return fullPath.substring(root.rootPath.length() + 1); // Remove the leading "/"
        }
        else
        {
            return "";
        }
    }

    public static String[] getParts(String fullPath)
    {
        String childPath = getDocumentPath(fullPath);

        if (childPath == null || childPath.contentEquals(""))
            return new String[0];
        else
            return childPath.split("\\/", -1);
    }

    private static void DBG(String str)
    {
        Log.d(TAG, str);
    }

    /*
        Holds the URI returned from ACTION_OPEN_DOCUMENT_TREE (important!)
        Also the File system 'root' this should point to E.G could be '/storage/emulated/0' for internal files
     */
    public static class TreeRoot
    {
        public Uri uri;
        public String rootPath;
        public String rootDocumentId;
        // Set by addTreeRoot; holds the root DocumentNode for this tree
        public DocumentNode documentRoot;

        public TreeRoot(Uri uri, String rootPath, String rootDocumentId)
        {
            this.uri = uri;
            this.rootPath = rootPath;
            this.rootDocumentId = rootDocumentId;
        }
    }
}
