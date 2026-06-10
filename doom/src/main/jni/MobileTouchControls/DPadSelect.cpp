#include "DPadSelect.h"
#include "TouchControlsConfig.h"

using namespace touchcontrols;

DPadSelect::DPadSelect(std::string tag, RectF pos, std::string dPadImage, int32_t selectValue, bool hidden, std::string description) :
        ControlSuper(TC_TYPE_DPAD_SELECT, tag, pos),
        dPadImage(dPadImage),
        selectValue(selectValue)
{
    glDPad = 0;
    pid = -1;

    memset(cellValues, 0, sizeof(cellValues));
    memset(cellGlTex, 0, sizeof(cellGlTex));
    memset(cellPressed, 0, sizeof(cellPressed));

    this->hidden = hidden;
    this->description = description;
    updateSize();
}

void DPadSelect::addCell(uint32_t x, std::string image, int32_t value)
{
    cellImages[x] = image;
    cellValues[x] = value;
}

void DPadSelect::updateSize()
{
    glRect.resize(controlPos.right - controlPos.left, controlPos.bottom - controlPos.top);
}


void DPadSelect::resetOutput()
{
    pid = -1;

    for(int n = 0; n < 4; n++)
        cellUp(n);
}

void DPadSelect::cellDown(uint32_t x)
{
    cellPressed[x] = true;
    signal_button.emit(1, cellValues[x]);
}

void DPadSelect::cellUp(uint32_t x)
{
    cellPressed[x] = false;
    signal_button.emit(0, cellValues[x]);
}

bool DPadSelect::processPointer(int action, int pid, float x, float y)
{
    return false;
}

bool DPadSelect::gamepadInput(bool down, GamePadKey key)
{
    LOGTOUCH("dpad button %d %d", down, key);

    switch(key)
    {
        case LEFT:
        {
            if(down)
                cellDown(DPAD_LEFT);
            else
                cellUp(DPAD_LEFT);
        }
            break;

        case RIGHT:
        {
            if(down)
                cellDown(DPAD_RIGHT);
            else
                cellUp(DPAD_RIGHT);
        }
            break;

        case UP:
        {
            if(down)
                cellDown(DPAD_UP);
            else
                cellUp(DPAD_UP);
        }
            break;

        case DOWN:
        {
            if(down)
                cellDown(DPAD_DOWN);
            else
                cellUp(DPAD_DOWN);
        }
            break;

        case SELECT:
        {
            if(selectValue)
            {
                if(down)
                    signal_button.emit(1, selectValue);
                else
                    signal_button.emit(0, selectValue);
            }
        }
            break;

        default:
            break;
    }


    if(down && key == BACK)
    {
        // This hides it
        signal_outside.emit(true);
    }

    return true;
}

bool DPadSelect::initGL()
{
    int x, y;
    glDPad = loadTextureFromPNG(dPadImage, x, y);

    for(int n = 0; n < 4; n++)
        cellGlTex[n] = loadTextureFromPNG(cellImages[n], x, y);

    return false;
}

bool DPadSelect::drawGL(bool forEditor)
{
    float gridWidth = controlPos.width() / 3;
    float gridHeight = (controlPos.width() / 3) * ScaleX / ScaleY;

    GLRect glRect;
    glRect.resize(gridWidth, gridHeight);
    gl_drawRect(glDPad, controlPos.left + gridWidth, controlPos.top + gridHeight, glRect);   // Background

    for(int n = 0; n < 4; n++)
    {
        if(cellPressed[n])
            gl_color4f(0, 0, 1, 1);

        if(n == DPAD_LEFT)
        {
            gl_drawRect(cellGlTex[n], controlPos.left, controlPos.top + gridHeight, glRect);
        }
        else if(n == DPAD_UP)
        {
            gl_drawRect(cellGlTex[n], controlPos.left + gridWidth, controlPos.top, glRect);
        }
        else if(n == DPAD_RIGHT)
        {
            gl_drawRect(cellGlTex[n], controlPos.left + gridWidth * 2, controlPos.top + gridHeight, glRect);
        }
        else if(n == DPAD_DOWN)
        {
            gl_drawRect(cellGlTex[n], controlPos.left + gridWidth, controlPos.top + gridHeight * 2, glRect);
        }

        gl_color4f(1, 1, 1, 1);

    }

    return false;
}

void DPadSelect::saveXML(TiXmlDocument &doc)
{
    TiXmlElement *root = new TiXmlElement(tag.c_str());
    doc.LinkEndChild(root);

    ControlSuper::saveXML(*root);
}

void DPadSelect::loadXML(TiXmlDocument &doc)
{
    TiXmlHandle hDoc(&doc);
    TiXmlElement *pElem = hDoc.FirstChild(tag).Element();

    if(!pElem)  //Check exists, if not just leave as default
        return;

    ControlSuper::loadXML(*pElem);
}
