#include "TouchControlsConfig.h"
#include "OpenGLUtils.h"
#include "GLRect.h"

using namespace touchcontrols;

GLRect::GLRect()
{
    for(int n = 0; n < 12; n++)
        vertices[n] = 0;

    GLfloat texture_[] =
            {
                    // Mapping coordinates for the vertices
                    0.0, -1.0,        // top left		(V2)
                    0.0, 0.0,        // bottom left	(V1)
                    1.0, -1.0,        // top right	(V4)
                    1.0, 0.0        // bottom right	(V3)
            };

    for(int n = 0; n < 8; n++)
        texture[n] = texture_[n];
}

void GLRect::resize(float w, float h)
{
    width = w;
    height = h;

    if(gl_getGLESVersion() >= 2)
        vertices[4] = GLESscaleY(-height);
    else
        vertices[1] = GLESscaleY(-height);

    vertices[6] = GLESscaleX(width);
    vertices[7] = GLESscaleY(-height);
    vertices[9] = GLESscaleX(width);
};

#define SWAP(A, B) t = texture[A]; texture[A] = texture[B]; texture[B] = t;

void GLRect::mirror(bool v, bool h)
{
    if(v)
    {
        GLfloat t;
        SWAP(0, 2)
        SWAP(1, 3)

        SWAP(4, 6)
        SWAP(5, 7)
    }

    if(h)
    {

    }
}


void GLRect::rotate90(bool ccw)
{
    if(!ccw)   // clockwise
    {
        GLfloat t1, t2;
        t1 = texture[0];
        t2 = texture[1];
        texture[0] = texture[4];
        texture[1] = texture[5];

        texture[4] = texture[6];
        texture[5] = texture[7];

        texture[6] = texture[2];
        texture[7] = texture[3];

        texture[2] = t1;
        texture[3] = t2;
    }
    else // counter clockwise
    {
        GLfloat t1, t2;
        t1 = texture[0];
        t2 = texture[1];
        texture[0] = texture[2];
        texture[1] = texture[3];

        texture[2] = texture[6];
        texture[3] = texture[7];

        texture[6] = texture[4];
        texture[7] = texture[5];

        texture[4] = t1;
        texture[5] = t2;
    }
}