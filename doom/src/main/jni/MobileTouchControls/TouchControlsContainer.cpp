
#include "GLheader_TC.h"

#include "TouchControlsContainer.h"


using namespace touchcontrols;

bool touchcontrols::g_touchJoyMultiTouch = false;

void touchcontrols::setTouchJoyMultiTouch(bool enable)
{
    g_touchJoyMultiTouch = enable;
}

TouchControlsContainer::TouchControlsContainer()
{
    alpha = 0.5;
    mouseShow = false;
}

void TouchControlsContainer::showMouse(bool show)
{
    mouseShow = show;
}

void TouchControlsContainer::mousePos(float x, float y)
{
    mouseX = x;
    mouseY = y;
}

void TouchControlsContainer::resetDefaults()
{
    int size = controls.size();

    for(int n = 0; n < size; n++)
    {
        TouchControls *cs = controls.at(n);
        cs->resetDefault();
    }
}

void TouchControlsContainer::addControlGroup(TouchControls *cntrl)
{
    controls.push_back((cntrl));
}

void TouchControlsContainer::editControls(TouchControls *ctrl)
{
    editingControls = ctrl;
    editingControls->edit();
}

void TouchControlsContainer::setColour(uint32_t defaultColor)
{
    this->defaultColor = defaultColor;
}

void TouchControlsContainer::setAlpha(float alpha)
{
    this->alpha = alpha;
}

bool TouchControlsContainer::isEditing()
{
    return (editingControls == 0) ? false : true;
}

void TouchControlsContainer::finishEditing()
{
    if(editingControls)
    {
        if(uiControls != NULL && uiControls->getEnabled())
        {
            uiControls->setEnabled(false);
        }
        else
        {
            editingControls->stopEdit();
            editingControls = 0;
            signal_settings.emit(0);
        }
    }
}

void TouchControlsContainer::showUIWindow(UI_Controls *ui)
{
    this->uiControlsTemp = ui;
}

void TouchControlsContainer::hideUIWindow(bool andDelete)
{
    if(andDelete)
        uiHide = 2;
    else
        uiHide = 1;
}

bool TouchControlsContainer::gamepadInput(bool down, GamePadKey key)
{
    int size = controls.size();

    for(int n = 0; n < size; n++)
    {
        TouchControls *cs = controls.at(n);

        if(cs->isEnabled())
        {
            if(cs->gamepadInput(down, key) == true)
            {
                return true;
            }
        }
    }

    return false;
}

bool TouchControlsContainer::processPointer(int action, int pid, float x, float y)
{
    //LOGTOUCH("%d  %d  %f  %f",action,pid,x,y);
    // Check if the UI is active
    if(uiControls != NULL)
    {
        if(uiControls->getEnabled())
        {
            uiControls->processPointer(action, pid, x, y);
            return false;
        }
    }

    if(drawEditButton && editorButton)
        editorButton->processPointer(action, pid, x, y);

#ifdef USE_LIBROCKET
    touchGui->processPointer(action, pid, x, y);
#endif

    if(editingControls == 0)
    {
        int size = controls.size();

        // We need to capture all the enabled controls, this is because it's possible the enabled controls can change while
        // in the loop to processes pointer data
        static TouchControls *enabled[64];

        int numEnabled = 0;

        for(int n = 0; n < size; n++)
        {
            TouchControls *c = controls.at(n);

            if(c->isEnabled())
            {
                enabled[numEnabled++] = c;
            }
        }

        // null terminator
        enabled[numEnabled] = NULL;

        int n = 0;
        TouchControls *cs = enabled[n++];

        while(cs != NULL)
        {
            if(cs->processPointer(action, pid, x, y) == true)
            {
                // Don't pass touch if returns true
                break;
            }

            cs = enabled[n++];
        }

        return false;
    }
    else
    {
        editingControls->processPointer(action, pid, x, y);

        return false;
    }
}

