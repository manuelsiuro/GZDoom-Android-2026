#include "sigc++/sigc++.h"
#include "ControlSuper.h"
#include "GLRect.h"
#include "OpenGLUtils.h"

#ifndef _UI_TextBox_H_
#define _UI_TextBox_H_

#define UI_TEXT_LEFT  0x1
#define UI_TEXT_CENTRE 0x2
#define UI_TEXT_RIGHT 0x4

namespace touchcontrols
{

    class UI_TextBox : public ControlSuper
    {
        bool pressed;

        int id;

        GLuint glTex;

        GLRect glRect;

        std::string image;
        std::string text;
        float textHeight;
        float textWidthScale = 0.625; //The glyph width is the height scaled by this
        float charSpacing;
        int fontSet; //0 or 1
        uint32_t params;

        uint32_t color;

        std::vector <FontInfo> fontInfoVec;

        float getCharWidth(unsigned char c);

        float getTotalWidth();

    public:

        UI_TextBox(std::string tag, RectF pos, std::string font_filename, int fontSet, uint32_t params, std::string text, float textSize,
                   uint32_t color = COLOUR_WHITE);

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
