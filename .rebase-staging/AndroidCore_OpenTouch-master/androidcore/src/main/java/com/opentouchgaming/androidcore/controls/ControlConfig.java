package com.opentouchgaming.androidcore.controls;

import static com.opentouchgaming.androidcore.DebugLog.Level.D;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.opentouchgaming.androidcore.AppInfo;
import com.opentouchgaming.androidcore.DebugLog;
import com.opentouchgaming.androidcore.R;
import com.opentouchgaming.saffal.FileSAF;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

public class ControlConfig implements Serializable
{
    public static final int LOOK_MODE_MOUSE = 0;
    public static final int LOOK_MODE_ABSOLUTE = 1;
    public static final int LOOK_MODE_JOYSTICK = 2;
    public static final int LOOK_MODE_GYRO = 3;
    static final String CONFIG_EXT = ".padconfig";

    private static final long serialVersionUID = 1L;
    static DebugLog log;

    static
    {
        log = new DebugLog(DebugLog.Module.CONTROLS, "ControlConfig");
    }

    String configFilename;
    ActionInputDefinition gamepadDefinition;
    ArrayList<ActionInput> actions = new ArrayList<ActionInput>();
    ActionInput actionMonitor = null;
    boolean monitoring = false;
    boolean gotInput = false;
    Listener listener;
    int[] axisTest = {
            /*
            MotionEvent.AXIS_GENERIC_1,
			MotionEvent.AXIS_GENERIC_2,
			MotionEvent.AXIS_GENERIC_3,
			MotionEvent.AXIS_GENERIC_4,
			MotionEvent.AXIS_GENERIC_5,
			MotionEvent.AXIS_GENERIC_6,
			MotionEvent.AXIS_GENERIC_7,
			MotionEvent.AXIS_GENERIC_8,
			MotionEvent.AXIS_GENERIC_9,
			MotionEvent.AXIS_GENERIC_10,
			MotionEvent.AXIS_GENERIC_11,
			MotionEvent.AXIS_GENERIC_12,
			MotionEvent.AXIS_GENERIC_13,
			MotionEvent.AXIS_GENERIC_14,
			MotionEvent.AXIS_GENERIC_15,
			MotionEvent.AXIS_GENERIC_16,
			 */

            MotionEvent.AXIS_HAT_X, MotionEvent.AXIS_HAT_Y, MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_RTRIGGER, MotionEvent.AXIS_RUDDER, MotionEvent.AXIS_RX,
            MotionEvent.AXIS_RY, MotionEvent.AXIS_RZ, MotionEvent.AXIS_THROTTLE, MotionEvent.AXIS_X, MotionEvent.AXIS_Y, MotionEvent.AXIS_Z,
            MotionEvent.AXIS_BRAKE, MotionEvent.AXIS_GAS,};

    public ControlConfig(ActionInputDefinition gamepadDefinition, Listener listener)
    {
        this.gamepadDefinition = gamepadDefinition;
        reset();
        this.listener = listener;
    }

    void reset()
    {
        actions.clear();

        Iterator<ActionInput> iterator = gamepadDefinition.actions.iterator();
        while (iterator.hasNext())
        {
            actions.add(iterator.next().clone());
        }
    }

    void saveControls(String filename) throws IOException
    {
        log.log(D, "saveControls, file = " + filename);

        configFilename = filename;

        FileSAF file = new FileSAF(AppInfo.getGamepadDirectory() + "/" + configFilename);
        file.createNewFile();

        ObjectOutputStream out = new ObjectOutputStream(file.getOutputStream());

        out.writeObject(actions);
        out.close();
    }

    public void loadControls(String filename) throws IOException, ClassNotFoundException
    {
        log.log(D, "loadControls, file = " + filename);

        configFilename = filename;

        FileSAF file = new FileSAF(AppInfo.getGamepadDirectory() + "/" + configFilename);

        ObjectInputStream in = new ObjectInputStream(file.getInputStream());

        ArrayList<ActionInput> cd = (ArrayList<ActionInput>) in.readObject();

        log.log(D, "loadControls, file loaded OK");

        in.close();

        for (ActionInput d : cd)
        {
            for (ActionInput a : actions)
            {
                if (d.tag != null && a.tag != null && d.tag.contentEquals(a.tag))
                {
                    a.invert = d.invert;
                    a.source = d.source;
                    a.sourceType = d.sourceType;
                    a.sourcePositive = d.sourcePositive;
                    a.scale = d.scale;
                    a.deadZone = d.deadZone;
                    if (a.scale == 0)
                        a.scale = 1;
                }
            }
        }

        //Now check no buttons are also assigned to analog, if it is, clear the buttons
        //This is because n00bs keep assigning movment analog AND buttons!
        for (ActionInput a : actions)
        {
            if ((a.source != -1) && (a.sourceType == ActionInput.SourceType.AXIS) && (a.actionType == ActionInput.ActionType.BUTTON))
            {
                for (ActionInput a_check : actions)
                {
                    if ((a_check.sourceType == ActionInput.SourceType.AXIS) && (a_check.actionType == ActionInput.ActionType.ANALOG))
                    {
                        if (a.source == a_check.source)
                        {
                            a.source = -1;
                            break;
                        }
                    }
                }
            }
        }

        in.close();
    }


