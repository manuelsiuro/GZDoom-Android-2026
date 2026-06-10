/*
Emile Belanger
GPL3
*/

#include "GLheader_TC.h"
#include "Framebuffer.h"
#include "OpenGLUtils.h"

#include <stdint.h>
#include <dlfcn.h>
#include <string.h>

#ifdef __ANDROID__
#include <android/log.h>
#define LOG(...)  __android_log_print(ANDROID_LOG_INFO,"FrameBuffer",__VA_ARGS__)
#endif

namespace touchcontrols
{
    static void (*gl4es_flush)();

    typedef unsigned int GLbitfield;
#define GL_COLOR_BUFFER_BIT 0x00004000

    static GLuint m_framebuffer = -1;
    static GLuint m_depthbuffer;
    static GLuint m_stencilbuffer;

    static int m_framebuffer_width, m_framebuffer_height;
    static GLuint m_framebuffer_texture;

    static GLuint m_positionLoc;
    static GLuint m_texCoordLoc;
    static GLuint m_samplerLoc;

    static GLuint r_program;

    static fbConfig m_fb_config = {0};

    static int m_glesVersion = 1;
    static bool m_useGL4ES = false;

#define CODEGEN_FUNCPTR

    static GLuint (CODEGEN_FUNCPTR *_ptrc_glCreateShader)(GLenum type);

#define glCreateShader _ptrc_glCreateShader

    static void (CODEGEN_FUNCPTR *_ptrc_glShaderSource)(GLuint shader, GLsizei count, const GLchar *const *string, const GLint *length);

#define glShaderSource _ptrc_glShaderSource

    static void (CODEGEN_FUNCPTR *_ptrc_glCompileShader)(GLuint shader);

#define glCompileShader _ptrc_glCompileShader

    static void (CODEGEN_FUNCPTR *_ptrc_glGetShaderInfoLog)(GLuint shader, GLsizei bufSize, GLsizei *length, GLchar *infoLog);

#define glGetShaderInfoLog _ptrc_glGetShaderInfoLog

    static void (CODEGEN_FUNCPTR *_ptrc_glGetShaderiv)(GLuint shader, GLenum pname, GLint *params);

#define glGetShaderiv _ptrc_glGetShaderiv

    static void (CODEGEN_FUNCPTR *_ptrc_glAttachShader)(GLuint program, GLuint shader);

#define glAttachShader _ptrc_glAttachShader

    static GLint (CODEGEN_FUNCPTR *_ptrc_glGetUniformLocation)(GLuint program, const GLchar *name);

#define glGetUniformLocation _ptrc_glGetUniformLocation

    static void (CODEGEN_FUNCPTR *_ptrc_glUseProgram)(GLuint program);

#define glUseProgram _ptrc_glUseProgram

    static GLuint (CODEGEN_FUNCPTR *_ptrc_glCreateProgram)(void);

#define glCreateProgram _ptrc_glCreateProgram

    static void (CODEGEN_FUNCPTR *_ptrc_glLinkProgram)(GLuint program);

#define glLinkProgram _ptrc_glLinkProgram

    static GLint (CODEGEN_FUNCPTR *_ptrc_glGetAttribLocation)(GLuint program, const GLchar *name);

#define glGetAttribLocation _ptrc_glGetAttribLocation

    static void (CODEGEN_FUNCPTR *_ptrc_glGetProgramiv)(GLuint program, GLenum pname, GLint *params);

#define glGetProgramiv _ptrc_glGetProgramiv

    static void (CODEGEN_FUNCPTR *_ptrc_glGetProgramInfoLog)(GLuint program, GLsizei bufSize, GLsizei *length, GLchar *infoLog);

#define glGetProgramInfoLog _ptrc_glGetProgramInfoLog

    static void (CODEGEN_FUNCPTR *_ptrc_glUniform1i)(GLint location, GLint v0);

#define glUniform1i _ptrc_glUniform1i

    static void (CODEGEN_FUNCPTR *_ptrc_glTexParameteri)(GLenum target, GLenum pname, GLint param);

#define glTexParameteri _ptrc_glTexParameteri

    static void
    (CODEGEN_FUNCPTR *_ptrc_glTexImage2D)(GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type,
                                          const void *pixels);

#define glTexImage2D _ptrc_glTexImage2D

    static void (CODEGEN_FUNCPTR *_ptrc_glGenFramebuffers)(GLsizei n, GLuint *framebuffers);

#define glGenFramebuffers _ptrc_glGenFramebuffers

    static void (CODEGEN_FUNCPTR *_ptrc_glGenRenderbuffers)(GLsizei n, GLuint *renderbuffers);

#define glGenRenderbuffers _ptrc_glGenRenderbuffers

    static void (CODEGEN_FUNCPTR *_ptrc_glGenTextures)(GLsizei n, GLuint *textures);

#define glGenTextures _ptrc_glGenTextures

