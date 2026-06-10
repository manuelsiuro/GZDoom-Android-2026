package com.opentouchgaming.androidcore;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.Log;

import com.opentouchgaming.saffal.FileSAF;
import com.opentouchgaming.saffal.UtilsSAF;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtils
{
    public static void extractInputStream(InputStream is, String destDir, ProgressDialog progressBar) throws IOException
    {
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null)
        {
            if (entry.isDirectory())
            {
                (new File(destDir, entry.getName())).mkdirs();
                continue;
            }

            (new File(destDir, entry.getName())).getParentFile().mkdirs();
            BufferedInputStream zin = new BufferedInputStream(zis);
            OutputStream out = new FileOutputStream(new File(destDir, entry.getName()));
            Utils.copyFile(zin, out, progressBar);
            out.flush();
            out.close();
        }
    }

    public static boolean extractFile(String zipFilename, String filename, String outputFile)
    {
        Log.i("ZipUtils", "extractFile zip: " + zipFilename + " Extract: " + filename + " Output: " + outputFile);

        boolean fileFound = false;

        if (UtilsSAF.isInSAFRoot(zipFilename))
        {
            try
            {
                FileSAF file = new FileSAF(zipFilename);
                InputStream inputStream = file.getInputStream();
                ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(inputStream));
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null)
                {
                    if (entry.getName().contentEquals(filename))
                    {
                        Log.i("ZipUtils", "FOUND");

                        new File(outputFile).getParentFile().mkdirs();

                        OutputStream out = new FileOutputStream(outputFile);
                        Utils.copyFile(zipInputStream, out);
                        inputStream.close();
                        fileFound = true;
                        break;
                    }
                }
                inputStream.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            try
            {
                File file = new File(zipFilename);
                if (file != null)
                {
                    ZipFile zipFile = new ZipFile(file);
                    File outFile = new File(outputFile);
                    FileHeader header = zipFile.getFileHeader(filename);
                    if (header != null)
                    {
                        zipFile.extractFile(header, outFile.getParent(), outFile.getName());
                        fileFound = true;
                    }
                }
            }
            catch (ZipException e)
            {
                e.printStackTrace();
                fileFound = false;
            }
        }
        return fileFound;
    }

    // Different from above because it is just looking at the filename in any directory
    public static boolean searchZip(String zipFilename, String searchName, String outputFile)
    {
        if (UtilsSAF.isInSAFRoot(zipFilename))
        {
            try
            {
                FileSAF file = new FileSAF(zipFilename);
                InputStream inputStream = file.getInputStream();
                ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(inputStream));
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null)
                {
                    if (entry.getName().toLowerCase().contains(searchName))
                    {
                        OutputStream out = new FileOutputStream(outputFile);
                        Utils.copyFile(zipInputStream, out);
                        inputStream.close();
                        return true;
                    }
                }
                inputStream.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            return false;
        }
        else
        {
            boolean fileFound = false;
            try
            {
                File file = new File(zipFilename);

                if (file != null)
                {
                    ZipFile zipFile = new ZipFile(file);
                    for (FileHeader header : zipFile.getFileHeaders())
                    {
                        if (!header.isDirectory())
                        {
                            // log.log(D,header.getFileName());
                            if (header.getFileName().toLowerCase().contains(searchName))
                            {
                                File outFile = new File(outputFile);
                                zipFile.extractFile(header, outFile.getParent(), outFile.getName());
                                fileFound = true;
                                break;
                            }
                        }
                    }
                }
            }
            catch (ZipException e)
            {
                e.printStackTrace();
            }
            return fileFound;
        }
    }

    public static ZipFile findAllFilesExtension(String zipFilename, String extension, ArrayList<String> output) throws ZipException
    {
        File file = new File(zipFilename);

        ZipFile zipFile = new ZipFile(file);
        for (FileHeader header : zipFile.getFileHeaders())
        {
            if (!header.isDirectory())
            {
                // log.log(D,header.getFileName());
                if (header.getFileName().toLowerCase().endsWith(extension))
                {
                    output.add(header.getFileName());

                }
            }
        }
        return zipFile;
    }

    public static InputStream getInputStream(String zipFilename, String filename)
    {
        ZipFile zip = new ZipFile(zipFilename);
        InputStream is = null;
        try
        {
            is = zip.getInputStream(zip.getFileHeader(filename));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return is;
    }

    public static boolean extractAsset(Context ctx, String file, String dest, long size)
    {
        try
        {
            AssetManager assetManager = ctx.getAssets();
            InputStream in = assetManager.open(file);
            in.close();
        }
        catch (IOException e)
        {
            return false;
        }

        ExtractAsset extract = new ExtractAsset(ctx, size);
        extract.execute(file, dest);
        return true;
    }

    static private class ExtractAsset extends AsyncTask<String, Integer, Long>
    {
        Context ctx;
        long totalSize;
        String errorstring = null;
        private ProgressDialog progressBar;

        ExtractAsset(Context ctx, long totalSize)
        {
            this.ctx = ctx;
            this.totalSize = totalSize;
        }

        @Override
        protected void onPreExecute()
        {
            progressBar = new ProgressDialog(ctx);
            progressBar.setMessage("Extracting files..");
            progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressBar.setCancelable(false);
            progressBar.show();
        }

        int getTotalZipSize(InputStream ins)
        {
            int ret = 0;
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(ins));
            ZipEntry entry;
            try
            {
                while ((entry = zis.getNextEntry()) != null)
                {
                    if (!entry.isDirectory())
                    {
                        ret += entry.getSize();
                    }
                }
                ins.reset();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            return ret;
        }

        protected Long doInBackground(String... info)
        {
            String file = info[0];
            String basePath = info[1];

            progressBar.setProgress(0);

            try
            {
                AssetManager assetManager = ctx.getAssets();
                InputStream ins = assetManager.open(file);

                if (file.endsWith(".zip"))
                {
                    if (totalSize != 0)
                        progressBar.setMax((int) totalSize);
                    else
                        progressBar.setMax(getTotalZipSize(ins));

                    extractInputStream(ins, basePath, progressBar);
                }
                else
                {
                    File outZipFile = new File(basePath, "temp.zip");
                    FileOutputStream fout = new FileOutputStream(outZipFile);
                    Utils.copyFile(ins, fout, progressBar);

                    outZipFile.renameTo(new File(basePath, file));
                    return 0L;
                }

            }
            catch (IOException e)
            {
                errorstring = e.toString();
                return 1L;
            }

            return 0L;
        }

        protected void onProgressUpdate(Integer... progress)
        {
        }

        protected void onPostExecute(Long result)
        {
            progressBar.dismiss();
            if (errorstring != null)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                builder.setMessage("Error extracting: " + errorstring).setCancelable(true).setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {

                    }
                });

                builder.show();
            }
        }
    }
}
