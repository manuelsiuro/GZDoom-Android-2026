package com.opentouchgaming.androidcore.controls;

import static com.opentouchgaming.androidcore.DebugLog.Level.D;
import static com.opentouchgaming.androidcore.DebugLog.Level.I;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.widget.PopupMenu;

import com.opentouchgaming.androidcore.AppInfo;
import com.opentouchgaming.androidcore.AppSettings;
import com.opentouchgaming.androidcore.DebugLog;
import com.opentouchgaming.androidcore.R;
import com.opentouchgaming.androidcore.ui.GamepadSaveLoad;

import java.io.IOException;


public class GamePadFragment extends Fragment implements ControlConfig.Listener
{

    static final String DEFAULT_CONFIG = "DEFAULT";
    static DebugLog log;

    static
    {
        log = new DebugLog(DebugLog.Module.CONTROLS, "GamePadFragment");
    }

    ListView listView;
    ControlListAdapter adapter;

    TextView info;

    ControlConfig config;

    String configFilename;

    /** True when launched with an engine-specific config override; suppresses saving to gamepad_config_filename. */
    boolean engineOverrideActive = false;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

    }

    void loadConfigFile(String file)
    {
        configFilename = file;

        if (configFilename == null)
        {
            configFilename = DEFAULT_CONFIG;
        }

        try
        {
            log.log(I, "Trying to load config from file...");
            config.loadControls(configFilename);
        }
        catch (Exception e)
        {
            log.log(I, "Failed to load file: " + e);
            log.log(I, "..file not found");
            configFilename = DEFAULT_CONFIG;
            try
            {
                config.saveControls(configFilename);
            }
            catch (IOException e1)
            {
                e1.printStackTrace();
            }
        }

        finishedMonitoring();
        adapter.notifyDataSetChanged();
        if (!engineOverrideActive)
            AppSettings.setStringOption(getActivity(), "gamepad_config_filename", configFilename);
    }

    void saveConfigFile(String file)
    {
        configFilename = file;

        if (configFilename == null)
        {
            configFilename = DEFAULT_CONFIG;
        }

        try
        {
            config.saveControls(configFilename);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            log.log(I, "Failed to save: " + e);
            configFilename = DEFAULT_CONFIG;

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Failed to save gamepad config");
            AlertDialog alert = builder.create();
            alert.show();
        }

        finishedMonitoring();
        adapter.notifyDataSetChanged();
        if (!engineOverrideActive)
            AppSettings.setStringOption(getActivity(), "gamepad_config_filename", configFilename);
    }

    @Override
    public void onHiddenChanged(boolean hidden)
    {
        super.onHiddenChanged(hidden);
    }


    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        config = new ControlConfig(GamepadDefinitions.getDefinition(AppInfo.app), this);

        View mainView = inflater.inflate(R.layout.fragment_gamepad, null);

        listView = mainView.findViewById(R.id.gamepad_listview);
        adapter = new ControlListAdapter(getActivity());
        listView.setAdapter(adapter);

        setListViewEnabled(TouchSettings.gamePadEnabled);

        //listView.setSelector(R.drawable.layout_sel_background);
        listView.setOnItemClickListener((arg0, v, pos, id) ->
                                        {
                                            // Make it so if you tap on it while monitoring it cancels it
                                            if (!config.isMonitoring())
                                                config.startMonitor(getActivity(), pos);
                                            else
                                                config.stopMonitor();

                                            adapter.notifyDataSetChanged();
                                        });

        listView.setOnItemLongClickListener((arg0, v, pos, id) -> config.showExtraOptions(getActivity(), pos));

        info = mainView.findViewById(R.id.gamepad_info_textview);
        finishedMonitoring();

        final ImageView menuButton = mainView.findViewById(R.id.menu_imageButton);
        menuButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                PopupMenu popup = new PopupMenu(getActivity(), v);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.gamepad_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
                {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem)
                    {
                        // RESET
                        if (menuItem.getItemId() == R.id.reset)
                        {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage("Reset controls to default settings?").setCancelable(true)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener()
                                    {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which)
                                        {
                                            config.reset();
                                            saveConfigFile(configFilename);
                                        }
                                    });

                            AlertDialog alert = builder.create();
                            alert.show();
                            // SAVE AS
                        }
                        else if (menuItem.getItemId() == R.id.save_as)
                        {
                            new GamepadSaveLoad(getActivity(), false)
                            {
                                public void selected(String file)
                                {
                                    log.log(D, "Save as: " + file);
                                    saveConfigFile(file);
                                }
                            };
                        }
                        else if (menuItem.getItemId() == R.id.load)
                        {
                            new GamepadSaveLoad(getActivity(), true)
                            {
                                public void selected(String file)
                                {
                                    log.log(D, "Load: " + file);
                                    loadConfigFile(file);
                                }
                            };
                        }

                        return false;
                    }
                });
                popup.show();
            }
        });

        String engineOverride = getActivity().getIntent().getStringExtra("engine_gamepad_config");
        engineOverrideActive = (engineOverride != null);
        String currentFile = engineOverrideActive ? engineOverride
                : AppSettings.getStringOption(getActivity(), "gamepad_config_filename", DEFAULT_CONFIG);
        loadConfigFile(currentFile);

        return mainView;
    }
    private void setListViewEnabled(boolean v)
    {
        listView.setEnabled(v);
        if (v)
        {
            listView.setAlpha(1);
        }
        else
        {
            listView.setAlpha(0.3f);
            //listView.setBackgroundColor(Color.GRAY);
        }
    }

    public boolean onGenericMotionEvent(MotionEvent event)
    {
        log.log(D, "onGenericMotionEvent: event = " + event.toString());

        if (config.onGenericMotionEvent(event))
            adapter.notifyDataSetChanged();

        return config.isMonitoring(); //This does not work, mouse appears anyway
        //return true; //If gamepas tab visible always steal
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        log.log(D, "onKeyDown: keyCode = " + keyCode + " event = " + event.toString());
        if (config.onKeyDown(keyCode, event))
        {
            adapter.notifyDataSetChanged();
            return true;
        }
        adapter.notifyDataSetChanged();
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        log.log(D, "onKeyUp: keyCode = " + keyCode + " event = " + event.toString());
        if (config.onKeyUp(keyCode, event))
        {
            adapter.notifyDataSetChanged();
            return true;
        }
        adapter.notifyDataSetChanged();
        return false;
    }

    @Override
    public void startMonitoring(ActionInput action)
    {
        if (action.actionType == ActionInput.ActionType.ANALOG)
            info.setText("Move Stick: " + action.description);
        else
            info.setText(action.description);

        info.setTextColor(getActivity().getResources().getColor(android.R.color.holo_green_light));
        //Make it flash
        Animation anim = new AlphaAnimation(0.2f, 1.0f);
        anim.setDuration(500); //You can manage the blinking time with this parameter
        anim.setStartOffset(20);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
        info.startAnimation(anim);
    }

    @Override
    public void finishedMonitoring()
    {
        info.setText("Config: " + configFilename);
        info.setTextColor(getActivity().getResources().getColor(android.R.color.holo_blue_light));
        info.clearAnimation();
        adapter.notifyDataSetChanged();
    }

    class ControlListAdapter extends BaseAdapter
    {
        private final Activity context;
        public ControlListAdapter(Activity context)
        {
            this.context = context;
        }
        public void add(String string)
        {

        }
        public int getCount()
        {
            return config.getSize();
        }
        public Object getItem(int arg0)
        {
            // TODO Auto-generated method stub
            return null;
        }
        public long getItemId(int arg0)
        {
            // TODO Auto-generated method stub
            return 0;
        }
        public View getView(int position, View convertView, ViewGroup list)
        {
            View v = config.getView(getActivity(), position);
            return v;
        }
    }
}