int TouchControlsContainer::draw()
{
    int size = controls.size();

    //Do this first to stop flicker when switching
    for(int n = 0; n < size; n++) //draw
    {
        TouchControls *c = controls.at(size - 1 - n);

        if(c->isEditing())
        {
            editingControls = c;
            signal_settings.emit(1); //Editing controls
            break;
        }
    }

    openGL_start.emit();

    if(editingControls == 0)
    {
        int drawEditButton_ = 0;

        for(int n = 0; n < size; n++) //draw
        {
            TouchControls *c = controls.at(size - 1 - n);

            if(c->enabled)
            {
                c->draw();

                if((c->hideEditButton == false) && c->editGroup != -1)
                    drawEditButton_ = 1;
            }
        }

        if(drawEditButton_ != drawEditButton)
            drawEditButton = drawEditButton_;

        if(drawEditButton && editorButton)
        {
            gl_loadIdentity();
            gl_scalef(GLScaleWidth, GLScaleHeight, 1);

            gl_color4f(defaultColor, alpha);
            editorButton->drawGL();
        }

#ifdef USE_LIBROCKET
        touchGui->update();
#endif
    }
    else // Editing controls
    {
        // Check if we have controls to draw in the background
        if(editingControls->getEditBackgroundControl())
        {
            editingControls->getEditBackgroundControl()->draw(0.4); // Draw background controls, low alpha
        }

        if(!editingControls->drawEditor())  //Check if finished editing..
        {
            signal_settings.emit(0);
            editingControls = 0;
        }

        if(drawEditButton && editorButton)
        {
            drawEditButton = true;
            gl_loadIdentity();
            gl_scalef(GLScaleWidth, GLScaleHeight, 1);
            editorButton->drawGL();
        }

#ifdef USE_LIBROCKET
        touchGui->update();
#endif
    }

    if(uiControlsTemp != NULL)
    {
        uiControls = uiControlsTemp;
        uiControlsTemp = NULL;
        uiControls->initGL();
        uiControls->setEnabled(true);
        uiControls->fade(touchcontrols::FADE_IN, 10);
    }


    if(uiControls != NULL)
    {
        if(uiHide == 1)  // We want to hide the active UI element
        {
            uiControls->setEnabled(false);
            uiHide = 0;
        }
        else if(uiHide == 2)   // We want to hide and delete the element
        {
            delete (uiControls);
            uiControls = NULL;
            uiHide = 0;
        }
        else if(uiControls->getEnabled())
        {
            //Grey out background
            GLRect rect;
            rect.resize(1, 1);
            gl_loadIdentity();
            gl_scalef(GLScaleWidth, GLScaleHeight, 1);
            gl_drawRect((GLfloat) 0, (GLfloat) 0, (GLfloat) 0, (GLfloat) 0.7, 0.f, 0.f, rect);

            gl_setFixAspect(false);
            uiControls->draw();
            gl_setFixAspect(true);
        }
    }

    if(mouseShow)
    {
        GLRect rect;
        rect.resize(0.05, 0.08);

        gl_loadIdentity();
        gl_color4f(COLOUR_WHITE, 1);
        gl_scalef(GLScaleWidth, GLScaleHeight, 1);
        gl_drawRect(mouseCursorGL, mouseX, mouseY, rect);
    }

    openGL_end.emit();

    return 0;
}

void TouchControlsContainer::initGL(const char *root_path)
{
    clearGlTexCache();

    gl_Init();

    if(!editorButton)
    {
        //Just for if we have an edit group
        editorButton = new touchcontrols::Button("edit_control", touchcontrols::RectF(12, 0, 14, 2), "settings", 0);
        editorButton->signal_button.connect(sigc::mem_fun(this, &TouchControlsContainer::editorButtonPress));
    }

    int x, y;
    mouseCursorGL = loadTextureFromPNG("mouse_cursor", x, y);
    //editorButton->updateSize();

    int size = controls.size();

    // NOTE! This must be loaded in this order, for some strange reason if loaded reversed it causes Mali G57 GPU to not load texture correctly
    // Must be bug in the driver
    for(int n = 0; n < size; n++) // Load GL
    {
        TouchControls *c = controls.at(n);
        c->initGL();
    }

    if(editorButton)
        editorButton->initGL();

#ifdef USE_LIBROCKET
    touchGui = new TouchGui(root_path, 640, 480);
#endif

}


void TouchControlsContainer::editorButtonPress(int state, int code)
{

    if(state)
    {
        if(editingControls == 0)  //Not currently editing
        {
            int size = controls.size();

            for(int n = 0; n < size; n++)
            {
                TouchControls *cs = controls.at(n);

                if(cs->enabled)
                {
                    if(cs->editGroup != -1)
                    {
                        cs->edit();
                        break;
                    }
                }
            }
        }
        else //Find next in group
        {
            int curEditGroup = -1;
            int size = controls.size();

            for(int n = 0; n < size; n++)
            {
                TouchControls *cs = controls.at(n);

                if(curEditGroup != -1)
                {
                    if(cs->editGroup == curEditGroup)
                    {
                        cs->edit();
                        break;
                    }
                }

                if(editingControls == cs)
                {
                    curEditGroup = cs->editGroup;
                    cs->stopEdit();
                }
            }
        }
    }
}

TouchControls *TouchControlsContainer::getEditingControls()
{
    return editingControls;
}
