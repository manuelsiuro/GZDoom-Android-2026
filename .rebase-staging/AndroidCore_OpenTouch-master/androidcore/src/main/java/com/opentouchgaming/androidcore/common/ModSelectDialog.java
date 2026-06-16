package com.opentouchgaming.androidcore.common;


import static com.opentouchgaming.androidcore.DebugLog.Level.D;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.arch.core.util.Function;

import com.opentouchgaming.androidcore.AppInfo;
import com.opentouchgaming.androidcore.AppSettings;
import com.opentouchgaming.androidcore.DebugLog;
import com.opentouchgaming.androidcore.R;
import com.opentouchgaming.androidcore.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class ModSelectDialog
{

    static DebugLog log;

    static
    {
        log = new DebugLog(DebugLog.Module.APP, "ModSelectDialog");
    }

    final int SORT_TYPE_NAME = 0;
    final int SORT_TYPE_DATE = 1;
    final Dialog dialog;
    String appDir;
    String appSecDir;
    String extraPath = "";
    ArrayList<ModFile> filesArray = new ArrayList<ModFile>();

    ArrayList<ModFile> filteredFiles = new ArrayList<ModFile>();

    String searchText;

    // ArrayList<String> selectedArray = new ArrayList<String>();
    CustomArgs customArgs;
    Activity activity;
    ListView listView;
    TextView resultTextView;
    TextView infoTextView;
    EditText searchEditText;
    RelativeLayout searchLayout;
    ModsListAdapter listAdapter;
    Function<ArrayList<String>, Void> result;

    ModSelectDialog(Activity act, String appDir, String appSecDir, CustomArgs args, Function<ArrayList<String>, Void> result)
    {
        this.appDir = appDir;
        this.appSecDir = appSecDir;
        this.result = result;

        activity = act;

        customArgs = args;

        dialog = new Dialog(activity, R.style.DialogEngineSettings);
        dialog.setTitle("Select files");
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_select_mods_wads);
        dialog.setCancelable(true);

        dialog.setOnKeyListener(new Dialog.OnKeyListener()
        {
            @Override
            public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event)
            {
                // TODO Auto-generated method stub
                if (keyCode == KeyEvent.KEYCODE_BACK)
                {
                    return goUp();
                }
                return false;
            }
        });

        resultTextView = dialog.findViewById(R.id.result_textView);
        infoTextView = dialog.findViewById(R.id.info_textView);
        searchLayout = dialog.findViewById(R.id.search_relativeLayout);
        searchEditText = dialog.findViewById(R.id.search_editText);

        listView = dialog.findViewById(R.id.listview);
        listAdapter = new ModsListAdapter(activity);
        listView.setAdapter(listAdapter);

        listView.setOnItemClickListener((parent, view, position, id) ->
                                        {

                                            if (filteredFiles.get(position).fileName.contentEquals(".."))
                                            {
                                                goUp();
                                            }
                                            else if (filteredFiles.get(position).fileName.startsWith("/"))
                                            {
                                                populateList(extraPath + filteredFiles.get(position).fileName);
                                            }
                                            else //select/deselect
                                            {
                                                boolean removed = false;
                                                for (Iterator<String> iter = customArgs.getFiles().listIterator(); iter.hasNext(); )
                                                {
                                                    String s = iter.next();
                                                    if (s.contentEquals(filteredFiles.get(position).file))
                                                    {
                                                        iter.remove();
                                                        removed = true;
                                                    }
                                                }

                                                if (!removed)
                                                    customArgs.getFiles().add(filteredFiles.get(position).file);

                                                updateUi();
                                            }
                                        });
