package com.opentouchgaming.saffal;

import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DocumentNode
{
    static final String mimeType = "plain";
    static String TAG = "DocumentNode";
    public String name;
    public String documentId;
    public boolean exists;
    public boolean isDirectory;
    public long size;
    public long modifiedDate;
    public DocumentNode parent;
    // The SAF tree URI this node belongs to (set on root nodes, propagated to all children)
    public Uri treeUri;

    private List<DocumentNode> children;

    /**
     * Traverse a tree to find a particular DocumentNode (file or directory). Path must look like:  folder1/folder2/file, or folder1/folder2/folder3
     *
     * @return The node if it exists, otherwise null;
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static DocumentNode findDocumentNode(DocumentNode rootNode, String documentPath)
    {
        DocumentNode node = null;

        if (documentPath != null)
        {
            // Split path into parts
            String[] parts = documentPath.split("\\/", -1);

            node = rootNode;

            for (String part : parts)
            {
                if (part.length() > 0)
                { // part will be an empty string if documentPath was an empty string

                    if (node == null)
                        break;

                    node = node.findChild(part);
                }
            }
        }

        return node;
    }

    /**
     * Create all the directories in documentPath up to the end (like mkdirs)
     *
     * @return The node if it exists or has been created, otherwise null;
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static DocumentNode createAllNodes(DocumentNode rootNode, String documentPath) throws FileNotFoundException
    {
        DBG("DocumentNode: createAllNodes: " + documentPath);

        DocumentNode node = null;

        if (documentPath != null)
        {
            // Split path into parts
            String[] parts = documentPath.split("\\/", -1);

            node = rootNode;

            for (String part : parts)
            {
                if (part.length() > 0)
                { // part will be an empty string if documentPath was an empty string
                    DBG("DocumentNode: createAllNodes: Checking part: " + part);
                    if (node != null && node.isDirectory)
                    {
                        DocumentNode next = node.findChild(part);
                        // Check if directory already exists
                        if (next == null)
                        {
                            // Try to create the next level of folder
                            node = node.createChild(true, part);
                        }
                        else
                        {
                            node = next;
                        }
                    }
                    else
                    {
                        DBG("DocumentNode: createAllNodes: Error, DocumentNode is not a directory");
                        node = null;
                        break;
                    }
                }
            }
        }

        return node;
    }

    private static void DBG(String str)
    {
        Log.d(TAG, str);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public DocumentNode findChild(String name)
    {
        DocumentNode node = null;

        findChildren();

        if (children != null)
        {
            for (DocumentNode n : children)
            {
                if (n.name.contentEquals(name))
                {
                    node = n;
                    break;
                }
            }

            // If didn't find the exact name, do a case insensitive search
            if (node == null && UtilsSAF.CASE_INSENSITIVE)
            {
                for (DocumentNode n : children)
                {
                    if (n.name.toLowerCase().contentEquals(name.toLowerCase()))
                    {
                        node = n;
                        break;
                    }
                }
            }
        }
        return node;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ArrayList<DocumentNode> getChildren()
    {
        findChildren();

        ArrayList<DocumentNode> ret = new ArrayList<>();

        if (children != null)
            ret.addAll(children);

        return ret;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public synchronized void findChildren()
    {
        if (isDirectory)
        {
            if (children == null) // Check if already been scanned, this is allows caching of the files
            {
                children = new ArrayList<>();

                Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId);

                Cursor cursor = null;
                try
                {
                    cursor = UtilsSAF.getContentResolver().query(childrenUri,
                            new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE,
                                    DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                                    DocumentsContract.Document.COLUMN_SIZE}, null, null, null);

                    while (cursor.moveToNext())
                    {
                        DocumentNode newNode = new DocumentNode();
                        newNode.parent = this;
                        newNode.treeUri = this.treeUri;
                        newNode.exists = true;
                        newNode.name = cursor.getString(0);
                        newNode.isDirectory = DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(1));
                        newNode.documentId = cursor.getString(2);
                        newNode.modifiedDate = cursor.getLong(3);
                        newNode.size = cursor.getLong(4);

                        // DBG("Found " + newNode.name + " size = " + newNode.size);

                        children.add(newNode);
                    }
                }
                catch (Exception ignored)
                {
                    DBG("findChildren: Some Exception: " + ignored);
                }
                finally
                {
                    try
                    {
                        if (cursor != null)
                            cursor.close();
                    }
                    catch (Exception ignored)
                    {
                        DBG("findChildren: Some Exception: " + ignored);
                    }
                }
            }
        }
        else
        {
            DBG("DocumentNode: ERROR, tried to findChildren of non directory");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public synchronized void clearCache()
    {
        children = null;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public DocumentNode createChild(boolean directory, String name) throws FileNotFoundException
    {
        DBG("DocumentNode: createChild: " + name);

        if (isDirectory)
        {
            if (findChild(name) != null)
            {
                DBG("DocumentNode: createChild ERROR, file " + name + " already exists, not creating");
            }
            else
            {
                Uri myUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId);

                try
                {
                    if (directory)
                        DocumentsContract.createDocument(UtilsSAF.getContentResolver(), myUri, DocumentsContract.Document.MIME_TYPE_DIR, name);
                    else
                        DocumentsContract.createDocument(UtilsSAF.getContentResolver(), myUri, mimeType, name);
                }
                catch (Exception e)
                {
                    throw new FileNotFoundException();
                }

                // We have created a new file, null the children cache so it's updated next time
                clearCache();

                return findChild(name);
            }
        }
        else
        {
            DBG("DocumentNode: createChild ERROR, tried to create child in non-directory");
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean deleteChild(String name) throws FileNotFoundException
    {
        DBG("DocumentNode: deleteChild: " + name);

        if (isDirectory)
        {
            DocumentNode child = findChild(name);
            if (child == null)
            {
                DBG("DocumentNode: deleteChild ERROR, file " + name + " does not exists");
            }
            else
            {
                Uri myUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, child.documentId);

                // clear cache
                clearCache();

                return DocumentsContract.deleteDocument(UtilsSAF.getContentResolver(), myUri);
            }
        }
        else
        {
            DBG("DocumentNode: deleteChild ERROR, tried to delete child in non-directory");
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public InputStream getInputStream() throws FileNotFoundException
    {
        Uri myUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId);

        return UtilsSAF.getContentResolver().openInputStream(myUri);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public OutputStream getOutputStream() throws FileNotFoundException
    {
        Uri myUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId);

        return UtilsSAF.getContentResolver().openOutputStream(myUri);
    }
}
