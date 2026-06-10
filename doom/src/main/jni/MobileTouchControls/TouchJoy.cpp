#include "TouchJoy.h"
#include "TouchControlsConfig.h"
#include <sys/time.h>
#include "math.h"

#define DOUBLE_TAP_SPEED 200

using namespace touchcontrols;

TouchJoy::TouchJoy(std::string tag, RectF pos, std::string floating_image, std::string background_image) :
        ControlSuper(TC_TYPE_TOUCHJOY, tag, pos)
{
    this->floating_image = floating_image;
    this->background_image = background_image;

    pid = -1;
    doubleTapState = 0;
    hideGraphics = false;
    centerAnchor = false;
    updateSize();
    glitchFix = 0;
    otherTouchJoySWAPFIX = NULL;
};

void TouchJoy::registerTouchJoySWAPFIX(TouchJoy *other)
{
    this->otherTouchJoySWAPFIX = other;
}


void TouchJoy::setHideGraphics(bool v)
{
    hideGraphics = v;
    //LOGTOUCH("setHideGraphics enabled = %d",enabled);
}

void TouchJoy::setCenterAnchor(bool v)
{
    centerAnchor = v;
}

void TouchJoy::resetOutput()
{
    reset();
}

void TouchJoy::updateSize()
{
    //glRect.resize(controlPos.right - controlPos.left, controlPos.bottom - controlPos.top);
    glRect.resize(1 * 0.1, 1.6 * 0.1);
}


bool TouchJoy::processPointer(int action, int tpid, float x, float y)
{
    if(action == P_DOWN)
    {
        if((g_touchJoyMultiTouch == false) || pid == -1)  // default (off): always accept; multitouch (on): only if inactive
        {
            if(controlPos.contains(x, y))
            {
                pid = tpid;

                glitchFix = 1; //Fix jumpy initial move

                last.x = x;
                last.y = y;

                if(centerAnchor)
                {
                    anchor.x = controlPos.left + (controlPos.width() / 2);
                    anchor.y = controlPos.top + (controlPos.height() / 2);
                    glitchFix = 0; // Dont do this
                }
                else
                {
                    anchor.x = x;
                    anchor.y = y;
                }

                fingerPos.x = x;
                fingerPos.y = y;

                if(doubleTapState == 0)  //First tap of double tap
                {
                    doubleTapState = 1;
                    doubleTapCounter = getMS();
                }
                else if(doubleTapState == 2) //Second down of double tap
                {
                    if((getMS() - doubleTapCounter) < DOUBLE_TAP_SPEED)
                    {
                        signal_double_tap.emit(1);
                        doubleTapState = 3;
                    }
                    else
                        doubleTapState = 0;
                }

                calcNewValue();

                return true;
            }
        }

        return false;
    }
    else if(action == P_UP)
    {
        if(pid == tpid)
        {
            if(doubleTapState == 1)  //First up of double tap
            {
                //Simple check to see if finger moved very much
                if(((getMS() - doubleTapCounter) < DOUBLE_TAP_SPEED) &&
                   (((abs(anchor.x - fingerPos.x) + abs(anchor.y - fingerPos.y))) < 0.05))
                {
                    doubleTapState = 2;
                    doubleTapCounter = getMS();
                }
                else
                    doubleTapState = 0;
            }
            else if(doubleTapState == 3)  //Finger up, finished double tap
            {
                signal_double_tap.emit(0);
                doubleTapState = 0;
                doubleTapCounter = 0;
            }

            reset();
            return true;
        }

        return false;
    }
    else if(action == P_MOVE)
    {
        if(pid == tpid)  //Finger already down
        {
            if((otherTouchJoySWAPFIX->pid == tpid) && (!controlPos.contains(x, y)))  // If the other control has got the point and not in control area, something FU
            {
                pid = -1; // Just stop
            }

            if(glitchFix)  //Need to wait until the values have changed at least once, otherwise inital jump
            {
                if((last.x != x) || (last.y != y))
                {
                    last.x = x;
                    last.y = y;
                    anchor.x = x;
                    anchor.y = y;
                    fingerPos.x = x;
                    fingerPos.y = y;
                    glitchFix--;
                }
            }

            if(!glitchFix)
            {
                fingerPos.x = x;
                fingerPos.y = y;
                calcNewValue();
            }

            return true;
        }
        else
        {
            if(otherTouchJoySWAPFIX != NULL)   //SWAPFIX
            {
                if(otherTouchJoySWAPFIX->pid == tpid)   // Other one has finger down, this is it
                {
                    if(controlPos.contains(x, y))  // UH OH! This probably means touch has swapped fingers
                    {
                        LOGTOUCH("SWAPFIX");
                        otherTouchJoySWAPFIX->processPointer(P_UP, tpid, x, y);   // Force finger up on the other one
                        //processPointer( P_DOWN, tpid, x, y ); // ALSO give the pointer to this one. Acctually don't do this, confusing movment otherwise
                        // Changed, actually DO take PID, but filter first few samples
                        pid = tpid;
                        glitchFix = 3; // Still swapping, so try to ignore the next few

                        return true;
                    }
                }
            }
        }

        return false;
    }

    return false;
}

