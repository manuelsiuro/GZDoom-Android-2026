//
// Created by Emile on 03/05/2017.
//

#ifndef OPENTOUCH_TEXTDRAW_H
#define OPENTOUCH_TEXTDRAW_H

#include "OpenGLUtils.h"

namespace touchcontrols
{

#define TEXT_DRAW_X_CENTER 0x1
#define TEXT_DRAW_Y_CENTRE 0x2

class TextDraw
{
    std::string font_filename;
    std::vector< FontInfo > fontInfoVec;
    GLuint glTex;
public:
    TextDraw();

    void initGL( std::string font_filename );

    float drawChar( char c, int fontSet, float x, float y, float height, uint32_t params = 0 );
};

}
#endif //OPENTOUCH_TEXTDRAW_H
