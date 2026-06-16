package com.opentouchgaming.androidcore;

import static com.opentouchgaming.androidcore.DebugLog.Level.D;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;

import com.opentouchgaming.androidcore.license.PackageVerif;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class ServerAPI
{
    static DebugLog log;
    static Activity ctx;
    static int size;
    static Callback callback;

    static
    {
        log = new DebugLog(DebugLog.Module.CORE, "ServerAPI");
    }

    public static void downloadFile(Activity ctx, String file, String path, int size, Callback cb)
    {
        ServerAPI.size = size;
        ServerAPI.ctx = ctx;
        ServerAPI.callback = cb;

        new DLFileThread().execute(file, path);
    }

    public interface Callback
    {
        void callback(boolean complete);
    }

    static private class DLFileThread extends AsyncTask<String, Integer, Long>
    {
        final int STATUS_COMPLETE = 0;
        final int STATUS_FILE_ERROR = -1;
        final int STATUS_BAD_CONNECTION = -2;
        final int STATUS_BAD_RESP_CODE = -3;
        final int STATUS_INCOMPLETE = -4;
        final int STATUS_EXCEP_WHILE_DL = -5;
        ProgressDialog progressBar;
        String errorstring = null;
        long downloadSize = -1;
        long downloadedBytes = 0;
        boolean cancel = false;

        @Override
        protected void onPreExecute()
        {
            progressBar = new ProgressDialog(ctx);
            progressBar.setMessage("Downloading/Extracting files..");
            progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressBar.setCancelable(false);
            progressBar.setCancelable(false);
            progressBar.setButton("Cancel", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    cancel = true;
                }
            });
            progressBar.show();
        }

        int getTotalZipSize(String file) throws IOException
        {
            int ret = 0;

            ZipFile zf = new ZipFile(file);
            Enumeration e = zf.entries();
            while (e.hasMoreElements())
            {
                ZipEntry ze = (ZipEntry) e.nextElement();
                ret += ze.getSize();
            }

            return ret;
        }

        private Status tryDownload(String downloadFilename, String destFilename)
        {
            File destFile;
            OutputStream fout;
            String urlString;

            try
            {
                destFile = new File(destFilename);
                downloadedBytes = destFile.length();

                log.log(D, "Already have " + downloadedBytes + " bytes downloaded");

                fout = new FileOutputStream(destFile, true);

                File f = new File(AppInfo.internalFiles + "/l.dat");
                BufferedReader b = new BufferedReader(new FileReader(f));
                String lic_data = b.readLine();
                String lic_sig = b.readLine();

                String apk_hash = PackageVerif.bytesToString(PackageVerif.packageSig(ctx).sig);

                urlString = "http://opentouchgaming.com/api/download_v5.php?" +
                            "ldata=" +
                            URLEncoder.encode(lic_data, "UTF-8") +
                            "&lsig=" +
                            URLEncoder.encode(lic_sig, "UTF-8") +
                            "&apkhash=" +
                            URLEncoder.encode(apk_hash, "UTF-8") +
                            "&file=" +
                            URLEncoder.encode(downloadFilename, "UTF-8") +
                            "&pos=" +
                            downloadedBytes;

                log.log(D, "urlString = " + urlString);

            }
            catch (IOException e)
            {
                // Can not recover from file errors
                return new Status(STATUS_FILE_ERROR, e.toString());
            }

            InputStream inputStream = null;

            // Detect if we downloaded anything
            boolean didDownload = false;
            try
            {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent",
                                              "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.110 " +
                                              "Safari/537.36");
                connection.setRequestMethod("GET");
                //connection.setDoOutput(true);
                connection.setRequestProperty("Connection", "keep-alive");
                connection.setRequestProperty("Accept-Encoding", "identity");
                connection.connect();

                int code = connection.getResponseCode();

                if (code != 200)
                {
                    log.log(D, "resp message = " + errorstring);
                    return new Status(STATUS_BAD_RESP_CODE, connection.getResponseMessage());
                }

                log.log(D, "resp code = " + code);

                inputStream = connection.getInputStream();

                downloadSize = connection.getContentLength();

                if (downloadSize == -1)
                {
                    return new Status(STATUS_BAD_CONNECTION, "Download size is unknown");
                }

                progressBar.setMax((int) downloadSize);

                log.log(D, "Download size is " + downloadSize);

                byte[] data = new byte[1024 * 10];
                int count;
                while ((downloadedBytes < downloadSize) && ((count = inputStream.read(data, 0, data.length)) != -1))
                {
                    fout.write(data, 0, count);
                    fout.flush();

                    didDownload = true;

                    downloadedBytes += count;

                    progressBar.setProgress((int) downloadedBytes);

                    if (cancel)
                        break;
                }
                inputStream.close();
                fout.close();

            }
            catch (IOException e)
            {
                e.printStackTrace();
                if (didDownload)
                    return new Status(STATUS_EXCEP_WHILE_DL, e.toString());
                else
                    return new Status(STATUS_BAD_CONNECTION, e.toString());
            }


            // Check we got a download size and download is complete
            downloadedBytes = new File(destFilename).length();

            if (downloadedBytes >= downloadSize)
            {
                return new Status(STATUS_COMPLETE, "");
            }
            else
            {
                return new Status(STATUS_INCOMPLETE, "Expected: " + downloadSize + " got: " + downloadedBytes);
            }
        }

        protected Long doInBackground(String... info)
        {
            String downloadFilename = info[0];
            String basePath = info[1];

            progressBar.setProgress(0);

            String cachedDownload = AppInfo.cacheFiles + "/" + downloadFilename + ".tmp";

            log.log(D, "cachedDownload = " + cachedDownload);

            Status status;

            int connectionTries = 10; // This number of connection attempts

            while (true)
            {
                status = tryDownload(downloadFilename, cachedDownload);
                log.log(D, "status.code = " + status.code + ", status.message = " + status.message);

                if (cancel)
                {
                    return 0L;
                }

                if (status.code == STATUS_COMPLETE)
                {
                    break;
                }
                else if (status.code == STATUS_BAD_CONNECTION)
                {
                    log.log(D, "connectionTries = " + connectionTries);
                    //progressBar.setMessage("connectionTries = " + connectionTries);
                    connectionTries--;
                    if (connectionTries > 0)
                    {
                        // Wait a second
                        try
                        {
                            Thread.sleep(1000);
                        }
                        catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                    }
                    else
                    {
                        errorstring = status.message;
                        return 0L;
                    }
                }
                else if (status.code == STATUS_EXCEP_WHILE_DL) // If it did download something, don't decrement the attempts
                {
                    // Wait a second
                    try
                    {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
                else // Unrecoverable error
                {
                    errorstring = status.message;
                    return 0L;
                }
            }


            if (status.code == STATUS_COMPLETE)
            {
                if (downloadFilename.endsWith(".zip"))
                {
                    try
                    {
                        int extractedSize = getTotalZipSize(cachedDownload);
                        progressBar.setMax(extractedSize);
                        progressBar.setProgress(0);

                        InputStream zipFile = new FileInputStream(cachedDownload);
                        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(zipFile));
                        ZipEntry entry;
                        while ((entry = zis.getNextEntry()) != null)
                        {
                            File f = new File(basePath, entry.getName());
                            String canonicalPath = f.getCanonicalPath();
                            if (!canonicalPath.startsWith(basePath))
                            {
                                throw new IOException("Bad path traversal");
                            }

                            if (entry.isDirectory())
                            {
                                (new File(basePath, entry.getName())).mkdirs();
                                continue;
                            }
                            log.log(D, "Extracting file: " + entry.getName());
                            (new File(basePath, entry.getName())).getParentFile().mkdirs();
                            BufferedInputStream zin = new BufferedInputStream(zis);
                            OutputStream out = new FileOutputStream(new File(basePath, entry.getName()));
                            Utils.copyFile(zin, out, progressBar);

                            if (cancel)
                                break;
                        }
                    }
                    catch (FileNotFoundException e)
                    {
                        errorstring = e.toString();
                        e.printStackTrace();
                    }
                    catch (IOException e)
                    {
                        errorstring = e.toString();
                        e.printStackTrace();
                    }

                    if (!cancel)
                    {
                        // Delete the file, could be corrupt or finished
                        new File(cachedDownload).delete();
                    }

                }
                else // Copy
                {
                    progressBar.setMax((int) downloadSize);
                    progressBar.setProgress(0);

                    try
                    {
                        InputStream in = new FileInputStream(cachedDownload);
                        OutputStream out = new FileOutputStream(new File(basePath, downloadFilename));
                        Utils.copyFile(in, out, progressBar);
                    }
                    catch (FileNotFoundException e)
                    {
                        errorstring = e.toString();
                        e.printStackTrace();
                    }
                    catch (IOException e)
                    {
                        errorstring = e.toString();
                        e.printStackTrace();
                    }
                    new File(cachedDownload).delete();
                }
            }

            return 0L;
        }

        protected void onProgressUpdate(Integer... progress)
        {

        }

        public void dismissWithExceptionHandling(ProgressDialog dialog)
        {
            try
            {
                dialog.dismiss();
            }
            catch (final IllegalArgumentException e)
            {
                // Do nothing.
            }
            catch (final Exception e)
            {
                // Do nothing.
            }
            finally
            {
                dialog = null;
            }
        }

        protected void onPostExecute(Long result)
        {
            if (progressBar != null && progressBar.isShowing())
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                {
                    if (!ctx.isFinishing() && !ctx.isDestroyed())
                    {
                        dismissWithExceptionHandling(progressBar);
                    }
                }
                else
                {
                    // Api < 17. Unfortunately cannot check for isDestroyed()
                    if (!ctx.isFinishing())
                    {
                        dismissWithExceptionHandling(progressBar);
                    }
                }
                progressBar = null;
            }

            if (errorstring != null)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                builder.setMessage("Error accessing server: " + errorstring).setCancelable(true).setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {

                    }
                });

                builder.show();
            }

            if (ServerAPI.callback != null)
            {
                ServerAPI.callback.callback(errorstring != null);
                ServerAPI.callback = null;
            }
        }

        class Status
        {
            int code;
            String message;

            Status(int code, String message)
            {
                this.code = code;
                this.message = message;
            }
        }
    }
}
