#include "ButtonGrid.h"
#include "TouchControlsConfig.h"

using namespace touchcontrols;

ButtonGrid::ButtonGrid(std::string tag, RectF pos, std::string cellBgImage, uint32_t xNbr, uint32_t yNbr, bool hidden, std::string description) :
        ControlSuper(TC_TYPE_BUTTONGRID, tag, pos),
        xNbr(xNbr),
        yNbr(yNbr),
        cellBgImage(cellBgImage)
{
    glCellBgTex = 0;
    pid = -1;
    valuePressed = -1;
    gamepadXSel = -1;
    gamepadYSel = -1;

    memset(cellValues, 0, sizeof(cellValues));
    memset(cellGlTex, 0, sizeof(cellGlTex));

    this->hidden = hidden;
    this->description = description;
    updateSize();
}

void ButtonGrid::addCell(uint32_t x, uint32_t y, std::string image, int32_t value)
{
    cellImages[x][y] = image;
    cellValues[x][y] = value;
}

void ButtonGrid::updateSize()
{
    glRect.resize(controlPos.right - controlPos.left, controlPos.bottom - controlPos.top);
}


void ButtonGrid::resetOutput()
{
    pid = -1;
    cellUp();
}

std::string ButtonGrid::getPrimaryImage()
{
    std::string ret = "";

    for(int cy = 0; cy < yNbr; cy++)
        for(int cx = 0; cx < xNbr; cx++)
            if(cellImages[cx][cy] != "")
                ret = cellImages[cx][cy];

    LOGTOUCH("getPrimaryImage = %s", ret.c_str());

    return ret;
}

void ButtonGrid::cellDown(uint32_t x, uint32_t y)
{
    valuePressed = cellValues[x][y];
    signal_button.emit(1, valuePressed);
}

void ButtonGrid::cellUp()
{
    if(valuePressed != -1)
    {
        signal_button.emit(0, valuePressed);
        valuePressed = -1;
    }
}

bool ButtonGrid::processPointer(int action, int pid, float x, float y)
{
    if(hidden)  //Hidden controls do not respond to inputs
        return false;

    if(action == P_DOWN)
    {
        if(controlPos.contains(x, y))
        {
            this->pid = pid;

            uint32_t cx = (uint32_t)((x - controlPos.left) / (controlPos.width() / (float) xNbr));
            uint32_t cy = (uint32_t)((y - controlPos.top) / (controlPos.height() / (float) yNbr));
            cellDown(cx, cy);
            return true;
        }
        else
        {
            signal_outside.emit(false);
        }

        return false;
    }
    else if(action == P_UP)
    {
        if(pid == this->pid)
        {
            this->pid = -1;
            cellUp();
            return true;
        }

        return false;
    }
    else if(action == P_MOVE)
    {
        return false;
    }

    return false;
}

bool ButtonGrid::gamepadInput(bool down, GamePadKey key)
{
    if(gamepadXSel == -1)   // Gamepad not used yet
    {
        gamepadXSel = 0;
        gamepadYSel = 0;
    }
    else
    {
        if(down)
        {
            switch(key)
            {
                case LEFT:
                {
                    if(gamepadXSel > 0)
                        gamepadXSel--;
                }
                    break;

                case RIGHT:
                {
                    if(gamepadXSel < xNbr - 1)
                        gamepadXSel++;
                }
                    break;

                case UP:
                {
                    if(gamepadYSel > 0)
                        gamepadYSel--;
                }
                    break;

                case DOWN:
                {
                    if(gamepadYSel < yNbr - 1)
                        gamepadYSel++;
                }
                    break;

                default:
                    break;
            }
        }

        if(key == SELECT)
        {
            if(down)
                cellDown(gamepadXSel, gamepadYSel);
            else
                cellUp();
        }
        else if(key == BACK)
        {
            // This hides it
            signal_outside.emit(true);
        }
    }

    return true;
}

bool ButtonGrid::initGL()
{
    int x, y;
    glCellBgTex = loadTextureFromPNG(cellBgImage, x, y);

    for(int cy = 0; cy < yNbr; cy++)
        for(int cx = 0; cx < xNbr; cx++)
            cellGlTex[cx][cy] = loadTextureFromPNG(cellImages[cx][cy], x, y);

    return false;
}

bool ButtonGrid::drawGL(bool forEditor)
{
    if(hidden)
        return false;

    GLRect glRect;
    float cellWidth = controlPos.width() / xNbr;
    float cellHeight = controlPos.height() / yNbr;
    glRect.resize(cellWidth, cellHeight);

    bool aspect = gl_getFixAspect(); // Save old

    gl_setFixAspect(false);   // Turn off aspect fixing for this

    for(int cy = 0; cy < yNbr; cy++)
        for(int cx = 0; cx < xNbr; cx++)
        {
            if(cellValues[cx][cy])  //Only show cells with a valid value
            {
                float x = controlPos.left + cx * cellWidth;
                float y = controlPos.top + cy * cellHeight;

                if(cx == gamepadXSel && cy == gamepadYSel)
                    gl_disable(GL_BLEND);

                gl_drawRect(glCellBgTex, x, y, glRect);   // Background

                gl_enable(GL_BLEND);  //reset

                gl_drawRect(cellGlTex[cx][cy], x, y, glRect);
            }
        }

    gl_setFixAspect(aspect);

    return false;
}

void ButtonGrid::saveXML(TiXmlDocument &doc)
{
    TiXmlElement *root = new TiXmlElement(tag.c_str());
    doc.LinkEndChild(root);

    ControlSuper::saveXML(*root);
}

void ButtonGrid::loadXML(TiXmlDocument &doc)
{
    TiXmlHandle hDoc(&doc);
    TiXmlElement *pElem = hDoc.FirstChild(tag).Element();

    if(!pElem)  //Check exists, if not just leave as default
        return;

    ControlSuper::loadXML(*pElem);
}
