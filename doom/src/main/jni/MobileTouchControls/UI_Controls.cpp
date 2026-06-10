//
// Created by Emile on 18/04/2017.
//
#include "UI_Controls.h"
#include "UI_Window.h"
#include "OpenGLUtils.h"

using namespace touchcontrols;

UI_Controls::UI_Controls(std::string t) :
        tag(t)
{
    yOffset = 0;
    scrolling = false;
    controlActive = NULL;
}

void UI_Controls::setEnabled(bool v)
{
    enabled = v;
    signalEnable(enabled);
}

bool UI_Controls::getEnabled()
{
    return enabled;
}


void UI_Controls::addControl(ControlSuper *cntrl)
{
    controls.push_back((cntrl));
}

void UI_Controls::deleteControls()
{
    int size = controls.size();

    for(int n = 0; n < size; n++)
    {
        delete (controls.at(n));
    }

    controls.clear();
    yOffset = 0;
}

void UI_Controls::scaleAllControls(float x, float y)
{
    int size = controls.size();

    for(int n = 0; n < size; n++)
    {
        controls.at(n)->scaleSize(x, y);
        controls.at(n)->updateSize();
    }
}

void UI_Controls::translateAllControls(float x, float y)
{
    int size = controls.size();

    for(int n = 0; n < size; n++)
    {
        controls.at(n)->positionTranslate(x, y);
        controls.at(n)->updateSize();
    }
}

void UI_Controls::offsetYOffset(float offset)
{
    float totalHeight = 0; //Find to limit scroll
    float windowHeight = 0;

    int size = controls.size();

    for(int n = 0; n < size; n++)
    {
        ControlSuper *cs = controls.at(n);

        if(cs->type == TC_TYPE_UI_WINDOW)
        {
            windowHeight = cs->controlPos.bottom - cs->controlPos.top;
        }

        if(cs->controlPos.bottom > totalHeight)
        {
            totalHeight = cs->controlPos.bottom;
        }
    }

    yOffset += offset;

    if(yOffset > (totalHeight - windowHeight))
    {
        yOffset = (totalHeight - windowHeight);
    }

    if(yOffset < 0)
        yOffset = 0;
}


bool UI_Controls::processPointer(int action, int pid, float x, float y)
{

    bool eventUsed = false;

    int size = controls.size();

    for(int n = 0; n < size; n++)
    {
        if(controlActive)
        {
            controlActive->processPointer(action, pid, x, y);
            eventUsed = true;
        }

        ControlSuper *cs = controls.at(size - 1 - n); // Start from top of stack

        if(cs->isEnabled() && !eventUsed)  // Now !eventUsed, only one control should use the event, to fix DropDown control
        {
            float ys;
            ys = (cs->type == TC_TYPE_UI_WINDOW) ? y : y + yOffset; //Dont apply scroll offset to window

            if(cs->processPointer(action, pid, x, ys))
            {
                eventUsed = true;
            }
        }
    }

    if(action == P_UP)
    {
        scrolling = false;
        flingMomentum = yMoveSpeed;
        LOGTOUCH("flingMomentum = %f", flingMomentum);
    }

    //if( !eventUsed ) // Finger not over a usable control
    // UPDATE, now scroll all the time
    {
        if(action == P_DOWN)
        {
            finger1.x = x;
            finger1.y = y;
            scrolling = true;
            flingMomentum = 0.0;

            // Used to calc speed
            yMoveSpeed = 0;
            lastMoveTime = getMS();
            lastYpos = y;
        }
        else if(action == P_MOVE) //Scroll controls
        {
            if(scrolling)
            {
                offsetYOffset(finger1.y - y);
                finger1.x = x;
                finger1.y = y;

                uint64_t timeNow = getMS();
                float duration = (timeNow - lastMoveTime) / 1000.0;

                // There might be very fast pointer updates, this slows down our calculations to at least 25ms
                if(duration > 0.025)
                {
                    // Speed = distance/ time
                    yMoveSpeed = (lastYpos - y) / duration;
                    //LOGTOUCH("yMoveSpeed = %f", yMoveSpeed);
                    lastMoveTime = getMS();
                    lastYpos = y;
                }
            }
        }
    }

    return true;
}

int UI_Controls::draw()
{
    if(fading)
    {
        if(fadeDir == FADE_IN)  //Fading in
        {
            fadePos += fadeStep;

            if(fadePos >= 1)
                fading = false;

        }
        else //Fading out
        {
            fadePos -= fadeStep;

            if(fadePos <= 0)
            {
                fading = false;
                setEnabled(false); //now also disable the control
            }
        }

        gl_color4f(1, 1, 1, fadePos);
    }
    else
        gl_color4f(1, 1, 1, 1);


    uint64_t timeNow = getMS();
    float frameDuration = (float) (timeNow - lastDrawTime) / 1000.0;

    float slowRate = frameDuration * 4;

    if(flingMomentum > 0)
    {
        if(flingMomentum > slowRate)
            flingMomentum -= slowRate;
        else
            flingMomentum = 0.0;
    }
    else if(flingMomentum < 0)
    {
        if(flingMomentum < -slowRate)
            flingMomentum += slowRate;
        else
            flingMomentum = 0.0;
    }


    offsetYOffset(flingMomentum * frameDuration);

    int size = controls.size();
    ControlSuper *controlActive_temp = NULL; // Use temp so reduce threading issues

    for(int n = 0; n < size; n++) //draw
    {
        ControlSuper *c = controls.at(n);

        if(c->isEnabled())
        {
            gl_loadIdentity();

            gl_scalef(GLScaleWidth, GLScaleHeight, 1);

            // Scroll, but don't scroll the background window
            if(c->type != TC_TYPE_UI_WINDOW)
            {
                gl_translatef(0, yOffset, 0);
            }

            // Draw the controls, also check if it wants to show somethign over the screen, if it does, store it here.
            if(c->UI_drawGL(false))     // drawGL really gets called
            {
                controlActive_temp = c;
            }
        }
    }

    // Save current active control for the touch control to work
    controlActive = controlActive_temp;

    if(controlActive)
    {
        // Reset this so it's in the middle and not scrolled
        gl_loadIdentity();
        gl_scalef(GLScaleWidth, GLScaleHeight, 1);

        // Grey out UI controls under
        GLRect rect;
        rect.resize(1, 1);
        gl_drawRect(0.0, 0.0, 0.0, 0.7, 0.0, 0.0, rect);

        // Draw it (active = true)
        controlActive->UI_drawGL(true);   // drawGL really gets called
    }

    // The UI window enables this
    gl_disable(GL_SCISSOR_TEST);

    lastDrawTime = getMS();

    return 0;
}

void UI_Controls::initGL()
{
    int size = controls.size();

    for(int n = 0; n < size; n++) //draw
    {
        ControlSuper *c = controls.at(n);
        c->initGL();
    }
}

void UI_Controls::setAlpha(float a)
{

}

void UI_Controls::fade(fadedir_t dir, int steps)
{
    if(dir == FADE_IN)  //Fade in
    {
        fadePos = 0;
    }
    else //Fade out
    {
        fadePos = 1;
    }

    fadeDir = dir;
    fadeStep = (float) 1 / (float) steps;
    fading = true;
}

void *UI_Controls::getControl(std::string name)
{
    for(int n = 0; n < controls.size(); n++) //draw
    {
        ControlSuper *c = controls.at(n);

        if(c->tag == name)
            return c;
    }

    return NULL;
}