package com.opentouchgaming.androidcore.common;

import static com.opentouchgaming.androidcore.DebugLog.Level.D;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.opentouchgaming.androidcore.AboutDialog;
import com.opentouchgaming.androidcore.AppInfo;
import com.opentouchgaming.androidcore.AppSettings;
import com.opentouchgaming.androidcore.DebugLog;
import com.opentouchgaming.androidcore.GameEngine;
import com.opentouchgaming.androidcore.GamepadActivity;
import com.opentouchgaming.androidcore.ItemClickSupport;
import com.opentouchgaming.androidcore.LogViewDialog;
import com.opentouchgaming.androidcore.R;
import com.opentouchgaming.androidcore.ScopedStorage;
import com.opentouchgaming.androidcore.SubGame;
import com.opentouchgaming.androidcore.SubGameRecyclerViewAdapter;
import com.opentouchgaming.androidcore.Utils;
import com.opentouchgaming.androidcore.controls.Dpad;
import com.opentouchgaming.androidcore.license.LicenseCheck;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.opentouchgaming.androidcore.ui.AppPromoDialog;
import com.opentouchgaming.androidcore.ui.EnginesPanel;
import com.opentouchgaming.androidcore.ui.FileSelectDialog;
import com.opentouchgaming.androidcore.ui.OptionsDialogKt;
import com.opentouchgaming.androidcore.ui.StorageConfigDialog;
import com.opentouchgaming.androidcore.ui.SuperMod.SuperModDialog;
import com.opentouchgaming.androidcore.ui.SuperMod.SuperModItem;
import com.opentouchgaming.androidcore.ui.ToolsPanel;

import java.io.File;
import com.opentouchgaming.androidcore.ui.tutorial.TutorialDialog;

import java.util.ArrayList;


public class MainFragment extends Fragment implements ToolsPanel.Listener, EnginesPanel.Listener
{
    // Set by the entry activity
    static DebugLog log;

    static
    {
        log = new DebugLog(DebugLog.Module.GAMEFRAGMENT, "MainFragment");
    }

