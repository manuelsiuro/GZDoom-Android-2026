package org.libsdl.app;

import android.app.Activity;
import android.app.Dialog;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.opentouchgaming.androidcore.R;

import java.util.ArrayList;

/**
 * Created by Emile on 26/04/2018.
 */

public class NativeConsoleBox
{
    private static Activity activity;
    private static Dialog dialog;
    private static TextView logTextView;

    private static final int LINE_LEN = 64;

    private static ArrayList<String> lines = new ArrayList<>();
    private static byte[] currentLine = new byte[LINE_LEN];
    private static int currentLinePos = 0;

    public static void init(Activity a)
    {
        activity = a;
    }

    public static void openConsoleBox(final String title)
    {
        Log.d("NativeConsole", "Title = " + title);

        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                currentLinePos = 0;
                lines.clear();

                dialog = new Dialog(activity);
                dialog.setTitle(title);
                dialog.setCancelable(false);

                dialog.setContentView(R.layout.dialog_console_box);
                logTextView = dialog.findViewById(R.id.textView);

                logTextView.setMovementMethod(new ScrollingMovementMethod());
                logTextView.setText("");

                Button cancelButton = dialog.findViewById(R.id.cancel_button);
                cancelButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        dialog.dismiss();
                        dialog = null;
                        cancel();
                    }
                });
                dialog.show();
            }
        });
    }

    private static void insertLine()
    {
        String line = new String(currentLine, 0, currentLinePos);
        lines.add(line);
    }

    public static void addTextConsoleBox(final String text)
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (dialog != null)
                {
                    for( byte c: text.getBytes() )
                    {
                        if( c == '\r')
                            currentLinePos = 0;
                        else if (c == '\n')
                            insertLine();
                        if( currentLinePos < LINE_LEN)
                        {
                            currentLine[currentLinePos++] = c;
                        }
                    }

                    StringBuilder sb = new StringBuilder();
                    for( String line:  lines)
                    {
                        sb.append(line + "\n");
                    }

                    String cl = new String(currentLine, 0, currentLinePos);

                    logTextView.setText(sb.toString() + cl);
                }
            }
        });
    }

    public static void closeConsoleBox()
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (dialog != null)
                {
                    dialog.dismiss();
                    //dialog = null;
                }
            }
        });
    }

    public static native void cancel();

}
