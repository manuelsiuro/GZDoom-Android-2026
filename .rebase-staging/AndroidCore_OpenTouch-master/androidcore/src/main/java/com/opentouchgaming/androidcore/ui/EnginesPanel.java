package com.opentouchgaming.androidcore.ui;

import android.app.Activity;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.widget.AppCompatImageView;

import com.opentouchgaming.androidcore.DebugLog;
import com.opentouchgaming.androidcore.GameEngine;
import com.opentouchgaming.androidcore.R;
import com.opentouchgaming.androidcore.Utils;

import java.util.ArrayList;

/**
 * Created by Emile on 25/07/2017.
 */

public class EnginesPanel
{
    static DebugLog log;

    static
    {
        log = new DebugLog(DebugLog.Module.GAMEFRAGMENT, "EnginesPanel");
    }

    GameEngine[] gameEngines;
    GameEngine currentEngine;
    SlidePanel leftSlidePanel;
    EnginesPanel.Listener listener;
    boolean useGroups;
    ArrayList<EngineGroup> engineGroups = new ArrayList<>();

    public EnginesPanel(Activity context, View topView, GameEngine[] engines, int sideImage, boolean useGroups, final Listener listener)
    {
        this.listener = listener;
        this.useGroups = useGroups;

        gameEngines = engines;

        View leftPanelTopView = topView.findViewById(R.id.relative_top_left_panel);
        LinearLayout leftPanelLayout = topView.findViewById(R.id.linear_left_hidden_panel);
        ImageButton leftPanelButton = topView.findViewById(R.id.imagebutton_entry_open_left);
        ImageView sideImageView = topView.findViewById(R.id.imageview_side_image);

        // Collect the engines into the UI groups
        EngineGroup group = null;
        int uiGroup = -1;
        for (int n = 0; n < gameEngines.length; n++)
        {
            if (useGroups)
            {
                // New (or first ui group)
                if (uiGroup != gameEngines[n].uiGroup)
                {
                    group = new EngineGroup();
                    engineGroups.add(group);
                    uiGroup = gameEngines[n].uiGroup;
                }
            }
            else
            {
                group = new EngineGroup();
                engineGroups.add(group);
            }

            group.engines.add(gameEngines[n]);
        }

        // Find largest number of engines in a group
        int largestGroup = 1;
        for (EngineGroup g : engineGroups)
        {
            //log.log(DebugLog.Level.D, "Group has " + g.engines.size());
            if (g.engines.size() > largestGroup)
                largestGroup = g.engines.size();
        }

        // Button to open panel
        leftPanelButton.setFocusable(false);
        leftPanelButton.setOnClickListener(view ->
                                           {
                                               if (isOpen())
                                                   close();
                                               else
                                                   open();
                                               updateFocus();
                                           });


        // Total width of the panel excluding the open button
        int leftPanelSlideAmmount = 0;
        int screenHeightPx;  // Get screen height in pixels

        //if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
        if (false)
        {
            DisplayMetrics outMetrics = new DisplayMetrics();
            context.getWindowManager().getDefaultDisplay().getRealMetrics(outMetrics);
            screenHeightPx = outMetrics.heightPixels;
        }
        else // Very old, Android 16!
        {
            Configuration configuration = context.getResources().getConfiguration();
            screenHeightPx = Utils.dpToPx(context.getResources(), configuration.screenHeightDp);
        }

        // Check if we have a side image
        if (sideImage != 0)
        {
            sideImageView.setImageResource(sideImage);

            ViewGroup.LayoutParams sideLayout = sideImageView.getLayoutParams();
            sideLayout.width = screenHeightPx / 7; //Image should be 1024 * 150 which is a ratio of approx 7
            sideImageView.setLayoutParams(sideLayout);

            leftPanelSlideAmmount += sideLayout.width;
        }
        else
        {
            sideImageView.setVisibility(View.GONE);
        }

        float cfgButtonSize = 0.8f;

        // Calculate square button size
        // Give equal size for each ui group
        int buttonSize = screenHeightPx / engineGroups.size();
        int buttonCfgSize;
        int totalWidth;

        buttonCfgSize = (int) (buttonSize * cfgButtonSize);

        if (useGroups)
        {
            totalWidth = buttonSize * largestGroup + buttonCfgSize;
        }
        else
        {
            totalWidth = buttonSize + buttonCfgSize;
        }

        // Set the layout width, equal to the button size x largest group
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) leftPanelLayout.getLayoutParams();
        lp.width = totalWidth;
        leftPanelLayout.setLayoutParams(lp);
        leftPanelSlideAmmount += lp.width;

        // Create sliding panel
        leftSlidePanel = new SlidePanel(leftPanelTopView, SlidePanel.SlideSide.LEFT, leftPanelSlideAmmount, 300);

