#include "MultitouchMouse.h"
#include "TouchControlsConfig.h"
#include <math.h>

using namespace touchcontrols;

MultitouchMouse::MultitouchMouse(std::string tag, RectF pos, std::string image_filename) :
        ControlSuper(TC_TYPE_MULTITOUCHMOUSE, tag, pos)
{
    image = image_filename;
    id = -1;
    id2 = -1;
    glLines = new GLLines(2);
    hideGraphics = false;
    updateSize();
};

void MultitouchMouse::setHideGraphics(bool v)
{
    hideGraphics = v;
}

void MultitouchMouse::resetOutput()
{
    reset();
}

void MultitouchMouse::updateSize()
{
    glRect.resize(0.1, 0.16);

    glLines->vertices[0] = controlPos.left;
    glLines->vertices[1] = -controlPos.top;
    glLines->vertices[3] = controlPos.right;
    glLines->vertices[4] = -controlPos.top;


    glLines->vertices[6] = controlPos.right;
    glLines->vertices[7] = -controlPos.top;
    glLines->vertices[9] = controlPos.right;
    glLines->vertices[10] = -controlPos.bottom;
}

float MultitouchMouse::distancePoints(PointF p1, PointF p2)
{
    float dist = ((p1.x - p2.x) * (p1.x - p2.x)) + ((p1.y - p2.y) * (p1.y - p2.y));
    dist = sqrt(dist);
    return dist;
}

bool MultitouchMouse::processPointer(int action, int pid, float x, float y)
{
    if(action == P_DOWN)
    {
        if(id == -1)  //Only process if not active
        {
            if(controlPos.contains(x, y))
            {
                id = pid;

                last.x = x;
                last.y = y;
                anchor.x = x;
                anchor.y = y;

                tapDetect.reset();
                tapDetect.processPointer(action, pid, x, y);

                signal_action.emit(MULTITOUCHMOUSE_DOWN, last.x, last.y, 0, 0);
                return true;
            }
        }
        else //second finger down
        {
            if(controlPos.contains(x, y))
            {
                id2 = pid;
                last2.x = x;
                last2.y = y;
                signal_action.emit(MULTITOUCHMOUSE_2_DOWN, last2.x, last2.y, 0, 0);
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
                signal_action.emit(MULTITOUCHMOUSE_TAP, last.x, last.y, 0, 0);
            }

            signal_action.emit(MULTITOUCHMOUSE_UP, last.x, last.y, 0, 0);
            signal_action.emit(MULTITOUCHMOUSE_2_UP, last.x, last.y, 0, 0);
            reset();
            return true;
        }
        else if(id2 == pid)
        {
            id2 = -1;
            signal_action.emit(MULTITOUCHMOUSE_2_UP, x, y, 0, 0);
            return true;
        }

        return false;
    }
    else if(action == P_MOVE)
    {
        tapDetect.processPointer(action, pid, x, y);

        if((pid == id) && (id2 == -1))  //One finger moving
        {
            float dx = last.x - x;
            float dy = last.y - y;

            last.x = x;
            last.y = y;

            signal_action.emit(MULTITOUCHMOUSE_MOVE, last.x, last.y, dx, dy);

            return true;
        }
        else if(((pid == id) && (id2 != -1)) || (pid == id2))  //2 fingers down and one of them moving
        {
            float old_dist = distancePoints(last, last2);

            if(pid == id)
            {
                last.x = x;
                last.y = y;
            }
            else
            {
                last2.x = x;
                last2.y = y;
            }

            float new_dist = distancePoints(last, last2);

            float zoom = new_dist - old_dist;
            signal_action.emit(MULTITOUCHMOUSE_ZOOM, zoom, 0, 0, 0);
        }

        return false;
    }

    return false;
}

bool MultitouchMouse::initGL()
{
    int x, y;
    glTex = loadTextureFromPNG(image, x, y);

    return false;
}

bool MultitouchMouse::drawGL(bool editor)
{
    if(!hideGraphics)
    {
        if(id != -1)
            gl_drawRect(glTex, last.x - glRect.width / 2, last.y - glRect.height / 2, glRect);
        else
            gl_drawRect(glTex, controlPos.left + controlPos.width() / 2 - glRect.width / 2, controlPos.top + controlPos.height() / 2 - glRect.height / 2, glRect);
    }

    return false;
}

void MultitouchMouse::reset()
{
    id = -1;
    id2 = -1;
}

void MultitouchMouse::saveXML(TiXmlDocument &doc)
{
    TiXmlElement *root = new TiXmlElement(tag.c_str());
    doc.LinkEndChild(root);

    ControlSuper::saveXML(*root);
}

void MultitouchMouse::loadXML(TiXmlDocument &doc)
{
    TiXmlHandle hDoc(&doc);
    TiXmlElement *pElem = hDoc.FirstChild(tag).Element();

    if(!pElem)  //Check exists, if not just leave as default
        return;

    ControlSuper::loadXML(*pElem);
}
