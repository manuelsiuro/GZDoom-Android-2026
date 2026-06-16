#include "sigc++/sigc++.h"
#include "ControlSuper.h"
#include "GLRect.h"
#include "PointF.h"
#include "OpenGLUtils.h"

#ifndef _WheelSelect_H_
#define _WheelSelect_H_


namespace touchcontrols
{

    enum WheelSelectMode
    {
        WHEELSELECT_GP_MODE_HOLD,
        WHEELSELECT_GP_MODE_TAP,
    };

    class WheelSelect : public ControlSuper
    {
        bool pressed;
        bool hideGraphics;

        int id;

        std::string image;

        GLuint glTex;
        GLuint glTexFade;
        GLuint glTexSelected;

        GLRect glRect;
        GLRect glRectFade;
        GLRect glRectSelected;

        PointF last;
        PointF fingerPos;
        PointF anchor;

        PointF centre;

        bool visible;

        int nbrSegs;

        int selectedSeg;

        WheelSelectMode gamepadMode;
        bool gamepadMultiTap; // True to allow rapid taping of button to cycle weapons
        int gamepadAutoTimeout;
        bool gamepadInUse;
        float gamepadLastX;
        float gamepadLastY;

        uint64_t gamepadLastMoveTime; // Store the time the gamepad axis last moved, used for auto timout

        bool axisBlock;
        uint64_t axisBlockDelay; // Stop the gamepad axis working for a short amount of time after selection

        bool useFadeSegs; //default no
        int enabledSegs;
    public:

        sigc::signal<void, int> signal_selected;
        sigc::signal<void, int> signal_enabled;
        sigc::signal<void, int> signal_scroll;

        WheelSelect(std::string tag, RectF pos, std::string image_filename, int segments);

        void setWheelVisible(bool visible);

        void setSegmentEnabled(int seg, bool v);

        void setHideGraphics(bool v);

        void resetOutput();

        bool processPointer(int action, int pid, float x, float y);

        void processGamepad(float x, float y);

        bool blockGamepad(); // Returns true if need to block the axis values

        void setGamepadMode(WheelSelectMode mode, int autoTimeout);

        bool gamepadActionButton(int state);

        bool drawGL(bool forEditor = false);

        bool initGL();

        void updateSize();

        void saveXML(TiXmlDocument &doc);

        void loadXML(TiXmlDocument &doc);

        void reset();

    private:

        void calcNewValue();

        void doUpdate();

        float distCentre(float x, float y);

        bool inCentre(float x, float y);

        bool gamepadUpdateSeg();

        void gamepadSelect();
    };

}

#endif