    static void (CODEGEN_FUNCPTR *_ptrc_glBindTexture)(GLenum target, GLuint texture);

#define glBindTexture _ptrc_glBindTexture

    static GLenum (CODEGEN_FUNCPTR *_ptrc_glCheckFramebufferStatus)(GLenum target);

#define glCheckFramebufferStatus _ptrc_glCheckFramebufferStatus

    static void (CODEGEN_FUNCPTR *_ptrc_glBindRenderbuffer)(GLenum target, GLuint renderbuffer);

#define glBindRenderbuffer _ptrc_glBindRenderbuffer

    static void (CODEGEN_FUNCPTR *_ptrc_glRenderbufferStorage)(GLenum target, GLenum internalformat, GLsizei width, GLsizei height);

#define glRenderbufferStorage _ptrc_glRenderbufferStorage

    static void (CODEGEN_FUNCPTR *_ptrc_glBindFramebuffer)(GLenum target, GLuint framebuffer);

#define glBindFramebuffer _ptrc_glBindFramebuffer

    static void (CODEGEN_FUNCPTR *_ptrc_glFramebufferTexture2D)(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level);

#define glFramebufferTexture2D _ptrc_glFramebufferTexture2D

    static void (CODEGEN_FUNCPTR *_ptrc_glFramebufferRenderbuffer)(GLenum target, GLenum attachment, GLenum renderbuffertarget, GLuint renderbuffer);

#define glFramebufferRenderbuffer _ptrc_glFramebufferRenderbuffer

    static void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribPointer)(GLuint index, GLint size, GLenum type, GLboolean normalized, GLsizei stride, const void *pointer);

#define glVertexAttribPointer _ptrc_glVertexAttribPointer

    static void (CODEGEN_FUNCPTR *_ptrc_glEnableVertexAttribArray)(GLuint index);

#define glEnableVertexAttribArray _ptrc_glEnableVertexAttribArray

    static void (CODEGEN_FUNCPTR *_ptrc_glDisableVertexAttribArray)(GLuint index);

#define glDisableVertexAttribArray _ptrc_glDisableVertexAttribArray

    static void (CODEGEN_FUNCPTR *_ptrc_glViewport)(GLint x, GLint y, GLsizei width, GLsizei height);

#define glViewport _ptrc_glViewport

    static void (CODEGEN_FUNCPTR *_ptrc_glDisable)(GLenum cap);

#define glDisable _ptrc_glDisable

    static void (CODEGEN_FUNCPTR *_ptrc_glEnable)(GLenum cap);

#define glEnable _ptrc_glEnable

    static void (CODEGEN_FUNCPTR *_ptrc_glDrawArrays)(GLenum mode, GLint first, GLsizei count);

#define glDrawArrays _ptrc_glDrawArrays

    static const GLubyte *(CODEGEN_FUNCPTR *_ptrc_glGetString)(GLenum name);

#define glGetString _ptrc_glGetString

    static void (CODEGEN_FUNCPTR *_ptrc_glVertexPointer)(GLint size, GLenum type, GLsizei stride, const void *pointer);

#define glVertexPointer _ptrc_glVertexPointer

    static void (CODEGEN_FUNCPTR *_ptrc_glTexCoordPointer)(GLint size, GLenum type, GLsizei stride, const void *pointer);

#define glTexCoordPointer _ptrc_glTexCoordPointer

    static void (CODEGEN_FUNCPTR *_ptrc_glScalef)(GLfloat x, GLfloat y, GLfloat z);

#define glScalef _ptrc_glScalef

    static void (CODEGEN_FUNCPTR *_ptrc_glClear)(GLbitfield mask);

#define glClear _ptrc_glClear

    static void (CODEGEN_FUNCPTR *_ptrc_glClearColor)(GLfloat red, GLfloat green, GLfloat blue, GLfloat alpha);

#define glClearColor _ptrc_glClearColor

    static void (CODEGEN_FUNCPTR *_ptrc_glBlendFunc)(GLenum sfactor, GLenum dfactor);

#define glBlendFunc _ptrc_glBlendFunc

    static void (CODEGEN_FUNCPTR *_ptrc_glBindBuffer)(GLenum target, GLuint buffer);

#define glBindBuffer _ptrc_glBindBuffer

    static void (CODEGEN_FUNCPTR *_ptrc_glLoadMatrixf)(const GLfloat *m);

#define glLoadMatrixf _ptrc_glLoadMatrixf

    static void (CODEGEN_FUNCPTR *_ptrc_glMatrixMode)(GLenum mode);

#define glMatrixMode _ptrc_glMatrixMode

    static void (CODEGEN_FUNCPTR *_ptrc_glLoadIdentity)(void);

#define glLoadIdentity _ptrc_glLoadIdentity

    static void (CODEGEN_FUNCPTR *_ptrc_glGetFloatv)(GLenum pname, GLfloat *data);

#define glGetFloatv _ptrc_glGetFloatv

    static void (CODEGEN_FUNCPTR *_ptrc_glGetIntegerv)(GLenum pname, GLint *data);

