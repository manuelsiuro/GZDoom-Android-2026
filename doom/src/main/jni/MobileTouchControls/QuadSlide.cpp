#include "QuadSlide.h"
#include "TouchControlsConfig.h"

using namespace touchcontrols;

#define STATE_UP         0
#define STATE_DOWN_UP    1
#define STATE_DOWN_LEFT  2
#define STATE_DOWN_DOWN  4
#define STATE_DOWN_RIGHT 8
#define STATE_DOWN       (STATE_DOWN_UP|STATE_DOWN_LEFT|STATE_DOWN_DOWN|STATE_DOWN_RIGHT)

QuadSlide::QuadSlide(std::string tag, RectF pos, std::string bgImage, std::string arrowImage, int32_t valueUp, int32_t valueRight, int32_t valueDown, int32_t valueLeft,
                     bool hidden, std::string description) :
        ControlSuper(TC_TYPE_QUADSLIDE, tag, pos)
{
    glBgTex = 0;
    glArrowTex = 0;
    pid = -1;
    arrowSlidePos = 0;
    arrowSlideTime = 0;
    valueCurrent = 0;
    showDemoArrowsTime = 0;

    this->hidden = hidden;
    this->description = description;
    this->bgImage = bgImage;
    this->arrowImage = arrowImage;

    this->valueUp = valueUp;
    this->valueDown = valueDown;
    this->valueLeft = valueLeft;
    this->valueRight = valueRight;

    updateSize();

    resetOutput();
}


void QuadSlide::updateSize()
{
    glRect.resize(controlPos.right - controlPos.left, controlPos.bottom - controlPos.top);
}


void QuadSlide::resetOutput()
{
    pid = -1;
    slideState = STATE_UP;
    valueOutput(false, 0);
    valueCurrent = 0;
}

void QuadSlide::valueOutput(bool down, int32_t value)
{
    if(down)
    {
        if(value != valueCurrent)   // Check if change
        {
            if(valueCurrent != 0)   // If pressing a button then release it first
            {
                signal.emit(0, valueCurrent);
            }

            signal.emit(1, value);
            valueCurrent = value;
        }
    }
    else
    {
        if(valueCurrent != 0)   // If pressing a button then release it first
        {
            signal.emit(0, valueCurrent);
            valueCurrent = 0;
        }
    }
}

bool QuadSlide::processPointer(int action, int pid, float x, float y)
{
    if(hidden)  //Hidden controls do not respond to inputs
        return false;

    if(action == P_DOWN)
    {
        if(controlPos.contains(x, y))
        {
            this->pid = pid;
            anchorPoint.x = x;
            anchorPoint.y = y;
            slideState = STATE_DOWN;
            showDemoArrowsTime = 0;
            return true;
        }
        else
        {

        }

        return false;
    }
    else if(action == P_UP)
    {
        if(pid == this->pid)
        {
            if(valueCurrent == 0)   // This means we pressed it but didn't slide, so show demo arrows
            {
                showDemoArrowsTime = 100; // Number of frames to show demo arrows
            }

            resetOutput();
            return true;
        }

        return false;
    }
    else if(action == P_MOVE)
    {
        if(pid == this->pid)
        {
            float triggerDist = controlPos.width(); //

            if(valueLeft && (x < anchorPoint.x - triggerDist))
            {
                slideState = STATE_DOWN_LEFT;
                valueOutput(true, valueLeft);
            }
            else if(valueRight && (x > anchorPoint.x + triggerDist))
            {
                slideState = STATE_DOWN_RIGHT;
                valueOutput(true, valueRight);
            }
            else if(valueUp && (y < anchorPoint.y - triggerDist))
            {
                slideState = STATE_DOWN_UP;
                valueOutput(true, valueUp);
            }
            else if(valueDown && (y > anchorPoint.y + triggerDist))
            {
                slideState = STATE_DOWN_DOWN;
                valueOutput(true, valueDown);
            }
            else
            {
                slideState = STATE_DOWN;
                valueOutput(false, 0);
            }

            return true;
        }

        return false;
    }

    return false;
}

