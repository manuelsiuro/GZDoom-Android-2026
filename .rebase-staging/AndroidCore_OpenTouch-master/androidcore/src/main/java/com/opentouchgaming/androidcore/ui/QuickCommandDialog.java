package com.opentouchgaming.androidcore.ui;

import android.app.Activity;
import android.app.Dialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.opentouchgaming.androidcore.DebugLog;
import com.opentouchgaming.androidcore.DragSortRecycler;
import com.opentouchgaming.androidcore.ItemClickSupport;
import com.opentouchgaming.androidcore.R;
import com.opentouchgaming.saffal.FileSAF;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;


public class QuickCommandDialog
{
    static DebugLog log;
    static String QC_FILENAME = "QuickCommands.json";

    static
    {
        log = new DebugLog(DebugLog.Module.GAMEFRAGMENT, "QuickCommandDialog");
    }

    final Activity activity;
    final Dialog dialog;
    final RecyclerView recyclerView;
    final PathExampleViewAdapter rvAdapter;
    final LinearLayout editLayout;
    final EditText titleEditText;
    final EditText commandEditText;
    String dataPath;
    ArrayList<QuickCommand> commandList;

    public QuickCommandDialog(final Activity activity, String mainPath, String modPath, Function<String, Void> callback)
    {
        log.log(DebugLog.Level.D, "QuickCommandDialog mainPath = " + mainPath);
        log.log(DebugLog.Level.D, "QuickCommandDialog modPath = " + modPath);

        this.activity = activity;

        dialog = new Dialog(activity);

        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_quick_command);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);

        //Button main = dialog.findViewById(R.id.main_button);
        //Button mod = dialog.findViewById(R.id.mod_button);
        ImageButton add = dialog.findViewById(R.id.add_button);
        Button save = dialog.findViewById(R.id.save_button);
        titleEditText = dialog.findViewById(R.id.name_edittext);
        commandEditText = dialog.findViewById(R.id.command_edittext);

        editLayout = dialog.findViewById(R.id.edit_linearLayout);

        commandList = new ArrayList<>();

        recyclerView = dialog.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setFocusable(true);
        rvAdapter = new PathExampleViewAdapter();
        recyclerView.setAdapter(rvAdapter);


        TabLayout versionTabLayout = dialog.findViewById(R.id.select_tabLayout);

        TabLayout.Tab tab = versionTabLayout.newTab().setText("Common");
        versionTabLayout.addTab(tab);

        tab = versionTabLayout.newTab().setText("Mod");
        versionTabLayout.addTab(tab);

        versionTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
        {
            @Override
            public void onTabSelected(TabLayout.Tab tab)
            {
                if (tab.getPosition() == 0)
                    setDataPath(mainPath);
                else
                    setDataPath(modPath);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab)
            {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab)
            {

            }
        });

        add.setOnClickListener(v ->
                               {
                                   if (editLayout.getVisibility() == View.GONE)
                                       editLayout.setVisibility(View.VISIBLE);
                                   else
                                       editLayout.setVisibility(View.GONE);
                               });

        save.setOnClickListener(v ->
                                {
                                    String title = titleEditText.getText().toString();
                                    String command = commandEditText.getText().toString();
                                    if (title != null && title.length() > 0 && command != null & command.length() > 0)
                                    {
                                        QuickCommand newCmd = new QuickCommand(title, command);
                                        commandList.add(0, newCmd);
                                        editLayout.setVisibility(View.GONE);
                                        saveCommands();
                                    }
                                    rvAdapter.notifyDataSetChanged();
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
                PathExampleViewAdapter.ViewHolder h = (PathExampleViewAdapter.ViewHolder) viewHolder;
                commandList.remove(h.getAdapterPosition());
                saveCommands();
                rvAdapter.notifyDataSetChanged();
            }
        };

        simpleItemTouchCallback.setDefaultSwipeDirs(ItemTouchHelper.RIGHT);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        DragSortRecycler dragSortRecycler = new DragSortRecycler();
        dragSortRecycler.setViewHandleId(R.id.imageView); //View you wish to use as the handle

        dragSortRecycler.setOnItemMovedListener((from, to) ->
                                                {
                                                    Log.d("TEST", "onItemMoved " + from + " to " + to);

                                                    QuickCommand command = commandList.get(from);

                                                    commandList.add(from > to ? to : to + 1, command);
                                                    commandList.remove(from > to ? from + 1 : from);
                                                    saveCommands();
                                                    rvAdapter.notifyDataSetChanged();
                                                });

        recyclerView.addItemDecoration(dragSortRecycler);
        recyclerView.addOnItemTouchListener(dragSortRecycler);
        recyclerView.setOnScrollListener(dragSortRecycler.getScrollListener());

        ItemClickSupport.addTo(recyclerView).setOnItemClickListener((recyclerView, position, v) ->
                                                                    {
                                                                        //selected( position );
                                                                        callback.apply(commandList.get(position).getCommand());
                                                                        dialog.hide();
                                                                    });

        ItemClickSupport.addTo(recyclerView).setOnItemLongClickListener((recyclerView1, position, v) ->
                                                                        {
                                                                            QuickCommand command = commandList.get(position);

                                                                            titleEditText.setText(command.getTitle());
                                                                            commandEditText.setText(command.getCommand());
                                                                            editLayout.setVisibility(View.VISIBLE);
                                                                            return true;
                                                                        });

        setDataPath(mainPath);
    }

    public void show()
    {
        dialog.show();
    }

    private void saveCommands()
    {
        FileSAF pathFile = new FileSAF(dataPath);

        if (!pathFile.exists())
            pathFile.mkdirs();

        try
        {
            String jsonInString = new Gson().toJson(commandList);
            JSONArray jsonObject = new JSONArray(jsonInString);

            FileSAF file = new FileSAF(pathFile + "/" + QC_FILENAME);

            if (!file.exists())
                file.createNewFile();

            BufferedWriter output = new BufferedWriter(new OutputStreamWriter((file.getOutputStream())));
            output.write(jsonObject.toString(4));
            output.close();
        }
        catch (JSONException | IOException e)
        {
            e.printStackTrace();
        }
    }

    private void setDataPath(String path)
    {
        dataPath = path;

        Gson gson = new Gson();

        try
        {
            FileSAF file = new FileSAF(dataPath + "/" + QC_FILENAME);
            if (file.exists() && file.getInputStream() != null)
            {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileSAF(dataPath + "/" + QC_FILENAME).getInputStream()));
                Type type = new TypeToken<ArrayList<QuickCommand>>()
                {
                }.getType();

                commandList = gson.fromJson(br, type);
            }
            else
            {
                commandList = new ArrayList<>();
            }
        }
        catch (FileNotFoundException e)
        {
            commandList = new ArrayList<>();
            e.printStackTrace();
        }

        rvAdapter.notifyDataSetChanged();
    }

    public class QuickCommand implements Serializable
    {
        private static final long serialVersionUID = 1L;

        String title;
        String command;

        QuickCommand(String title, String command)
        {
            this.title = title;
            this.command = command;
        }

        public String getTitle()
        {
            return title;
        }

        public void setTitle(String title)
        {
            this.title = title;
        }

        public String getCommand()
        {
            return command;
        }

        public void setCommand(String command)
        {
            this.command = command;
        }

    }

    public class PathExampleViewAdapter extends RecyclerView.Adapter<PathExampleViewAdapter.ViewHolder>
    {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_quick_command_item, parent, false);
            return new PathExampleViewAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position)
        {
            QuickCommand cmd = commandList.get(position);
            holder.title.setText(cmd.getTitle());
            holder.command.setText(cmd.getCommand());
        }


        @Override
        public int getItemCount()
        {
            return commandList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder
        {
            public final TextView title;
            public final TextView command;

            public ViewHolder(View view)
            {
                super(view);

                this.title = view.findViewById(R.id.title_textview);
                this.command = view.findViewById(R.id.command_textview);
            }
        }
    }
}