#define glGetIntegerv _ptrc_glGetIntegerv

    static void (CODEGEN_FUNCPTR *_ptrc_glOrthof)(GLfloat left, GLfloat right, GLfloat bottom, GLfloat top, GLfloat zNear, GLfloat zFar);

#define glOrthof _ptrc_glOrthof

    static void (CODEGEN_FUNCPTR *_ptrc_glDisableClientState)(GLenum ren_array);

#define glDisableClientState _ptrc_glDisableClientState

    static void (CODEGEN_FUNCPTR *_ptrc_glEnableClientState)(GLenum ren_array);

#define glEnableClientState _ptrc_glEnableClientState

    static void (CODEGEN_FUNCPTR *_ptrc_glTexEnvf)(GLenum target, GLenum pname, GLfloat param);

#define glTexEnvf _ptrc_glTexEnvf

    static void (CODEGEN_FUNCPTR *_ptrc_glBindSampler)(GLuint unit, GLuint sampler);

#define glBindSampler _ptrc_glBindSampler

    static void (CODEGEN_FUNCPTR *_ptrc_glBindVertexArray)(GLuint ren_array);

#define glBindVertexArray _ptrc_glBindVertexArray

    static void (CODEGEN_FUNCPTR *_ptrc_glActiveTexture)(GLenum texture);

#define glActiveTexture _ptrc_glActiveTexture

    static void (CODEGEN_FUNCPTR *_ptrc_glColor4f)(GLfloat red, GLfloat green, GLfloat blue, GLfloat alpha);

