#include "UI_DropDown.h"
#include "TouchControlsConfig.h"


using namespace touchcontrols;


template<typename Out>
void split(const std::string &s, char delim, Out result)
{
    std::stringstream ss(s);
    std::string item;

    while(std::getline(ss, item, delim))
    {
        *(result++) = item;
    }
}

UI_DropDown::UI_DropDown(std::string tag, RectF pos, uint32_t uid, std::string font_filename, int fontSet,
                         std::string lableText, std::string optionList, float textSize, std::string bg_image)
        : ControlSuper(TC_TYPE_UI_DROPDOWN, tag, pos)
{
    touchId = -1;
    this->bg_image = bg_image;
    this->uid = uid;
    this->lableText = lableText;
    this->font_filename = font_filename;
    glTex = 0;
    isOpen = false;
    fadePos = 0;
    selectedItem = 0;
    // Split the options into the vector
    split(optionList, ':', std::back_inserter(listItems));

    listItemHeight = controlPos.height() / 1.5;
    listTop = controlPos.top - (listItemHeight * listItems.size()); // Put list above control
    listBottom = listTop + listItemHeight * listItems.size();

    updateSize();
}

void UI_DropDown::setSelected(uint32_t n)
{
    selectedItem = n;
}


void UI_DropDown::updateSize()
{
    glRect.resize(controlPos.right - controlPos.left, controlPos.bottom - controlPos.top);
}

void UI_DropDown::scaleSize(float x, float y)
{
    ControlSuper::scaleSize(x, y);
    textDraw.scaleWidth(x);
}

bool UI_DropDown::processPointer(int action, int pid, float x, float y)
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
                    //isOpen = true;
                    fadePos = 0;
                    tapDetect.reset();
                    tapDetect.processPointer(action, pid, x, y);
                    return true;
                }
            }
        }
        else
        {
            isOpen = false;

            // Check lef/right bounds of control
            if(x > controlPos.left && x < controlPos.right && y > listTop && y < listBottom)
            {
                uint32_t n = (y - listTop) / listItemHeight;
                LOGTOUCH("LIST = %d", n);

                if(n < listItems.size())   // Should always be, just in case
                {
                    selectedItem = n;
                    signal.emit(uid, selectedItem);
                }
            }

            return true;
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

            //signal.emit(1 , uid );
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

void UI_DropDown::resetOutput()
{
    touchId = -1;
}

bool UI_DropDown::initGL()
{
    int x, y;
    glTex = loadTextureFromPNG(bg_image, x, y);
    textDraw.initGL(font_filename);
    return false;
}


bool UI_DropDown::drawGL(bool forEditor)
{
    gl_drawRect(glTex, controlPos.left, controlPos.top, glRect);

    float textHeight = controlPos.height() / 2;

    textDraw.drawText((lableText + listItems[selectedItem]).c_str(), 0, controlPos.left + controlPos.height() / 4,
                      controlPos.top + controlPos.height() / 4,
                      textHeight);

    if(isOpen || fadePos > 0)
    {
        // Draw background
        GLRect glRect;
        glRect.resize(controlPos.width(), listBottom - listTop);
        gl_drawRect(0, 0, 0, fadePos, controlPos.left, listTop, glRect);

        gl_color4f(1, 1, 1, fadePos);

        float y = listTop; //Inital offset from bottom for the list

        for(int n = 0; n < listItems.size(); n++)
        {
            textDraw.drawText(listItems[n].c_str(), 0, controlPos.left + controlPos.height() / 4,
                              y,
                              textHeight * 1.2);
            y += listItemHeight;
        }

        gl_color4f(1, 1, 1, 1);

        if(isOpen)
        {
            if(fadePos < 1)
                fadePos += 0.1;
        }
        else
        {
            if(fadePos > 0)
                fadePos -= 0.1;
        }
    }

    return false;
}

void UI_DropDown::saveXML(TiXmlDocument &doc)
{
    TiXmlElement *root = new TiXmlElement(tag.c_str());
    doc.LinkEndChild(root);

    ControlSuper::saveXML(*root);
}

void UI_DropDown::loadXML(TiXmlDocument &doc)
{
    TiXmlHandle hDoc(&doc);
    TiXmlElement *pElem = hDoc.FirstChild(tag).Element();

    if(!pElem)    //Check exists, if not just leave as default
    {
        return;
    }

    ControlSuper::loadXML(*pElem);
}
