package com.opentouchgaming.androidcore.common;

import android.app.Activity;
import android.app.Dialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.opentouchgaming.androidcore.AppInfo;
import com.opentouchgaming.androidcore.DragSortRecycler;
import com.opentouchgaming.androidcore.R;

/**
 * Created by Emile on 31/10/2017.
 */

public class CustomArgsFileRearrange
{
    RecyclerView recyclerView;
    ArgsRecyclerViewAdapter rvAdapter;

    CustomArgs customArgs;

    public CustomArgsFileRearrange(final Activity act, final CustomArgs customArgs, Runnable update)
    {
        this.customArgs = customArgs;

        final Dialog dialog = new Dialog(act);
        //dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        dialog.setTitle("Mod files");
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
                customArgs.getFiles().remove(h.getAdapterPosition());
                rvAdapter.notifyDataSetChanged();
                if (customArgs.getFiles().size() == 0)
                    dialog.dismiss();
            }
        };
        simpleItemTouchCallback.setDefaultSwipeDirs(ItemTouchHelper.RIGHT);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);


        DragSortRecycler dragSortRecycler = new DragSortRecycler();
        dragSortRecycler.setViewHandleId(R.id.imageView); //View you wish to use as the handle

        dragSortRecycler.setOnItemMovedListener(new DragSortRecycler.OnItemMovedListener()
        {
            @Override
            public void onItemMoved(int from, int to)
            {
                Log.d("TEST", "onItemMoved " + from + " to " + to);
                String f = customArgs.getFiles().get(from);
                customArgs.getFiles().add(from > to ? to : to + 1, f);
                customArgs.getFiles().remove(from > to ? from + 1 : from);
                rvAdapter.notifyDataSetChanged();
            }
        });

        recyclerView.addItemDecoration(dragSortRecycler);
        recyclerView.addOnItemTouchListener(dragSortRecycler);
        recyclerView.setOnScrollListener(dragSortRecycler.getScrollListener());

        dialog.setOnDismissListener(dialog1 -> update.run());

        dialog.show();
    }

    public class ArgsRecyclerViewAdapter extends RecyclerView.Adapter<ArgsRecyclerViewAdapter.ViewHolder>
    {
        public ArgsRecyclerViewAdapter()
        {

        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_args_rearrange, parent, false);
            view.setFocusable(true);
            view.setBackgroundResource(R.drawable.focusable);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position)
        {
            holder.item = customArgs.getFiles().get(position);
            holder.textView.setText(AppInfo.hideAppPaths(holder.item));
        }

        @Override
        public int getItemCount()
        {
            return customArgs.getFiles().size();
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
