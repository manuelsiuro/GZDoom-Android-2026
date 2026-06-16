#include "Mouse.h"
#include "TouchControlsConfig.h"
#include <sys/time.h>
#include "math.h"

using namespace touchcontrols;

Mouse::Mouse(std::string tag, RectF pos, std::string image_filename) :
        ControlSuper(TC_TYPE_MOUSE, tag, pos)
{
    image = image_filename;
    id = -1;
    id2 = -1;

    hideGraphics = false;
    updateSize();
    glitchFix = 0;
};

void Mouse::setHideGraphics(bool v)
{
    hideGraphics = v;
}

void Mouse::resetOutput()
{
    reset();
}

void Mouse::updateSize()
{
    glRect.resize(controlPos.width(), controlPos.height());
}

void Mouse::emit(int action, float x, float y, float dx, float dy)
{
    x = x - controlPos.left;
    x = x * (1 / controlPos.width());

    if(x < 0)
        x = 0;
    else if(x > 1)
        x = 1;

    y = y - controlPos.top;
    y = y * (1 / controlPos.height());

    if(y < 0)
        y = 0;
    else if(y > 1)
        y = 1;

    signal_action.emit(action, x, y, dx, dy);
}

bool Mouse::processPointer(int action, int pid, float x, float y)
{
    if(action == P_DOWN)
    {
        if(id == -1)  //Only process if not active
        {
            if(controlPos.contains(x, y))
            {
                id = pid;

                glitchFix = 2; //Fix jumpy initial move

                last.x = x;
                last.y = y;
                anchor.x = x;
                anchor.y = y;
                fingerPos.x = x;
                fingerPos.y = y;

                tapDetect.reset();
                tapDetect.processPointer(action, pid, x, y);

                emit(TOUCHMOUSE_DOWN, fingerPos.x, fingerPos.y, 0, 0);
                return true;
            }
        }
        else //second finger down
        {
            if(controlPos.contains(x, y))
            {
                id2 = pid;
                emit(TOUCHMOUSE_UP, fingerPos.x, fingerPos.y, 0, 0); //This also up?...
                emit(TOUCHMOUSE_2_DOWN, fingerPos.x, fingerPos.y, 0, 0);
            }
        }

        return false;
    }
    else if(action == P_UP)
    {
        tapDetect.processPointer(action, pid, x, y);

        if(id == pid)
        {
            if(tapDetect.didTap())
            {
                emit(TOUCHMOUSE_TAP, fingerPos.x, fingerPos.y, 0, 0);
            }

            emit(TOUCHMOUSE_UP, fingerPos.x, fingerPos.y, 0, 0);
            emit(TOUCHMOUSE_2_UP, fingerPos.x, fingerPos.y, 0, 0);
            reset();
            return true;
        }

        return false;
    }
    else if(action == P_MOVE)
    {
        tapDetect.processPointer(action, pid, x, y);

        if(pid == id)  //Finger already down
        {
            if(glitchFix)  //Need to wait untill the values have changed at least once, otherwise inital jump
            {
                //LOGTOUCH("glitchFix %d",glitchFix);
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

        return false;
    }

    return false;
}

bool Mouse::initGL()
{
    int x, y;
    glTex = loadTextureFromPNG(image, x, y);

    return false;
}

bool Mouse::drawGL(bool editor)
{
    bool fa = gl_getFixAspect();
    gl_setFixAspect(false);   // Fill the mouse screen with the graphic

    gl_drawRect(glTex, controlPos.left, controlPos.top, glRect);

    gl_setFixAspect(fa);

    return false;
}

void Mouse::reset()
{
    if(id != -1)
    {
        emit(TOUCHMOUSE_UP, fingerPos.x, fingerPos.y, 0, 0);
    }

    if(id2 != -1)
    {
        emit(TOUCHMOUSE_2_UP, fingerPos.x, fingerPos.y, 0, 0);
    }

    id = -1;
    id2 = -1;
    valueRel.x = 0;
    valueRel.y = 0;
}

void Mouse::calcNewValue()
{
    float dx = last.x - fingerPos.x;
    float dy = last.y - fingerPos.y;
    valueRel.x = dx;
    valueRel.y = dy;
    last.x = fingerPos.x;
    last.y = fingerPos.y;

    doUpdate();
}

void Mouse::doUpdate()
{
    emit(TOUCHMOUSE_MOVE, fingerPos.x, fingerPos.y, valueRel.x, valueRel.y);
}

void Mouse::saveXML(TiXmlDocument &doc)
{
    TiXmlElement *root = new TiXmlElement(tag.c_str());
    doc.LinkEndChild(root);

    ControlSuper::saveXML(*root);
}

void Mouse::loadXML(TiXmlDocument &doc)
{
    TiXmlHandle hDoc(&doc);
    TiXmlElement *pElem = hDoc.FirstChild(tag).Element();

    if(!pElem)  //Check exists, if not just leave as default
        return;

    ControlSuper::loadXML(*pElem);
}
