#include "sigc++/sigc++.h"
#include "ControlSuper.h"
#include "GLRect.h"
#include "OpenGLUtils.h"

#ifndef _DPadSelect_H_
#define _DPadSelect_H_


namespace touchcontrols
{

    static const uint32_t DPAD_LEFT = 0;
    static const uint32_t DPAD_UP = 1;
    static const uint32_t DPAD_RIGHT = 2;
    static const uint32_t DPAD_DOWN = 3;


    class DPadSelect : public ControlSuper
    {
        int32_t pid;

        GLuint glDPad;
        GLRect glRect;

        std::string dPadImage;

        std::string cellImages[4];
        GLuint cellGlTex[4];
        int32_t cellValues[4];

        bool cellPressed[4];

        int32_t selectValue;

        void cellDown(uint32_t x);

        void cellUp(uint32_t x);

    public:
        sigc::signal<void, int32_t, int32_t> signal_button;

        sigc::signal<void, bool> signal_outside; // Signaled when press down OUTSIDE of control (used to hide it)

        DPadSelect(std::string tag, RectF pos, std::string dPadImage, int32_t selectValue, bool hidden = false, std::string description = "");

        void addCell(uint32_t x, std::string image, int32_t value);

        void resetOutput();

        bool processPointer(int action, int pid, float x, float y);

        bool gamepadInput(bool down, GamePadKey key);

        bool drawGL(bool forEditor = false);

        bool initGL();

        void updateSize();

        void saveXML(TiXmlDocument &doc);

        void loadXML(TiXmlDocument &doc);
    };

}

#endif
