#include "GLheader_TC.h"

#include "TouchControls.h"
#include <math.h>

extern int android_app_is_shutting_down;

using namespace touchcontrols;

#define TAP_TIME 300

static std::string xlmAppend = "";
namespace touchcontrols
{
    void setGlobalXmlAppend(const char *append)
    {
        xlmAppend = append;
    }
}

TouchControls::TouchControls(std::string t, bool en, bool editable, int edit_group, bool showExtraSettings)
{
    tag = t;
    enabled = en;
    animating = false;
    fading = false;
    editing = false;
    fixAspect = true;
    hideEditButton = false;
    alpha = 0.5;
    defaultColor = COLOUR_WHITE;
    editGroup = edit_group;
    passThroughTouch = ALWAYS;

    int lines = ScaleX + 1 + ScaleY + 1;

    settingsButton = 0;

    if(editable)
    {
        grid = new GLLines(lines);

        int l = 0;

        for(int n = 0; n < ScaleX; n++)
        {
            grid->vertices[l + 0] = GLESscaleX((float) n / (float) ScaleX);
            grid->vertices[l + 1] = GLESscaleX(0);
            grid->vertices[l + 3] = GLESscaleX((float) n / (float) ScaleX);
            grid->vertices[l + 4] = GLESscaleX(-1);

            l += 6;
        }

        for(int n = 1; n < ScaleY + 1; n++)
        {
            grid->vertices[l + 0] = GLESscaleY(0);
            grid->vertices[l + 1] = GLESscaleY((float) -n / (float) ScaleY);
            grid->vertices[l + 3] = GLESscaleY(1);
            grid->vertices[l + 4] = GLESscaleY((float) -n / (float) ScaleY);

            l += 6;
        }

        if(showExtraSettings)
        {
            settingsButton = new touchcontrols::ButtonExt("settings_control", touchcontrols::RectF(12, 2, 14, 4), "settings_bars", 0);
            settingsButton->signal_button.connect(sigc::mem_fun(this, &TouchControls::settingsButtonPress));
        }

        // Set the size of the resize handles, then we just use offset to move them
        resizeHandleHeight = 0.1; // Set the size here
        resizeHandleWidth = resizeHandleHeight / ((float) GLScaleWidth / (float) -GLScaleHeight); // Make it square

        for(int n = 0; n < RH_SIZE; n++)
        {
            resizeHandleRects[n].left = 0;
            resizeHandleRects[n].top = 0;
            resizeHandleRects[n].right = resizeHandleWidth;
            resizeHandleRects[n].bottom = resizeHandleHeight;
        }

        resizeHandleSelected = RH_NONE;
    }
}


void TouchControls::setPassThroughTouch(PassThrough v)
{
    passThroughTouch = v;
}

void TouchControls::resetDefault()
{
    if(xmlFilename.length() > 0)
        loadXML(xmlFilename + ".default");
}

void TouchControls::setAllButtonsEnable(bool value)
{
    for(int n = 0; n < controls.size(); n++)
    {
        ControlSuper *c = controls.at(n);

        if(c->type == TC_TYPE_BUTTON)
        {
            c->setEnabled(value);
        }
    }
}

void TouchControls::animateIn(int steps)
{
    float top = 1;

    for(int n = 0; n < controls.size(); n++) // Find lowest number (nearest top of screen) edge
    {
        ControlSuper *c = controls.at(n);

        if(c->isEnabled())
        {
            if(c->controlPos.top < top)
                top = c->controlPos.top;
        }
    }

    animating = true;
    slideDir = 0;
    slidePos = 1 - top; //So we want it to slide in from the bottom
    animateStep = slidePos / (float) steps;
    setEnabled(true);
}

void TouchControls::animateOut(int steps)
{
    float top = 1;

    for(int n = 0; n < controls.size(); n++) // Find lowest number (nearest top of screen) edge
    {
        ControlSuper *c = controls.at(n);

        if(c->isEnabled())
        {
            if(c->controlPos.top < top)
                top = c->controlPos.top;
        }
    }

    animating = true;
    slideDir = 1;
    slidePos = 0;
    slideMax = (1 - top);
    animateStep = slideMax / (float) steps;
    //setEnabled(false);
}

