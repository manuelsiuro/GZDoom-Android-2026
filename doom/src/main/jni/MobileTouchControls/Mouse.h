#include "sigc++/sigc++.h"
#include "ControlSuper.h"
#include "GLRect.h"
#include "PointF.h"
#include "OpenGLUtils.h"
#include "TapDetect.h"

#ifndef _Mouse_H_
#define _Mouse_H_

#define TOUCHMOUSE_MOVE 0
#define TOUCHMOUSE_TAP  1
#define TOUCHMOUSE_DOWN 2
#define TOUCHMOUSE_UP   3

#define TOUCHMOUSE_2_DOWN 4
#define TOUCHMOUSE_2_UP   5

namespace touchcontrols
{

    class Mouse : public ControlSuper
    {
        bool pressed;
        bool hideGraphics;

        int id;

        int id2;

        std::string image;

        GLuint glTex;

        GLRect glRect;
        //GLLines *glLines;

        PointF valueRel;

        PointF last;
        PointF fingerPos;
        PointF anchor;
        int glitchFix;

        TapDetect tapDetect;

        void emit(int, float, float, float, float);

    public:
        sigc::signal<void, int, float, float, float, float> signal_action;

        Mouse(std::string tag, RectF pos, std::string image_filename);

        void setHideGraphics(bool v);

        void resetOutput();

        bool processPointer(int action, int pid, float x, float y);

        bool drawGL(bool);

        bool initGL();

        void updateSize();

        void saveXML(TiXmlDocument &doc);

        void loadXML(TiXmlDocument &doc);

    private:
        void reset();

        void calcNewValue();

        void doUpdate();
    };

}

#endif
