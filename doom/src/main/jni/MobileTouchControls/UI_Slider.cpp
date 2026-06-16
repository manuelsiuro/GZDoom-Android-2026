#include "UI_Slider.h"
#include "TouchControlsConfig.h"


using namespace touchcontrols;

#define MOVE_LOCK 0.01
#define TAP_TIME  250

UI_Slider::UI_Slider(std::string tag, RectF pos, uint32_t uid, std::string bg_texture, std::string handle_texture) : ControlSuper(TC_TYPE_UI_SLIDER, tag, pos)
{

    this->bg_texture = bg_texture;
    this->handle_texture = handle_texture;
    this->uid = uid;

    glTex = 0;

    value = 0.5;

    touchId = -1;

    updateSize();
}


float UI_Slider::getValue()
{
    return value;
}

void UI_Slider::setValue(float v)
{
    if(v < 0)
        v = 0;
    else if(v > 1)
        v = 1;

    value = v;
}

void UI_Slider::updateSize()
{
    glRect.resize(controlPos.right - controlPos.left, controlPos.bottom - controlPos.top);
    float height = controlPos.height() / 5;
    float width = height * (-GLScaleHeight / GLScaleWidth);
    glRectHandle.resize(width, height);
}

void UI_Slider::updateValue(float x)
{
    float v = (x - controlPos.left) / controlPos.width();

    if(v < 0)
        v = 0;
    else if(v > 1)
        v = 1;

    //Snap to middle
    //if( abs(v - 0.5) < 0.05 )
    //    v = 0.5;

    value = v;

    signal.emit(uid, value);
}

bool UI_Slider::processPointer(int action, int pid, float x, float y)
{
    if(action == P_DOWN)
    {
        if(touchId == -1)  //Only process if not active
        {
            if(controlPos.contains(x, y))
            {
                timeDown = getMS();
                touchId = pid;
                anchor.x = x;
                anchor.y = y;
                lockState = 0;
                //updateValue( x );
                return true;
            }
        }

        return false;
    }
    else if(action == P_UP)
    {
        if(pid == touchId)
        {
            uint64_t timeNow = getMS();

            // If have not scrolled down and did a quick tap, then update
            if((lockState != -1) && (timeNow - timeDown) < TAP_TIME)
            {
                updateValue(x);
            }

            touchId = -1;
            return true;
        }

        return false;
    }
    else if(action == P_MOVE)
    {
        if(pid == touchId)  //Finger already down
        {
            if(lockState == 0)
            {
                float dx = abs(anchor.x - x);
                float dy = abs(anchor.y - y);

                if((dy > MOVE_LOCK) || (dx > MOVE_LOCK))
                {
                    if((dy / 1.6) > dx) // Probably trying to scroll, lock it out
                    {
                        lockState = -1;
                    }
                    else // OK
                    {
                        lockState = 1;
                    }
                }
            }

            if(lockState == 1)
            {
                updateValue(x);
            }

            return true;
        }

        return false;
    }

    return false;
}

void UI_Slider::resetOutput()
{

}

bool UI_Slider::initGL()
{
    int x, y;
    glTex = loadTextureFromPNG(bg_texture, x, y);
    glTexHandle = loadTextureFromPNG(handle_texture, x, y);

    return false;
}

bool UI_Slider::drawGL(bool forEditor)
{
    gl_drawRect(glTex, controlPos.left, controlPos.top, glRect);
    //Draw in the middle
    gl_drawRect(glTexHandle, controlPos.left + (controlPos.width() * value) - (glRectHandle.width / 2),
                controlPos.top + (controlPos.height() - glRectHandle.height) / 2, glRectHandle);

    return false;
}

void UI_Slider::saveXML(TiXmlDocument &doc)
{
    TiXmlElement *root = new TiXmlElement(tag.c_str());
    doc.LinkEndChild(root);

    //ControlSuper::saveXML(*root);
}

void UI_Slider::loadXML(TiXmlDocument &doc)
{
    TiXmlHandle hDoc(&doc);
    TiXmlElement *pElem = hDoc.FirstChild(tag).Element();

    if(!pElem)  //Check exists, if not just leave as default
        return;

    //ControlSuper::loadXML(*pElem);
}
