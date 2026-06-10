
# include <GLES/gl.h>
#include "jwzglesI.h"

#include <android/log.h>
#define LOG_TAG "JWZGLES"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

#define LOGI(...)

//#define  __MULTITEXTURE_SUPPORT__

typedef struct
{
    float x;
    float y;
    float z;
#if !defined(__MULTITEXTURE_SUPPORT__)
    float padding;
#endif

#if COLOR_BYTE
    unsigned char red;
    unsigned char green;
    unsigned char blue;
    unsigned char alpha;
#else
    float red;
    float green;
    float blue;
    float alpha;
#endif

    float s;
    float t;
#if defined(__MULTITEXTURE_SUPPORT__)
    float s_multi;
    float t_multi;
#endif
} VertexAttrib;

static GLenum wrapperPrimitiveMode = GL_QUADS;
GLboolean useTexCoordArray = GL_FALSE;

#define SIZE_VERTEXATTRIBS 200000
static VertexAttrib vertexattribs[SIZE_VERTEXATTRIBS];

#define SIZE_INDEXARRAY ( SIZE_VERTEXATTRIBS * 4 )
static GLushort indexArray[SIZE_INDEXARRAY];

static GLuint vertexCount = 0;
static GLuint indexCount = 0;
static GLuint vertexMark = 0;
static int indexbase = 0;

static VertexAttrib* ptrVertexAttribArray = NULL;
static VertexAttrib* ptrVertexAttribArrayMark = NULL;

static VertexAttrib currentVertexAttrib = {0};

static GLushort* ptrIndexArray = NULL;

static int glBegin_active = 0;

void FlushOnStateChange()
{
    //LOGI("FlushOnStateChange");
    /*
    	if (delayedttmuchange)
    	{
    		LOGI("FlushOnStateChange delayedttmuchange");
    		delayedttmuchange = GL_FALSE;
    		GLESIMPL glActiveTexture(delayedtmutarget);
    		CHECKGLERROR;
    	}
    */
    if (!vertexCount)
        return;

    LOGI("FlushOnStateChange drawing %d", vertexCount);


    //if (!arraysValid)
    {
        //LOGI("FlushOnStateChange arrays NOT valid");

        glClientActiveTexture(GL_TEXTURE0);

        if( state->element_array_buffer != 0 )
        {
            glBindBuffer (GL_ELEMENT_ARRAY_BUFFER, 0);
            state->element_array_buffer = 0;
        }

        if( state->array_buffer != 0 )
        {
            glBindBuffer (GL_ARRAY_BUFFER, 0);
            state->array_buffer = 0;
        }

        if( !state->vertPrtValid )
        {
            if (wrapperPrimitiveMode == GL_LINES)
                glVertexPointer(2, GL_FLOAT, sizeof(VertexAttrib), &vertexattribs[0].x);
            else
                glVertexPointer(3, GL_FLOAT, sizeof(VertexAttrib), &vertexattribs[0].x);

            state->vertPrtValid = 1;
        }

        if( !state->colorPtrValid )
        {
            glColorPointer(4, GL_FLOAT, sizeof(VertexAttrib), &vertexattribs[0].red);
            state->colorPtrValid = 1;
        }

        if( !state->texPrtValid )
        {
            glTexCoordPointer(2, GL_FLOAT, sizeof(VertexAttrib), &vertexattribs[0].s);
            state->texPrtValid = 1;
        }

        if( !(state->enabled & ISENABLED_VERT_ARRAY) )
        {
            glEnableClientState(GL_VERTEX_ARRAY);
        }

        if( !(state->enabled & ISENABLED_TEX_ARRAY) )
        {
            glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        }

        if( !(state->enabled & ISENABLED_COLOR_ARRAY) )
        {
            glEnableClientState(GL_COLOR_ARRAY);
        }

#if defined(__MULTITEXTURE_SUPPORT__)
        glClientActiveTexture(GL_TEXTURE1);

        glTexCoordPointer(2, GL_FLOAT, sizeof(VertexAttrib), &vertexattribs[0].s_multi);

        glEnableClientState(GL_TEXTURE_COORD_ARRAY);

        glClientActiveTexture(GL_TEXTURE0);

#endif
        //arraysValid = GL_TRUE;
    }


#if CHECK_OVERFLOW
    if ( ptrVertexAttribArray >= vertexattribs + SIZE_VERTEXATTRIBS)
    {
        FatalError("vertexattribs overflow\n");
    }
    if ( ptrIndexArray >= indexArray + SIZE_INDEXARRAY)
    {
        FatalError("indexArray overflow\n");
    }
#endif

    //LOGI("FlushOnStateChange draw ");
    //glEnable(GL_DEPTH_TEST) ;
    //glClear(GL_DEPTH_BUFFER_BIT);

    if (wrapperPrimitiveMode == GL_LINES)
    {
        glDrawElements( GL_LINES,vertexCount,GL_UNSIGNED_SHORT, indexArray );
    }
    else if (wrapperPrimitiveMode == GL_QUADS)
    {
        glDrawElements( GL_TRIANGLES,vertexCount,GL_UNSIGNED_SHORT, indexArray );
    }
    else
        glDrawElements( GL_TRIANGLES,vertexCount,GL_UNSIGNED_SHORT, indexArray );


    if( !(state->enabled & ISENABLED_VERT_ARRAY) )
    {
        glDisableClientState(GL_VERTEX_ARRAY);
    }

    if( !(state->enabled & ISENABLED_TEX_ARRAY) )
    {
        glDisableClientState(GL_TEXTURE_COORD_ARRAY);
    }

    if( !(state->enabled & ISENABLED_COLOR_ARRAY) )
    {
        glDisableClientState(GL_COLOR_ARRAY);
    }

/*
    if( state->element_array_buffer != 0 )
        glBindBuffer (GL_ELEMENT_ARRAY_BUFFER, state->element_array_buffer);
*/

    if( state->array_buffer != 0 )
    {
        //glBindBuffer (GL_ARRAY_BUFFER, state->array_buffer);
    }

    vertexCount = 0;
    indexCount = 0;
    ptrVertexAttribArray = vertexattribs;
    ptrVertexAttribArrayMark = ptrVertexAttribArray;
    ptrIndexArray = indexArray;
    useTexCoordArray = GL_FALSE;
}