        for (int g = 0; g < engineGroups.size(); g++)
        {
            //log.log(DebugLog.Level.D,"g = " + g +  " engineGroups.size() = " + engineGroups.size());
            group = engineGroups.get(g);

            // Create new ui group layout, complete row of engines and config button
            int margin = 0;
            LinearLayout groupLayout = new LinearLayout(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 0);
            params.weight = 1;
            params.setMargins(margin, margin, margin, margin);
            groupLayout.setLayoutParams(params);

            // Contains one or more engine icons in a row
            LinearLayout enginesLayout = new LinearLayout(context);
            params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(margin, margin, margin, margin);
            enginesLayout.setLayoutParams(params);

            // Contains all the config buttons in a row, on top of each other
            RelativeLayout configButtonsLayout = new RelativeLayout(context);
            configButtonsLayout.setLayoutParams(new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                                                                LinearLayout.LayoutParams.MATCH_PARENT));

            // Add buttons to the group
            for (int e = 0; e < group.engines.size(); e++)
            {
                GameEngine engine = group.engines.get(e);

                // MAIN BUTTON ------------------------------------------------------------------
                AppCompatImageView button = new AppCompatImageView(context);
                params = new LinearLayout.LayoutParams(buttonSize, buttonSize);
                params.setMargins(margin, margin, margin, margin);
                button.setTag(engine); // Used for the click listener callback
                button.setImageResource(engine.iconRes);
                button.setScaleType(ImageView.ScaleType.FIT_CENTER);
                button.setFocusableInTouchMode(true);

                if (engine.backgroundDrawable != 0)
                    button.setBackgroundResource(engine.backgroundDrawable);
                else
                    button.setBackgroundResource(R.drawable.focusable);

                button.setOnClickListener(view ->
                                          {
                                              GameEngine engine12 = (GameEngine) view.getTag();
                                              selectEngine(engine12);
                                              close();
                                          });

                button.setLayoutParams(params);
                enginesLayout.addView(button);

                // CFG BUTTON  ------------------------------------------------------------------
                AppCompatImageView buttonCfg;
                buttonCfg = new AppCompatImageView(context);
                buttonCfg.setTag(engine); // Used for the click listener callback
                buttonCfg.setScaleType(ImageView.ScaleType.FIT_CENTER);
                buttonCfg.setBackgroundResource(R.drawable.focusable);
                buttonCfg.setFocusableInTouchMode(true);

                if (engine.engineOptions != null) // Only show if available
                    buttonCfg.setImageResource(R.drawable.ic_settings_black_24dp);

                buttonCfg.setOnClickListener(view ->
                                             {
                                                 GameEngine engine1 = (GameEngine) view.getTag();
                                                 listener.engineConfig(engine1);
                                             });

                RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(buttonCfgSize, buttonCfgSize);
                buttonParams.addRule(RelativeLayout.CENTER_VERTICAL);
                buttonCfg.setLayoutParams(buttonParams);

                // Add button to button relative layout
                configButtonsLayout.addView(buttonCfg);

                engine.imageButton = button;
                engine.imageButtonCfg = buttonCfg;
            }

            groupLayout.addView(enginesLayout);
            groupLayout.addView(configButtonsLayout);

            // Add full row
            leftPanelLayout.addView(groupLayout);
        }
        updateFocus();
    }

    private void updateFocus()
    {
        for (int n = 0; n < gameEngines.length; n++)
        {
            GameEngine button = gameEngines[n];
            if (leftSlidePanel.isOpen())
            {
                button.imageButton.setFocusable(true);
                button.imageButtonCfg.setFocusable(true);
                // Set focus to current engine
                if (currentEngine == button)
                {
                    button.imageButton.requestFocus();
                }
            }
            else
            {
                button.imageButton.setFocusable(false);
                button.imageButtonCfg.setFocusable(false);
            }
        }
    }

    public boolean isOpen()
    {
        return leftSlidePanel.isOpen();
    }

    public void close()
    {
        leftSlidePanel.close();
        listener.enginePanelStateChange(false);
        //fadeHandleButton(false);
        updateFocus();
    }

    public void open()
    {
        leftSlidePanel.open();
        listener.enginePanelStateChange(true);
        // Fade out button
        //fadeHandleButton(true);
        updateFocus();
    }

    public void closeIfOpen()
    {
        leftSlidePanel.closeIfOpen();
        listener.enginePanelStateChange(false);
        //fadeHandleButton(false);
        updateFocus();
    }

    public void selectEngine(GameEngine engine)
    {
        // Update buttons so they are highlighted
        for (int n = 0; n < gameEngines.length; n++)
        {
            GameEngine button = gameEngines[n];
            if (engine == gameEngines[n]) // Set selected image
            {
                button.imageButton.setActivated(true);
                button.imageButtonCfg.setVisibility(View.VISIBLE);
            }
            else
            {
                button.imageButton.setActivated(false);
                button.imageButtonCfg.setVisibility(View.INVISIBLE);
            }
        }

        currentEngine = engine;
        listener.engineSelected(currentEngine);
    }

    public interface Listener
    {
        void engineSelected(GameEngine engine);

        void engineConfig(GameEngine engine);

        void enginePanelStateChange(boolean open);
    }

    private class EngineGroup
    {
        ArrayList<GameEngine> engines = new ArrayList<>();
        ImageButton button;
    }
}
