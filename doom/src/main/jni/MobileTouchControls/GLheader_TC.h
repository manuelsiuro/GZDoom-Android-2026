//
//  GLheader.h
//  touchcontrols
//
//  Created by Emile on 05/12/2015.
//  Copyright © 2015 Beloko games. All rights reserved.
//

#ifndef GLheader_h
#define GLheader_h


typedef float GLfloat;
typedef unsigned int GLuint;
typedef int GLint;
typedef unsigned int GLenum;
typedef int GLsizei;
typedef char GLchar;
typedef void GLvoid;
typedef unsigned char GLboolean;
typedef unsigned char GLubyte;

#define GL_BLEND                0x0BE2
#define GL_SCISSOR_TEST         0x0C11
#define GL_FLOAT                0x1406
#define GL_TEXTURE_2D           0x0DE1
#define GL_TEXTURE0                0x84C0
#define GL_TRIANGLE_FAN         0x0006
#define GL_LINES                0x0001
#define GL_RGBA                    0x1908
#define GL_UNSIGNED_BYTE        0x1401
#define GL_TEXTURE_MAG_FILTER   0x2800
#define GL_TEXTURE_MIN_FILTER   0x2801
#define GL_NEAREST              0x2600
#define GL_LINEAR               0x2601
#define GL_TRIANGLE_STRIP       0x0005

#define GL_TRUE 1
#define GL_INFO_LOG_LENGTH 0x8B84
#define GL_COMPILE_STATUS 0x8B81
#define GL_VERTEX_SHADER 0x8B31
#define GL_FRAGMENT_SHADER 0x8B30

#define GL_ARRAY_BUFFER                   0x8892
#define GL_ELEMENT_ARRAY_BUFFER           0x8893
#define GL_UNIFORM_BUFFER                 0x8A11
#define GL_FRAMEBUFFER                    0x8D40
#define GL_RENDERBUFFER                   0x8D41
#define GL_SRC_ALPHA                      0x0302
#define GL_ONE_MINUS_SRC_ALPHA            0x0303
#define GL_MATRIX_MODE                    0x0BA0
#define GL_MODELVIEW                     0x1700
#define GL_PROJECTION                     0x1701
#define GL_MODELVIEW_MATRIX               0x0BA6
#define GL_PROJECTION_MATRIX              0x0BA7
#define GL_ALPHA_TEST                     0x0BC0
#define GL_DEPTH_TEST                     0x0B71
#define GL_CULL_FACE                      0x0B44

#define GL_DRAW_FRAMEBUFFER               0x8CA9
#define GL_READ_FRAMEBUFFER               0x8CA8
#define GL_COLOR_ATTACHMENT0              0x8CE0
#define GL_DEPTH_ATTACHMENT               0x8D00
#define GL_STENCIL_ATTACHMENT             0x8D20
#define GL_DEPTH_COMPONENT16              0x81A5
#define GL_DEPTH_COMPONENT24              0x81A6
#define GL_DEPTH_COMPONENT32              0x81A7
#define GL_STENCIL_INDEX8                 0x8D48
#define GL_FRAMEBUFFER_COMPLETE           0x8CD5
#define GL_EXTENSIONS                     0x1F03


#define GL_VERTEX_ARRAY 0x8074
#define GL_NORMAL_ARRAY 0x8075
#define GL_COLOR_ARRAY 0x8076
#define GL_TEXTURE_COORD_ARRAY 0x8078
#define GL_TEXTURE_ENV_MODE 0x2200
#define GL_TEXTURE_ENV_COLOR 0x2201
#define GL_TEXTURE_ENV 0x2300
#define GL_MODULATE 0x2100
#define GL_REPLACE 0x1E01
#endif