#define glColor4f _ptrc_glColor4f

    static void *glesLib = NULL;

    static void *loadGlesFunc(const char *name)
    {
        void *ret = NULL;
        ret = dlsym(glesLib, name);

        if(!ret)
        {
            LOG("Failed to load: %s", name);
        }
        else
        {
            LOG("Loaded %s func OK", name);
        }

        return ret;
    }

    static void loadGles(bool useGL4ES, bool isGles2)
    {
        int flags = RTLD_LOCAL | RTLD_NOW;

        if(!isGles2)
        {
            glesLib = dlopen("libGLESv1_CM.so", flags);

            if(!glesLib)
            {
                glesLib = dlopen("libGLES_CM.so", flags);
            }
        }
        else if(useGL4ES)
        {
            glesLib = dlopen("libGL4ES.so", flags);
            gl4es_flush = (void (*)()) loadGlesFunc("gl4es_flush");
        }
        else
        {
            glesLib = dlopen("libGLESv2_CM.so", flags);

            if(!glesLib)
            {
                glesLib = dlopen("libGLESv2.so", flags);
            }
        }

        _ptrc_glDisable = (void (CODEGEN_FUNCPTR *)(GLenum cap)) loadGlesFunc("glDisable");
        _ptrc_glEnable = (void (CODEGEN_FUNCPTR *)(GLenum cap)) loadGlesFunc("glEnable");
        _ptrc_glBindTexture = (void (CODEGEN_FUNCPTR *)(GLenum target, GLuint texture)) loadGlesFunc("glBindTexture");
        _ptrc_glDrawArrays = (void (CODEGEN_FUNCPTR *)(GLenum mode, GLint first, GLsizei count)) loadGlesFunc("glDrawArrays");
        _ptrc_glGenTextures = (void (CODEGEN_FUNCPTR *)(GLsizei n, GLuint *textures)) loadGlesFunc("glGenTextures");
        _ptrc_glTexImage2D = (void (CODEGEN_FUNCPTR *)(GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLint border, GLenum format,
                                                       GLenum type, const void *pixels)) loadGlesFunc("glTexImage2D");
        _ptrc_glTexParameteri = (void (CODEGEN_FUNCPTR *)(GLenum target, GLenum pname, GLint param)) loadGlesFunc("glTexParameteri");
        _ptrc_glBindFramebuffer = (void (CODEGEN_FUNCPTR *)(GLenum target, GLuint framebuffer)) loadGlesFunc("glBindFramebuffer");
        _ptrc_glViewport = (void (CODEGEN_FUNCPTR *)(GLint x, GLint y, GLsizei width, GLsizei height)) loadGlesFunc("glViewport");
        _ptrc_glGetString = (const GLubyte *(CODEGEN_FUNCPTR *)(GLenum name)) loadGlesFunc("glGetString");
        _ptrc_glClear = (void (CODEGEN_FUNCPTR *)(GLbitfield mask)) loadGlesFunc("glClear");
        _ptrc_glClearColor = (void (CODEGEN_FUNCPTR *)(GLfloat red, GLfloat green, GLfloat blue, GLfloat alpha)) loadGlesFunc("glClearColor");
        _ptrc_glBindBuffer = (void (CODEGEN_FUNCPTR *)(GLenum target, GLuint buffer)) loadGlesFunc("glBindBuffer");
        _ptrc_glBlendFunc = (void (CODEGEN_FUNCPTR *)(GLenum sfactor, GLenum dfactor)) loadGlesFunc("glBlendFunc");

        if(!isGles2) // ES 1
        {
            _ptrc_glLoadMatrixf = (void (CODEGEN_FUNCPTR *)(const GLfloat *m)) loadGlesFunc("glLoadMatrixf");
            _ptrc_glMatrixMode = (void (CODEGEN_FUNCPTR *)(GLenum mode)) loadGlesFunc("glMatrixMode");
            _ptrc_glLoadIdentity = (void (CODEGEN_FUNCPTR *)(void)) loadGlesFunc("glLoadIdentity");

            _ptrc_glVertexPointer = (void (CODEGEN_FUNCPTR *)(GLint size, GLenum type, GLsizei stride, const void *pointer)) loadGlesFunc("glVertexPointer");
            _ptrc_glTexCoordPointer = (void (CODEGEN_FUNCPTR *)(GLint size, GLenum type, GLsizei stride, const void *pointer)) loadGlesFunc("glTexCoordPointer");

            _ptrc_glGenRenderbuffers = (void (CODEGEN_FUNCPTR *)(GLsizei n, GLuint *renderbuffers)) loadGlesFunc("glGenRenderbuffersOES");
            _ptrc_glCheckFramebufferStatus = (GLenum(CODEGEN_FUNCPTR *)(GLenum target)) loadGlesFunc("glCheckFramebufferStatusOES");
            _ptrc_glGenFramebuffers = (void (CODEGEN_FUNCPTR *)(GLsizei n, GLuint *framebuffers)) loadGlesFunc("glGenFramebuffersOES");
            _ptrc_glBindRenderbuffer = (void (CODEGEN_FUNCPTR *)(GLenum target, GLuint renderbuffer)) loadGlesFunc("glBindRenderbufferOES");
            _ptrc_glBindFramebuffer = (void (CODEGEN_FUNCPTR *)(GLenum target, GLuint framebuffer)) loadGlesFunc("glBindFramebufferOES");
            _ptrc_glFramebufferTexture2D = (void (CODEGEN_FUNCPTR *)(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level)) loadGlesFunc(
                    "glFramebufferTexture2DOES");
            _ptrc_glFramebufferRenderbuffer = (void (CODEGEN_FUNCPTR *)(GLenum target, GLenum attachment, GLenum renderbuffertarget, GLuint renderbuffer)) loadGlesFunc(
                    "glFramebufferRenderbufferOES");
            _ptrc_glRenderbufferStorage = (void (CODEGEN_FUNCPTR *)(GLenum target, GLenum internalformat, GLsizei width, GLsizei height)) loadGlesFunc(
                    "glRenderbufferStorageOES");
            _ptrc_glScalef = (void (CODEGEN_FUNCPTR *)(GLfloat x, GLfloat y, GLfloat z)) loadGlesFunc("glScalef");
            _ptrc_glGetFloatv = (void (CODEGEN_FUNCPTR *)(GLenum pname, GLfloat *data)) loadGlesFunc("glGetFloatv");
            _ptrc_glGetIntegerv = (void (CODEGEN_FUNCPTR *)(GLenum pname, GLint *data)) loadGlesFunc("glGetIntegerv");
            _ptrc_glOrthof = (void (CODEGEN_FUNCPTR *)(GLfloat left, GLfloat right, GLfloat bottom, GLfloat top, GLfloat zNear, GLfloat zFar)) loadGlesFunc("glOrthof");
            _ptrc_glDisableClientState = (void (CODEGEN_FUNCPTR *)(GLenum ren_array)) loadGlesFunc("glDisableClientState");
            _ptrc_glEnableClientState = (void (CODEGEN_FUNCPTR *)(GLenum ren_array)) loadGlesFunc("glEnableClientState");
            _ptrc_glTexEnvf = (void (CODEGEN_FUNCPTR *)(GLenum target, GLenum pname, GLfloat param)) loadGlesFunc("glTexEnvf");
            _ptrc_glColor4f = (void (CODEGEN_FUNCPTR *)(GLfloat red, GLfloat green, GLfloat blue, GLfloat alpha)) loadGlesFunc("glColor4f");
        }
        else // ES 2
        {
            _ptrc_glCreateShader = (GLuint(CODEGEN_FUNCPTR *)(GLenum type)) loadGlesFunc("glCreateShader");
            _ptrc_glShaderSource = (void (CODEGEN_FUNCPTR *)(GLuint shader, GLsizei count, const GLchar *const *string, const GLint *length)) loadGlesFunc(
                    "glShaderSource");
            _ptrc_glCompileShader = (void (CODEGEN_FUNCPTR *)(GLuint shader)) loadGlesFunc("glCompileShader");
            _ptrc_glGetShaderInfoLog = (void (CODEGEN_FUNCPTR *)(GLuint shader, GLsizei bufSize, GLsizei *length, GLchar *infoLog)) loadGlesFunc("glGetShaderInfoLog");
            _ptrc_glCreateProgram = (GLuint(CODEGEN_FUNCPTR *)(void)) loadGlesFunc("glCreateProgram");
            _ptrc_glLinkProgram = (void (CODEGEN_FUNCPTR *)(GLuint program)) loadGlesFunc("glLinkProgram");
            _ptrc_glGetAttribLocation = (GLint(CODEGEN_FUNCPTR *)(GLuint program, const GLchar *name)) loadGlesFunc("glGetAttribLocation");
            _ptrc_glGetShaderiv = (void (CODEGEN_FUNCPTR *)(GLuint shader, GLenum pname, GLint *params)) loadGlesFunc("glGetShaderiv");
            _ptrc_glAttachShader = (void (CODEGEN_FUNCPTR *)(GLuint program, GLuint shader)) loadGlesFunc("glAttachShader");
            _ptrc_glGetUniformLocation = (GLint(CODEGEN_FUNCPTR *)(GLuint program, const GLchar *name)) loadGlesFunc("glGetUniformLocation");
            _ptrc_glUseProgram = (void (CODEGEN_FUNCPTR *)(GLuint program)) loadGlesFunc("glUseProgram");
            _ptrc_glVertexAttribPointer = (void (CODEGEN_FUNCPTR *)(GLuint index, GLint size, GLenum type, GLboolean normalized, GLsizei stride,
                                                                    const void *pointer)) loadGlesFunc("glVertexAttribPointer");
            _ptrc_glEnableVertexAttribArray = (void (CODEGEN_FUNCPTR *)(GLuint index)) loadGlesFunc("glEnableVertexAttribArray");
            _ptrc_glDisableVertexAttribArray = (void (CODEGEN_FUNCPTR *)(GLuint index)) loadGlesFunc("glDisableVertexAttribArray");
            _ptrc_glUniform1i = (void (CODEGEN_FUNCPTR *)(GLint location, GLint v0)) loadGlesFunc("glUniform1i");
            _ptrc_glGetProgramInfoLog = (void (CODEGEN_FUNCPTR *)(GLuint program, GLsizei bufSize, GLsizei *length, GLchar *infoLog)) loadGlesFunc(
                    "glGetProgramInfoLog");
            _ptrc_glGetProgramiv = (void (CODEGEN_FUNCPTR *)(GLuint program, GLenum pname, GLint *params)) loadGlesFunc("glGetProgramiv");
            _ptrc_glGenFramebuffers = (void (CODEGEN_FUNCPTR *)(GLsizei n, GLuint *framebuffers)) loadGlesFunc("glGenFramebuffers");
            _ptrc_glGenRenderbuffers = (void (CODEGEN_FUNCPTR *)(GLsizei n, GLuint *renderbuffers)) loadGlesFunc("glGenRenderbuffers");
            _ptrc_glCheckFramebufferStatus = (GLenum(CODEGEN_FUNCPTR *)(GLenum target)) loadGlesFunc("glCheckFramebufferStatus");
            _ptrc_glBindRenderbuffer = (void (CODEGEN_FUNCPTR *)(GLenum target, GLuint renderbuffer)) loadGlesFunc("glBindRenderbuffer");
            _ptrc_glRenderbufferStorage = (void (CODEGEN_FUNCPTR *)(GLenum target, GLenum internalformat, GLsizei width, GLsizei height)) loadGlesFunc(
                    "glRenderbufferStorage");
            _ptrc_glBindFramebuffer = (void (CODEGEN_FUNCPTR *)(GLenum target, GLuint framebuffer)) loadGlesFunc("glBindFramebuffer");
            _ptrc_glFramebufferTexture2D = (void (CODEGEN_FUNCPTR *)(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level)) loadGlesFunc(
                    "glFramebufferTexture2D");
            _ptrc_glFramebufferRenderbuffer = (void (CODEGEN_FUNCPTR *)(GLenum target, GLenum attachment, GLenum renderbuffertarget, GLuint renderbuffer)) loadGlesFunc(
                    "glFramebufferRenderbuffer");
            _ptrc_glActiveTexture = (void (CODEGEN_FUNCPTR *)(GLenum texture)) loadGlesFunc("glActiveTexture");
            _ptrc_glBindSampler = (void (CODEGEN_FUNCPTR *)(GLuint unit, GLuint sampler)) loadGlesFunc("glBindSampler");
            _ptrc_glBindVertexArray = (void (CODEGEN_FUNCPTR *)(GLuint ren_array)) loadGlesFunc("glBindVertexArray");;
        }

    }

    static GLint matrixMode;
    static GLfloat projection[16];
    static GLfloat model[16];

    void gl_startRender()
    {
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_BLEND);
        glDisable(GL_CULL_FACE);

        // Need this otherwise GLES2 mode for GZDoom v1.9.1 controls are blank
        if(m_useGL4ES)
            glDisable(GL_ALPHA_TEST);

        if(m_glesVersion == 1)
        {
            glGetIntegerv(GL_MATRIX_MODE, &matrixMode);
            glGetFloatv(GL_PROJECTION_MATRIX, projection);
            glGetFloatv(GL_MODELVIEW_MATRIX, model);

            glDisableClientState(GL_COLOR_ARRAY);
            glEnableClientState(GL_VERTEX_ARRAY);
            glEnableClientState(GL_TEXTURE_COORD_ARRAY);

            glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            glEnable(GL_TEXTURE_2D);
            glDisable(GL_ALPHA_TEST);

            // State for the Frame buffer
            glMatrixMode(GL_PROJECTION);
            glLoadIdentity();
            glMatrixMode(GL_MODELVIEW);
            glLoadIdentity();

            gl_color4f(1, 1, 1, 1);
            R_FrameBufferEnd();

            // State for the touch controls
            glMatrixMode(GL_PROJECTION);
            glLoadIdentity();
            glViewport(0, 0, (int) touchcontrols::GLScaleWidth, -(int) touchcontrols::GLScaleHeight);
            glOrthof(0.0f, (int) touchcontrols::GLScaleWidth, -(int) touchcontrols::GLScaleHeight, 0.0f, -1.0f, 1.0f);
            glMatrixMode(GL_MODELVIEW);
            glLoadIdentity();
        }
        else if(m_glesVersion == 2)
        {
            glActiveTexture(GL_TEXTURE0);
            glDisable(GL_DEPTH_TEST);
            glViewport(0, 0, GLScaleWidth, -GLScaleHeight);

            R_FrameBufferEnd();
        }
        else if(m_glesVersion == 3)
        {
            glActiveTexture(GL_TEXTURE0);
            glDisable(GL_DEPTH_TEST);

            glViewport(0, 0, GLScaleWidth, -GLScaleHeight);
            glBindBuffer(GL_UNIFORM_BUFFER, 0);
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
            glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);
            glBindSampler(0, 0);
            glBindVertexArray(0);

            R_FrameBufferEnd();
        }
    }

    void gl_endRender()
    {
        R_FrameBufferStart();

        if(fb_getGLESVersion() == 1)
        {
            glMatrixMode(GL_MODELVIEW);
            glLoadMatrixf(model);
            glMatrixMode(GL_PROJECTION);
            glLoadMatrixf(projection);
            glMatrixMode(matrixMode);
        }
    }

    void gl_setupForSDLSW()
    {
        glDisable(GL_BLEND);
        glColor4f(1, 1, 1, 1);
        glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
    }

    void gl_resetGL4ES()
    {
        glUseProgram(0);
        // This is a hack to force GL4ES to draw the remaining draw call
        glBindTexture(GL_TEXTURE_2D, 0);

        //glDrawArrays( GL_TRIANGLE_FAN, 0, 4 );
        if(gl4es_flush)
            gl4es_flush();
    }

    static int fixNpot(int v)
    {
        int ret = 1;

        while(ret < v)
            ret <<= 1;

        return ret;
    }

    static int loadShader(int shaderType, const char *source)
    {
        int shader = glCreateShader(shaderType);

        if(shader != 0)
        {
            glShaderSource(shader, 1, &source, NULL);
            glCompileShader(shader);

            GLint length;

            glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &length);

            if(length)
            {
                char *buffer = new char[length];
                glGetShaderInfoLog(shader, length, NULL, buffer);
                LOG("shader = %s\n", buffer);
                delete[] buffer;

                GLint success;
                glGetShaderiv(shader, GL_COMPILE_STATUS, &success);

                if(success != GL_TRUE)
                {
                    LOG("ERROR compiling shader\n");
                }
            }
        }
        else
        {
            LOG("FAILED to create shader");
        }

        return shader;
    }

    static int createProgram(const char *vertexSource, const char *fragmentSource)
    {
        int vertexShader = loadShader(GL_VERTEX_SHADER, vertexSource);
        int pixelShader = loadShader(GL_FRAGMENT_SHADER, fragmentSource);

        int program = glCreateProgram();

        if(program != 0)
        {
            glAttachShader(program, vertexShader);
            // checkGlError("glAttachShader");
            glAttachShader(program, pixelShader);
            // checkGlError("glAttachShader");
            glLinkProgram(program);
#define GL_LINK_STATUS 0x8B82
            int linkStatus[1];
            glGetProgramiv(program, GL_LINK_STATUS, linkStatus);

            if(linkStatus[0] != GL_TRUE)
            {
                LOG("Could not link program: ");
                char log[256];
                GLsizei size;
                glGetProgramInfoLog(program, 256, &size, log);
                LOG("Log: %s", log);
                //glDeleteProgram(program);
                program = 0;
            }

        }
        else
        {
            LOG("FAILED to create program");
        }

        LOG("Program linked OK %d", program);
        return program;
    }

    static void createShaders(void)
    {
        const GLchar *vertSource = \
                               "attribute vec4 a_position;                                     \n \
			attribute vec2 a_texCoord;                                     \n \
			varying vec2 v_texCoord;                                       \n \
			void main()                                                    \n \
			{                                                              \n \
			   gl_Position = a_position;                                   \n \
			   v_texCoord = a_texCoord;                                    \n \
			}                                                              \n \
			";

        const GLchar *fragSource = \
                               "precision mediump float;                                \n  \
			varying vec2 v_texCoord;                                 \n  \
			uniform sampler2D s_texture;                             \n  \
			void main()                                              \n  \
			{                                                        \n  \
				gl_FragColor =  texture2D( s_texture, v_texCoord );  \n  \
				//gl_FragColor =  vec4( 0.8, 0.5, 0.9, 1.0 );  \n  \
			}                                                        \n  \
			";


        r_program = createProgram(vertSource, fragSource);

        glUseProgram(r_program);

        // get attrib locations
        m_positionLoc = glGetAttribLocation(r_program, "a_position");
        m_texCoordLoc = glGetAttribLocation(r_program, "a_texCoord");
        m_samplerLoc = glGetUniformLocation(r_program, "s_texture");

        if(m_positionLoc == -1)
            LOG("Failed to get m_positionLoc");

        if(m_texCoordLoc == -1)
            LOG("Failed to get m_texCoordLoc");

        if(m_samplerLoc == -1)
            LOG("Failed to get m_samplerLoc");

        glUniform1i(m_samplerLoc, 0);
    }

    static bool checkExtension(const char *name)
    {
        const char *exts = (const char *) glGetString(GL_EXTENSIONS);

        if(!strstr(exts, name))
        {
            LOG("%s FALSE", name);
            return false;
        }
        else
        {
            LOG("%s TRUE", name);
            return true;
        }
    }

    void R_FrameBufferConfig(fbConfig config)
    {
        m_fb_config = config;
    }

    void R_FrameBufferSetRenderer(bool useGL4ES, int glesVersion)
    {
        m_glesVersion = glesVersion;
        m_useGL4ES = useGL4ES;
    }

    int fb_getGLESVersion()
    {
        return m_glesVersion;
    }

    void R_FrameBufferInit()
    {
        LOG("GLES = %d, GL4ES = %d, Real[%d, %d] -> Framebuffer[%d,%d]. Maint aspect : %d", m_glesVersion, m_useGL4ES, m_fb_config.vidWidthReal,
            m_fb_config.vidHeightReal, m_fb_config.vidWidth, m_fb_config.vidHeight, m_fb_config.maintainAspect);

        // Always load this
        loadGles(m_useGL4ES, m_glesVersion > 1);

        // This runs on every new EGL surface/context (e.g. device rotation),
        // so always forget the previous FBO: on a fresh context the handle is
        // stale, and after rotating back to a non-letterboxed orientation it
        // must not be bound again. (If the context survived, the old FBO is
        // left to die with it - no delete pointers are loaded here.)
        m_framebuffer = -1;

        if(m_fb_config.vidWidthReal == 0 || m_fb_config.vidHeightReal == 0 || m_fb_config.vidWidth == 0 || m_fb_config.vidHeight == 0)
        {
            LOG("ERROR, framebuffer config not set");
            return;
        }

        if(m_fb_config.vidWidthReal == m_fb_config.vidWidth && m_fb_config.vidHeightReal == m_fb_config.vidHeight)
        {
            LOG("Not using framebuffer");
            return;
        }

        float aspectReal = (float) m_fb_config.vidWidthReal / (float) m_fb_config.vidHeightReal;
        float aspectFb = (float) m_fb_config.vidWidth / (float) m_fb_config.vidHeight;

        if(m_fb_config.maintainAspect && fabs(aspectReal - aspectFb) < 0.01)
        {
            LOG("Aspect is close enough, disabling aspect correction");
            m_fb_config.maintainAspect = false;
        }

        m_fb_config.isGles2 = m_glesVersion > 1;

        m_fb_config.npotAvailable = checkExtension("GL_OES_texture_npot");
        m_fb_config.depthStencilAvailable = checkExtension("GL_OES_packed_depth_stencil");
        m_fb_config.filter = 0;

        m_framebuffer_width = m_fb_config.vidWidth;
        m_framebuffer_height = m_fb_config.vidHeight;

        if(!m_fb_config.npotAvailable)
        {
            m_framebuffer_width = fixNpot(m_framebuffer_width);
            m_framebuffer_height = fixNpot(m_framebuffer_height);
        }

        LOG("Framebuffer buffer size = [%d, %d]", m_framebuffer_width, m_framebuffer_height);

        // Create texture
        GLuint tex;
        glGenTextures(1, &tex);
        m_framebuffer_texture = tex;

        glBindTexture(GL_TEXTURE_2D, m_framebuffer_texture);
        LOG("Framebuffer TEXTURE = %d", m_framebuffer_texture);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, m_framebuffer_width, m_framebuffer_height, 0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);

        if(m_fb_config.filter == false)
        {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }
        else
        {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        }

        // Create framebuffer
        glGenFramebuffers(1, &m_framebuffer);

        // Create renderbuffer
        glGenRenderbuffers(1, &m_depthbuffer);
        glBindRenderbuffer(GL_RENDERBUFFER, m_depthbuffer);

        if(m_fb_config.depthStencilAvailable)
        {
#define GL_DEPTH24_STENCIL8_OES    0x88F0
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8_OES, m_framebuffer_width, m_framebuffer_height);
            //glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, m_framebuffer_width, m_framebuffer_height);
        }
        else
        {
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, m_framebuffer_width, m_framebuffer_height);

            // Need separate Stencil buffer
            glGenRenderbuffers(1, &m_stencilbuffer);
            glBindRenderbuffer(GL_RENDERBUFFER, m_stencilbuffer);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_STENCIL_INDEX8, m_framebuffer_width, m_framebuffer_height);
        }

        glBindTexture(GL_TEXTURE_2D, 0);

        if(m_fb_config.isGles2)
            createShaders();
    }

    void R_FrameBufferStart()
    {
        if(m_framebuffer == -1)
            return;

        // Reset viewport to the framebuffer size
        glViewport(0, 0, m_fb_config.vidWidth, m_fb_config.vidHeight);

        // Render to framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, m_framebuffer);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, m_framebuffer_texture, 0);

        // Attach combined depth+stencil
        if(m_fb_config.depthStencilAvailable)
        {
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, m_depthbuffer);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, m_depthbuffer);
        }
        else
        {
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, m_depthbuffer);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, m_stencilbuffer);
        }

        GLenum result = glCheckFramebufferStatus(GL_FRAMEBUFFER);

        if(result != GL_FRAMEBUFFER_COMPLETE)
        {
            LOG("ERROR binding Framebuffer: %d\n", result);
        }
    }


    void R_FrameBufferEnd()
    {
        if(m_framebuffer == -1)
            return;

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);

        // Bind the texture
        glBindTexture(GL_TEXTURE_2D, m_framebuffer_texture);

        GLfloat smax = 1;
        GLfloat tmax = 1;

        if(!m_fb_config.npotAvailable)
        {
            smax = (float) m_fb_config.vidWidth / (float) m_framebuffer_width;
            tmax = (float) m_fb_config.vidHeight / (float) m_framebuffer_height;
        }

        float left = -1;
        float right = 1;
        float top = 1;
        float bottom = -1;

        if(m_fb_config.maintainAspect)
        {
            float realRatio = (float) m_fb_config.vidWidthReal / (float) m_fb_config.vidHeightReal;
            float fbRatio = (float) m_fb_config.vidWidth / (float) m_fb_config.vidHeight;

            float xScale = fbRatio / realRatio;
            float yScale = realRatio / fbRatio;

            if(xScale < 1)
            {
                left = -xScale;
                right = xScale;
            }
            else
            {
                top = yScale;
                bottom = -yScale;
            }

            glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);
        }

        GLfloat vert[] =
                {
                        left, bottom, 0.0f,  // 0. left-bottom
                        left, top, 0.0f,  // 1. left-top
                        right, top, 0.0f,    // 2. right-top
                        right, bottom, 0.0f,   // 3. right-bottom
                };

        GLfloat texVert[] =
                {
                        0.0f, 0.0f, // TexCoord 0
                        0.0f, tmax, // TexCoord 1
                        smax, tmax, // TexCoord 2
                        smax, 0.0f  // TexCoord 3
                };

        if(m_fb_config.isGles2)
        {
            glUseProgram(r_program);
            // Set the sampler texture unit to 0
            glUniform1i(m_samplerLoc, 0);

            glVertexAttribPointer(m_positionLoc, 3, GL_FLOAT,
                                  false,
                                  3 * 4,
                                  vert);

            glVertexAttribPointer(m_texCoordLoc, 2, GL_FLOAT,
                                  false,
                                  2 * 4,
                                  texVert);

            glEnableVertexAttribArray(m_positionLoc);
            glEnableVertexAttribArray(m_texCoordLoc);

            glViewport(0, 0, m_fb_config.vidWidthReal, m_fb_config.vidHeightReal);

            glDisable(GL_BLEND);
            glDisable(GL_SCISSOR_TEST);
            glDisable(GL_DEPTH_TEST);

            glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

            glEnable(GL_BLEND);
        }
        else // GLES 1
        {
            glVertexPointer(3, GL_FLOAT, 0, vert);
            glTexCoordPointer(2, GL_FLOAT, 0, texVert);

            glViewport(0, 0, m_fb_config.vidWidthReal, m_fb_config.vidHeightReal);

            glDisable(GL_BLEND);
            glDisable(GL_SCISSOR_TEST);
            glDisable(GL_DEPTH_TEST);

            glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

            glEnable(GL_BLEND);
        }

        glBindTexture(GL_TEXTURE_2D, 0);
    }
}