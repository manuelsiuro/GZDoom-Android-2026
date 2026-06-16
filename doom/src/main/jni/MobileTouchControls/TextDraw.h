//
// Created by Emile on 03/05/2017.
//

#ifndef OPENTOUCH_TEXTDRAW_H
#define OPENTOUCH_TEXTDRAW_H

#include "OpenGLUtils.h"

namespace touchcontrols
{

#define TEXT_DRAW_X_CENTER 0x1
#define TEXT_DRAW_Y_CENTER 0x2

    class TextDraw
    {
        std::string font_filename;
        std::vector <FontInfo> fontInfoVec;
        GLuint glTex;

        float getCharWidth(unsigned char c, int fontSet, float height);

        float getTotalWidth(const char *text, int fontSet, float height);

        float glyphScaleWidth = 1.0;
    public:
        TextDraw();

        void initGL(std::string font_filename);

        void scaleWidth(float scale);

        float drawChar(char c, int fontSet, float x, float y, float height, uint32_t params = 0);

        float drawText(const char *text, int fontSet, float x, float y, float height, uint32_t params = 0);
    };

}
#endif //OPENTOUCH_TEXTDRAW_H
