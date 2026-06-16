package org.libsdl.app;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * In-game native console output dialog. The native side (libtouchcontrols'
 * JNITouchControlsUtils.cpp) calls openConsoleBox/addTextConsoleBox/
 * closeConsoleBox by reflection on this exact class name. This version builds
 * its views programmatically so it has no resource dependencies on
 * AndroidCore (the original inflated com.opentouchgaming.androidcore
 * R.layout.dialog_console_box).
 */
public class NativeConsoleBox
{
    private static Activity activity;
    private static Dialog dialog;
    private static TextView logTextView;

    private static final int LINE_LEN = 64;

    private static final ArrayList<String> lines = new ArrayList<>();
    private static final byte[] currentLine = new byte[LINE_LEN];
    private static int currentLinePos = 0;

    public static void init(Activity a)
    {
        activity = a;
    }

    public static void openConsoleBox(final String title)
    {
        Log.d("NativeConsole", "Title = " + title);

        if (activity == null)
            return;

        activity.runOnUiThread(() -> {
            currentLinePos = 0;
            lines.clear();

            dialog = new Dialog(activity);
            dialog.setTitle(title);
            dialog.setCancelable(false);

            LinearLayout layout = new LinearLayout(activity);
            layout.setOrientation(LinearLayout.VERTICAL);
            int pad = (int) (8 * activity.getResources().getDisplayMetrics().density);
            layout.setPadding(pad, pad, pad, pad);

            logTextView = new TextView(activity);
            logTextView.setTypeface(Typeface.MONOSPACE);
            logTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            logTextView.setTextColor(Color.LTGRAY);
            logTextView.setBackgroundColor(Color.BLACK);
            logTextView.setMovementMethod(new ScrollingMovementMethod());
            logTextView.setText("");
            layout.addView(logTextView, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

            Button cancelButton = new Button(activity);
            cancelButton.setText(android.R.string.cancel);
            cancelButton.setOnClickListener(v -> {
                dialog.dismiss();
                dialog = null;
                cancel();
            });
            layout.addView(cancelButton, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            dialog.setContentView(layout, new LinearLayout.LayoutParams(
                    (int) (320 * activity.getResources().getDisplayMetrics().density),
                    (int) (240 * activity.getResources().getDisplayMetrics().density)));
            dialog.show();
        });
    }

    private static void insertLine()
    {
        String line = new String(currentLine, 0, currentLinePos);
        lines.add(line);
    }

    public static void addTextConsoleBox(final String text)
    {
        if (activity == null)
            return;

        activity.runOnUiThread(() -> {
            if (dialog != null)
            {
                for (byte c : text.getBytes())
                {
                    if (c == '\r')
                        currentLinePos = 0;
                    else if (c == '\n')
                        insertLine();
                    if (currentLinePos < LINE_LEN)
                    {
                        currentLine[currentLinePos++] = c;
                    }
                }

                StringBuilder sb = new StringBuilder();
                for (String line : lines)
                {
                    sb.append(line).append("\n");
                }

                String cl = new String(currentLine, 0, currentLinePos);

                logTextView.setText(sb + cl);
            }
        });
    }

    public static void closeConsoleBox()
    {
        if (activity == null)
            return;

        activity.runOnUiThread(() -> {
            if (dialog != null)
            {
                dialog.dismiss();
            }
        });
    }

    public static native void cancel();
}