    final int TOOL_BUTTON_GAMEPAD = 0;
    final int TOOL_BUTTON_SETTINGS = 1;
    final int TOOL_BUTTON_LOG = 2;
    final int TOOL_BUTTON_INFO = 3;
    final int TOOL_BUTTON_HELP = 4;
    final int TOOL_BUTTON_EMAIL = 5;
    final int TOOL_BUTTON_STORAGE = 6;
    final int TOOL_BUTTON_WEBSITE = 7;
    public final ToolsPanel.ToolsPanelButton[] toolsButtons = new ToolsPanel.ToolsPanelButton[]{
            new ToolsPanel.ToolsPanelButton(TOOL_BUTTON_GAMEPAD, "Gamepad", R.drawable.ic_videogame_asset_black_24dp),
            new ToolsPanel.ToolsPanelButton(TOOL_BUTTON_SETTINGS, "Settings", R.drawable.ic_settings_black_24dp),
            new ToolsPanel.ToolsPanelButton(TOOL_BUTTON_STORAGE, "Files", R.drawable.ic_baseline_sd_card_black),
            new ToolsPanel.ToolsPanelButton(TOOL_BUTTON_LOG, "View last log", R.drawable.ic_computer_black_24dp),
            new ToolsPanel.ToolsPanelButton(TOOL_BUTTON_INFO, "Changes", R.drawable.ic_info_outline_black_24dp),
            AppInfo.website == null ? new ToolsPanel.ToolsPanelButton(TOOL_BUTTON_EMAIL,
                                                                      "Email log",
                                                                      R.drawable.ic_email_black_24dp) : new ToolsPanel.ToolsPanelButton(TOOL_BUTTON_WEBSITE,
                                                                                                                                        "Other apps",
                                                                                                                                        R.drawable.baseline_android_24),
            new ToolsPanel.ToolsPanelButton(TOOL_BUTTON_HELP, "Tutorials", R.drawable.ic_help_green),};
    private final ArrayList<SubGame> multiselectGames = new ArrayList<>();
    public boolean noLicCheck = false;
    // App data to be saved/loaded
    public AppData appData;
    // Data to be saved for the current engine, currently the argument history
    public EngineData engineData;
    // Handel dpad arrows
    public Dpad dpadControl = new Dpad();
    // Left/right panels
    public EnginesPanel enginesLeftPanel;
    public ToolsPanel toolsPanel;
    public SubGameRecyclerViewAdapter subGameAdapter;
    // UI elements
    public RecyclerView recyclerView;
    public ImageView backgroundImageView;
    public TextView appTitleTextView;
    public TextView engineTitleTextView;
    public TextView argsTextView;
    public ImageButton swapVerImageButton;
    public ImageButton startButton;
    public ImageButton showArgsButton;
    public ImageButton multiplayerButton;
    public ImageButton superModButton;
    public ImageButton downloadNewVersion;
    public Drawable subgameSeparatorLine; // So we cna change the color of the line
    public int selectedVersion = 0;
    public ArrayList<SubGame> availableSubGames = new ArrayList<>();
    public SubGame selectedSubGame;
    //CustomArgs customArgs = new CustomArgs();
    public String argsFinal = "";
    // Current gamepad focus mode
    public FocusMode focusMode;
    public GameLauncherInterface launcher;
    private boolean multiselectEnable = false;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MainFragment()
    {
        log.log(D, "New instant created!");
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        for (GameEngine engine : AppInfo.gameEngines)
        {
            engine.init(getActivity());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        log.log(D, "onCreateView");

        ScopedStorage.checkStorageOK(getActivity());

        View view = inflater.inflate(R.layout.fragment_alpha, container, false);

        enginesLeftPanel = new EnginesPanel(getActivity(), view, AppInfo.gameEngines, AppInfo.sidePanelImage, AppInfo.groupSimilarEngines, this);

        toolsPanel = new ToolsPanel(getContext(), view, toolsButtons, this);

        recyclerView = view.findViewById(R.id.list);
        backgroundImageView = view.findViewById(R.id.imageview_doom_background);
        argsTextView = view.findViewById(R.id.textview_doom_args);

        swapVerImageButton = view.findViewById(R.id.imagebutton_change_version);
        swapVerImageButton.setBackgroundResource(R.drawable.focusable);
        swapVerImageButton.setOnClickListener(view1 -> cycleVersion());
        superModButton = view.findViewById(R.id.imageview_super_mod);
        downloadNewVersion = view.findViewById(R.id.imagebutton_new_version);

        // Title text and set font
        appTitleTextView = view.findViewById(R.id.app_title_textive);
        Typeface face = Typeface.createFromAsset(getContext().getAssets(), "recharge_font.ttf");
        appTitleTextView.setTypeface(face);
        engineTitleTextView = view.findViewById(R.id.engine_title_textView);
        engineTitleTextView.setTypeface(face);

        Context context = view.getContext();

        recyclerView.setFocusable(false); // We always intercept gamepad to control the list

        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        subGameAdapter = new SubGameRecyclerViewAdapter(availableSubGames, input ->
        {
            if (multiselectEnable)
            {
                if (multiselectGames.contains(input))
                    multiselectGames.remove(input);
                else
                    multiselectGames.add(input);

                updateArgs();
            }
            return null;
        });

        recyclerView.setAdapter(subGameAdapter);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), LinearLayoutManager.VERTICAL);
        subgameSeparatorLine = ResourcesCompat.getDrawable(getResources(), R.drawable.wad_separate_line, null);
        dividerItemDecoration.setDrawable(subgameSeparatorLine);
        recyclerView.addItemDecoration(dividerItemDecoration);

