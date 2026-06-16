package com.opentouchgaming.androidcore.common;

import android.app.Activity;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.opentouchgaming.androidcore.AppInfo;
import com.opentouchgaming.androidcore.ItemClickSupport;
import com.opentouchgaming.androidcore.R;

import java.util.ArrayList;

/**
 * Created by Emile on 31/10/2017.
 */

public class CustomArgsHistoryDialog
{
    RecyclerView recyclerView;
    ArgsRecyclerViewAdapter rvAdapter;

    ArrayList<CustomArgs> argsHistory;

    protected CustomArgsHistoryDialog(final Activity act, final ArrayList<CustomArgs> argsHistory)
    {
        this.argsHistory = argsHistory;

        if(argsHistory.isEmpty())
            return;

        final Dialog dialog = new Dialog(act, R.style.DialogEngineSettingsWrap);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
       
        dialog.setTitle("Args history");
        dialog.setContentView(R.layout.dialog_args_history);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);

        recyclerView = dialog.findViewById(R.id.recyclerView);
        //recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(act));
        recyclerView.setFocusable(true);
        rvAdapter = new ArgsRecyclerViewAdapter();
        recyclerView.setAdapter(rvAdapter);

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
                argsHistory.remove(h.item);
                rvAdapter.notifyDataSetChanged();
            }
        };
        simpleItemTouchCallback.setDefaultSwipeDirs(ItemTouchHelper.RIGHT);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        ItemClickSupport.addTo(recyclerView).setOnItemClickListener((recyclerView, position, v) ->
                                                                    {
                                                                        selected(position);
                                                                        dialog.dismiss();
                                                                    });

        dialog.show();
    }

    public void selected(int position)
    {
        // Override me
    }

    public class ArgsRecyclerViewAdapter extends RecyclerView.Adapter<ArgsRecyclerViewAdapter.ViewHolder>
    {
        public ArgsRecyclerViewAdapter()
        {

        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_args_history, parent, false);
            view.setFocusable(true);
            view.setBackgroundResource(R.drawable.focusable);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position)
        {
            holder.item = argsHistory.get(position);
            holder.textView.setText(AppInfo.hideAppPaths(holder.item.getFinalArgs()));
        }

        @Override
        public int getItemCount()
        {
            return argsHistory.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder
        {
            public final TextView textView;
            public final View view;
            public CustomArgs item;

            public ViewHolder(View view)
            {
                super(view);
                this.view = view;
                this.textView = view.findViewById(R.id.textView);
            }
        }
    }
}
