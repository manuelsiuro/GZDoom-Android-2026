package com.opentouchgaming.androidcore;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.arch.core.util.Function;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.opentouchgaming.androidcore.ui.OptionsDialogKt;

import java.util.ArrayList;


public class SubGameRecyclerViewAdapter extends RecyclerView.Adapter<SubGameRecyclerViewAdapter.ViewHolder>
{

    private final ArrayList<SubGame> mValues;

    private final Function<SubGame, Void> subgameAdditional;

    private boolean multiSelect = false;

    public SubGameRecyclerViewAdapter(ArrayList<SubGame> items, Function<SubGame, Void> subgameAdditional)
    {
        mValues = items;
        this.subgameAdditional = subgameAdditional;
    }

    public void setMultiSelect(boolean ms)
    {
        multiSelect = ms;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        boolean useMini = AppSettings.getBoolOption(parent.getContext(), OptionsDialogKt.USE_MINI_UI, false);

        View view = LayoutInflater.from(parent.getContext()).inflate(useMini ? R.layout.list_item_subgame_mini : R.layout.list_item_subgame, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position)
    {
        holder.mItem = mValues.get(position);

        holder.mIdView.setText(holder.mItem.title);
        holder.mDetail1.setText(AppInfo.replaceRootPaths(holder.mItem.detail1));
        holder.mDetail2.setText(AppInfo.replaceRootPaths(holder.mItem.detail2));
        if (holder.mItem.getImagePng() != null)
        {
            holder.mImage.setScaleType(ImageView.ScaleType.FIT_XY);
            RequestOptions requestOptions = new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE) // because file name is always same
                    .skipMemoryCache(false);

            Glide.with(AppInfo.getContext()).load(holder.mItem.getImagePng()).apply(requestOptions).into(holder.mImage);
        }
        else
        {
            holder.mImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
            holder.mImage.setImageResource(holder.mItem.image);
        }

        if (holder.mItem.selected)
        {
            holder.mView.setBackgroundResource(R.drawable.subgame_selected);
        }
        else
        {
            holder.mView.setBackgroundResource(0);
        }

        if (multiSelect && !holder.mItem.selected)
        {
            holder.mGameTypeImage.setImageResource(R.drawable.ic_add);
        }
        else
        {
            holder.mGameTypeImage.setImageResource(0);
        }

        holder.mGameTypeImage.setOnClickListener(v ->
                                                 {
                                                     subgameAdditional.apply(holder.mItem);
                                                 });
    }

    @Override
    public int getItemCount()
    {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder
    {
        public final View mView;
        public final ImageView mImage;
        public final ImageView mGameTypeImage;
        public final TextView mIdView;
        public final TextView mDetail1;
        public final TextView mDetail2;

        public SubGame mItem;

        public ViewHolder(View view)
        {
            super(view);
            mView = view;
            mIdView = view.findViewById(R.id.textview_iwad_title);
            mDetail1 = view.findViewById(R.id.textview_iwad_detail1);
            mDetail2 = view.findViewById(R.id.textview_iwad_detail2);
            mImage = view.findViewById(R.id.imageview);
            mGameTypeImage = view.findViewById(R.id.game_type_imageview);
        }
    }
}