void TouchControls::fade(fadedir_t dir, int steps)
{
    if(steps)
    {
        //LOGTOUCH("fade %d  %d",in,steps);
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
    else // No fading, jsut do it
    {
        if(dir == FADE_OUT)
        {
            setEnabled(false);
        }
    }
}


void TouchControls::edit()
{
    editing = true;
    selectedCtrl = 0;
    finger1.enabled = false;
    finger2.enabled = false;
}

void TouchControls::stopEdit()
{
    save();
    editing = false;
}

bool TouchControls::isEditing()
{
    return editing;
}

void TouchControls::setEnabled(bool v)
{
    enabled = v;
}

void TouchControls::setFixAspect(bool v)
{
    fixAspect = v;
}


bool TouchControls::isFixAspect()
{
    return fixAspect;
}

void TouchControls::resetOutput()
{
    int size = controls.size();

    for(int n = 0; n < size; n++)
    {
        ControlSuper *cs = controls.at(n);
        // LOGTOUCH("setEnabled reset to %s",cs->tag.c_str());
        cs->resetOutput();
    }
}

void TouchControls::setAlpha(float a)
{
    alpha = a;
}

void TouchControls::setColour(uint32_t c)
{
    defaultColor = c;
}

bool TouchControls::isEnabled()
{
    return enabled && !animating;
}


void TouchControls::addControl(Button *cntrl)
{
    cntrl->signal_buttonDown.connect(sigc::mem_fun(this, &TouchControls::buttonDown));
    cntrl->signal_buttonUp.connect(sigc::mem_fun(this, &TouchControls::buttonUp));
    cntrl->signal_button.connect(sigc::mem_fun(this, &TouchControls::button));
    controls.push_back((cntrl));
}

void TouchControls::addControl(ButtonExt *cntrl)
{
    cntrl->signal_button.connect(sigc::mem_fun(this, &TouchControls::button));
    controls.push_back((cntrl));
}

void TouchControls::addControl(ButtonGrid *cntrl)
{
    cntrl->signal_button.connect(sigc::mem_fun(this, &TouchControls::button));
    controls.push_back((cntrl));
}

void TouchControls::addControl(ControlSuper *cntrl)
{
    controls.push_back((cntrl));
}

bool TouchControls::gamepadInput(bool down, GamePadKey key)
{
    bool controlUsed = false;

    int size = controls.size();

    for(int n = 0; n < size; n++)
    {
        ControlSuper *cs = controls.at(n);

        if(cs->isEnabled() && !cs->isHidden())
            if(cs->gamepadInput(down, key))
            {
                // Gamepad actions only ever go to one control
                controlUsed = true;
                break;
            }
    }

    return controlUsed;
}


bool TouchControls::processPointer(int action, int pid, float x, float y)
{
#ifdef __ANDROID__

    if(android_app_is_shutting_down)
        return 0;

#endif

    if(!editing)
    {
        int size = controls.size();
        bool controlUsed = false;

        for(int n = 0; n < size; n++)
        {
            ControlSuper *cs = controls.at(n);

            if(cs->isEnabled())
            {
                // If there is something above the mouse which used the pointer data, don't send it to the mouse
                if(cs->type == TC_TYPE_MOUSE && controlUsed)
                {
                    continue;
                }

                if(cs->processPointer(action, pid, x, y))
                {
                    controlUsed = true;

                    //If it is a touch pad or mouse, break out so nothing under it gets data
                    // REMOVED so SWAPFIX works
                    //if (( cs->type == TC_TYPE_TOUCHJOY ) || ( cs->type == TC_TYPE_MOUSE ))
                    //	break;
                    if(cs->type == TC_TYPE_QUADSLIDE)   // Quad slide we should never pass data through
                        return true;
                }

                // Check if point is in control at all
                if(!cs->isAllowPassThrough() && cs->controlPos.contains(x, y))
                {
                    return true;
                }
            }
        }

        if(passThroughTouch == ALWAYS)
            return false;
        else if(passThroughTouch == NO_CONTROL)
            return controlUsed;
        else
            return true;
    }
    else // EDITOR
    {
        if(settingsButton)
            settingsButton->processPointer(action, pid, x, y);

        if(action == P_DOWN)
        {
            if(pid < 2)
            {
                // Check if control is selected and pressed a resize handle
                resizeHandleSelected = RH_NONE;

                if(selectedCtrl)
                {
                    for(int n = 0; n < RH_SIZE; n++)
                    {
                        // Check if in a resize handle
                        if(resizeHandleRects[n].contains(x, y))
                        {
                            resizeHandleSelected = (ResizeHandle) n;
                            LOGTOUCH("resizeHandleSelected = %d", resizeHandleSelected);
                            break;
                        }
                    }
                }

                if((!finger1.enabled) && (!finger2.enabled) && (pid == 0))
                {
                    tapTime = getMS();
                    totalFingerMove = 0;
                    finger1.enabled = true;
                    finger1.x = x;
                    finger1.y = y;
                }
                else if((finger1.enabled) && (pid == 1))
                {
                    finger2.enabled = true;
                    finger2.x = x;
                    finger2.y = y;
                    oldDist.x = fabs(finger1.x - finger2.x);
                    oldDist.y = fabs(finger1.y - finger2.y);
                }
            }
        }
        else if(action == P_UP)
        {
            if(pid < 2)
            {
                // Select a new control if did a quick tap
                if((resizeHandleSelected == RH_NONE) && (getMS() - tapTime < TAP_TIME) && (totalFingerMove < 0.03) && (pid == 0))
                {
                    selectedCtrl = 0;

                    for(int n = 0; n < controls.size(); n++)
                    {
                        ControlSuper *cs = controls.at(n);

                        if(cs->isEnabled() && !cs->isHidden() && cs->isEditable()) //&&  (cs->type != TC_TYPE_MOUSE))
                            if(cs->controlPos.contains(x, y))
                            {
                                selectedCtrl = cs;
                                break;
                            }
                    }
                }

                if(selectedCtrl != 0)
                {
                    snapControl(selectedCtrl);
                    moveResizeHandles(selectedCtrl);
                }

                if(pid == 0)
                    finger1.enabled = false;
                else if(pid == 1)
                    finger2.enabled = false;
            }
        }
        else if(action == P_MOVE)
        {
            if(selectedCtrl != 0)
            {
                if((resizeHandleSelected != RH_NONE) && (finger1.enabled) && (!finger2.enabled))
                {
                    if(resizeHandleSelected == RH_TOP_LEFT)
                    {
                        selectedCtrl->controlPos.left += x - finger1.x;
                        selectedCtrl->controlPos.top += y - finger1.y;
                    }
                    else if(resizeHandleSelected == RH_TOP_RIGHT)
                    {
                        selectedCtrl->controlPos.right += x - finger1.x;
                        selectedCtrl->controlPos.top += y - finger1.y;
                    }
                    else if(resizeHandleSelected == RH_BOT_RIGHT)
                    {
                        selectedCtrl->controlPos.right += x - finger1.x;
                        selectedCtrl->controlPos.bottom += y - finger1.y;
                    }
                    else if(resizeHandleSelected == RH_BOT_LEFT)
                    {
                        selectedCtrl->controlPos.left += x - finger1.x;
                        selectedCtrl->controlPos.bottom += y - finger1.y;
                    }
                }
                else if((finger1.enabled) && (!finger2.enabled))   //drag
                {
                    totalFingerMove += fabs(x - finger1.x) + fabs(y - finger1.y);
                    selectedCtrl->controlPos.offset(x - finger1.x, y - finger1.y);
                }
                else if((finger1.enabled) && (finger2.enabled))   //zoom
                {
                    float newDistX = fabs(finger1.x - finger2.x);

                    if(fabs(newDistX - oldDist.x) > ((float) 1 / (float) ScaleX / 2))
                    {
                        selectedCtrl->controlPos.right += (newDistX - oldDist.x);
                        oldDist.x = newDistX;
                    }

                    float newDistY = fabs(finger1.y - finger2.y);

                    if(fabs(newDistY - oldDist.y) > ((float) 1 / (float) ScaleY / 2))
                    {
                        selectedCtrl->controlPos.bottom += (newDistY - oldDist.y);
                        oldDist.y = newDistY;
                    }

                    snapControl(selectedCtrl);
                }

                windowControl(selectedCtrl);
                moveResizeHandles(selectedCtrl);

                if(pid == 0)
                {
                    finger1.x = x;
                    finger1.y = y;
                }
                else if(pid == 1)
                {
                    finger2.x = x;
                    finger2.y = y;
                }
            }
        }
    }

    return false;
}


void TouchControls::settingsButtonPress(int state, int code)
{
    LOGTOUCH("settingsButtonPress %d %d", state, code);

    if(state == BUTTONEXT_TAP)   //Only want to activate this if we do a real tap
    {
        signal_settingsButton.emit(1);
    }
}

void TouchControls::windowControl(ControlSuper *ctrl)
{
    if(ctrl->controlPos.left < 0)
        ctrl->controlPos.offsetTo(0, ctrl->controlPos.top);
    else if(ctrl->controlPos.right > 1)
        ctrl->controlPos.offsetTo(1 - (ctrl->controlPos.right - ctrl->controlPos.left), ctrl->controlPos.top);

    if(ctrl->controlPos.top < 0)
        ctrl->controlPos.offsetTo(ctrl->controlPos.left, 0);
    else if(ctrl->controlPos.bottom > 1)
        ctrl->controlPos.offsetTo(ctrl->controlPos.left, 1 - (ctrl->controlPos.bottom - ctrl->controlPos.top));

    // Check isn't too small or too large
    if(ctrl->controlPos.width() < (1 / (float) ScaleX / 2))
    {
        ctrl->controlPos.right = ctrl->controlPos.left + (1 / (float) ScaleX / 2);
    }
    else if(ctrl->controlPos.width() > MAXIMUM_CONTROL_SIZE)
    {
        ctrl->controlPos.right = ctrl->controlPos.left + MAXIMUM_CONTROL_SIZE;
    }

    if(ctrl->controlPos.height() < (1 / (float) ScaleY / 2))
    {
        ctrl->controlPos.bottom = ctrl->controlPos.top + (1 / (float) ScaleY / 2);
    }
    else if(ctrl->controlPos.height() > 1.0)
    {
        ctrl->controlPos.bottom = ctrl->controlPos.top + 1.0;
    }

    ctrl->updateSize();
}

void TouchControls::snapControl(ControlSuper *ctrl)
{
    uint32_t snapX = ScaleX * 2;
    uint32_t snapY = ScaleY * 2;
    int t = floor((ctrl->controlPos.left * (float) snapX) + 0.5);
    ctrl->controlPos.left = (float) t / (float) snapX;
    t = floor((ctrl->controlPos.right * (float) snapX) + 0.5);
    ctrl->controlPos.right = (float) t / (float) snapX;
    t = floor((ctrl->controlPos.top * (float) snapY) + 0.5);
    ctrl->controlPos.top = (float) t / (float) snapY;
    t = floor((ctrl->controlPos.bottom * (float) snapY) + 0.5);
    ctrl->controlPos.bottom = (float) t / (float) snapY;
    ctrl->updateSize();
}

void TouchControls::moveResizeHandles(ControlSuper *ctrl)
{
    resizeHandleRects[RH_TOP_LEFT].offsetTo(ctrl->controlPos.left - resizeHandleWidth, ctrl->controlPos.top - resizeHandleHeight);
    resizeHandleRects[RH_TOP_RIGHT].offsetTo(ctrl->controlPos.right, ctrl->controlPos.top - resizeHandleHeight);
    resizeHandleRects[RH_BOT_RIGHT].offsetTo(ctrl->controlPos.right, ctrl->controlPos.bottom);
    resizeHandleRects[RH_BOT_LEFT].offsetTo(ctrl->controlPos.left - resizeHandleWidth, ctrl->controlPos.bottom);
}

// Draw with custom alpha
int TouchControls::draw(float alphaDraw)
{
    float tempAlpha = alpha;
    alpha = alphaDraw;

    draw();

    alpha = tempAlpha;

    return 0;
}

int TouchControls::draw()
{
    bool fixAspectOrig = gl_getFixAspect();
    gl_setFixAspect(fixAspect);

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
    }
    else
    {
        fadePos = 1;
    }

    int size = controls.size();

    for(int n = 0; n < size; n++) //draw
    {
        ControlSuper *c = controls.at(size - 1 - n);

        if(c->isEnabled())
        {
            gl_loadIdentity();
            gl_scalef(GLScaleWidth, GLScaleHeight, 1);

            if(animating)
                gl_translatef(0, -slidePos, 0);

            // check if control has default color or not
            if(c->color == COLOUR_NONE)
                gl_color4f(defaultColor, alpha * fadePos);
            else
                gl_color4f(c->color, alpha * fadePos);

            c->drawGL();
        }
    }

    if(animating)
    {
        if(slideDir == 0)
        {
            slidePos -= animateStep;

            if(slidePos <= 0)
                animating = false;
        }
        else //Animate out
        {
            slidePos += animateStep;

            if(slidePos >= slideMax)
            {
                animating = false;
                setEnabled(false);
            }
        }
    }

    gl_setFixAspect(fixAspectOrig);

    if(editing)
        return 1;
    else
        return 0;
}

