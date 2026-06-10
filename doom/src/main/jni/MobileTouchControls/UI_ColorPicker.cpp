#include "UI_ColorPicker.h"
#include "TouchControlsConfig.h"
#include "Colors.h"

using namespace touchcontrols;

#define COLORS_NBR_X 5
#define COLORS_NBR_Y 4


static const float CUBE_SIZE = 0.1f;


uint32_t colors[COLORS_NBR_Y][COLORS_NBR_X] =
        {
                {COLOUR_NONE,    COLOUR_WHITE,   COLOUR_GREY2,  COLOUR_BLACK, COLOUR_RED1},
                {COLOUR_PINK,    COLOUR_YELLOW1, COLOUR_GREEN1, COLOUR_BLUE1, COLOUR_RED2},
                {COLOUR_PURPLE1, COLOUR_YELLOW2, COLOUR_GREEN2, COLOUR_BLUE2, COLOUR_RED3},
                {COLOUR_PURPLE2, COLOUR_ORANGE,  COLOUR_GREEN3, COLOUR_BLUE3, COLOUR_REDDY}
        };

UI_ColorPicker::UI_ColorPicker(std::string tag, RectF pos, uint32_t uid, uint32_t currentColor)
        : ControlSuper(TC_TYPE_UI_COLORPICKER, tag, pos)
{
    touchId = -1;
    this->uid = uid;
    this->currentColor = currentColor;

    glTex = 0;
    isOpen = false;
    fadePos = 0;
    selectedItem = 0;

    updateSize();
}


void UI_ColorPicker::setColor(uint32_t color)
{
    currentColor = color;
}

void UI_ColorPicker::updateSize()
{
    glRect.resize(controlPos.right - controlPos.left, controlPos.bottom - controlPos.top);
}


bool UI_ColorPicker::processPointer(int action, int pid, float x, float y)
{
    if(action == P_DOWN)
    {
        if(!isOpen)
        {
            if(touchId == -1)    //Only process if not active
            {
                if(controlPos.contains(x, y))
                {
                    touchId = pid;
                    fadePos = 0;
                    tapDetect.reset();
                    tapDetect.processPointer(action, pid, x, y);
                    return true;
                }
            }
        }
        else
        {
            touchId = pid;
        }

        return false;
    }
    else if(action == P_UP)
    {
        tapDetect.processPointer(action, pid, x, y);

        if(pid == touchId)
        {
            if(!isOpen)
            {
                if(tapDetect.didTap())
                {
                    isOpen = true;
                }
            }
            else
            {
                // Check left/right bounds of control
                int32_t selectedX = (x - colorGridLeft) / CUBE_SIZE;
                int32_t selectedY = (y - colorGridTop) / CUBE_SIZE;

                if(selectedX >= 0 && selectedX < COLORS_NBR_X && selectedY >= 0 && selectedY < COLORS_NBR_Y)
                {
                    // function returns true to keep it open
                    isOpen = signal.emit(uid, colors[selectedY][selectedX]);

                    // Update own color if selected
                    if(!isOpen)
                    {
                        currentColor = colors[selectedY][selectedX];
                    }
                }
            }

            touchId = -1;
            return true;
        }

        return false;
    }
    else if(action == P_MOVE)
    {
        tapDetect.processPointer(action, pid, x, y);

        return false;
    }

    return false;
}

void UI_ColorPicker::resetOutput()
{
    touchId = -1;
}

bool UI_ColorPicker::initGL()
{
    int x, y;
    glTex = loadTextureFromPNG(bg_image, x, y);
    glColorCube = loadTextureFromPNG("ui_color_cube", x, y);
    glRedCross = loadTextureFromPNG("red_strike", x, y);
    return false;
}


bool UI_ColorPicker::drawGL(bool active)
{

    if(!active)  // Active is true when color pallet is open
    {

        if(currentColor == COLOUR_NONE)
        {
            gl_color4f(COLOUR_WHITE, 1);
            gl_drawRect(glRedCross, controlPos.left, controlPos.top, glRect);
        }
        else
        {
            gl_color4f(currentColor, 1);
            gl_drawRect(glColorCube, controlPos.left, controlPos.top, glRect);
        }
    }
    else
    {
        if(isOpen || fadePos > 0)
        {
            // Put to the middle of the screen
            colorGridTop = 0.5 - (CUBE_SIZE * COLORS_NBR_Y / 2.f);
            colorGridLeft = 0.5 - (CUBE_SIZE * COLORS_NBR_X / 2.f);

            GLRect glRectCube;

            // Draw black ackground to the vubes
            glRectCube.resize(CUBE_SIZE * COLORS_NBR_X, CUBE_SIZE * COLORS_NBR_Y);
            gl_color4f(0, 0, 0, 1);
            gl_drawRect(glColorCube, colorGridLeft, colorGridTop, glRectCube);

            // Draw cubes
            glRectCube.resize(CUBE_SIZE, CUBE_SIZE);

            for(int y = 0; y < COLORS_NBR_Y; y++)
            {
                for(int x = 0; x < COLORS_NBR_X; x++)
                {
                    uint32_t color = colors[y][x];

                    if(color == COLOUR_NONE)
                    {
                        gl_color4f(COLOUR_WHITE, 1);
                        gl_drawRect(glRedCross, colorGridLeft + x * CUBE_SIZE, colorGridTop + y * CUBE_SIZE, glRectCube);
                    }
                    else
                    {
                        gl_color4f(color, 1);
                        gl_drawRect(glColorCube, colorGridLeft + x * CUBE_SIZE, colorGridTop + y * CUBE_SIZE, glRectCube);
                    }
                }
            }
        }
    }

    gl_color4f(1, 1, 1, 1);

    return isOpen; // if is open, return true then drawGL gets called again with active = true
}

void UI_ColorPicker::saveXML(TiXmlDocument &doc)
{
    TiXmlElement *root = new TiXmlElement(tag.c_str());
    doc.LinkEndChild(root);

    ControlSuper::saveXML(*root);
}

void UI_ColorPicker::loadXML(TiXmlDocument &doc)
{
    TiXmlHandle hDoc(&doc);
    TiXmlElement *pElem = hDoc.FirstChild(tag).Element();

    if(!pElem)    //Check exists, if not just leave as default
    {
        return;
    }

    ControlSuper::loadXML(*pElem);
}
