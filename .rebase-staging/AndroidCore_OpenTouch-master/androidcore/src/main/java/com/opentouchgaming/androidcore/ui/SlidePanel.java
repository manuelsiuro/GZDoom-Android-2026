package com.opentouchgaming.androidcore.ui;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.RelativeLayout;

/**
 * Created by Emile on 09/07/2017.
 */

public class SlidePanel
{

    final int distance;
    final View view;
    SlideSide side;
    boolean isOpen = false;
    ValueAnimator valueAnimator;
    AnimatorSet animatorSet;

    SlidePanel(final View view, final SlideSide side, final int distance, final int duration)
    {
        this.distance = distance;
        this.view = view;
        this.side = side;
        valueAnimator = ValueAnimator.ofInt(0, distance).setDuration(duration);

        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                Integer value = (Integer) animation.getAnimatedValue();
                RelativeLayout.LayoutParams Params = (RelativeLayout.LayoutParams) view.getLayoutParams();

                switch (side)
                {
                    case LEFT:
                    {
                        // Opening
                        if (isOpen)
                        {
                            Params.leftMargin = value.intValue() - distance;
                        }
                        else //Closing left panel
                        {
                            Params.leftMargin = -value.intValue();
                        }
                        break;
                    }
                    case RIGHT:
                    {
                        // Opening
                        if (isOpen)
                        {
                            Params.rightMargin = value.intValue() - distance;
                        }
                        else //Closing left panel
                        {
                            Params.rightMargin = -value.intValue();
                        }
                        break;
                    }
                    case BOTTOM:
                    {
                        // Opening
                        if (isOpen)
                        {
                            Params.bottomMargin = value.intValue() - distance;
                        }
                        else //Closing left panel
                        {
                            Params.bottomMargin = -value.intValue();
                        }
                    }
                }


                view.setLayoutParams(Params);
                view.requestLayout();
            }
        });

        // Call this to make it closed by default
        valueAnimator.end();

        animatorSet = new AnimatorSet();

        animatorSet.play(valueAnimator);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());

    }

    void open()
    {
        isOpen = true;
        animatorSet.start();
    }

    void close()
    {
        isOpen = false;
        animatorSet.start();
    }

    void closeIfOpen()
    {
        if (isOpen)
        {
            isOpen = false;
            animatorSet.start();
        }
    }

    void toggle()
    {
        isOpen = !isOpen;
        animatorSet.start();
    }

    boolean isOpen()
    {
        return isOpen;
    }

    enum SlideSide
    {
        LEFT, RIGHT, TOP, BOTTOM
    }

}
