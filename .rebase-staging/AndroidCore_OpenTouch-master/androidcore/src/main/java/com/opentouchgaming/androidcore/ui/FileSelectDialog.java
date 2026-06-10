package com.opentouchgaming.androidcore.ui;


import static com.opentouchgaming.androidcore.DebugLog.Level.D;

import android.app.Activity;
import android.app.Dialog;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.opentouchgaming.androidcore.DebugLog;
import com.opentouchgaming.androidcore.R;
import com.opentouchgaming.saffal.FileSAF;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class FileSelectDialog
{
    static DebugLog log;

    static
    {
        log = new DebugLog(DebugLog.Module.CONTROLS, "FileSelectDialog");
    }

    final String[] extensions;
    final boolean multiselect;
    final Dialog dialog;
    final FileSelectCallback callback;
    String basePath;
    ArrayList<String> filesArray = new ArrayList<String>();
    ArrayList<String> selectedFiles = new ArrayList<String>();
    Activity activity;
    ListView listView;
    TextView resultTextView;
    TextView infoTextView;
    ModsListAdapter listAdapter;

    public FileSelectDialog(final Activity act, final FileSelectCallback callback, String path, final String[] extensions, final boolean multiselect)
    {
        activity = act;
        this.extensions = extensions;
        this.multiselect = multiselect;
        this.callback = callback;

        dialog = new Dialog(activity, R.style.DialogEngineSettings);
        dialog.setTitle("Select files");
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_select_files);
        dialog.setCancelable(true);

        dialog.setOnKeyListener((arg0, keyCode, event) ->
                                {
                                    // TODO Auto-generated method stub
                                    if (keyCode == KeyEvent.KEYCODE_BACK)
                                    {
                                        return goUp();
                                    }
                                    return false;
                                });

        resultTextView = dialog.findViewById(R.id.result_textView);
        infoTextView = dialog.findViewById(R.id.info_textView);

        listView = dialog.findViewById(R.id.listview);
        listAdapter = new ModsListAdapter(activity);
        listView.setAdapter(listAdapter);

        listView.setOnItemClickListener(new OnItemClickListener()
        {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {

                if (filesArray.get(position).contentEquals(".."))
                {
                    goUp();
                }
                else if (filesArray.get(position).startsWith("/"))
                {
                    populateList(basePath + filesArray.get(position));
                }
                else //select/deselect
                {
                    if (multiselect)
                    {
                        boolean removed = false;
                        for (Iterator<String> iter = selectedFiles.listIterator(); iter.hasNext(); )
                        {
                            String s = iter.next();
                            if (s.contentEquals(basePath + "/" + filesArray.get(position)))
                            {
                                iter.remove();
                                removed = true;
                            }
                        }

                        if (!removed)
                            selectedFiles.add(basePath + "/" + filesArray.get(position));
                    }
                    else // Single select only
                    {
                        selectedFiles.clear();
                        selectedFiles.add(basePath + "/" + filesArray.get(position));
                    }
                    //	Log.d("TEST","list size = " + selectedArray.size());

                    listAdapter.notifyDataSetChanged();
                }
            }
        });
/*
        //Add folders on long press
        listView.setOnItemLongClickListener(new OnItemLongClickListener()
        {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int position, long id)
            {
                if (filesArray.get(position).startsWith("/"))
                {
                    boolean removed = false;
                    String name = filesArray.get(position).substring(1, filesArray.get(position).length());
                    for (Iterator<String> iter = selectedFiles.listIterator(); iter.hasNext(); )
                    {
                        String s = iter.next();
                        if (s.contentEquals(basePath + "/" + name))
                        {
                            iter.remove();
                            removed = true;
                        }
                    }

                    if (!removed)
                        selectedFiles.add(basePath + "/" + name);

                    //	Log.d("TEST","list size = " + selectedArray.size());

                    listAdapter.notifyDataSetChanged();
                    resultTextView.setText(basePath);
                    return true;
                }
                return false;
            }
        });
*/

        Button cancel_button = dialog.findViewById(R.id.cancel_button);
        cancel_button.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dialog.dismiss();
                callback.dismiss(null);
            }
        });

        Button ok_button = dialog.findViewById(R.id.ok_button);
        ok_button.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dialog.dismiss();
                callback.dismiss(selectedFiles);
            }
        });

        populateList(path);

        dialog.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    public void returnResult(ArrayList<String> result)
    {

    }

    private boolean goUp()
    {
        if (basePath.isEmpty() || !basePath.contains("/"))
            return false;
        else
        {
            basePath = basePath.substring(0, basePath.lastIndexOf("/"));
            populateList(basePath);
            return true;
        }
    }

    private void populateList(String path)
    {
        basePath = path;

        log.log(D, " basePath = " + basePath);

        FileSAF[] files = new FileSAF(basePath).listFiles();

        filesArray.clear();

        // Check if in a directory
        if (basePath.contains("/"))
        {
            filesArray.add("..");
        }

        if (files != null)
            for (File f : files)
            {
                if (!f.isDirectory())
                {

                    if (extensions == null) // If null, view ALL files
                    {
                        filesArray.add(f.getName());
                    }
                    else
                    {
                        String file = f.getName().toLowerCase();
                        for (String ext : extensions)
                        {
                            if (file.endsWith(ext))
                            {
                                filesArray.add(f.getName());
                                break;
                            }
                        }
                    }
                }
                else //Now also do directories
                {
                    filesArray.add("/" + f.getName());
                }
            }

        Collections.sort(filesArray, String.CASE_INSENSITIVE_ORDER);

        if (filesArray.size() == 0)
            infoTextView.setText("Copy wad/mods here: " + basePath + "/" + path);
        else
            infoTextView.setText("");

        listAdapter.notifyDataSetChanged();
        String extStr = "";
        if (extensions != null)
        {
            extStr = "  (";
            for (String ext : extensions)
            {
                extStr += ext + ", ";
            }
            extStr += ")";
        }
        resultTextView.setText(basePath + extStr);
    }

    public interface FileSelectCallback
    {
        void dismiss(ArrayList<String> filesArray);
    }

    class ModsListAdapter extends BaseAdapter
    {
        public ModsListAdapter(Activity context)
        {

        }

        public void add(String string)
        {

        }

        public int getCount()
        {
            return filesArray.size();
        }

        public Object getItem(int arg0)
        {
            // TODO Auto-generated method stub
            return null;
        }

        public long getItemId(int arg0)
        {
            // TODO Auto-generated method stub
            return 0;
        }

        public View getView(int position, View convertView, ViewGroup list)
        {

            View view;

            if (convertView == null)
                view = activity.getLayoutInflater().inflate(R.layout.listview_item_file_selector, null);
            else
                view = convertView;

            boolean selected = false;
            for (String s : selectedFiles)
            {
                if (s.contentEquals(basePath + "/" + filesArray.get(position)))
                {
                    selected = true;
                }
            }


            if (selected)
                view.setBackgroundResource(R.drawable.layout_sel_background);
            else
                view.setBackgroundResource(0);

            ImageView iv = view.findViewById(R.id.imageview);

            //iv.setImageResource(game.getImage());

            if (filesArray.get(position).startsWith("/") || filesArray.get(position).contentEquals(".."))
                iv.setImageResource(R.drawable.file_folder);
            else if (filesArray.get(position).endsWith(".pk3"))
                iv.setImageResource(R.drawable.file_zip);
            else if (filesArray.get(position).endsWith(".pk7"))
                iv.setImageResource(R.drawable.file_zip);
            else
                iv.setImageResource(R.drawable.file_unknown);


            TextView title = view.findViewById(R.id.name_textview);

            title.setText(filesArray.get(position));
            return view;
        }
    }
}
