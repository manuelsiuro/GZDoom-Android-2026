//
// Created by Emile on 03/05/2017.
//

#include "TextDraw.h"


using namespace touchcontrols;


TextDraw::TextDraw()
{
  fontInfoVec.resize(256);
}


void TextDraw::initGL( std::string font_filename )
{
    this->font_filename = font_filename;
    int x, y;
    glTex = loadTextureFromPNG( font_filename, x, y, &fontInfoVec );
}


#define CHAR_TO_GLYPH( C, F ) ((C - 32) + 128 * F)

float TextDraw::drawChar( char c, int fontSet, float x, float y, float height, uint32_t params )
{
    // Scale width
    float width = height * ( -GLScaleHeight / GLScaleWidth );

    char g = CHAR_TO_GLYPH( c, fontSet );

    int32_t tx = g % 16;
    int32_t ty = ( g >> 4 );
    float fx = ( float )tx / 16.f;
    float fy = -( float )ty / 16.f;

    // Find how must space needs to be removed from the sides of the character
    float leftPad = fontInfoVec[g].leftGap;
    float rightPad = fontInfoVec[g].rightGap;

    // Create a new rect because the width needs to change
    GLRect glRectTemp;

    glRectTemp.texture[0] = fx + (leftPad  / 16.f);
    glRectTemp.texture[1] = fy - 0.0625f;
    glRectTemp.texture[2] = fx + (leftPad / 16.f);
    glRectTemp.texture[3] = fy;
    glRectTemp.texture[4] = fx + 0.0625f - (rightPad / 16.f);
    glRectTemp.texture[5] = fy - 0.0625f;
    glRectTemp.texture[6] = fx + 0.0625f - (rightPad / 16.f);
    glRectTemp.texture[7] = fy;

    float charWidth;
    if( c == ' ' )
    {
        charWidth = width / 4;
    }
    else
    {
        float leftPad = fontInfoVec[CHAR_TO_GLYPH( c, fontSet )].leftGap;
        float rightPad = fontInfoVec[CHAR_TO_GLYPH( c, fontSet )].rightGap;

        charWidth = width * (1 - (leftPad + rightPad) );
    }

    glRectTemp.resize( charWidth, height );

    // Center on the x axis
    if( params & TEXT_DRAW_X_CENTER )
        x = x - (charWidth / 2);

    drawRect( glTex, x, y, glRectTemp );

    return charWidth;
}