int TouchControls::drawEditor()
{
    gl_clearColor(0.0f, 0.0f, 0.0f, 1.0f);

    gl_loadIdentity();
    gl_scalef(GLScaleWidth, GLScaleHeight, 1);
    gl_color4f(COLOUR_WHITE, 1);
    gl_drawLines(0, 0, *grid);

    int size = controls.size();

    for(int n = 0; n < size; n++) //draw
    {
        ControlSuper *c = controls.at(size - 1 - n);

        if(c->isEnabled())  //&&  (c->type != TC_TYPE_MOUSE))
        {
            GLRect rect;
            rect.resize(c->controlPos.right - c->controlPos.left, c->controlPos.bottom - c->controlPos.top);
            gl_loadIdentity();
            gl_scalef(GLScaleWidth, GLScaleHeight, 1);

            if(!c->isHidden())  //Not hidden control
                gl_drawRect((GLfloat) 1, (GLfloat) 0.5, (GLfloat) 0, (GLfloat) 0.2, c->controlPos.left, c->controlPos.top, rect);

            gl_loadIdentity();
            gl_scalef(GLScaleWidth, GLScaleHeight, 1);

            if(c->color == COLOUR_NONE)
                gl_color4f(defaultColor, 1);
            else
                gl_color4f(c->color, 1);

            c->drawGL(true);

            gl_loadIdentity();
            gl_scalef(GLScaleWidth, GLScaleHeight, 1);
        }
    }

    ControlSuper *sel = selectedCtrl; //Save pointer because drawn of different thread

    if(sel != 0)
    {
        GLRect rect;

        // Make selected control a different color
        rect.resize(sel->controlPos.width(), sel->controlPos.height());
        gl_loadIdentity();
        gl_scalef(GLScaleWidth, GLScaleHeight, 1);
        gl_drawRect((GLfloat) 0.5, (GLfloat) 0.3, (GLfloat) 0.8, (GLfloat) 0.5, sel->controlPos.left, sel->controlPos.top, rect);

        // Draw resize handles
        rect.resize(resizeHandleRects[RH_TOP_LEFT].width(), resizeHandleRects[RH_TOP_LEFT].height());
        gl_drawRect(glTexResizeHandle, resizeHandleRects[RH_TOP_LEFT].left, resizeHandleRects[RH_TOP_LEFT].top, rect, true);
        rect.rotate90(false);
        gl_drawRect(glTexResizeHandle, resizeHandleRects[RH_TOP_RIGHT].left, resizeHandleRects[RH_TOP_RIGHT].top, rect, true);
        rect.rotate90(false);
        gl_drawRect(glTexResizeHandle, resizeHandleRects[RH_BOT_RIGHT].left, resizeHandleRects[RH_BOT_RIGHT].top, rect, true);
        rect.rotate90(false);
        gl_drawRect(glTexResizeHandle, resizeHandleRects[RH_BOT_LEFT].left, resizeHandleRects[RH_BOT_LEFT].top, rect, true);
    }

    if(editing)
    {
        if(settingsButton)
        {
            gl_loadIdentity();
            gl_scalef(GLScaleWidth, GLScaleHeight, 1);
            gl_color4f(COLOUR_WHITE, 1);
            settingsButton->drawGL();
        }
    }

    if(editing)
        return 1;
    else
        return 0;
}