/*
        //Add folders on long press
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            if (filteredFiles.get(position).fileName.startsWith("/")) {
                boolean removed = false;
                String name = filteredFiles.get(position).name.substring(1);
                for (Iterator<String> iter = customArgs.files.listIterator(); iter.hasNext(); ) {
                    String s = iter.next();
                    if (s.contentEquals(extraPath + "/" + name)) {
                        iter.remove();
                        removed = true;
                    }
                }

                if (!removed)
                    customArgs.files.add(extraPath + "/" + name);

                //	Log.d("TEST","list size = " + selectedArray.size());

                listAdapter.notifyDataSetChanged();
                resultTextView.setText(customArgs.getModsString());
                return true;
            }
            return false;
        });
*/
        Button wads_button = dialog.findViewById(R.id.maps_button);
        wads_button.setOnClickListener(v -> populateList("maps"));

        Button mods_button = dialog.findViewById(R.id.mods_button);
        mods_button.setOnClickListener(v -> populateList("mods"));

        Button demos_button = dialog.findViewById(R.id.demos_button);
        demos_button.setOnClickListener(v -> populateList("demos"));


        Button ok_button = dialog.findViewById(R.id.ok_button);
        ok_button.setOnClickListener(v -> dialog.dismiss());

        ImageButton sortButton = dialog.findViewById(R.id.sort_imageButton);
        sortButton.setOnClickListener(v ->
                                      {
                                          String[] colors = {"Name", "Date"};

                                          AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                                          builder.setTitle("Sort by..");
                                          builder.setItems(colors, (dialog, which) ->
                                          {
                                              AppSettings.setIntOption(activity, "mod_filenames_sort", which);
                                              populateList("mods");
                                          });
                                          builder.show();
                                      });

        // SEARCH STUFF ----------------------------------------------------------------------------------

        final ImageButton searchButton = dialog.findViewById(R.id.search_imageButton);
        searchButton.setOnClickListener(v ->
                                        {
                                            if (searchLayout.getVisibility() == View.GONE)
                                            {
                                                searchText = AppSettings.getStringOption(activity, "mod_search_filter", null);

                                                if (searchText != null)
                                                    searchEditText.setText(searchText);

                                                searchLayout.setVisibility(View.VISIBLE);
                                                applySearch();
                                            }
                                            else
                                            {
                                                searchText = null;
                                                searchLayout.setVisibility(View.GONE);
                                                applySearch();
                                            }
                                        });

        searchEditText.addTextChangedListener(new TextWatcher()
        {

            @Override
            public void afterTextChanged(Editable s)
            {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                searchText = s.toString();
                AppSettings.setStringOption(activity, "mod_search_filter", searchText);
                applySearch();
            }
        });

        ImageButton clearSearch = dialog.findViewById(R.id.clear_search_imageButton);
        clearSearch.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                searchText = null;
                searchEditText.setText("");
                applySearch();
            }
        });

        //------------------------------------------------------------------------------------------------

        dialog.setOnDismissListener(dialog -> result.apply(customArgs.getFiles()));

        populateList("mods");


        dialog.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    private boolean goUp()
    {
        if (extraPath.isEmpty() || !extraPath.contains("/"))
            return false;
        else
        {
            extraPath = extraPath.substring(0, extraPath.lastIndexOf("/"));
            populateList(extraPath);
            return true;
        }
    }

    private void populateList(String path)
    {
        extraPath = path;

        ArrayList<File> files = Utils.listFiles(new String[]{appDir + "/" + path, appSecDir + "/" + path});

        filesArray.clear();

        // Check if in a directory
        if (extraPath.contains("/"))
        {
            filesArray.add(new ModFile(null, "..", 0, Long.MAX_VALUE));
        }

        if (files != null)
            for (File f : files)
            {
                if (!f.isDirectory())
                {
                    String file = f.getName().toLowerCase();
                    if ((file.endsWith(".wad") ||
                         file.endsWith(".pk3") ||
                         file.endsWith(".zip") ||
                         file.endsWith(".pk7") ||
                         file.endsWith(".deh") ||
                         file.endsWith(".bex") ||
                         file.endsWith(".lmp") ||
                         file.endsWith(".sf2") ||
                         file.endsWith(".txt") ||
                         file.endsWith(".grp") ||
                         file.endsWith(".con")) &&
                        !file.endsWith("Put your mods (.pk3 etc) files here.txt".toLowerCase()) &&
                        !file.endsWith("Put your mods files here.txt".toLowerCase()))
                    {
                        filesArray.add(new ModFile(f.getAbsolutePath(), f.getName(), f.length(), f.lastModified()));
                    }
                }
                else //Now also do directories
                {
                    filesArray.add(new ModFile(null, "/" + f.getName(), 0, f.lastModified()));
                }
            }

        Comparator<ModFile> comparator = null;
        int sortType = AppSettings.getIntOption(activity, "mod_filenames_sort", SORT_TYPE_NAME);

        log.log(D, "sortTpe = " + sortType);
        if (sortType == SORT_TYPE_NAME)
        {
            comparator = (lhs, rhs) ->
            {
                int res = String.CASE_INSENSITIVE_ORDER.compare(lhs.fileName, rhs.fileName);
                return res;
            };
        }
        else if (sortType == SORT_TYPE_DATE)
        {
            comparator = (lhs, rhs) ->
            {
                // This puts the largest date at the top
                int res = (rhs.date < lhs.date) ? -1 : (rhs.date == lhs.date) ? 0 : 1;
                return res;
            };
        }

        if (comparator != null)
            Collections.sort(filesArray, comparator);

        if (filesArray.size() == 0)
        {
            String hintTest = "Copy wad/mods here:\n" + AppInfo.replaceRootPaths(appDir) + "/" + path;
            if (appSecDir != null)
                hintTest += "\n   OR:\n" + AppInfo.replaceRootPaths(appSecDir) + "/" + path;
            infoTextView.setText(hintTest);
        }
        else
            infoTextView.setText("");

        applySearch();

        updateUi();
    }

    private void applySearch()
    {
        filteredFiles.clear();

        if (searchText == null || searchText.length() == 0)
            filteredFiles.addAll(filesArray);
        else
        {
            for (ModFile file : filesArray)
            {
                if (file.fileName.contentEquals("..") || file.fileName.toLowerCase().contains(searchText.toLowerCase()))
                {
                    filteredFiles.add(file);
                }
            }
        }

        listAdapter.notifyDataSetChanged();
    }

    private void updateUi()
    {
        resultTextView.setText(AppInfo.hideAppPaths(customArgs.getModsString()));
        listAdapter.notifyDataSetChanged();
    }

    class ModFile
    {
        String file;
        String fileName;
        long date;
        long size;

        ModFile(String file, String fileName, long size, long date)
        {
            this.file = file;
            this.fileName = fileName;
            this.date = date;
            this.size = size;
        }
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
            return filteredFiles.size();
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

            ModFile file = filteredFiles.get(position);

            if (convertView == null)
                view = activity.getLayoutInflater().inflate(R.layout.listview_item_mods_wads, null);
            else
                view = convertView;

            boolean selected = false;
            for (String s : customArgs.getFiles())
            {
                if (file.file != null && s.contentEquals(file.file))
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

            if (file.fileName.startsWith("/") || file.fileName.contentEquals(".."))
                iv.setImageResource(R.drawable.file_folder);
            else if (file.fileName.endsWith(".pk3"))
                iv.setImageResource(R.drawable.file_zip);
            else if (file.fileName.endsWith(".pk7"))
                iv.setImageResource(R.drawable.file_zip);
            else if (file.fileName.endsWith(".zip"))
                iv.setImageResource(R.drawable.file_zip);
            else
                iv.setImageResource(R.drawable.file_unknown);

            TextView title = view.findViewById(R.id.name_textview);
            title.setText(file.fileName);

            TextView detail = view.findViewById(R.id.detail_textview);
            if (file.size == 0)
                detail.setText("");
            else
                detail.setText(Utils.humanReadableByteCount(file.size,false));

            return view;
        }
    }
}
