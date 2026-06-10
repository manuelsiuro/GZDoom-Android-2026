package com.opentouchgaming.saffal;

import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class FileSAF extends File
{
    final String TAG = "FileSAF";

    // Full path
    String fullPath;

    boolean isRealFile = false;
    // is Directory
    boolean isDirectory;

    DocumentNode documentNode;

    // Keep this for NDK. I think the file gets closed if this is GC'ed
    ParcelFileDescriptor parcelFileDescriptor;

    // FD if getFd called
    int fd;

    public FileSAF(String path, String name)
    {
        this(path + "/" + name);
    }

    public FileSAF(FileSAF path, String name)
    {
        this(path.getPath() + "/" + name);
    }

    public FileSAF(String path)
    {
        super(path);

        isRealFile = !UtilsSAF.isInSAFRoot(path);

        try
        {
            this.fullPath = new File(path).getCanonicalPath();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public boolean isRealFile()
    {
        return isRealFile;
    }

    public String getPath()
    {
        return fullPath;
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public FileSAF getParentFile()
    {
        return new FileSAF(getParent());
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean isDirectory()
    {
        if (isRealFile)
            return super.isDirectory();
        else
        {
            updateDocumentNode(false);
            return isDirectory;
        }
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean canWrite()
    {
        if (isRealFile)
            return super.canWrite();
        else
        {
            updateDocumentNode(true);
            // Presume we can always write to SAF area
            return documentNode != null;
        }
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean exists()
    {
        if (isRealFile)
        {
            boolean exists = super.exists();

            if (!exists)
            {
                // Try to open it to test it exists also, sometimes the exists() function does not work?!
                try
                {
                    FileInputStream o = new FileInputStream(this);
                    o.close();
                    exists = true;
                }
                catch (FileNotFoundException e)
                {
                    //e.printStackTrace();
                }
                catch (IOException e)
                {
                    //e.printStackTrace();
                }
            }
            return exists;
        }
        else
        {
            updateDocumentNode(true);
            return documentNode != null;
        }
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public long length()
    {
        if (isRealFile)
            return super.length();
        else
        {
            updateDocumentNode(false);
            if (documentNode != null)
            {
                return documentNode.size;
            }
            return 0;
        }
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public long lastModified()
    {
        if (isRealFile)
            return super.lastModified();
        else
        {
            updateDocumentNode(false);
            if (documentNode != null)
            {
                return documentNode.modifiedDate;
            }
            return 0;
        }
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean createNewFile() throws IOException
    {
        if (isRealFile)
            return super.createNewFile();
        else
        {
            updateDocumentNode(true);
            if (documentNode == null)
            {
                DocumentNode parentNode = UtilsSAF.findDocumentNode(getParent());

                if (parentNode != null && parentNode.isDirectory)
                {
                    String filename = getName();
                    parentNode.createChild(false, filename);
                    return true;
                }
                else
                {
                    throw new IOException("Parent directory is invalid");
                }
            }
            return false;
        }
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean mkdirs()
    {
        if (isRealFile)
            return super.mkdirs();
        else
        {

            updateDocumentNode(true);

            if (documentNode == null)
            {
                UtilsSAF.TreeRoot root = UtilsSAF.getTreeRootForPath(fullPath);

                try
                {
                    String documentPath = UtilsSAF.getDocumentPath(fullPath);
                    documentNode = root != null ? DocumentNode.createAllNodes(root.documentRoot, documentPath) : null;
                }
                catch (FileNotFoundException e)
                {
                    e.printStackTrace();
                }

                if (documentNode != null)
                {
                    DBG("FileSAF (" + fullPath + ") created folders: DocumentId = " + documentNode.documentId);
                    isDirectory = documentNode.isDirectory;
                    return true;
                }
            }
            else // Already exists
            {
                return true;
            }
        }
        return false;
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean isFile()
    {
        if (isRealFile)
            return super.isFile();
        else
        {
            updateDocumentNode(false);
            if (documentNode != null)
            {
                return !documentNode.isDirectory;
            }
            else
                return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public InputStream getInputStream() throws FileNotFoundException
    {
        InputStream is = null;
        if (isRealFile)
        {
            is = new FileInputStream(this);
        }
        else
        {
            updateDocumentNode(true);

            if (documentNode != null)
            {
                is = documentNode.getInputStream();
            }
            else
            {
                throw new FileNotFoundException();
            }
        }

        return is;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public OutputStream getOutputStream() throws FileNotFoundException
    {
        OutputStream os = null;
        if (isRealFile)
        {
            os = new FileOutputStream(this);
        }
        else
        {
            updateDocumentNode(true);

            if (documentNode != null)
            {
                os = documentNode.getOutputStream();
            }
        }

        return os;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public int getFd(boolean write, boolean detach)
    {
        updateDocumentNode(true);

        if (documentNode != null)
        {
            try
            {
                parcelFileDescriptor = UtilsSAF.getParcelDescriptor(documentNode.documentId, documentNode.treeUri, write);
                if (detach)
                    fd = parcelFileDescriptor.detachFd();
                else
                    fd = parcelFileDescriptor.getFd();
                return fd;
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return -1; // Failed
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public File getRealFile(String path)
    {
        File file = null;

        if (exists())
        {
            int fd = getFd(false, true);

            String linkFileName = UtilsSAF.getFdPath(fd);

            if (linkFileName != null && linkFileName.length() > 0)
            {
                file = new File(linkFileName);
            }

            DBG("getRealFile: Real File = " + linkFileName);
        }

        return file;
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public String[] list()
    {
        if (isRealFile)
        {
            return super.list();
        }
        else
        {
            ArrayList<String> strRet = new ArrayList<>();
            FileSAF[] files = listFiles();
            for (FileSAF file : files)
            {
                strRet.add(file.getName());
            }
            return strRet.toArray(new String[files.length]);
        }
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public FileSAF[] listFiles()
    {

        if (isRealFile)
        {
            ArrayList<FileSAF> files = new ArrayList<>();
            File[] realFiles = super.listFiles();
            if (realFiles != null)
            {
                for (File f : realFiles)
                {
                    files.add(new FileSAF(f.getAbsolutePath()));
                }
            }
            return files.toArray(new FileSAF[files.size()]);
        }
        else
        {
            updateDocumentNode(true);

            ArrayList<FileSAF> files = new ArrayList<>();

            if (documentNode != null)
            {
                if (documentNode.isDirectory)
                {
                    {
                        ArrayList<DocumentNode> nodes = documentNode.getChildren();

                        // For each valid document, create a new FileSAF and return
                        for (DocumentNode node : nodes)
                        {
                            files.add(new FileSAF(fullPath + "/" + node.name));
                        }
                    }
                }
            }
            return files.toArray(new FileSAF[files.size()]);
        }
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean delete()
    {
        if (isRealFile)
        {
            return super.delete();
        }
        else
        {
            DocumentNode parentNode = UtilsSAF.findDocumentNode(getParent());

            if (parentNode != null && parentNode.isDirectory)
            {
                String filename = getName();
                try
                {
                    return parentNode.deleteChild(filename);
                }
                catch (FileNotFoundException e)
                {
                    e.printStackTrace();
                }

                return true;
            }
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean rename(String newName) throws IOException
    {
        if (isRealFile)
        {
            return false;
        }
        else
        {
            updateDocumentNode(true);

            if (documentNode != null)
            {
                // Renames the file
                UtilsSAF.renameDocument(documentNode.documentId, documentNode.treeUri, newName);

                // Get the parent node and clear its cache because it's changed
                DocumentNode parentNode = UtilsSAF.findDocumentNode(getParent());

                // Clear the cache, not very efficient as clears everything but fine for now
                if (parentNode != null && parentNode.isDirectory)
                {
                    parentNode.clearCache();
                }
            }

            return true;
        }
    }

    public void clearCache()
    {
        updateDocumentNode(true);
        if(documentNode != null && documentNode.isDirectory)
        {
            documentNode.clearCache();
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void updateDocumentNode(boolean forceUpdate)
    {
        if (documentNode == null || forceUpdate)
        {
            documentNode = UtilsSAF.findDocumentNode(fullPath);

            if (documentNode != null)
            {
                //DBG("FileSAF (" + fullPath + ") found Document: " + documentNode.documentId);
                isDirectory = documentNode.isDirectory;
            }
        }
    }



    private void DBG(String str)
    {
        Log.d(TAG, str);
    }
}
