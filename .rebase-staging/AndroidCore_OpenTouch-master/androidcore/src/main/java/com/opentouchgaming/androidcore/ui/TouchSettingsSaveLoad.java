package com.opentouchgaming.androidcore.ui;

import static com.opentouchgaming.androidcore.DebugLog.Level.D;
import static com.opentouchgaming.androidcore.DebugLog.Level.E;
import static com.opentouchgaming.androidcore.DebugLog.Level.I;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.opentouchgaming.androidcore.DebugLog;
import com.opentouchgaming.androidcore.ItemClickSupport;
import com.opentouchgaming.androidcore.R;
import com.opentouchgaming.androidcore.controls.ControlInterface;
import com.opentouchgaming.saffal.FileSAF;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class TouchSettingsSaveLoad
{


    static DebugLog log;

    static
    {
        log = new DebugLog(DebugLog.Module.APP, "TouchSettingsSaveLoad");
    }

    final String layoutsFolder = "touch_layouts";
    final String layoutsInfoFilename = "info.dat";
    final Activity activity;
    final Dialog dialog;
    String userFolder;
    RecyclerView recyclerView;
    RecyclerViewAdapter rvAdapter;
    EditText nameEditText;
    ArrayList<TouchSettingSaveInfo> layouts = new ArrayList<>();
    boolean saving = false;
    ControlInterface nativeIf;

    public TouchSettingsSaveLoad(final Activity act, String userFolder, ControlInterface nativeIf)
    {
        log.log(D, "userFolder = " + userFolder);

        this.userFolder = userFolder;
        this.nativeIf = nativeIf;
        activity = act;

        new File(getLayoutsFolder()).mkdirs();

        dialog = new Dialog(act);

        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        dialog.setContentView(R.layout.dialog_touch_settings_load_save);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(true);


        RelativeLayout saveLayout = dialog.findViewById(R.id.save_layout);
        saveLayout.setVisibility(View.GONE);

        nameEditText = dialog.findViewById(R.id.name_edittext);

        recyclerView = dialog.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(act));
        recyclerView.setFocusable(true);
        rvAdapter = new RecyclerViewAdapter();
        recyclerView.setAdapter(rvAdapter);

        // Toggle save
        ImageView addButton = dialog.findViewById(R.id.add_imageview);
        addButton.setOnClickListener(v ->
                                     {
                                         saving = !saving;
                                         saveLayout.setVisibility(saving ? View.VISIBLE : View.GONE);
                                     });

        // Save button
        Button saveButton = dialog.findViewById(R.id.save_button);
        saveButton.setOnClickListener(v ->
                                      {
                                          String name = nameEditText.getText().toString().trim();
                                          if (name.length() > 0)
                                          {
                                              saveLayout(name);
                                          }
                                      });

        // Swipe to dismiss
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT)
        {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target)
            {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir)
            {
                //Remove swiped item from list and notify the RecyclerView
                RecyclerViewAdapter.ViewHolder h = (RecyclerViewAdapter.ViewHolder) viewHolder;
                TouchSettingSaveInfo info = h.item;
                File layoutDir = new File(getLayoutsFolder() + "/" + info.folder);

                // Delete layout files
                File[] contents = layoutDir.listFiles();
                if (contents != null)
                {
                    for (File f : contents)
                    {
                        f.delete();
                    }
                }
                layoutDir.delete();

                findLayouts();
            }
        };

        // List item press
        simpleItemTouchCallback.setDefaultSwipeDirs(ItemTouchHelper.RIGHT);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        ItemClickSupport.addTo(recyclerView).setOnItemClickListener((recyclerView, position, v) ->
                                                                    {
                                                                        if (saving)
                                                                        {
                                                                            nameEditText.setText(layouts.get(position).name);
                                                                        }
                                                                        else
                                                                        {
                                                                            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
                                                                            //dialogBuilder.setTitle("Load settings");
                                                                            dialogBuilder.setMessage("Load touch settings? (" +
                                                                                                     layouts.get(position).name +
                                                                                                     ")");
                                                                            dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener()
                                                                            {
                                                                                @Override
                                                                                public void onClick(DialogInterface alertdialog, int which)
                                                                                {
                                                                                    String layoutDir = getLayoutsFolder() + "/" + layouts.get(position).folder;
                                                                                    // Call native to code to load
                                                                                    int error = nativeIf.loadSettings_if(layoutDir);

                                                                                    if (error == 0)
                                                                                    {
                                                                                        Toast.makeText(act, "Loaded layout", Toast.LENGTH_LONG).show();
                                                                                        dialog.dismiss();
                                                                                    }
                                                                                    else
                                                                                        Toast.makeText(act, "ERROR loading layout: " + error, Toast.LENGTH_LONG)
                                                                                                .show();
                                                                                }
                                                                            });
                                                                            AlertDialog alertdialog = dialogBuilder.create();
                                                                            alertdialog.show();
                                                                        }
                                                                    });

        findLayouts();

        dialog.show();
    }

    private String getLayoutsFolder()
    {
        return userFolder + "/" + layoutsFolder;
    }

    private void saveLayout(String name, FileSAF path)
    {
        int error = nativeIf.saveSettings_if(path.getAbsolutePath());

        if (error == 0)
        {
            // Create new TouchSettingSaveInfo object
            TouchSettingSaveInfo info = new TouchSettingSaveInfo();
            info.name = name;
            info.timeSaved = new Date().getTime();

            // Serialise new file
            FileSAF infoFile = new FileSAF(path, layoutsInfoFilename);

            try
            {
                if (!infoFile.exists())
                    infoFile.createNewFile();

                ObjectOutputStream out = null;
                out = new ObjectOutputStream(infoFile.getOutputStream());
                out.writeObject(info);
                out.close();

                Toast.makeText(activity, "Saved layout", Toast.LENGTH_LONG).show();

                log.log(D, "Saved: " + infoFile.getAbsolutePath());

                dialog.dismiss();
            }
            catch (FileNotFoundException e)
            {
                log.log(E, "Could not open file " + infoFile.getAbsolutePath() + " :" + e);
                e.printStackTrace();
            }
            catch (IOException e)
            {
                log.log(E, "Error writing file " + infoFile.getAbsolutePath() + " :" + e);
                e.printStackTrace();
            }
        }
    }

    private void saveLayout(String name)
    {
        long nextDir = findLayouts() + 1;

        FileSAF layoutDir = null;

        // Check if a name already exists
        for (TouchSettingSaveInfo info : layouts)
        {
            if (info.name.contentEquals(name))
                layoutDir = new FileSAF(getLayoutsFolder() + "/" + info.folder);
        }

        // No existing name was found, choose the next
        if (layoutDir == null)
            layoutDir = new FileSAF(getLayoutsFolder() + "/" + nextDir);

        if (layoutDir.exists())
        {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
            //dialogBuilder.setTitle("Load settings");
            dialogBuilder.setMessage("Overwrite setting? (" + name + ")");
            FileSAF finalLayoutDir = layoutDir;
            dialogBuilder.setPositiveButton("OK", (alertdialog, which) ->
            {
                saveLayout(name, finalLayoutDir);
                dialog.dismiss();
            });
            AlertDialog alertdialog = dialogBuilder.create();
            alertdialog.show();
        }
        else
        {
            layoutDir.mkdirs();
            saveLayout(name, layoutDir);
            dialog.dismiss();
        }
    }

    private long findLayouts()
    {
        layouts.clear();

        long lastLayoutNumber = 0;

        FileSAF[] layoutsDirs = new FileSAF(getLayoutsFolder()).listFiles();
        if (layoutsDirs != null)
        {
            for (FileSAF dir : layoutsDirs)
            {
                // Only dirs which are numbers are valid
                try
                {
                    long num = Long.parseLong(dir.getName());
                    if (num > lastLayoutNumber)
                        lastLayoutNumber = num;

                    FileSAF infoFile = new FileSAF(dir, layoutsInfoFilename);

                    // Try to De-seraialize the file
                    TouchSettingSaveInfo info = null;
                    if (infoFile.exists())
                    {
                        try
                        {
                            ObjectInputStream in = new ObjectInputStream(infoFile.getInputStream());
                            info = (TouchSettingSaveInfo) in.readObject();
                            in.close();
                            log.log(I, "File " + infoFile + " loaded");
                        }
                        catch (FileNotFoundException e)
                        {
                            log.log(I, "File " + infoFile + " not found");
                        }
                        catch (IOException e)
                        {
                            log.log(E, "Could not open file " + infoFile + " :" + e);

                        }
                        catch (ClassNotFoundException e)
                        {
                            log.log(E, "Error reading file " + infoFile + " :" + e);
                        }
                    }

                    if (info != null)
                    {
                        log.log(E, "Loaded folder " + num + " name = " + info.name);

                        // Update folder to real folder
                        info.folder = num;

                        layouts.add(info);
                    }
                    else
                    {
                        log.log(E, "Failed to load info file");
                    }
                }
                catch (NumberFormatException nfe)
                {
                    // Not a number, ignore
                }
            }
        }


        Collections.sort(layouts, (o1, o2) -> (int) (o2.timeSaved - o1.timeSaved));

        rvAdapter.notifyDataSetChanged();

        return lastLayoutNumber;
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>
    {
        public RecyclerViewAdapter()
        {

        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_gamepad_load, parent, false);
            view.setFocusable(true);
            view.setBackgroundResource(R.drawable.focusable);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position)
        {
            holder.item = layouts.get(position);

            holder.textView.setText(holder.item.name);
        }

        @Override
        public int getItemCount()
        {
            return layouts.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder
        {
            public final TextView textView;
            public final View view;
            public TouchSettingSaveInfo item;

            public ViewHolder(View view)
            {
                super(view);

                this.view = view;
                this.textView = view.findViewById(R.id.textView);
            }
        }
    }
}
