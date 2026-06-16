package com.opentouchgaming.androidcore;

import android.content.Context;
import android.content.SharedPreferences;

import com.opentouchgaming.androidcore.controls.TouchSettings;

import java.util.Set;

public class AppSettings
{
    public static void reloadSettings(Context ctx)
    {
        TouchSettings.reloadSettings(ctx);
    }

    public static float getFloatOption(Context ctx, String name, float def)
    {
        SharedPreferences settings = ctx.getSharedPreferences("OPTIONS", Context.MODE_MULTI_PROCESS);
        return settings.getFloat(name, def);
    }

    public static void setFloatOption(Context ctx, String name, float value)
    {
        SharedPreferences settings = ctx.getSharedPreferences("OPTIONS", Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat(name, value);
        editor.commit();
    }

    public static boolean getBoolOption(Context ctx, String name, boolean def)
    {
        SharedPreferences settings = ctx.getSharedPreferences("OPTIONS", Context.MODE_MULTI_PROCESS);
        return settings.getBoolean(name, def);
    }

    public static void setBoolOption(Context ctx, String name, boolean value)
    {
        SharedPreferences settings = ctx.getSharedPreferences("OPTIONS", Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(name, value);
        editor.commit();
    }

    public static int getIntOption(Context ctx, String name, int def)
    {
        SharedPreferences settings = ctx.getSharedPreferences("OPTIONS", Context.MODE_MULTI_PROCESS);
        return settings.getInt(name, def);
    }

    public static void setIntOption(Context ctx, String name, int value)
    {
        SharedPreferences settings = ctx.getSharedPreferences("OPTIONS", Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(name, value);
        editor.commit();
    }

    public static long getLongOption(Context ctx, String name, long def)
    {
        SharedPreferences settings = ctx.getSharedPreferences("OPTIONS", Context.MODE_MULTI_PROCESS);
        return settings.getLong(name, def);
    }

    public static void setLongOption(Context ctx, String name, long value)
    {
        SharedPreferences settings = ctx.getSharedPreferences("OPTIONS", Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(name, value);
        editor.commit();
    }

    public static String getStringOption(Context ctx, String name, String def)
    {
        SharedPreferences settings = ctx.getSharedPreferences("OPTIONS", Context.MODE_MULTI_PROCESS);
        return settings.getString(name, def);
    }

    public static void setStringOption(Context ctx, String name, String value)
    {
        SharedPreferences settings = ctx.getSharedPreferences("OPTIONS", Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(name, value);
        editor.commit();
    }

    public static Set<String> getStringSetOption(Context ctx, String name, String def)
    {
        SharedPreferences settings = ctx.getSharedPreferences("OPTIONS", Context.MODE_MULTI_PROCESS);
        return settings.getStringSet(name, null);
    }

    public static void setStringSetOption(Context ctx, String name, Set<String> value)
    {
        SharedPreferences settings = ctx.getSharedPreferences("OPTIONS", Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = settings.edit();
        editor.putStringSet(name, value);
        editor.commit();
    }

    public static void deleteAllOptions(Context ctx)
    {
        SharedPreferences settings = ctx.getSharedPreferences("OPTIONS", Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.commit();
    }
}