bool TouchJoy::initGL()
{
    int x, y;
    glTex = loadTextureFromPNG(floating_image, x, y);
    glTexBackground = loadTextureFromPNG(background_image, x, y);
    return false;
}

bool TouchJoy::drawGL(bool forEditor)
{

    if(!enabled)
        return false;

    if(!hideGraphics)
    {
        if(centerAnchor) // If fixed joystick, show the background
        {
            GLRect bgRect;
            float width, height;

            // In case not square, size to fit
            if(controlPos.width() * 1.6 < controlPos.height()) // tall and thin
            {
                width = controlPos.width();
                height = width * 1.6;
            }
            else
            {
                width = controlPos.height() / 1.6;
                height = width * 1.6;
            }

            bgRect.resize(width, height);
            // draw centred
            gl_drawRect(glTexBackground, controlPos.left + (controlPos.width() - width) / 2, controlPos.top + (controlPos.height() - height) / 2, bgRect);
        }
        else
        {
            if(pid != -1)
                gl_drawRect(glTex, fingerPos.x - glRect.width / 2, fingerPos.y - glRect.height / 2, glRect);
            else
                gl_drawRect(glTex, controlPos.left + controlPos.width() / 2 - glRect.width / 2, controlPos.top + controlPos.height() / 2 - glRect.height / 2, glRect);
        }
    }

    //LOGTOUCH("state = %d, counter = %d",doubleTapState,doubleTapCounter);

    return false;
}

void TouchJoy::reset()
{
    pid = -1;
    valueTouch.x = 0;
    valueTouch.y = 0;
    valueJoy.x = 0;
    valueJoy.y = 0;

    doUpdate();
}

void TouchJoy::calcNewValue()
{
    float dx = last.x - fingerPos.x;
    float dy = last.y - fingerPos.y;

    //LOGTOUCH("%s %f  %f   %f %f",tag.c_str(),fingerPos.x,fingerPos.y,dx,dy);
    // SWAPFIX. Filter out very fast movments when a swap occurs
    if((abs(dx) > 0.09) || (abs(dy) > 0.09))
    {
        dx = 0;
        dy = 0;
    }

    valueTouch.x = dx;
    valueTouch.y = dy;

    last.x = fingerPos.x;
    last.y = fingerPos.y;

    dx = anchor.x - fingerPos.x;
    dy = anchor.y - fingerPos.y;

    valueJoy.x = dx;
    valueJoy.y = dy;

    if(valueJoy.x > 1)
        valueJoy.x = 1;
    else if(valueJoy.x < -1)
        valueJoy.x = -1;

    if(valueJoy.y > 1)
        valueJoy.y = 1;
    else if(valueJoy.y < -1)
        valueJoy.y = -1;

    doUpdate();
}

void TouchJoy::doUpdate()
{
    //LOGTOUCH("xT = %f yT = %f,xJ = %f yJ = %f",valueTouch.x,valueTouch.y,valueJoy.x ,valueJoy.y);
    signal_move.emit(valueJoy.x, valueJoy.y, valueTouch.x, valueTouch.y);
}

void TouchJoy::saveXML(TiXmlDocument &doc)
{
    TiXmlElement *root = new TiXmlElement(tag.c_str());
    doc.LinkEndChild(root);

    ControlSuper::saveXML(*root);
}

void TouchJoy::loadXML(TiXmlDocument &doc)
{
    TiXmlHandle hDoc(&doc);
    TiXmlElement *pElem = hDoc.FirstChild(tag).Element();

    if(!pElem)  //Check exists, if not just leave as default
        return;

    ControlSuper::loadXML(*pElem);
}
