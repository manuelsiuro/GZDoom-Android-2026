#include <string>
#include <stdbool.h>
#include <vector>

#include "TouchControlsInterface.h"
#include "TouchControlsConfig.h"
#include "TouchControls.h"
#include "UI_Controls.h"

#ifdef USE_LIBROCKET
#include "TouchGui.h"
#endif

#ifndef _TouchControlsContainer_H_
#define _TouchControlsContainer_H_

//#define ScaleX
namespace touchcontrols
{


    class TouchControlsContainer : public TouchControlsInterface
    {
        std::vector<TouchControls *> controls;

        TouchControls *editingControls = NULL;


        //For when we have an edit group
        Button *editorButton;

        void editorButtonPress(int state, int code);

        int drawEditButton;

        // Set to UI control group
        UI_Controls *uiControls = NULL;
        UI_Controls *uiControlsTemp = NULL; // To try and avoid threading issues this is set then copied to uiControls
        uint32_t uiHide = 0; // Set to 1 to hide the cureent UI, set to 2 to also delete it

        uint32_t defaultColor = COLOUR_WHITE; // Default white
        float alpha;

        bool mouseShow = false;
        float mouseX;
        float mouseY;
        int mouseCursorGL = 0;

    public:

        sigc::signal<void, int> signal_settings;

        sigc::signal<void> openGL_start;
        sigc::signal<void> openGL_end;

        TouchControlsContainer();

        void addControlGroup(TouchControls *cntrl);

        void editControls(TouchControls *ctrl);

        void setColour(uint32_t defaultColor);

        void setAlpha(float alpha);

        int draw();

        void initGL(const char *root_path = NULL);

        bool processPointer(int action, int pid, float x, float y);

        bool gamepadInput(bool down, GamePadKey key);

        bool isEditing();

        void finishEditing();

        void resetDefaults();

        TouchControls *getEditingControls();

        void showUIWindow(UI_Controls *uiControls);

        void hideUIWindow(bool andDelete = false);

        void showMouse(bool show);

        void mousePos(float x, float y);
        //Needs to be here for IOS, breaks otherwise. Memory or compiler bug
#ifdef USE_LIBROCKET
        TouchGui *touchGui;
#endif

    };

}


#endif