void
jwzgles_glBegin_OVERRIDE(int mode)
{
    wrapperPrimitiveMode = mode;
}

void
jwzgles_glBegin(int mode)
{
    LOGI("glBegin mode = %d, vcount = %d, icount = %d", mode,vertexCount,indexCount);
    static int first = 1;

    glBegin_active = 1;

    if(first)
    {
        first = 0;
        wrapperPrimitiveMode = mode;
        vertexCount = 0;
        indexCount = 0;
        ptrVertexAttribArray = vertexattribs;
        ptrVertexAttribArrayMark = ptrVertexAttribArray;
        ptrIndexArray = indexArray;
    }
    else
    {
        wrapperPrimitiveMode = mode;
        vertexMark = vertexCount;
        ptrVertexAttribArrayMark = ptrVertexAttribArray;
        indexbase = indexCount;
    }
}


void jwzgles_glEnd(void)
{
    int count ;

    LOGI("glEnd");

    glBegin_active = 0;

    vertexCount+=((unsigned char*)ptrVertexAttribArray-(unsigned char*)ptrVertexAttribArrayMark)/sizeof(VertexAttrib);
    if (vertexCount < ((wrapperPrimitiveMode == GL_LINES)?2:3))
    {
        return;
    }
    switch (wrapperPrimitiveMode)
    {
    case GL_LINES:
    {
        int  vcount = (vertexCount-vertexMark)/2;
        for ( count = 0; count < vcount; count++)
        {
            *ptrIndexArray++ = indexCount;
            *ptrIndexArray++ = indexCount+1;
            indexCount+=2;
        }
        //indexCount+=4;
        //vertexCount+=2;
    }
    break;
    case GL_QUADS:
    {
        int i;
        int  vcount = (vertexCount-vertexMark);
        for (i = 0; i < vcount * 3 ; i+=6 )
        {
            int q = i / 6 * 4;
            ptrIndexArray[ i + 0 ] = q + 0;
            ptrIndexArray[ i + 1 ] = q + 1;
            ptrIndexArray[ i + 2 ] = q + 2;

            ptrIndexArray[ i + 3 ] = q + 0;
            ptrIndexArray[ i + 4 ] = q + 2;
            ptrIndexArray[ i + 5 ] = q + 3;

            indexCount+=4;
        }
        vertexCount =  vcount / 4 * 6;
    }
    break;
    /*
    case GL_QUAD_STRIP:
    {
        int i;
        int  vcount = (vertexCount-vertexMark);
        for (i = 0; i < vcount * 3 ; i+=6 )
        {
            int q = i / 6 * 4;
            ptrIndexArray[ i + 0 ] = q + 0;
            ptrIndexArray[ i + 1 ] = q + 1;
            ptrIndexArray[ i + 2 ] = q + 2;

            ptrIndexArray[ i + 3 ] = q + 0;
            ptrIndexArray[ i + 4 ] = q + 2;
            ptrIndexArray[ i + 5 ] = q + 3;

            indexCount+=4;
        }
        vertexCount =  vcount / 4 * 6;
    }
    break;
    */
    case GL_TRIANGLES:
    {
        int  vcount = (vertexCount-vertexMark)/3;
        for ( count = 0; count < vcount; count++)
        {
            *ptrIndexArray++ = indexCount;
            *ptrIndexArray++ = indexCount+1;
            *ptrIndexArray++ = indexCount+2;
            indexCount+=3;
        }
    }
    break;
    case GL_TRIANGLE_STRIP:
    {
        *ptrIndexArray++ = indexCount;
        *ptrIndexArray++ = indexCount+1;
        *ptrIndexArray++ = indexCount+2;
        indexCount+=3;
        int vcount = ((vertexCount-vertexMark)-3);

        if (vcount && ((long)ptrIndexArray & 0x02))
        {
            *ptrIndexArray++ = indexCount-1; // 2
            *ptrIndexArray++ = indexCount-2; // 1
            *ptrIndexArray++ = indexCount;   // 3
            indexCount++;
            vcount-=1;

            // bullshit optimization ....

            int odd = vcount&1;
            vcount/=2;
            unsigned int* longptr = (unsigned int*) ptrIndexArray;

            for ( count = 0; count < vcount; count++)
            {
                *(longptr++) = (indexCount-2) | ((indexCount-1)<<16);
                *(longptr++) = (indexCount) | ((indexCount)<<16);
                *(longptr++) = (indexCount-1) | ((indexCount+1)<<16);
                indexCount+=2;
            }
            ptrIndexArray = (unsigned short*)(longptr);


            if (odd)
            {
                *ptrIndexArray++ = indexCount-2; // 2
                *ptrIndexArray++ = indexCount-1; // 1
                *ptrIndexArray++ = indexCount;   // 3
                indexCount++;
            }
        }
        else
        {
            //already aligned
            int odd = vcount&1;
            vcount/=2;
            unsigned int* longptr = (unsigned int*) ptrIndexArray;

            for ( count = 0; count < vcount; count++)
            {
                *(longptr++) = (indexCount-1) | ((indexCount-2)<<16);
                *(longptr++) = (indexCount) | ((indexCount-1)<<16);
                *(longptr++) = (indexCount) | ((indexCount+1)<<16);
                indexCount+=2;

            }
            ptrIndexArray = (unsigned short*)(longptr);
            if (odd)
            {

                *ptrIndexArray++ = indexCount-1; // 2
                *ptrIndexArray++ = indexCount-2; // 1
                *ptrIndexArray++ = indexCount;   // 3
                indexCount++;
            }
        }
        vertexCount+=(vertexCount-vertexMark-3)*2;
    }


    break;
    case GL_POLYGON:
    case GL_TRIANGLE_FAN:
    //case GL_QUAD_STRIP:
    {

        *ptrIndexArray++ = indexCount++;
        *ptrIndexArray++ = indexCount++;
        *ptrIndexArray++ = indexCount++;
        int vcount = ((vertexCount-vertexMark)-3);
        for ( count = 0; count < vcount; count++)
        {
            *ptrIndexArray++ = indexbase;
            *ptrIndexArray++ = indexCount-1;
            *ptrIndexArray++ = indexCount++;
            vertexCount+=2;
        }
    }
    break;

    default:
        break;
    }

    // flush after glEnd()
    if (wrapperPrimitiveMode == GL_LINES)
        FlushOnStateChange(); //For gzdoom automap

    //FlushOnStateChange(); //TEST
}


