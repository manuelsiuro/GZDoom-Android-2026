package com.opentouchgaming.saffal;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileJNI
{
    final static String TAG = "FileJNI JAVA";

    // Init the C library with all current SAF root paths so path checks can be done in C code
    public static native void initSAFPaths(String[] SAFPaths, int cacheNativeFs);

    public static int fopen(final String filePath, final String mode)
    {
        // Log.d(TAG, "fopen file = " + filePath);

        boolean write = mode.contains("w") || mode.contains("a");

        FileSAF fileSAF = new FileSAF(filePath);

        // If not in the SAF area return -1
        if (!UtilsSAF.isInSAFRoot(fileSAF.getPath()))
        {
            return -1;
        }

        // Try to create if it does not exist
        if (write && !fileSAF.exists())
        {
            try
            {
                fileSAF.createNewFile();
            }
            catch (IOException e)
            {
                return -1;
            }
        }

        int fd = fileSAF.getFd(write, true);
        //Log.d(TAG, "fd = " + fd);

        return fd;
    }


    public static int mkdir(String path)
    {
        Log.d(TAG, "mkdir path = " + path);

        FileSAF fileSAF = new FileSAF(path);

        //If the path we are trying to make is already at the start of the SAF root then return success
        if (UtilsSAF.isRootOfSAFRoot(fileSAF.getPath()))
        {
            return 0;
        }// If not in the SAF area return -1 so the NDK code knows this
        else if (!UtilsSAF.isInSAFRoot(fileSAF.getPath()))
        {
            return -1;
        }

        if (fileSAF.mkdirs() == true) // Success
        {
            return 0;
        }
        else // Failed
        {
            return 1;
        }
    }

    public static int exists(String path)
    {
        //Log.d(TAG, "exists path = " + path);

        FileSAF fileSAF = new FileSAF(path);

        if (fileSAF.exists())
        {
            return 1;
        }
        else
        {
            return 0;
        }
    }

    public static int delete(String path)
    {
        Log.d(TAG, "delete path = " + path);

        FileSAF fileSAF = new FileSAF(path);

        if (fileSAF.delete())
        {
            return 1;
        }
        else
        {
            return 0;
        }
    }

    public static int rename(String oldFilename, String newFilename)
    {
        Log.d(TAG, "rename old = " + oldFilename + ", new = " + newFilename);
        FileSAF oldFile = new FileSAF(oldFilename);
        FileSAF newFile = new FileSAF(newFilename);

        // This implementation had a limitation in that the file path can not change in the rename, can be fixed later if needed
        String oldParent = oldFile.getParentFile().getPath();
        String newParent = newFile.getParentFile().getPath();
        if (oldParent.contentEquals(newParent))
        {
            // Attempt file rename
            try
            {
                // Delete target file, otherwise SAF creates random new filenames
                if(newFile.exists())
                    newFile.delete();

                oldFile.rename(newFile.getName());
                return 0;
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return -1;
            }
        }
        else
        {
            // If both files in SAF need to copy the data for a rename
            if (UtilsSAF.isInSAFRoot(oldParent) && UtilsSAF.isInSAFRoot(newParent))
            {
                if (oldFile.exists())
                {
                    try
                    {
                        if(newFile.exists())
                            newFile.delete();

                        newFile.createNewFile();
                        InputStream in = oldFile.getInputStream();
                        DataOutputStream out = new DataOutputStream(newFile.getOutputStream());

                        byte[] buffer = new byte[1024 * 4];
                        int read;
                        while ((read = in.read(buffer)) != -1)
                        {
                            out.write(buffer, 0, read);
                        }

                        // Delete the file moved
                        oldFile.delete();

                        // Clear the caches of the parents as files have changed
                        new FileSAF(oldParent).clearCache();
                        new FileSAF(newParent).clearCache();

                        return 0;
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to rename file by copying " + oldParent + " -> " + newParent);
                        return -1;
                    }
                }
                Log.e(TAG, "Files already or do not exist for rename");
                return -1;
            }
            else
            {
                Log.e(TAG, "Parent paths for rename do not match: " + oldParent + " -> " + newParent);
                return -1;
            }
        }
    }

    /**
     * Try to open the path in SAF and return a string array of the items in the directory
     *
     * @param path
     * @return String array of the items in the directory. Appened with F (is a file) or D (if a directory)
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static String[] opendir(String path)
    {
        //Log.d(TAG, "opendir path = " + path);

        FileSAF fileSAF = new FileSAF(path);

        if (fileSAF.exists() && fileSAF.isDirectory)
        {
            FileSAF[] files = fileSAF.listFiles();
            String[] ret = new String[files.length];

            for (int n = 0; n < files.length; n++)
            {
                ret[n] = (files[n].isDirectory() ? "D" : "F") + files[n].getName();
            }

            return ret;
        }
        else
        {
            return new String[]{};
        }
    }
}