void TouchControls::initGL()
{
    int size = controls.size();

    for(int n = 0; n < size; n++) //draw
    {
        ControlSuper *c = controls.at(size - 1 - n);
        c->initGL();
    }

    if(settingsButton)
        settingsButton->initGL();

    int x, y;
    glTexResizeHandle = loadTextureFromPNG("resize_handle", x, y);
}

void TouchControls::setXMLFile(std::string file)
{
    xmlFilename = file + xlmAppend;
    saveXML(xmlFilename + ".default"); //Save incase we want to reset positions
    loadXML(xmlFilename);
}

void TouchControls::save()
{
    saveXML(xmlFilename);
}

void TouchControls::saveXML(std::string filename)
{
    if(filename.length() == 0)
        return;

    TiXmlDocument doc;
    TiXmlDeclaration *decl = new TiXmlDeclaration("1.0", "", "");
    doc.LinkEndChild(decl);

    for(int n = 0; n < controls.size(); n++) //draw
    {
        ControlSuper *c = controls.at(n);
        c->saveXML(doc);
    }

    doc.SaveFile(filename);
}

void TouchControls::loadXML(std::string filename)
{
    //The hell..crashes on destructor when on stack..
    //TiXmlDocument doc(filename.c_str());
    TiXmlDocument *doc = new TiXmlDocument(filename.c_str());

    if(!doc->LoadFile()) return;

    for(int n = 0; n < controls.size(); n++) //draw
    {
        ControlSuper *c = controls.at(n);
        c->loadXML(*doc);
        c->updateSize();
    }

    delete doc;
}

std::vector<ControlSuper *> *TouchControls::getControls()
{
    return &controls;
}

void *TouchControls::getControl(std::string name)
{
    for(int n = 0; n < controls.size(); n++) //draw
    {
        ControlSuper *c = controls.at(n);

        if(c->tag == name)
            return c;
    }

    return NULL;
}

