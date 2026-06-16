package com.opentouchgaming.androidcore;

import android.content.Context;

public class FPSLimit
{
    int fps;
    long millisPerFrame;
    long lastTime = 0;

    public FPSLimit(Context ctx)
    {
        fps = AppSettings.getIntOption(ctx, "max_fps", 0);
        if (fps > 0)
            millisPerFrame = 1000 / fps;
    }

    public void tick()
    {
        if (fps > 0)
        {
            long timeNow = System.currentTimeMillis();

            long wait = millisPerFrame - (timeNow - lastTime);
            if (wait > 0)
            {
                try
                {
                    Thread.sleep(wait);
                }
                catch (InterruptedException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            lastTime = System.currentTimeMillis();
        }
    }
}
