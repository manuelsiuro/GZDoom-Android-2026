package com.opentouchgaming.androidcore.ui;

import android.app.Activity;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.opentouchgaming.androidcore.AppInfo;
import com.opentouchgaming.androidcore.ItemClickSupport;
import com.opentouchgaming.androidcore.R;
import com.opentouchgaming.saffal.FileSAF;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Emile on 31/10/2017.
 */

public class GamepadSaveLoad
{
    RecyclerView recyclerView;
    ArgsRecyclerViewAdapter rvAdapter;

    ArrayList<String> files = new ArrayList<>();

    boolean load;

    EditText nameEditText;

    public GamepadSaveLoad(final Activity act, final boolean load)
    {
        this.load = load;

        final Dialog dialog = new Dialog(act, R.style.DialogEngineSettingsWrap);
        //dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        if (load)
            dialog.setTitle("Load");
        else
            dialog.setTitle("Save As");

        dialog.setContentView(R.layout.dialog_gamepad_load_save);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);
        dialog.show();

        RelativeLayout saveLayout = dialog.findViewById(R.id.save_layout);

        nameEditText = dialog.findViewById(R.id.name_edittext);

        recyclerView = dialog.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(act));
        recyclerView.setFocusable(true);
        rvAdapter = new ArgsRecyclerViewAdapter();
        recyclerView.setAdapter(rvAdapter);

        Button saveButton = dialog.findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (nameEditText.getText().toString().length() > 0)
                {
                    selected(nameEditText.getText().toString());
                    dialog.dismiss();
                }
            }
        });

        if (load)
            saveLayout.setVisibility(View.GONE);
        else //saving
            recyclerView.setVisibility(View.GONE);

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
                ArgsRecyclerViewAdapter.ViewHolder h = (ArgsRecyclerViewAdapter.ViewHolder) viewHolder;

                FileSAF file = new FileSAF(AppInfo.getGamepadDirectory() + "/" + h.item);
                file.delete();
                findFiles();
            }
        };
        simpleItemTouchCallback.setDefaultSwipeDirs(ItemTouchHelper.RIGHT);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        ItemClickSupport.addTo(recyclerView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener()
        {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v)
            {
                if (load)
                {
                    selected(files.get(position));
                    dialog.dismiss();
                }
                else
                {
                    nameEditText.setText(files.get(position));
                }
            }
        });

        findFiles();
    }

    public void selected(String file)
    {
        // Override me
    }

    private void findFiles()
    {
        String[] gamepadFiles = new FileSAF(AppInfo.getGamepadDirectory()).list();
        files.clear();
        files.addAll(new ArrayList<String>(Arrays.asList(gamepadFiles)));
        rvAdapter.notifyDataSetChanged();
    }

    public class ArgsRecyclerViewAdapter extends RecyclerView.Adapter<ArgsRecyclerViewAdapter.ViewHolder>
    {
        public ArgsRecyclerViewAdapter()
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
            holder.item = files.get(position);

            holder.textView.setText(holder.item);
        }

        @Override
        public int getItemCount()
        {
            return files.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder
        {
            public final TextView textView;
            public final View view;
            public String item;

            public ViewHolder(View view)
            {
                super(view);

                this.view = view;
                this.textView = view.findViewById(R.id.textView);
            }
        }
    }
}
