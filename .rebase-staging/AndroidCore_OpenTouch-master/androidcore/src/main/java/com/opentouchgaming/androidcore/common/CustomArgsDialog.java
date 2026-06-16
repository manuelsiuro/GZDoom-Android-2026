package com.opentouchgaming.androidcore.common;


import android.app.Activity;
import android.app.Dialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.opentouchgaming.androidcore.AppInfo;
import com.opentouchgaming.androidcore.R;

import java.util.ArrayList;

public class CustomArgsDialog
{
    static CustomArgs copyPasteArgs = null;
    public final Dialog dialog;
    final EngineData engineData;
    String appDir;
    String appSecDir;
    String extraPath = "";
    Activity activity;
    TextView customModsTextView;
    EditText customArgsEditText;
    ImageButton pasteButton;
    ImageButton clearMods;
    ArrayList<String> undoClearMods = new ArrayList<>();

    public CustomArgsDialog(final Activity act, final String appDir, String appSecDir, EngineData ed, boolean hideModsWads)
    {
        this.appDir = appDir;
        this.appSecDir = appSecDir;

        activity = act;
        this.engineData = ed;

        dialog = new Dialog(activity, R.style.DialogEngineSettings);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_custom_args);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        dialog.setOnKeyListener((arg0, keyCode, event) -> false);

        // Hide the mods and wads if needed
        LinearLayout modWadsLayout = dialog.findViewById(R.id.mod_wads_layout);
        if (hideModsWads)
            modWadsLayout.setVisibility(View.GONE);

        customModsTextView = dialog.findViewById(R.id.textView_custom_mods);
        customArgsEditText = dialog.findViewById(R.id.editText_custom_args);

        clearMods = dialog.findViewById(R.id.imagebutton_clear_mods);
        clearMods.setOnClickListener(v ->
                                     {
                                         // Doing an undo
                                         if (engineData.getCurrentCustomArgs().getFiles().size() == 0 && undoClearMods.size() > 0)
                                         {
                                             engineData.getCurrentCustomArgs().getFiles().addAll(undoClearMods);
                                             undoClearMods.clear();
                                         }
                                         else
                                         {
                                             // Save in case we want to undo
                                             undoClearMods.clear();
                                             undoClearMods.addAll(engineData.getCurrentCustomArgs().getFiles());

                                             engineData.getCurrentCustomArgs().getFiles().clear();
                                         }
                                         update();
                                     });

        ImageButton selectMods = dialog.findViewById(R.id.imagebutton_select_mods);
        selectMods.setOnClickListener(v -> new ModSelectDialog(act, appDir, appSecDir, engineData.getCurrentCustomArgs(), input ->
        {
            update();
            return null;
        }));

        ImageButton clearArgs = dialog.findViewById(R.id.imagebutton_clear_custom);
        clearArgs.setOnClickListener(v ->
                                     {
                                         engineData.getCurrentCustomArgs().setArgs("");
                                         update();
                                     });

        ImageButton history = dialog.findViewById(R.id.imagebutton_args_history);
        history.setOnClickListener(v -> new CustomArgsHistoryDialog(act, engineData.getArgsHistory())
        {
            public void selected(int position)
            {
                // Set the current args to one of the ones in history
                engineData.getCurrentCustomArgs().setArgs(engineData.getArgsHistory().get(position).getArgsString());
                engineData.getCurrentCustomArgs().getFiles().clear();
                engineData.getCurrentCustomArgs().getFiles().addAll(engineData.getArgsHistory().get(position).getFiles());
                update();
            }
        });

        customArgsEditText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                engineData.getCurrentCustomArgs().setArgs(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s)
            {
            }
        });

        customModsTextView.setOnClickListener(v ->
                                              {
                                                  if (engineData.getCurrentCustomArgs().getFiles().size() > 0)
                                                      new CustomArgsFileRearrange(activity, engineData.getCurrentCustomArgs(), () -> update());
                                              });

        ImageButton copy = dialog.findViewById(R.id.imagebutton_copy);
        copy.setOnClickListener(v ->
                                {
                                    copyPasteArgs = new CustomArgs();
                                    copyPasteArgs.copy(engineData.getCurrentCustomArgs());
                                    Toast.makeText(act, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                                    update();
                                });

        pasteButton = dialog.findViewById(R.id.imagebutton_paste);
        pasteButton.setOnClickListener(v ->
                                       {
                                           if (copyPasteArgs != null)
                                               engineData.getCurrentCustomArgs().copy(copyPasteArgs);
                                           update();
                                       });

        update();
        dialog.show();
    }

    private void update()
    {
        customModsTextView.setText(AppInfo.hideAppPaths(engineData.getCurrentCustomArgs().getModsString()));
        customArgsEditText.setText(engineData.getCurrentCustomArgs().getArgsString());
        pasteButton.setVisibility(copyPasteArgs == null ? View.GONE : View.VISIBLE);

        if (engineData.getCurrentCustomArgs().getFiles().isEmpty() && !undoClearMods.isEmpty())
        {
            clearMods.setImageResource(R.drawable.ic_undo_white_24);
        }
        else
        {
            clearMods.setImageResource(R.drawable.ic_clear_white_24dp);
        }
    }
}