        ItemClickSupport.addTo(recyclerView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener()
        {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v)
            {
                enginesLeftPanel.closeIfOpen();
                toolsPanel.closeIfOpen();
                selectSubGame(position);
            }
        });

        ItemClickSupport.addTo(recyclerView).setOnItemLongClickListener(new ItemClickSupport.OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClicked(RecyclerView recyclerView, int position, View v)
            {
                selectSubGame(position);
                SubGame.DialogCallback callback = new SubGame.DialogCallback()
                {
                    @Override
                    public void dismiss()
                    {
                        refreshSubGames();
                        selectSubGame(engineData.selectedSubGamePos);
                    }
                };

                if (selectedSubGame != null)
                    selectedSubGame.edit(getActivity(), callback);
                return true;
            }
        });

        startButton = view.findViewById(R.id.imageview_game_start);

        multiplayerButton = view.findViewById(R.id.imageview_doom_multiplayer);
        multiplayerButton.setBackgroundResource(R.drawable.focusable);
        multiplayerButton.setOnClickListener(v ->
                AppInfo.currentEngine.engineOptions.launchMultiplayer(getActivity(),
                        AppInfo.currentEngine,
                        selectedVersion,
                        argsFinal,
                        mpArgs -> launchGame(AppInfo.currentEngine, true, mpArgs)));

        showArgsButton = view.findViewById(R.id.imageview_doom_show_args);

        showArgsButton.setBackgroundResource(R.drawable.focusable);
        showArgsButton.setOnClickListener(v -> new CustomArgsDialog(getActivity(),
                                                                    launcher.getRunDirectory(),
                                                                    launcher.getSecondaryDirectory(),
                                                                    engineData,
                                                                    AppInfo.hideModWads &
                                                                    !launcher.forceShowModsWads()).dialog.setOnDismissListener(dialog -> updateArgs()));

        // START game
        startButton.setBackgroundResource(R.drawable.focusable);
        startButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                boolean ok = noLicCheck || LicenseCheck.checkLicenseFile(getContext(), AppInfo.key);

                if (!ok)
                {
                    LicenseCheck.fetchLicense(getActivity(), true, AppInfo.key);
                }
                else
                {
                    launchGame(AppInfo.currentEngine, true, "");
                }
            }
        });

        setFocusMode(FocusMode.GAMES);

        recyclerView.setFocusable(false); // We always intercept gamepad to control the list
        recyclerView.setFocusableInTouchMode(false);
        // SUPER MOD BUTTON
        superModButton.setBackgroundResource(R.drawable.focusable);
        superModButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (selectedSubGame != null)
                {
                    SuperModItem superMod = null;

                    // Ones with blank filenames are downloads or other non games
                    //if (!selectedSubGame.getFilename().contentEquals(""))
                    {
                        superMod = new SuperModItem(AppInfo.currentEngine.engine,
                                                    selectedVersion,
                                                    selectedSubGame.getTag(),
                                                    selectedSubGame.getImagePng(),
                                                    engineData.getCurrentCustomArgs());
                    }

                    new SuperModDialog(getActivity(), superMod,
                        superModItem ->
                        {
                            // Called when a super mod is selected
                            GameEngine engine = AppInfo.getGameEngine(superModItem.engine);
                            // Set version first
                            changeEngineVersion(engine, superModItem.version);
                            // Change engine, this reloads the iwads
                            enginesLeftPanel.selectEngine(engine);

                            // Replace args and files
                            engineData.getCurrentCustomArgs().copy(superModItem.customArgs);
                            // Try to find IWAD
                            for (int n = 0; n < availableSubGames.size(); n++)
                            {
                                SubGame subgame = availableSubGames.get(n);
                                if (subgame.getTag().contentEquals(superModItem.subgameTag))
                                {
                                    // Found!
                                    selectSubGame(n);
                                    break;
                                }
                            }
                        },
                        (item, onImageSelected) ->
                        {
                            FileSelectDialog.FileSelectCallback callback = filesArray ->
                            {
                                String imageOverride = (filesArray == null || filesArray.size() == 0) ? null : filesArray.get(0);
                                onImageSelected.accept(imageOverride);
                            };
                            new FileSelectDialog(getActivity(), callback, AppInfo.getAppDirectory(), new String[]{".png", ".jpg"}, false);
                        },
                        (item, imageView) ->
                        {
                            if (item.gameTypeImage != null)
                            {
                                imageView.setVisibility(View.VISIBLE);
                                imageView.setImageURI(Uri.fromFile(new File(item.gameTypeImage)));
                            }
                            else if (AppInfo.defaultAppImage != 0)
                            {
                                imageView.setVisibility(View.VISIBLE);
                                imageView.setImageResource(AppInfo.defaultAppImage);
                            }
                            else
                                imageView.setVisibility(View.GONE);
                        },
                        (item, imageView) ->
                        {
                            if (item.modImage != null)
                            {
                                imageView.setVisibility(View.VISIBLE);
                                Glide.with(getActivity()).load("zip_pic:" + item.modImage)
                                        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)).into(imageView);
                            }
                            else
                            {
                                imageView.setVisibility(View.GONE);
                            }
                        });
                }
            }
        });

        return view;
    }

    public void launchGame(final GameEngine engine, boolean download, final String multiplayerArgs)
    {
        log.log(D, "ERROR, launchGame must be overridden");
    }

    public void setLauncher()
    {
        log.log(D, "ERROR, setLauncher must be overridden");
    }

    public View getExtraOptions()
    {
        // Override if we have extra options
        return null;
    }

    public void setFocusMode(FocusMode mode)
    {
        log.log(D, "setFocusMode: mode = " + mode.toString());

        focusMode = mode;

        switch (focusMode)
        {
            case LAUNCH:
            {
                setLaunchButtonsFocus(true);
                break;
            }
            case GAMES:
            case ENGINE:
            case TOOLS:
            {
                setLaunchButtonsFocus(false);
                break;
            }
        }
    }

    public void setLaunchButtonsFocus(boolean enabled)
    {
        startButton.setFocusable(enabled);
        showArgsButton.setFocusable(enabled);
        swapVerImageButton.setFocusable(enabled);
        multiplayerButton.setFocusable(enabled);
        superModButton.setFocusable(enabled);
        if (enabled)
            startButton.requestFocus();
    }

    public void cycleVersion()
    {
        int newVersion = selectedVersion + 1;
        if (newVersion > AppInfo.currentEngine.versions.length - 1)
            newVersion = 0;

        // Update engine version
        changeEngineVersion(AppInfo.currentEngine, newVersion);
        // Update display
        selectEngine(AppInfo.currentEngine);
    }

    public boolean onBackPressed()
    {
        boolean ret = false;
        if (enginesLeftPanel.isOpen())
        {
            enginesLeftPanel.close();
            ret = true;
        }

        if (toolsPanel.isOpen())
        {
            toolsPanel.close();
            ret = true;
        }

        return ret;
    }

    @Override
    public void onPause()
    {
        super.onPause();

        AppData.saveToFile(AppInfo.internalFiles + "/AppData.dat", appData);
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        // Show about dialog in 1 second
        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                if (getActivity() != null)
                {
                    if (AboutDialog.showAbout(getActivity()))
                    {
                        AboutDialog.show(getActivity(), AppInfo.showRateButton);
                        // Also open the panel so people know about it
                        enginesLeftPanel.open();
                    }
                }
            }
        }, 1000);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        ScopedStorage.checkStorageOK(getActivity());
        updateAll();
    }

    public void updateAll()
    {
        appData = AppData.loadFromFile(AppInfo.internalFiles + "/AppData.dat");

        enginesLeftPanel.closeIfOpen();

        String lastEngine = AppSettings.getStringOption(getContext(), "last_engine", null);
        if (lastEngine != null)
        {
            for (GameEngine engine : AppInfo.gameEngines)
            {
                if (lastEngine.equals(engine.engine.toString()))
                {
                    enginesLeftPanel.selectEngine(engine);
                }
            }
        }

        if (AppInfo.currentEngine == null)
        {
            enginesLeftPanel.selectEngine(AppInfo.gameEngines[0]);
        }

        refreshSubGames();
        selectSubGame(engineData.selectedSubGamePos);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        log.log(D, "onKeyDown: event = " + event.toString() + " keyCode = " + keyCode);

        if ((event.getSource() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
        {
            if (event.getRepeatCount() == 0)
            {
                switch (keyCode)
                {
                    case KeyEvent.KEYCODE_BUTTON_A:
                    {
                        if (focusMode == FocusMode.GAMES)
                        {
                            setFocusMode(FocusMode.LAUNCH);
                            return true;
                        }
                        break;
                    }
                    case KeyEvent.KEYCODE_BUTTON_B:
                    {
                        if (focusMode == FocusMode.LAUNCH)
                        {
                            setFocusMode(FocusMode.GAMES);
                            return true;
                        }
                    }
                    default:

                        break;
                }
            }
        }
        return false;
    }

    public boolean onGenericMotionEvent(MotionEvent event)
    {
        //log.log(D, "onGenericMotionEvent: event = " + event.toString());

        if (Dpad.isDpadDevice(event))
        {
            int press = dpadControl.getDirectionPressed(event);
            switch (press)
            {
                case Dpad.LEFT: //Open left panel
                    // This is a mess. If launch buttons then we want left/right working
                    if (focusMode != FocusMode.GAMES)
                        return false;

                    // Close right if open
                    if (toolsPanel.isOpen())
                    {
                        toolsPanel.close();
                        return true;
                    }
                    // Open left
                    if (!enginesLeftPanel.isOpen())
                    {
                        enginesLeftPanel.open();
                        return true;
                    }

                    break;
                case Dpad.RIGHT:

                    if (focusMode != FocusMode.GAMES)
                        return false;

                    // Close left if open
                    if (enginesLeftPanel.isOpen())
                    {
                        enginesLeftPanel.close();
                        return true;
                    }

                    //Open right
                    if (!toolsPanel.isOpen())
                    {
                        toolsPanel.open();
                        return true;
                    }
                    break;
                case Dpad.UP:
                    if (focusMode == FocusMode.GAMES)
                    {
                        selectSubGame(engineData.selectedSubGamePos - 1);
                        return true;
                    }
                    break;
                case Dpad.DOWN:
                    if (focusMode == FocusMode.GAMES)
                    {
                        selectSubGame(engineData.selectedSubGamePos + 1);
                        return true;
                    }
                    break;
            }
        }
        return false;
    }

    public void changeEngineVersion(GameEngine engine, int version)
    {
        AppSettings.setIntOption(getContext(), "last_version_" + engine.engine.toString(), version);
    }

    public void selectEngine(GameEngine engine)
    {
        if (engine.wideBackground != 0)
            backgroundImageView.setImageResource(engine.wideBackground);
        else
            backgroundImageView.setImageResource(engine.iconRes);

        selectedVersion = AppSettings.getIntOption(getContext(), "last_version_" + engine.engine.toString(), 0);

        if (selectedVersion > engine.versions.length - 1)
            selectedVersion = 0;

        // Only DP can multiselect
        multiselectEnable = engine.engine == GameEngine.Engine.QUAKEDP;

        subGameAdapter.setMultiSelect(multiselectEnable);

        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new AccelerateInterpolator());
        fadeIn.setDuration(500);
        backgroundImageView.startAnimation(fadeIn);

        if (engine.versions.length > 1)
        {
            swapVerImageButton.setVisibility(View.VISIBLE);
            swapVerImageButton.startAnimation(fadeIn);
        }
        else
        {
            swapVerImageButton.setVisibility(View.GONE);
        }

        if (engine.engineOptions != null && engine.engineOptions.hasMultiplayer())
        {
            multiplayerButton.setVisibility(View.VISIBLE);
            multiplayerButton.startAnimation(fadeIn);
        }
        else
        {
            multiplayerButton.setVisibility(View.GONE);
        }

        appTitleTextView.setText(AppInfo.title);
        //appTitleTextView.setTextColor(0xFF000000 | engine.color);
        appTitleTextView.setTextColor(0xFF808080);
        engineTitleTextView.setText(engine.title + " - " + engine.versions[selectedVersion]);
        engineTitleTextView.setTextColor(0xFF000000 | engine.color);

        // Change the color of the separating lines
        GradientDrawable shapeDrawable = (GradientDrawable) subgameSeparatorLine;
        shapeDrawable.setColor(0xFF000000 | engine.color);

        AppInfo.currentEngine = engine;

        AppSettings.setStringOption(getContext(), "last_engine", AppInfo.currentEngine.engine.toString());

        engineData = appData.getEngineData(AppInfo.currentEngine.engine);

        setLauncher();

        refreshSubGames();
        selectSubGame(engineData.selectedSubGamePos);
    }

    public void refreshSubGames()
    {
        launcher.updateSubGames(AppInfo.currentEngine, availableSubGames);
        subGameAdapter.notifyDataSetChanged();
    }

    public void updateArgs()
    {
        if (selectedSubGame != null)
        {
            argsFinal = launcher.getArgs(AppInfo.currentEngine, selectedSubGame);

            for (SubGame sg : multiselectGames)
            {
                argsFinal += launcher.getArgs(AppInfo.currentEngine, sg);
            }

            argsFinal += " " + selectedSubGame.getExtraArgs();

            argsFinal += " " + engineData.getCurrentCustomArgs().getFinalArgs();
            //argsTextView.setText(AppInfo.replaceRootPaths(argsFinal));
            argsTextView.setText(AppInfo.hidePaths(argsFinal, launcher.getRunDirectory(), launcher.getSecondaryDirectory()));
        }
        else
        {
            argsTextView.setText("Select game");
        }
    }

    public void selectSubGame(int newPos)
    {
        multiselectGames.clear();

        if (availableSubGames.size() == 0)
            return;

        if (newPos >= availableSubGames.size() || newPos < 0)
            newPos = 0;

        engineData.selectedSubGamePos = newPos;

        // Make the iwad highlighted
        if (selectedSubGame != null)
        {
            selectedSubGame.selected = false;
        }

        selectedSubGame = availableSubGames.get(engineData.selectedSubGamePos);
        selectedSubGame.selected = true;
        subGameAdapter.notifyDataSetChanged();
        recyclerView.scrollToPosition(engineData.selectedSubGamePos);
        updateArgs();
    }

    @Override
    public void engineSelected(GameEngine engine)
    {
        selectEngine(engine);
    }

    @Override
    public void engineConfig(GameEngine engine)
    {
        if (engine.engineOptions != null)
        {
            engine.engineOptions.showDialog(getActivity(), engine, selectedVersion, version ->
            {
                // For redraw of items incase changed layouts
                recyclerView.setAdapter(null);
                recyclerView.setLayoutManager(null);
                recyclerView.setAdapter(subGameAdapter);
                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

                changeEngineVersion(AppInfo.currentEngine, version);
                selectEngine(AppInfo.currentEngine);
                return null;
            });
        }
    }

    @Override
    public void toolsOnClick(int code)
    {
        if (code == TOOL_BUTTON_GAMEPAD)
        {
            Intent intent = new Intent(getContext(), GamepadActivity.class);
            intent.putExtra("app", AppInfo.app.name());
            startActivity(intent);
        }
        else if (code == TOOL_BUTTON_SETTINGS)
        {
            //new OptionsDialog(getActivity(), getExtraOptions(), this::updateAll);
            new OptionsDialogKt(getActivity(), getExtraOptions(), this::updateAll);
        }
        else if (code == TOOL_BUTTON_STORAGE)
        {
            StorageConfigDialog scd = new StorageConfigDialog(getActivity(), AppInfo.storageExamples, () -> updateAll());
        }
        else if (code == TOOL_BUTTON_LOG)
        {
            new LogViewDialog(getActivity(), AppInfo.currentEngine.getLogFilename(), AppInfo.currentEngine.name);
        }
        else if (code == TOOL_BUTTON_INFO)
        {
            AboutDialog.show(getActivity(), AppInfo.showRateButton);
        }
        else if (code == TOOL_BUTTON_EMAIL)
        {
            Utils.SendDebugEmail(getActivity(), AppInfo.emailAddress, AppInfo.packageId, AppInfo.currentEngine.getLogFilename());
        }
        else if (code == TOOL_BUTTON_HELP)
        {
            new TutorialDialog(getActivity(), AppInfo.tutorials);
        }
        else if (code == TOOL_BUTTON_WEBSITE)
        {
            new AppPromoDialog().showDialog(getActivity());
        }
    }

    /* This is pretty shit and confusing, opening the panel will change its own focus mode */
    @Override
    public void toolsPanelStateChange(boolean open)
    {
        if (open)
            setFocusMode(FocusMode.TOOLS);
        else
            setFocusMode(FocusMode.GAMES);
    }

    @Override
    public void enginePanelStateChange(boolean open)
    {
        if (open)
            setFocusMode(FocusMode.ENGINE);
        else
            setFocusMode(FocusMode.GAMES);
    }

    public enum FocusMode
    {
        GAMES, LAUNCH, ENGINE, TOOLS
    }
}