bool QuadSlide::initGL()
{
    int x, y;
    glBgTex = loadTextureFromPNG(bgImage, x, y);
    glArrowTex = loadTextureFromPNG(arrowImage, x, y);

    return false;
}

bool QuadSlide::drawGL(bool forEditor)
{
    // Change to white because this control is currently colored
    gl_color3f(COLOUR_WHITE);

    uint32_t drawArrows = 0;

    if(forEditor)
    {
        if(!hidden)
        {
            gl_drawRect(glBgTex, controlPos.left, controlPos.top, glRect);
            drawArrows =
                    (valueUp ? STATE_DOWN_UP : 0) |
                    (valueDown ? STATE_DOWN_DOWN : 0) |
                    (valueLeft ? STATE_DOWN_LEFT : 0) |
                    (valueRight ? STATE_DOWN_RIGHT : 0);
        }
    }
    else //Draw normal in game
    {
        if(!hidden)
        {
            gl_drawRect(glBgTex, controlPos.left, controlPos.top, glRect);

            if(slideState == STATE_UP)
            {
                if(showDemoArrowsTime)
                {
                    showDemoArrowsTime--;
                    drawArrows =
                            (valueUp ? STATE_DOWN_UP : 0) |
                            (valueDown ? STATE_DOWN_DOWN : 0) |
                            (valueLeft ? STATE_DOWN_LEFT : 0) |
                            (valueRight ? STATE_DOWN_RIGHT : 0);
                }
            }
            else if(slideState == STATE_DOWN)
            {
                drawArrows = 0;
            }
            else
            {
                drawArrows = slideState;
            }
        }
    }


    if(drawArrows != 0)
    {
        float arrowOffset = arrowSlidePos * controlPos.width();

        GLRect arrowRect;
        // Top
        arrowRect.resize(controlPos.width(), controlPos.height() / 3);

        if(drawArrows & STATE_DOWN_UP)
            gl_drawRect(glArrowTex, controlPos.left, controlPos.top - arrowRect.height - arrowOffset, arrowRect);

        //Bottom
        arrowRect.mirror(true, false);

        if(drawArrows & STATE_DOWN_DOWN)
            gl_drawRect(glArrowTex, controlPos.left, controlPos.top + controlPos.height() + arrowOffset, arrowRect);

        // Left
        arrowRect.resize(controlPos.width() / 3, controlPos.height());
        arrowRect.rotate90(false);

        if(drawArrows & STATE_DOWN_LEFT)
            gl_drawRect(glArrowTex, controlPos.left - arrowRect.width - arrowOffset, controlPos.top, arrowRect);

        // right
        arrowRect.rotate90(false);
        arrowRect.rotate90(false);

        if(drawArrows & STATE_DOWN_RIGHT)
            gl_drawRect(glArrowTex, controlPos.left + controlPos.width() + arrowOffset, controlPos.top, arrowRect);

#define SLIDE_TIME_MS 300

        uint64_t timeNow = getMS();
        uint64_t diff = timeNow - arrowSlideTime;
        arrowSlidePos = (float) (diff % SLIDE_TIME_MS) / (float) SLIDE_TIME_MS;
        //LOGTOUCH("time now = %" PRId64 " slid pos = %f",timeNow,arrowSlidePos);
    }


    return false;
}

void QuadSlide::saveXML(TiXmlDocument &doc)
{
    TiXmlElement *root = new TiXmlElement(tag.c_str());
    doc.LinkEndChild(root);

    ControlSuper::saveXML(*root);
}

void QuadSlide::loadXML(TiXmlDocument &doc)
{
    TiXmlHandle hDoc(&doc);
    TiXmlElement *pElem = hDoc.FirstChild(tag).Element();

    if(!pElem)  //Check exists, if not just leave as default
        return;

    ControlSuper::loadXML(*pElem);
}
