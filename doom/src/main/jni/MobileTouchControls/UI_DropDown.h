#include "sigc++/sigc++.h"
#include <vector>
#include "ControlSuper.h"
#include "GLRect.h"
#include "OpenGLUtils.h"
#include "TextDraw.h"
#include "TapDetect.h"

#ifndef _UI_DropDown_H_
#define _UI_DropDown_H_


namespace touchcontrols
{

    class UI_DropDown : public ControlSuper
    {
        bool pressed;

        int touchId;

        GLuint glTex;

        GLRect glRect;

        std::string bg_image;
        std::string font_filename;

        std::string lableText;
        std::vector <std::string> listItems;

        uint32_t uid;

        TextDraw textDraw;

        uint32_t selectedItem;
        bool isOpen;
        float fadePos;

        float listTop, listBottom, listItemHeight;

        TapDetect tapDetect;

    public:

        sigc::signal<void, uint32_t, uint32_t> signal;

        void setSelected(uint32_t n);

        UI_DropDown(std::string tag, RectF pos, uint32_t uid, std::string font_filename, int fontSet,
                    std::string lableText, std::string optionList, float textSize, std::string bg_image);

        bool processPointer(int action, int pid, float x, float y);

        void resetOutput();

        bool drawGL(bool forEditor = false);

        bool initGL();

        void updateSize();

        void scaleSize(float x, float y);

        void saveXML(TiXmlDocument &doc);

        void loadXML(TiXmlDocument &doc);
    };

}

#endif
