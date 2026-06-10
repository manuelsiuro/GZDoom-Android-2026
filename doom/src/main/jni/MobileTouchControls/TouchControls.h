

#include <string>
#include <stdbool.h>
#include <vector>

#include "TouchControlsConfig.h"
#include "Button.h"
#include "ButtonExt.h"
#include "TouchJoy.h"
#include "Mouse.h"
#include "WheelSelect.h"
#include "MultitouchMouse.h"
#include "PolarSelect.h"
#include "JoyStick.h"
#include "ButtonGrid.h"
#include "QuadSlide.h"
#include "DPadSelect.h"

#include "UI_Window.h"
#include "UI_Slider.h"
#include "UI_TextBox.h"
#include "UI_Button.h"
#include "UI_Switch.h"
#include "UI_Keyboard.h"
#include "UI_DropDown.h"
#include "UI_ColorPicker.h"

#include "Colors.h"

#ifndef _TouchControls_H_
#define _TouchControls_H_

//#define ScaleX
namespace touchcontrols
{

//const int ScaleX = 26;
//const int ScaleY = 16;
    class ControlData
    {

    public:
        float left, top, right, bottom;
        float alpha;
        std::string tag;
        bool enabled;
    };


    void setGlobalXmlAppend(const char *append);

    class TouchControls
    {
    public:
        enum PassThrough
        {
            ALWAYS,
            NO_CONTROL,
            NEVER
        };

    private:

        const float MAXIMUM_CONTROL_SIZE = 0.8; // Maximum size an editable element can be (1 is will size of screen)

        std::vector<ControlSuper *> controls;

        void buttonDown(int code)
        {
            signal_buttonDown.emit(code);
        }

        void buttonUp(int code)
        {
            signal_buttonUp.emit(code);
        }

        void button(int state, int code)
        {
            signal_button.emit(state, code);
        }

        PassThrough passThroughTouch;

        //EDITOR
        std::string xmlFilename;

        bool editing;

        GLLines *grid;
        ControlSuper *selectedCtrl;
        PointF finger1, finger2;
        PointF oldDist;

        uint64_t tapTime;
        float totalFingerMove;

        void windowControl(ControlSuper *ctrl);

        void snapControl(ControlSuper *ctrl);

        ButtonExt *settingsButton;

        void settingsButtonPress(int state, int code);

        //ANIMATIONS STUFF
        float slidePos; //current pos
        float slideMax; //post to slide to when animating out
        int slideDir; //0 = in, 1 = out

        float animateStep;
        bool animating;

        float fadePos; //current fade
        fadedir_t fadeDir;
        float fadeStep;
        bool fading;

        bool fixAspect; // Make circles circle, default on.

        // Resize handle stuff
        float resizeHandleWidth = 0.05;
        float resizeHandleHeight = 0.05;

        enum ResizeHandle
        {
            RH_TOP_LEFT,
            RH_TOP_RIGHT,
            RH_BOT_RIGHT,
            RH_BOT_LEFT,
            RH_SIZE,
            RH_NONE
        };

        GLuint glTexResizeHandle;
        ResizeHandle resizeHandleSelected = RH_NONE;
        RectF resizeHandleRects[RH_SIZE];

        void moveResizeHandles(ControlSuper *ctrl);

        TouchControls *editingBackgroundControls = NULL; // Set to show other controls in the background when editing (used for custom controls)
    public:

        bool enabled;

        uint32_t defaultColor;
        float alpha;

        int editGroup;
        bool hideEditButton;

        std::string tag;

        sigc::signal<void, int> signal_buttonDown;
        sigc::signal<void, int> signal_buttonUp;
        sigc::signal<void, int, int> signal_button;

        sigc::signal<void, int> signal_settingsButton;

        TouchControls(std::string t, bool en, bool editable, int edit_group = -1, bool showExtraSettings = true);

        void setEditBackgroundControl(TouchControls *bg)
        {
            editingBackgroundControls = bg;
        }

        TouchControls *getEditBackgroundControl()
        {
            return editingBackgroundControls;
        }

        void setPassThroughTouch(PassThrough v);

        void animateIn(int steps);

        void animateOut(int steps);

        void fade(fadedir_t dir, int steps);

        void edit();

        void stopEdit();

        bool isEditing();

        void setEnabled(bool v);

        bool isEnabled();

        void setFixAspect(bool v);

        bool isFixAspect();

        void setAlpha(float a);

        void setColour(uint32_t defaultColor);

        void addControl(Button *cntrl);

        void addControl(ButtonExt *cntrl);

        void addControl(ButtonGrid *cntrl);

        void addControl(ControlSuper *cntrl);

        int draw();

        int draw(float alpha);

        int drawEditor();

        void initGL();

        bool processPointer(int action, int pid, float x, float y);

        bool gamepadInput(bool down, GamePadKey key);

        void saveXML(std::string filename);

        void loadXML(std::string filename);

        void save();

        void setXMLFile(std::string file);

        void resetDefault(); //Reset control positions to init XML

        void resetOutput(); //Make all controls output the reset state

        //Enable or disable all BUTTONS in control group
        void setAllButtonsEnable(bool value);

        std::vector<ControlSuper *> *getControls();

        void *getControl(std::string name); //Get control by name, obviously you must cast to correct type!
    };

}


#endif
