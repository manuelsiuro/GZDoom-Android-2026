#include "sigc++/sigc++.h"
#include <vector>
#include "ControlSuper.h"
#include "GLRect.h"
#include "OpenGLUtils.h"
#include "TextDraw.h"
#include "TapDetect.h"

#ifndef _UI_ColorPicker_H_
#define _UI_ColorPicker_H_


namespace touchcontrols
{

    class UI_ColorPicker : public ControlSuper
    {
        bool pressed;

        int touchId;

        GLuint glTex;
        GLuint glColorCube;
        GLuint glRedCross;

        GLRect glRect;

        std::string bg_image;

        uint32_t uid;

        uint32_t selectedItem;

        uint32_t currentColor;

        float colorGridTop;
        float colorGridLeft;


        bool isOpen;
        float fadePos;

        float listTop, listBottom, listItemHeight;

        TapDetect tapDetect;

    public:

        sigc::signal<bool, uint32_t, uint32_t> signal;

        void setColor(uint32_t color);

        UI_ColorPicker(std::string tag, RectF pos, uint32_t uid, uint32_t currentColor);

        bool processPointer(int action, int pid, float x, float y);

        void resetOutput();

        bool drawGL(bool active = false);

        bool UI_drawGL(bool active = false);

        bool initGL();

        void updateSize();

        void saveXML(TiXmlDocument &doc);

        void loadXML(TiXmlDocument &doc);
    };

}

#endif
