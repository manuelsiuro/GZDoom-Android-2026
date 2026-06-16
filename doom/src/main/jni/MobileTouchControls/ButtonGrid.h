#include "sigc++/sigc++.h"
#include "ControlSuper.h"
#include "GLRect.h"
#include "OpenGLUtils.h"

#ifndef _ButtonGrid_H_
#define _ButtonGrid_H_


namespace touchcontrols
{

    static const uint32_t MAX_X_CELLS = 8;
    static const uint32_t MAX_Y_CELLS = 4;


    class ButtonGrid : public ControlSuper
    {
        int32_t pid;

        GLuint glCellBgTex;
        GLRect glRect;

        int32_t valuePressed;

        std::string cellBgImage;

        uint32_t xNbr;
        uint32_t yNbr;

        std::string cellImages[MAX_X_CELLS][MAX_Y_CELLS];
        GLuint cellGlTex[MAX_X_CELLS][MAX_Y_CELLS];
        int32_t cellValues[MAX_X_CELLS][MAX_Y_CELLS];

        int32_t gamepadXSel;
        int32_t gamepadYSel;

        void cellDown(uint32_t x, uint32_t y);

        void cellUp();

    public:
        sigc::signal<void, int32_t, int32_t> signal_button;

        sigc::signal<void, bool> signal_outside; // Signaled when press down OUTSIDE of control (used to hide it)

        ButtonGrid(std::string tag, RectF pos, std::string cellBgImage, uint32_t xNbr, uint32_t yNbr, bool hidden = false, std::string description = "");

        void addCell(uint32_t x, uint32_t y, std::string image, int32_t value);

        std::string getPrimaryImage();

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
