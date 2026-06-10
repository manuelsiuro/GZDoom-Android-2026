package com.opentouchgaming.saffalexample;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.opentouchgaming.saffal.FileSAF;
import com.opentouchgaming.saffal.UtilsSAF;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
{
    String TAG = "SAF";

    public static final int REQUEST_CODE_OPEN_DIRECTORY = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                UtilsSAF.openDocumentTree(MainActivity.this, REQUEST_CODE_OPEN_DIRECTORY);
            }
        });

        Button button2 = findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                SharedPreferences myPrefs = getSharedPreferences("rootUri", 0);

                Uri treeUri = null;

                try {
                    treeUri = Uri.parse(myPrefs.getString("url", "defaultString"));
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                Log.d(TAG,"url = " + treeUri.toString());

                test(treeUri);
            }
        });

        // Save context
        UtilsSAF.setContext(getApplication());

        // Try to load saved URI info
        if( UtilsSAF.loadTreeRoot(this) )
        {
            Log.d(TAG,"Loaded URI");
        }
    }


    void test( Uri treeUri )
    {


        FileSAF file = new FileSAF("/storage/emulated/0/OpenTouch/Delta/freedoom2.wad");

        file.listFiles();
        file.renameTo("FREEEEDOOOMw.WAD");
        //file.delete();
/*
        FileSAF mkd1 = new FileSAF("/storage/emulated/0/Hello ME/ANOTHER");
        mkd1.mkdir();

        FileSAF mkd2 = new FileSAF("/storage/emulated/0/Hello ME");
        mkd2.mkdir();
        mkd1.mkdir();

        FileSAF test2 = new FileSAF("/storage/emulated/0/Lots/of/folders/");
        test2.mkdirs();

        FileSAF test3 = new FileSAF("/storage/emulated/0/Lots/of/folders/myotherfile.zip");
        test3.createNewFile();

        FileSAF test4 = new FileSAF("/storage/emulated/0/Lots/of/myotherfile2.zip");
        test4.createNewFile();
        test4.createNewFile();

        FileSAF test5 = new FileSAF("/storage/emulated/0/Lots/ofddd/myotherfile2.zip");
        test5.createNewFile();
*/

    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_DIRECTORY && resultCode == Activity.RESULT_OK) {

            Log.d(TAG, String.format("Open Directory result Uri : %s", data.getData()));

            // Get the tree URI
            Uri treeUri = data.getData();

            // This is the path which FileSAF removes from the start of each file path
            // It can be set to anything you want, but in this example we set it to the devices internal flash memory
            String rootPath = Environment.getExternalStorageDirectory().toString();

            // Setup the UtilsSAF static data
            UtilsSAF.setTreeRoot(new UtilsSAF.TreeRoot(treeUri,rootPath));

            // Save URI for next launch
            UtilsSAF.saveTreeRoot(this);

            // Take permissions for ever
            final int takeFlags = data.getFlags()
                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(treeUri, takeFlags);

        }
    }


    private  String[] getExtSdCardPaths() {
        List<String> paths = new ArrayList<>();
        for (File file : getExternalFilesDirs("external")) {
            if (file != null && !file.equals(getExternalFilesDir("external"))) {
                int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                if (index < 0) {
                    Log.w(TAG, "Unexpected external file dir: " + file.getAbsolutePath());
                }
                else {
                    String path = file.getAbsolutePath().substring(0, index);
                    try {
                        path = new File(path).getCanonicalPath();
                    }
                    catch (IOException e) {
                        // Keep non-canonical path.
                    }
                    paths.add(path);
                }
            }
        }
        return paths.toArray(new String[paths.size()]);
    }



}