    void updated()
    {
        log.log(D, "updated");

        try
        {
            saveControls(configFilename);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public boolean showExtraOptions(Activity act, int pos)
    {
        final ActionInput in = actions.get(pos);

        if (in.extraDialog != null)
        {
            in.extraDialog.show(act, in, this::updated);
            return true;
        }
        else
            return false;
    }

    public void startMonitor(Activity act, int pos)
    {
        actionMonitor = actions.get(pos);

        // Don't set the headers!
        if (actionMonitor.tag == null)
            return;

        monitoring = true;
        gotInput = false;
        if (listener != null)
            listener.startMonitoring(actionMonitor);
    }

    public void stopMonitor()
    {
        monitoring = false;
        if (listener != null)
            listener.finishedMonitoring();
    }

    public boolean onGenericMotionEvent(MotionEvent event)
    {
        log.log(D, "onGenericMotionEvent");

        if (monitoring)
        {
            if (actionMonitor != null && !gotInput)
            {
                for (int a : axisTest)
                {
                    if (Math.abs(event.getAxisValue(a)) > 0.6)
                    {
                        actionMonitor.source = a;
                        actionMonitor.sourceType = ActionInput.SourceType.AXIS;
                        //Used for button actions
                        actionMonitor.sourcePositive = event.getAxisValue(a) > 0;

                        //monitoring = false;
                        gotInput = true;

                        log.log(D, actionMonitor.description + " = Analog (" + actionMonitor.source + ")");

                        updated();
                        return true;
                    }
                }
            }
            else // Keep monitoring until all the axis are back in the centre
            {
                boolean allCentre = true;
                for (int a : axisTest)
                {
                    if (Math.abs(event.getAxisValue(a)) > 0.2)
                    {
                        allCentre = false;
                    }
                }
                if (allCentre)
                {
                    stopMonitor();
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isMonitoring()
    {
        return monitoring;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        log.log(D, "onKeyDown " + keyCode);

        if (monitoring)
        {
            if (keyCode == KeyEvent.KEYCODE_BACK) //Cancel and clear button assignment
            {
                actionMonitor.source = -1;
                actionMonitor.sourceType = ActionInput.SourceType.BUTTON;

                stopMonitor();
                updated();
                return true;
            }
            else
            {
                if (actionMonitor != null)
                {
                    if (actionMonitor.actionType != ActionInput.ActionType.ANALOG)
                    {
                        actionMonitor.source = keyCode;
                        actionMonitor.sourceType = ActionInput.SourceType.BUTTON;

                        stopMonitor();
                        updated();
                        return true;
                    }
                }
            }
            return true;
        }

        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        return monitoring;
    }

    public int getSize()
    {
        return actions.size();
    }

    public View getView(final Activity ctx, final int nbr)
    {
        ActionInput ai = actions.get(nbr);

        if (ai.tag != null) // If tag is null it's a header, otherwise normal item
        {
            View view = ctx.getLayoutInflater().inflate(R.layout.controls_listview_item, null);
            ImageView image = view.findViewById(R.id.imageView);
            TextView name = view.findViewById(R.id.name_textview);
            TextView binding = view.findViewById(R.id.binding_textview);
            ImageView setting_image = view.findViewById(R.id.settings_imageview);


            if ((ai.actionType == ActionInput.ActionType.BUTTON) || (ai.actionType == ActionInput.ActionType.MENU))
            {
                if ((ai.actionType == ActionInput.ActionType.MENU))
                {
                    name.setTextColor(0xFF00aeef); //BLUEY
                    image.setImageResource(R.drawable.gamepad_menu);
                }
                else
                {
                    name.setTextColor(0xFF02ad2a); //GREEN
                    image.setImageResource(R.drawable.gamepad);
                }
            }
            else if (ai.actionType == ActionInput.ActionType.ANALOG)
            {
                binding.setText(MotionEvent.axisToString(ai.source));
                name.setTextColor(0xFFf7941d); //ORANGE
            }

            if ((ai.actionType == ActionInput.ActionType.ANALOG) || (ai.extraDialog != null))
            {
                setting_image.setVisibility(View.VISIBLE);
                setting_image.setOnClickListener(new OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        showExtraOptions(ctx, nbr);
                    }
                });
            }
            else
                setting_image.setVisibility(View.GONE);

            // If monitoring, override the settings button so it can clear the action
            if (monitoring && (ai == actionMonitor))
            {
                setting_image.setImageResource(R.drawable.setting_trash);
                setting_image.setVisibility(View.VISIBLE);
                setting_image.setOnClickListener(v ->
                                                 {

                                                     actionMonitor.source = -1;
                                                     actionMonitor.sourceType = ActionInput.SourceType.BUTTON;

                                                     stopMonitor();
                                                     updated();
                                                 });
            }

            if (ai.source == -1)
            {
                binding.setText("not set");
            }
            else
            {
                if (ai.sourceType == ActionInput.SourceType.AXIS)
                    binding.setText(MotionEvent.axisToString(ai.source));
                else
                    binding.setText(KeyEvent.keyCodeToString(ai.source));
            }

            if (actionMonitor != null && actionMonitor == ai && monitoring)
            {
                view.setBackgroundResource(R.drawable.layout_sel_background);
            }
            else
            {
                view.setBackgroundResource(0);
            }

            name.setText(ai.description);

            return view;
        }
        else
        {
            View view = ctx.getLayoutInflater().inflate(R.layout.controls_listview_header_item, null);
            TextView title = view.findViewById(R.id.title_textview);
            title.setText(ai.description);
            return view;
        }
    }

    public interface Listener
    {
        void startMonitoring(ActionInput action);

        void finishedMonitoring();
    }
}
