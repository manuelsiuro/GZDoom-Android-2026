package com.opentouchgaming.androidcore.ui;

import static com.opentouchgaming.androidcore.DebugLog.Level.D;

import android.app.Activity;
import android.app.Dialog;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.opentouchgaming.androidcore.AppInfo;
import com.opentouchgaming.androidcore.DebugLog;
import com.opentouchgaming.androidcore.R;
import com.opentouchgaming.androidcore.ScopedStorage;
import com.opentouchgaming.saffal.UtilsSAF;

import java.util.List;
import java.util.function.Function;


public class ScopedStorageDialog
{

    static DebugLog log;

    static
    {
        log = new DebugLog(DebugLog.Module.GAMEFRAGMENT, "ScopedStorageDialog");
    }

    Activity activity;
    RecyclerView recyclerView;
    ViewAdapter viewAdapter;
    Tutorial tutorial;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public ScopedStorageDialog(final Activity activity, Tutorial tutorial, Runnable update)
    {
        this.activity = activity;
        this.tutorial = tutorial;

        final Dialog dialog = new Dialog(activity, R.style.DialogEngineSettings);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_scoped_storage);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(true);

        TextView t = dialog.findViewById(R.id.suggested_folder_textView);
        t.setText(tutorial.folder);

        recyclerView = dialog.findViewById(R.id.image_recyclerView);
        //recyclerView.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setLayoutManager(new GridLayoutManager(activity, 2));
        viewAdapter = new ViewAdapter();
        recyclerView.setAdapter(viewAdapter);


        Button goButton = dialog.findViewById(R.id.go_button);

        goButton.setOnClickListener(v ->
                                    {
                                        UtilsSAF.openDocumentTree(activity, ScopedStorage.OPENDOCUMENT_TREE_RESULT);
                                    });

        dialog.setOnDismissListener(dialog1 -> update.run());

        dialog.show();

        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        ScopedStorage.openDocumentCallback = new Function<String, Void>()
        {
            @Override
            public Void apply(String error)
            {
                if (error != null)
                {
                    log.log(D, "ERROR: " + error);
                    AppInfo.setAppSecDirectory(null);
                    Toast.makeText(activity, "ERROR: " + error, Toast.LENGTH_LONG).show();
                }
                else
                {
                    dialog.dismiss();
                }
                return null;
            }
        };

        viewAdapter.notifyDataSetChanged();
    }

    public static class Tutorial
    {
        public String folder;

        public List<Item> items;

        public static class Item
        {
            String desc;
            int imageA;
            int imageB;

            public Item(String desc, int imageA, int imageB)
            {
                this.desc = desc;
                this.imageA = imageA;
                this.imageB = imageB;
            }
        }
    }

    public class ViewAdapter extends RecyclerView.Adapter<ViewAdapter.ViewHolder>
    {

        @Override
        public ViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_scoped_tutorial, parent, false);

            return new ViewAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position)
        {
            holder.image.setImageResource(tutorial.items.get(position).imageA);
            if (tutorial.items.get(position).imageB != 0)
            {
                holder.image2.setImageResource(tutorial.items.get(position).imageB);

                Animation animation = new AlphaAnimation(1, 0); //to change visibility from visible to invisible
                animation.setDuration(300); //1 second duration for each animation cycle
                animation.setInterpolator(new LinearInterpolator());
                animation.setRepeatCount(Animation.INFINITE); //repeating indefinitely
                animation.setRepeatMode(Animation.REVERSE); //animation will start from end point once ended.
                holder.image2.startAnimation(animation); //to start animation
            }
            else
            {
                holder.image2.setVisibility(View.GONE);
            }
            holder.number.setText("");
            holder.info.setText(tutorial.items.get(position).desc);
        }


        @Override
        public int getItemCount()
        {
            return tutorial.items.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder
        {
            public final ImageView image;
            public final ImageView image2;
            public final TextView number;
            public final TextView info;

            public ViewHolder(View view)
            {
                super(view);
                this.image = view.findViewById(R.id.imageView);
                this.image2 = view.findViewById(R.id.imageView2);
                this.number = view.findViewById(R.id.number_textView);
                this.info = view.findViewById(R.id.info_textView);
            }
        }
    }
}