void jwzgles_glVertex4fv (const GLfloat *v)
{
    currentVertexAttrib.x = v[0];
    currentVertexAttrib.y = v[1];
    currentVertexAttrib.z = v[2];

    ptrVertexAttribArray->x = currentVertexAttrib.x;
    ptrVertexAttribArray->y = currentVertexAttrib.y;
    ptrVertexAttribArray->z = currentVertexAttrib.z;

    ptrVertexAttribArray->red = currentVertexAttrib.red;
    ptrVertexAttribArray->green = currentVertexAttrib.green;
    ptrVertexAttribArray->blue = currentVertexAttrib.blue;
    ptrVertexAttribArray->alpha = currentVertexAttrib.alpha;

    ptrVertexAttribArray->s = currentVertexAttrib.s;
    ptrVertexAttribArray->t = currentVertexAttrib.t;
#if defined(__MULTITEXTURE_SUPPORT__)
    ptrVertexAttribArray->s_multi = currentVertexAttrib.s_multi;
    ptrVertexAttribArray->t_multi = currentVertexAttrib.t_multi;
#endif
    if(ptrVertexAttribArray < vertexattribs + SIZE_VERTEXATTRIBS)
        ptrVertexAttribArray ++;
}

void
jwzgles_glTexCoord4fv (const GLfloat *v)
{
    currentVertexAttrib.s = v[0];
    currentVertexAttrib.t = v[1];
#if defined(__MULTITEXTURE_SUPPORT__)
    currentVertexAttrib.s_multi = v[2];
    currentVertexAttrib.t_multi = v[3];
#endif
}

void
jwzgles_glColor4fv (const GLfloat *v)
{
    currentVertexAttrib.red = v[0];
    currentVertexAttrib.green =  v[1];
    currentVertexAttrib.blue =  v[2];
    currentVertexAttrib.alpha =  v[3];

    if(!glBegin_active)
        glColor4f (v[0], v[1], v[2], v[3]);
}