#include "sigc++/sigc++.h"
#include "ControlSuper.h"
#include "GLRect.h"
#include "PointF.h"
#include "OpenGLUtils.h"

#ifndef _TouchJoy_H_
#define _TouchJoy_H_


namespace touchcontrols
{

    class TouchJoy : public ControlSuper
    {
        bool pressed;
        bool hideGraphics;

    public: // SWAPFIX. Make this avaible to the other TouchJoy
        int pid;
    private:

        std::string floating_image;
        std::string background_image;

        GLuint glTex;
        GLuint glTexBackground;

        GLRect glRect;

        PointF valueTouch;
        PointF valueJoy;

        PointF last;
        PointF fingerPos;
        PointF anchor;
        int glitchFix;

        // Anchor point is centre of control, not where first tapped
        bool centerAnchor;

        // POINTER SWAP FIX
        TouchJoy *otherTouchJoySWAPFIX;

        //Double tap stuff
        int doubleTapState; //0 = waiting for first press, 1 = waiting for first lift,
        double doubleTapCounter;
    public:
        sigc::signal<void, float, float, float, float> signal_move;

        sigc::signal<void, int> signal_double_tap;


        TouchJoy(std::string tag, RectF pos, std::string floating_image, std::string background_image);

        void setCenterAnchor(bool v);

        void setHideGraphics(bool v);

        void resetOutput();

        bool processPointer(int action, int pid, float x, float y);

        bool drawGL(bool forEditor = false);

        bool initGL();

        void updateSize();

        void registerTouchJoySWAPFIX(TouchJoy *other);

        void saveXML(TiXmlDocument &doc);

        void loadXML(TiXmlDocument &doc);

    private:

        void reset();

        void calcNewValue();

        void doUpdate();
    };

}

#endif
