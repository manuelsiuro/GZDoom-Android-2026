#include "GLRect.h"
#include "GLLines.h"
#include <string>
#include <vector>
#include "TouchControlsConfig.h"

extern "C" {
#include <libpng/png.h>
}


#ifndef _OpenGLUtils_H_
#define _OpenGLUtils_H_

#define TEXTURE_LOAD_ERROR -1

namespace touchcontrols
{

    struct FontInfo
    {
        float leftGap;
        float rightGap;
    };

    extern float GLScaleWidth;
    extern float GLScaleHeight;

    void gl_setGLESVersion(int v);

    int gl_getGLESVersion();

    void gl_Init();

    void gl_setGraphicsBasePath(std::string path);

    void gl_useGL4ES();

    void gl_resetGL4ES();

    void gl_startRender();

    void gl_endRender();

    void gl_setupForSDLSW();

    float GLESscaleX(float X);

    float GLESscaleY(float Y);

    void gl_drawRect(GLuint texture, float x, float y, GLRect &rect, bool forceFixAspectOff = false);

    void gl_drawRect(GLfloat r, GLfloat g, GLfloat b, GLfloat a, float x, float y, GLRect &rect);

    void gl_drawLines(GLfloat x, GLfloat y, GLLines &lines);

    void gl_setFixAspect(bool v);

    bool gl_getFixAspect();

    void gl_color4f(GLfloat r, GLfloat g, GLfloat b, GLfloat a);

    void gl_color4f(uint32_t rgb, GLfloat a);

    // Current colour last set via gl_color4f (used to e.g. scale alpha relative to it).
    void gl_getColor(GLfloat &r, GLfloat &g, GLfloat &b, GLfloat &a);

    void gl_color3f(uint32_t rgb);

    void gl_clearColor(GLfloat r, GLfloat g, GLfloat b, GLfloat a);

    void gl_loadIdentity();

    void gl_scalef(GLfloat x, GLfloat y, GLfloat z);

    void gl_translatef(GLfloat x, GLfloat y, GLfloat z);

    void gl_disable(GLuint v);

    void gl_enable(GLuint v);

    void gl_scissor(GLint x, GLint y, GLint width, GLint height);

    GLuint loadTextureFromPNG(std::string filename, int &width, int &height, std::vector <FontInfo> * = NULL);

    void png_zip_read(png_structp png_ptr, png_bytep data, png_size_t length);

    void clearGlTexCache();

    void setTextureNumberStart(int start);

    GLuint getNextTexNum();
/*
#ifdef USE_GLES2
extern "C"
{
    void glColor4f( GLfloat r, GLfloat g, GLfloat b, GLfloat a );
    void glLoadIdentity();
    void glScalef( GLfloat x, GLfloat y, GLfloat z );
    void glTranslatef( GLfloat x, GLfloat y, GLfloat z );
}
#endif
*/

}


#endif
