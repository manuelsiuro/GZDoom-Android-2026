#include "OpenGLUtils.h"
#include <map>
#include <dlfcn.h>
#include "Framebuffer.h"

#ifdef USE_GLES2
//#include <GLES2/gl2.h>
#else
//#include <GLES/gl.h>
#endif

static bool initGlesDone = false;

// Handle to a program object
static int mProgramObject;
static int mProgramObjectColor;

// Attribute locations
static int mPositionLoc;
static int mTexCoordLoc;

// Sampler location
static int mSamplerLoc;
static int mColorLoc;
static int mPositionTranslateLoc;
static int mModelMatrixLoc;

static int mPositionLocColor;
static int mColorLocColor;
static int mPositionTranslateLocColor;
static int mModelMatrixColorLoc;

static float mModelMatrixGLSL[16] = {1, 0, 0, 0,
                                     0, 1, 0, 0,
                                     0, 0, 1, 0,
                                     0, 0, 0, 1
};
#define CODEGEN_FUNCPTR

// COMMON -------------------------------------------------
void (CODEGEN_FUNCPTR *_ptrc_glClearColor)(GLfloat red, GLfloat green, GLfloat blue, GLfloat alpha);

#define glClearColor _ptrc_glClearColor

void (CODEGEN_FUNCPTR *_ptrc_glDisable)(GLenum cap);

#define glDisable _ptrc_glDisable

void (CODEGEN_FUNCPTR *_ptrc_glEnable)(GLenum cap);

#define glEnable _ptrc_glEnable

void (CODEGEN_FUNCPTR *_ptrc_glScissor)(GLint x, GLint y, GLsizei width, GLsizei height);

#define glScissor _ptrc_glScissor

void (CODEGEN_FUNCPTR *_ptrc_glBindTexture)(GLenum target, GLuint texture);

#define glBindTexture _ptrc_glBindTexture

void (CODEGEN_FUNCPTR *_ptrc_glDrawArrays)(GLenum mode, GLint first, GLsizei count);

#define glDrawArrays _ptrc_glDrawArrays

void (CODEGEN_FUNCPTR *_ptrc_glGenTextures)(GLsizei n, GLuint *textures);

#define glGenTextures _ptrc_glGenTextures

void
(CODEGEN_FUNCPTR *_ptrc_glTexImage2D)(GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type,
                                      const void *pixels);

#define glTexImage2D _ptrc_glTexImage2D

void (CODEGEN_FUNCPTR *_ptrc_glTexParameteri)(GLenum target, GLenum pname, GLint param);

#define glTexParameteri _ptrc_glTexParameteri

void (CODEGEN_FUNCPTR *_ptrc_glDepthMask)(GLboolean flag);

#define glDepthMask _ptrc_glDepthMask

void (CODEGEN_FUNCPTR *_ptrc_glViewport)(GLint x, GLint y, GLsizei width, GLsizei height);

#define glViewport _ptrc_glViewport

// GLES1 -------------------------------------------------
void (CODEGEN_FUNCPTR *_ptrc_glColor4f)(GLfloat red, GLfloat green, GLfloat blue, GLfloat alpha);

#define glColor4f _ptrc_glColor4f

void (CODEGEN_FUNCPTR *_ptrc_glLoadIdentity)(void);

#define glLoadIdentity _ptrc_glLoadIdentity

void (CODEGEN_FUNCPTR *_ptrc_glScalef)(GLfloat x, GLfloat y, GLfloat z);

#define glScalef _ptrc_glScalef

void (CODEGEN_FUNCPTR *_ptrc_glTranslatef)(GLfloat x, GLfloat y, GLfloat z);

#define glTranslatef _ptrc_glTranslatef

void (CODEGEN_FUNCPTR *_ptrc_glPushMatrix)(void);

#define glPushMatrix _ptrc_glPushMatrix

void (CODEGEN_FUNCPTR *_ptrc_glVertexPointer)(GLint size, GLenum type, GLsizei stride, const void *pointer);

#define glVertexPointer _ptrc_glVertexPointer

void (CODEGEN_FUNCPTR *_ptrc_glTexCoordPointer)(GLint size, GLenum type, GLsizei stride, const void *pointer);

#define glTexCoordPointer _ptrc_glTexCoordPointer

void (CODEGEN_FUNCPTR *_ptrc_glPopMatrix)(void);

#define glPopMatrix _ptrc_glPopMatrix

void (CODEGEN_FUNCPTR *_ptrc_glOrthof)(GLfloat left, GLfloat right, GLfloat bottom, GLfloat top, GLfloat zNear, GLfloat zFar);

#define glOrthof _ptrc_glOrthof

void (CODEGEN_FUNCPTR *_ptrc_glDisableClientState)(GLenum ren_array);

#define glDisableClientState _ptrc_glDisableClientState

void (CODEGEN_FUNCPTR *_ptrc_glEnableClientState)(GLenum ren_array);

#define glEnableClientState _ptrc_glEnableClientState

void (CODEGEN_FUNCPTR *_ptrc_glTexEnvf)(GLenum target, GLenum pname, GLfloat param);

#define glTexEnvf _ptrc_glTexEnvf

// GLES2 -------------------------------------------------
GLuint (CODEGEN_FUNCPTR *_ptrc_glCreateShader)(GLenum type);

#define glCreateShader _ptrc_glCreateShader

void (CODEGEN_FUNCPTR *_ptrc_glShaderSource)(GLuint shader, GLsizei count, const GLchar *const *string, const GLint *length);

#define glShaderSource _ptrc_glShaderSource

void (CODEGEN_FUNCPTR *_ptrc_glCompileShader)(GLuint shader);

#define glCompileShader _ptrc_glCompileShader

void (CODEGEN_FUNCPTR *_ptrc_glGetShaderInfoLog)(GLuint shader, GLsizei bufSize, GLsizei *length, GLchar *infoLog);

#define glGetShaderInfoLog _ptrc_glGetShaderInfoLog

GLuint (CODEGEN_FUNCPTR *_ptrc_glCreateProgram)(void);

#define glCreateProgram _ptrc_glCreateProgram

void (CODEGEN_FUNCPTR *_ptrc_glLinkProgram)(GLuint program);

#define glLinkProgram _ptrc_glLinkProgram

GLint (CODEGEN_FUNCPTR *_ptrc_glGetAttribLocation)(GLuint program, const GLchar *name);

#define glGetAttribLocation _ptrc_glGetAttribLocation

void (CODEGEN_FUNCPTR *_ptrc_glGetShaderiv)(GLuint shader, GLenum pname, GLint *params);

#define glGetShaderiv _ptrc_glGetShaderiv

void (CODEGEN_FUNCPTR *_ptrc_glAttachShader)(GLuint program, GLuint shader);

#define glAttachShader _ptrc_glAttachShader

GLint (CODEGEN_FUNCPTR *_ptrc_glGetUniformLocation)(GLuint program, const GLchar *name);

#define glGetUniformLocation _ptrc_glGetUniformLocation

void (CODEGEN_FUNCPTR *_ptrc_glUseProgram)(GLuint program);

#define glUseProgram _ptrc_glUseProgram

void (CODEGEN_FUNCPTR *_ptrc_glEnableVertexAttribArray)(GLuint index);

#define glEnableVertexAttribArray _ptrc_glEnableVertexAttribArray

void (CODEGEN_FUNCPTR *_ptrc_glUniform4f)(GLint location, GLfloat v0, GLfloat v1, GLfloat v2, GLfloat v3);

#define glUniform4f _ptrc_glUniform4f

void (CODEGEN_FUNCPTR *_ptrc_glUniform1i)(GLint location, GLint v0);

#define glUniform1i _ptrc_glUniform1i

void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribPointer)(GLuint index, GLint size, GLenum type, GLboolean normalized, GLsizei stride, const void *pointer);

#define glVertexAttribPointer _ptrc_glVertexAttribPointer

void (CODEGEN_FUNCPTR *_ptrc_glActiveTexture)(GLenum texture);

#define glActiveTexture _ptrc_glActiveTexture

void (CODEGEN_FUNCPTR *_ptrc_glBindBuffer)(GLenum target, GLuint buffer);

#define glBindBuffer _ptrc_glBindBuffer

void (CODEGEN_FUNCPTR *_ptrc_glBindFramebuffer)(GLenum target, GLuint framebuffer);

#define glBindFramebuffer _ptrc_glBindFramebuffer

void (CODEGEN_FUNCPTR *_ptrc_glBlendFunc)(GLenum sfactor, GLenum dfactor);

#define glBlendFunc _ptrc_glBlendFunc

void (CODEGEN_FUNCPTR *_ptrc_glLoadMatrixf)(const GLfloat *m);

#define glLoadMatrixf _ptrc_glLoadMatrixf

void (CODEGEN_FUNCPTR *_ptrc_glMatrixMode)(GLenum mode);

#define glMatrixMode _ptrc_glMatrixMode

void (CODEGEN_FUNCPTR *_ptrc_glGetFloatv)(GLenum pname, GLfloat *data);

#define glGetFloatv _ptrc_glGetFloatv

void (CODEGEN_FUNCPTR *_ptrc_glGetIntegerv)(GLenum pname, GLint *data);

#define glGetIntegerv _ptrc_glGetIntegerv

void (CODEGEN_FUNCPTR *_ptrc_glGetProgramiv)(GLuint program, GLenum pname, GLint *params);

#define glGetProgramiv _ptrc_glGetProgramiv

void (CODEGEN_FUNCPTR *_ptrc_glGetProgramInfoLog)(GLuint program, GLsizei bufSize, GLsizei *length, GLchar *infoLog);

#define glGetProgramInfoLog _ptrc_glGetProgramInfoLog

void (CODEGEN_FUNCPTR *_ptrc_glBindSampler)(GLuint unit, GLuint sampler);

#define glBindSampler _ptrc_glBindSampler

void (CODEGEN_FUNCPTR *_ptrc_glBindVertexArray)(GLuint ren_array);

#define glBindVertexArray _ptrc_glBindVertexArray

void (CODEGEN_FUNCPTR *_ptrc_glUniformMatrix4fv)(GLint location, GLsizei count, GLboolean transpose, const GLfloat *value);

#define glUniformMatrix4fv _ptrc_glUniformMatrix4fv


void (*gl4es_flush)();

static void *glesLib = NULL;

static bool useGL4ES = false;

static void *loadGlesFunc(const char *name)
{
    void *ret = NULL;
    ret = dlsym(glesLib, name);

    if(!ret)
    {
        LOGTOUCH("Failed to load: %s", name);
    }
    else
    {
        // LOGTOUCH("Loaded %s func OK", name);
    }

    return ret;
}

static void loadGles(int version)
{
    int flags = RTLD_LOCAL | RTLD_NOW;
    //int flags = RTLD_NOLOAD | RTLD_GLOBAL;

    if(useGL4ES)
    {
        LOGTOUCH("Loading GL4ES");

        glesLib = dlopen("libGL4ES.so", flags);
        gl4es_flush = (void (*)()) loadGlesFunc("gl4es_flush");
    }
    else if(version == 1)
    {
        LOGTOUCH("Loading GLES 1");

        glesLib = dlopen("libGLESv1_CM.so", flags);

        if(!glesLib)
        {
            glesLib = dlopen("libGLES_CM.so", flags);
        }
    }
    else
    {
        LOGTOUCH("Loading GLES 2");

        glesLib = dlopen("libGLESv2_CM.so", flags);

        //glesLib = dlopen("libGLESv3.so", flags);
        if(!glesLib)
        {
            glesLib = dlopen("libGLESv2.so", flags);
        }
    }

    if(glesLib)
    {
        // Common
        _ptrc_glClearColor = (void (CODEGEN_FUNCPTR *)(GLfloat red, GLfloat green, GLfloat blue, GLfloat alpha)) loadGlesFunc("glClearColor");
        _ptrc_glDisable = (void (CODEGEN_FUNCPTR *)(GLenum cap)) loadGlesFunc("glDisable");
        _ptrc_glEnable = (void (CODEGEN_FUNCPTR *)(GLenum cap)) loadGlesFunc("glEnable");
        _ptrc_glScissor = (void (CODEGEN_FUNCPTR *)(GLint x, GLint y, GLsizei width, GLsizei height)) loadGlesFunc("glScissor");
        _ptrc_glBindTexture = (void (CODEGEN_FUNCPTR *)(GLenum target, GLuint texture)) loadGlesFunc("glBindTexture");
        _ptrc_glDrawArrays = (void (CODEGEN_FUNCPTR *)(GLenum mode, GLint first, GLsizei count)) loadGlesFunc("glDrawArrays");
        _ptrc_glGenTextures = (void (CODEGEN_FUNCPTR *)(GLsizei n, GLuint *textures)) loadGlesFunc("glGenTextures");
        _ptrc_glTexImage2D = (void (CODEGEN_FUNCPTR *)(GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLint border,
                                                       GLenum format, GLenum type, const void *pixels)) loadGlesFunc("glTexImage2D");
        _ptrc_glTexParameteri = (void (CODEGEN_FUNCPTR *)(GLenum target, GLenum pname, GLint param)) loadGlesFunc("glTexParameteri");


        _ptrc_glBindBuffer = (void (CODEGEN_FUNCPTR *)(GLenum target, GLuint buffer)) loadGlesFunc("glBindBuffer");
        _ptrc_glBindFramebuffer = (void (CODEGEN_FUNCPTR *)(GLenum target, GLuint framebuffer)) loadGlesFunc("glBindFramebuffer");
        _ptrc_glBlendFunc = (void (CODEGEN_FUNCPTR *)(GLenum sfactor, GLenum dfactor)) loadGlesFunc("glBlendFunc");

        _ptrc_glDepthMask = (void (CODEGEN_FUNCPTR *)(GLboolean)) loadGlesFunc("glDepthMask");
        _ptrc_glViewport = (void (CODEGEN_FUNCPTR *)(GLint x, GLint y, GLsizei width, GLsizei height)) loadGlesFunc("glViewport");


        if(version == 1)
        {
            _ptrc_glColor4f = (void (CODEGEN_FUNCPTR *)(GLfloat red, GLfloat green, GLfloat blue, GLfloat alpha)) loadGlesFunc("glColor4f");
            _ptrc_glLoadIdentity = (void (CODEGEN_FUNCPTR *)(void)) loadGlesFunc("glLoadIdentity");
            _ptrc_glScalef = (void (CODEGEN_FUNCPTR *)(GLfloat x, GLfloat y, GLfloat z)) loadGlesFunc("glScalef");
            _ptrc_glTranslatef = (void (CODEGEN_FUNCPTR *)(GLfloat x, GLfloat y, GLfloat z)) loadGlesFunc("glTranslatef");
            _ptrc_glPushMatrix = (void (CODEGEN_FUNCPTR *)(void)) loadGlesFunc("glPushMatrix");
            _ptrc_glVertexPointer = (void (CODEGEN_FUNCPTR *)(GLint size, GLenum type, GLsizei stride, const void *pointer)) loadGlesFunc("glVertexPointer");
            _ptrc_glTexCoordPointer = (void (CODEGEN_FUNCPTR *)(GLint size, GLenum type, GLsizei stride, const void *pointer)) loadGlesFunc(
                    "glTexCoordPointer");
            _ptrc_glPopMatrix = (void (CODEGEN_FUNCPTR *)(void)) loadGlesFunc("glPopMatrix");
            _ptrc_glLoadMatrixf = (void (CODEGEN_FUNCPTR *)(const GLfloat *m)) loadGlesFunc("glLoadMatrixf");
            _ptrc_glMatrixMode = (void (CODEGEN_FUNCPTR *)(GLenum mode)) loadGlesFunc("glMatrixMode");
            _ptrc_glGetFloatv = (void (CODEGEN_FUNCPTR *)(GLenum pname, GLfloat *data)) loadGlesFunc("glGetFloatv");
            _ptrc_glGetIntegerv = (void (CODEGEN_FUNCPTR *)(GLenum pname, GLint *data)) loadGlesFunc("glGetIntegerv");
            _ptrc_glOrthof = (void (CODEGEN_FUNCPTR *)(GLfloat left, GLfloat right, GLfloat bottom, GLfloat top, GLfloat zNear, GLfloat zFar)) loadGlesFunc(
                    "glOrthof");
            _ptrc_glDisableClientState = (void (CODEGEN_FUNCPTR *)(GLenum ren_array)) loadGlesFunc("glDisableClientState");
            _ptrc_glEnableClientState = (void (CODEGEN_FUNCPTR *)(GLenum ren_array)) loadGlesFunc("glEnableClientState");
            _ptrc_glTexEnvf = (void (CODEGEN_FUNCPTR *)(GLenum target, GLenum pname, GLfloat param)) loadGlesFunc("glTexEnvf");
        }
        else // GLES 2
        {
            _ptrc_glCreateShader = (GLuint(CODEGEN_FUNCPTR *)(GLenum type)) loadGlesFunc("glCreateShader");
            _ptrc_glShaderSource = (void (CODEGEN_FUNCPTR *)(GLuint shader, GLsizei count, const GLchar *const *string, const GLint *length)) loadGlesFunc(
                    "glShaderSource");
            _ptrc_glCompileShader = (void (CODEGEN_FUNCPTR *)(GLuint shader)) loadGlesFunc("glCompileShader");
            _ptrc_glGetShaderInfoLog = (void (CODEGEN_FUNCPTR *)(GLuint shader, GLsizei bufSize, GLsizei *length, GLchar *infoLog)) loadGlesFunc(
                    "glGetShaderInfoLog");
            _ptrc_glCreateProgram = (GLuint(CODEGEN_FUNCPTR *)(void)) loadGlesFunc("glCreateProgram");
            _ptrc_glLinkProgram = (void (CODEGEN_FUNCPTR *)(GLuint program)) loadGlesFunc("glLinkProgram");
            _ptrc_glGetAttribLocation = (GLint(CODEGEN_FUNCPTR *)(GLuint program, const GLchar *name)) loadGlesFunc("glGetAttribLocation");
            _ptrc_glGetShaderiv = (void (CODEGEN_FUNCPTR *)(GLuint shader, GLenum pname, GLint *params)) loadGlesFunc("glGetShaderiv");
            _ptrc_glAttachShader = (void (CODEGEN_FUNCPTR *)(GLuint program, GLuint shader)) loadGlesFunc("glAttachShader");
            _ptrc_glGetUniformLocation = (GLint(CODEGEN_FUNCPTR *)(GLuint program, const GLchar *name)) loadGlesFunc("glGetUniformLocation");
            _ptrc_glUseProgram = (void (CODEGEN_FUNCPTR *)(GLuint program)) loadGlesFunc("glUseProgram");
            _ptrc_glEnableVertexAttribArray = (void (CODEGEN_FUNCPTR *)(GLuint index)) loadGlesFunc("glEnableVertexAttribArray");
            _ptrc_glUniform4f = (void (CODEGEN_FUNCPTR *)(GLint location, GLfloat v0, GLfloat v1, GLfloat v2, GLfloat v3)) loadGlesFunc("glUniform4f");
            _ptrc_glUniform1i = (void (CODEGEN_FUNCPTR *)(GLint location, GLint v0)) loadGlesFunc("glUniform1i");
            _ptrc_glVertexAttribPointer = (void (CODEGEN_FUNCPTR *)(GLuint index, GLint size, GLenum type, GLboolean normalized, GLsizei stride,
                                                                    const void *pointer)) loadGlesFunc("glVertexAttribPointer");
            _ptrc_glActiveTexture = (void (CODEGEN_FUNCPTR *)(GLenum texture)) loadGlesFunc("glActiveTexture");

            _ptrc_glGetProgramiv = (void (CODEGEN_FUNCPTR *)(GLuint program, GLenum pname, GLint *params)) loadGlesFunc("glGetProgramiv");
            _ptrc_glGetProgramInfoLog = (void (CODEGEN_FUNCPTR *)(GLuint program, GLsizei bufSize, GLsizei *length, GLchar *infoLog)) loadGlesFunc(
                    "glGetProgramInfoLog");;
            _ptrc_glBindSampler = (void (CODEGEN_FUNCPTR *)(GLuint unit, GLuint sampler)) loadGlesFunc("glBindSampler");

            _ptrc_glBindVertexArray = (void (CODEGEN_FUNCPTR *)(GLuint ren_array)) loadGlesFunc("glBindVertexArray");;

            _ptrc_glUniformMatrix4fv = (void (CODEGEN_FUNCPTR *)(GLint location, GLsizei count, GLboolean transpose, const GLfloat *value)) loadGlesFunc(
                    "glUniformMatrix4fv");

        }
    }
    else
    {
        LOGTOUCH("FAILED TO LOAD GLES LIB");
    }
}

namespace touchcontrols
{

    void gl_Init()
    {
        initGlesDone = false;
    }

    float GLScaleWidth = 400;
    float GLScaleHeight = -300;

    static int glesVersion = 1;

    static bool isGLES2 = false;

    static bool m_fixAspect = true;


    void gl_setGLESVersion(int v)
    {
        glesVersion = v;

        if(v >= 2)
            isGLES2 = true;
        else
            isGLES2 = false;
    }

    int gl_getGLESVersion()
    {
        return glesVersion;
    }

    void gl_setFixAspect(bool v)
    {
        m_fixAspect = v;
    }

    bool gl_getFixAspect()
    {
        return m_fixAspect;
    }

    float GLESscaleX(float X)
    {
        if(isGLES2)
            return X * 2;
        else
            return X;
    }

    float GLESscaleY(float Y)
    {
        if(isGLES2)
            return Y * 2;
        else
            return Y;
    }

    static float glTranslateX, glTranslateY, glTranslateZ;

    static float glColorR, glColorG, glColorB, glColorA;

    void gl_color4f(GLfloat r, GLfloat g, GLfloat b, GLfloat a)
    {
        glColorR = r;
        glColorG = g;
        glColorB = b;
        glColorA = a;

        if(isGLES2 == false)
        {
            glColor4f(r, g, b, a);
        }
    }

    void gl_color4f(uint32_t rgb, GLfloat a)
    {
        gl_color4f(((rgb >> 16) & 0xFF) / 255.f,
                   ((rgb >> 8) & 0xFF) / 255.f,
                   ((rgb) & 0xFF) / 255.f,
                   a);
    }

    void gl_color3f(uint32_t rgb)
    {
        gl_color4f(rgb, glColorA);
    }

    void gl_getColor(GLfloat &r, GLfloat &g, GLfloat &b, GLfloat &a)
    {
        r = glColorR;
        g = glColorG;
        b = glColorB;
        a = glColorA;
    }

    void gl_loadIdentity()
    {
        glTranslateX = glTranslateY = glTranslateZ = 0;

        if(isGLES2)
        {
            float ident[16] = {1, 0, 0, 0,
                               0, 1, 0, 0,
                               0, 0, 1, 0,
                               0, 0, 0, 1
            };
            memcpy(mModelMatrixGLSL, ident, sizeof(ident));
        }
        else
        {
            glLoadIdentity();
        }
    }

    void gl_scalef(GLfloat x, GLfloat y, GLfloat z)
    {
        if(isGLES2)
        {
            //mModelMatrixGLSL[0] = mModelMatrixGLSL[0] * x;
            //mModelMatrixGLSL[5] = mModelMatrixGLSL[5] * y;
            //mModelMatrixGLSL[10] = mModelMatrixGLSL[10] * z;
        }
        else
        {
            glScalef(x, y, z);
        }
    }

    void gl_translatef(GLfloat x, GLfloat y, GLfloat z)
    {
        glTranslateX += x;
        glTranslateY -= y;
        glTranslateZ += z;

        if(isGLES2 == false)
        {
            glTranslatef(x, y, z);
        }
    }


    void gl_disable(GLuint v)
    {
        glDisable(v);
    }

    void gl_enable(GLuint v)
    {
        glEnable(v);
    }

    void gl_scissor(GLint x, GLint y, GLint width, GLint height)
    {
        glScissor(x, y, width, height);
    }

    void gl_clearColor(GLfloat r, GLfloat g, GLfloat b, GLfloat a)
    {
        glClearColor(r, g, b, a);
    }


    static float translateX(float X)
    {
        return -(1 - X * 2);
    }

    static float translateY(float Y)
    {
        return (-Y * 2 + 1);
    }

    const char vShaderStr[] =
            "attribute vec4 a_position;                                     \n \
			attribute vec2 a_texCoord;                                     \n \
			varying vec2 v_texCoord;                                       \n \
			uniform vec4 u_translate;                                      \n \
			uniform mat4 u_modelMatrix;                                    \n \
			void main()                                                    \n \
			{                                                              \n \
			   //gl_Position = (a_position + u_translate);                   \n \
			   gl_Position = (u_modelMatrix * a_position) + u_translate; \n \
			   v_texCoord = a_texCoord;                                    \n \
			}                                                              \n \
			";

    const char fShaderStr[] =
            "precision highp float;                            \
			varying vec2 v_texCoord;                            \
			uniform sampler2D s_texture;                        \
		    uniform vec4 u_color;    \
			void main()                                         \
			{                                                   \
				vec4 alpha = texture2D( s_texture, v_texCoord ); \
				gl_FragColor = alpha*u_color;  \
			}                                                   \
			";

    const char fShaderColorStr[] =
            "precision highp float;                            \
		    uniform vec4 u_color;    \
			void main()                                         \
			{                                                   \
				gl_FragColor = u_color;  \
			}                                                   \
			";

    int loadShader(int shaderType, const char *source)
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
                LOGTOUCH("shader = %s\n", buffer);
                delete[] buffer;

                GLint success;
                glGetShaderiv(shader, GL_COMPILE_STATUS, &success);

                if(success != GL_TRUE)
                {
                    LOGTOUCH("ERROR compiling shader\n");
                }
            }
        }
        else
        {
            LOGTOUCH("FAILED to create shader");
        }

        return shader;
    }

    void gl_useGL4ES()
    {
        useGL4ES = true;
    }


    int createProgram(const char *vertexSource, const char *fragmentSource)
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
                LOGTOUCH("Could not link program: ");
                char log[256];
                GLsizei size;
                glGetProgramInfoLog(program, 256, &size, log);
                LOGTOUCH("Log: %s", log);
                //glDeleteProgram(program);
                program = 0;
            }

        }
        else
        {
            LOGTOUCH("FAILED to create program");
        }

        LOGTOUCH("Program linked OK %d", program);
        return program;
    }

    GLfloat mTexVertices[] =
            {
                    0.0f, 0.0f, // TexCoord 0
                    0.0f, -1.0f, // TexCoord 1
                    1.0f, -1.0f, // TexCoord 2
                    1.0f, 0.0f // TexCoord 3
            };

/*
GLfloat mTexVertices[] =
{
        0.0f, 0.0f, // TexCoord 0
        0.0f, 1.0f, // TexCoord 1
        1.0f, 1.0f, // TexCoord 2
        1.0f, 0.0f // TexCoord 3
};
 */


    static void initGLES2()
    {
        // Load the shaders and get a linked program object
        mProgramObject = createProgram(vShaderStr, fShaderStr);
        mProgramObjectColor = createProgram(vShaderStr, fShaderColorStr);

        // Get the attribute locations
        mPositionLoc = glGetAttribLocation(mProgramObject, "a_position");
        mTexCoordLoc = glGetAttribLocation(mProgramObject, "a_texCoord");

        // Get the sampler location
        mSamplerLoc = glGetUniformLocation(mProgramObject, "s_texture");
        mPositionTranslateLoc = glGetUniformLocation(mProgramObject, "u_translate");
        mColorLoc = glGetUniformLocation(mProgramObject, "u_color");
        mModelMatrixLoc = glGetUniformLocation(mProgramObject, "u_modelMatrix");

        //COLOR
        mPositionLocColor = glGetAttribLocation(mProgramObjectColor, "a_position");
        mColorLocColor = glGetUniformLocation(mProgramObjectColor, "u_color");
        mPositionTranslateLocColor = glGetUniformLocation(mProgramObjectColor, "u_translate");
        mModelMatrixColorLoc = glGetUniformLocation(mProgramObjectColor, "u_modelMatrix");
    }

    static void gl_useProgram(int prog)
    {
        glUseProgram(prog);
    }

    void gl_drawRect(GLuint texture, float x, float y, GLRect &rect, bool forceFixAspectOff)
    {
        if(texture == -1)
        {
            return;
        }

        if(isGLES2)
        {
            // Bind the texture
            glBindTexture(GL_TEXTURE_2D, texture);

            gl_useProgram(mProgramObject);

            glVertexAttribPointer(mPositionLoc, 3, GL_FLOAT,
                                  false,
                                  3 * 4, rect.vertices);

            // I messed up the corrdinates for GLES so not the same as GLES2
            GLfloat texVert[] =
                    {
                            rect.texture[2], rect.texture[3], // TexCoord 0
                            rect.texture[0], rect.texture[1], // TexCoord 1
                            rect.texture[4], rect.texture[5], // TexCoord 2
                            rect.texture[6], rect.texture[7] // TexCoord 3
                    };

            glVertexAttribPointer(mTexCoordLoc, 2, GL_FLOAT,
                                  false,
                                  2 * 4,
                    //mTexVertices );
                                  texVert);

            glEnableVertexAttribArray(mPositionLoc);
            glEnableVertexAttribArray(mTexCoordLoc);

            // Set the sampler texture unit to 0
            glUniform1i(mSamplerLoc, 0);

            float yAspectFixTranslate = 0;
            float xAspectFixTranslate = 0;

            // Such a hack.The model matrix is just used to scale for gles2
            // Need to fix all of this so GLES1 and GLES2 coordinates behave the same and model matrix is used properly
            if(m_fixAspect && !forceFixAspectOff)
            {
                float nominal = (float) ScaleX / (float) ScaleY;
                float actual = GLScaleWidth / -GLScaleHeight;

                //printf("%f     %f\n",nominal,actual);
                float yScale = actual / nominal;
                float xScale = nominal / actual;

                if(yScale <= 1)
                {
                    yAspectFixTranslate = -(1 - yScale) * rect.height / 2;
                    mModelMatrixGLSL[5] = yScale;
                    gl_translatef(0, yAspectFixTranslate, 0);
                }
                else
                {
                    xAspectFixTranslate = (1 - xScale) * rect.width / 2;
                    mModelMatrixGLSL[0] = xScale;
                    gl_translatef(xAspectFixTranslate, 0, 0);
                }
            }

            glUniformMatrix4fv(mModelMatrixLoc, 1, false, mModelMatrixGLSL);
            glUniform4f(mPositionTranslateLoc, translateX(x + glTranslateX), translateY(y + glTranslateY), 0, 0);
            glUniform4f(mColorLoc, glColorR, glColorG, glColorB, glColorA);

            glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

            if(m_fixAspect && !forceFixAspectOff)
            {
                mModelMatrixGLSL[5] = 1;
                mModelMatrixGLSL[0] = 1;
                gl_translatef(-xAspectFixTranslate, -yAspectFixTranslate, 0);
            }
        }
        else
        {
            glPushMatrix();

            //LOGTOUCH("gl_drawRect %d",texture);
            glBindTexture(GL_TEXTURE_2D, texture);
            glVertexPointer(3, GL_FLOAT, 0, rect.vertices);
            glTexCoordPointer(2, GL_FLOAT, 0, rect.texture);

            glTranslatef(x, -y, 0);

            if(m_fixAspect && !forceFixAspectOff)
            {
                float nominal = (float) ScaleX / (float) ScaleY;
                float actual = GLScaleWidth / -GLScaleHeight;

                //printf("%f     %f\n",nominal,actual);
                float yScale = actual / nominal;

                // Choose correct sides to make smaller so it does not go out of the area
                if(yScale <= 1)
                {
                    glTranslatef(0, -(1 - yScale) * rect.height / 2, 0);
                    glScalef(1, yScale, 1);
                }
                else
                {
                    float xScale = nominal / actual;
                    glTranslatef((1 - xScale) * (rect.width / 2.0), 0, 0);
                    glScalef(xScale, 1, 1);
                }
            }


            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

            glPopMatrix();
        }
    }


    void gl_drawRect(GLfloat r, GLfloat g, GLfloat b, GLfloat a, float x, float y, GLRect &rect)
    {
        if(isGLES2)
        {
            gl_useProgram(mProgramObjectColor);

            gl_color4f(r, g, b, a);


            glVertexAttribPointer(mPositionLocColor, 3, GL_FLOAT,
                                  false,
                                  3 * 4, rect.vertices);


            glEnableVertexAttribArray(mPositionLocColor);

            // Bind the texture
            //glDeactiveTexture ( GL_TEXTURE0 );

            glUniformMatrix4fv(mModelMatrixColorLoc, 1, false, mModelMatrixGLSL);
            glUniform4f(mPositionTranslateLocColor, translateX(x + glTranslateX), translateY(y + glTranslateY), 0, 0);
            glUniform4f(mColorLocColor, glColorR, glColorG, glColorB, glColorA);

            glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

            gl_color4f(1, 1, 1, 1);
        }
        else
        {
            glPushMatrix();

            glDisable(GL_TEXTURE_2D);
            glColor4f(r, g, b, a);

            glVertexPointer(3, GL_FLOAT, 0, rect.vertices);
            glTranslatef(x, -y, 0);
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

            glColor4f(1, 1, 1, 1);
            glEnable(GL_TEXTURE_2D);

            glPopMatrix();
        }
    }

    void gl_drawLines(float x, float y, GLLines &lines)
    {
        if(isGLES2)
        {
            gl_useProgram(mProgramObjectColor);

            glVertexAttribPointer(mPositionLocColor, 3, GL_FLOAT,
                                  false,
                                  3 * 4, lines.vertices);


            glEnableVertexAttribArray(mPositionLocColor);

            // Bind the texture
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, 0);

            glUniformMatrix4fv(mModelMatrixColorLoc, 1, false, mModelMatrixGLSL);
            glUniform4f(mPositionTranslateLocColor, translateX(x + glTranslateX), translateY(y + glTranslateY), 0, 0);
            glUniform4f(mColorLocColor, glColorR, glColorG, glColorB, glColorA);

            glDrawArrays(GL_LINES, 0, lines.len);
        }
        else
        {
            glDisable(GL_TEXTURE_2D);

            glVertexPointer(3, GL_FLOAT, 0, lines.vertices);

            glTranslatef(x, -y, 0);

            glDrawArrays(GL_LINES, 0, lines.len);

            glEnable(GL_TEXTURE_2D);
        }
    }


#define GET_ALPHA_PIXEL(X, Y) imageData[ (height - 1 - Y) * (width * 4) + (X * 4) + 0 ]

    void calcFontSpacing(const unsigned char *imageData, int width, int height, std::vector <FontInfo> *fontInfoVec)
    {
        //LOGTOUCH( "fontSpacing w=%d, h=%d", width, height );

        // We have a 16 x 16 grid of characters
        int charWidth = width / 16;

        for(int c = 0; c < 256; c++)
        {
            int cx = c % 16;
            int cy = (c >> 4);

            int leftMost = charWidth;
            int rightMost = -1;

            for(int y = 0; y < charWidth; y++)
            {
                unsigned char alpha[256];

                bool hit = false;

                for(int x = 0; x < charWidth; x++)
                {
                    int px = x + cx * charWidth;
                    int py = y + cy * charWidth;
                    // Note image is upside down
                    int pos = (height - 1 - py) * (width * 4) + (px * 4) + 3;

                    if(imageData[pos] == 0)
                    {
                        alpha[x] = 0;
                    }
                    else
                    {
                        alpha[x] = 1;

                        if(x < leftMost)
                        {
                            leftMost = x;
                        }

                        if(x > rightMost)
                        {
                            rightMost = x;
                        }
                    }
                }

                //If still 64 there is no character there
                if(leftMost == charWidth)
                {
                    FontInfo fontInfo;
                    fontInfo.leftGap = 0;
                    fontInfo.rightGap = 0;
                    (*fontInfoVec).at(c) = fontInfo;
                }
                else
                {
                    // Calculate the padding as a proportion of the total width
                    FontInfo fontInfo;
                    fontInfo.leftGap = (leftMost / (float) charWidth);
                    fontInfo.rightGap = (charWidth - 1 - rightMost) / (float) charWidth;
                    (*fontInfoVec).at(c) = fontInfo;
                }

                /*
                        LOGTOUCH("%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d%d",
                                alpha[0+0],alpha[0+1],alpha[0+2],alpha[0+3],alpha[0+4],alpha[0+5],alpha[0+6],alpha[0+7],alpha[0+8],alpha[0+9],alpha[0+10],alpha[0+11],alpha[0+12],alpha[0+13],alpha[0+14],alpha[0+15],
                                alpha[16+0],alpha[16+1],alpha[16+2],alpha[16+3],alpha[16+4],alpha[16+5],alpha[16+6],alpha[16+7],alpha[16+8],alpha[16+9],alpha[16+10],alpha[16+11],alpha[16+12],alpha[16+13],alpha[16+14],alpha[16+15],
                                alpha[32+0],alpha[32+1],alpha[32+2],alpha[32+3],alpha[32+4],alpha[32+5],alpha[32+6],alpha[32+7],alpha[32+8],alpha[32+9],alpha[32+10],alpha[32+11],alpha[32+12],alpha[32+13],alpha[32+14],alpha[32+15],
                                alpha[48+0],alpha[48+1],alpha[48+2],alpha[48+3],alpha[48+4],alpha[48+5],alpha[48+6],alpha[48+7],alpha[48+8],alpha[48+9],alpha[48+10],alpha[48+11],alpha[48+12],alpha[48+13],alpha[48+14],alpha[48+15]);
                                */
            }

            // LOGTOUCH( "%c l=%d, r =%d", c, leftMost, rightMost );
        }
    }

    static FILE *file;

    void png_read(png_structp png_ptr, png_bytep data, png_size_t length)
    {
        fread(data, 1, length, file);
    }

//This needs to be set to the location of the PNG files
    std::string graphicsBasePath;

    void gl_setGraphicsBasePath(std::string path)
    {
        graphicsBasePath = path;
    }


//In case same texture is wanted, don't reload
    static std::map <std::string, GLuint> glTextureCache;
    static std::map <std::string, std::vector<FontInfo>> fontInfoCache;

    void clearGlTexCache()
    {
        glTextureCache.clear();
        fontInfoCache.clear();
    }

    static int glTexNumber = 0;

    void setTextureNumberStart(int start)
    {
        glTexNumber = start;
    }

    GLuint getNextTexNum()
    {
        GLuint texture;

        if(glTexNumber)
        {
            texture = glTexNumber;
            glTexNumber++;
        }
        else
        {
            glGenTextures(1, &texture);
        }

        return texture;
    }

    GLuint loadTextureFromPNG(std::string filename, int &width, int &height, std::vector <FontInfo> *fontInfoVec)
    {
        if(filename == "")
        {
            LOGTOUCH("Blank texture name\n");
            return -1;
        }

        //LOGTOUCH( "Loading png: %s\n", filename.c_str() );

        if(!initGlesDone)
        {
            loadGles(isGLES2 ? 2 : 1);

            if(isGLES2)
            {
                initGLES2();
            }

            initGlesDone = true;
        }

        //Check if already loaded
        std::map<std::string, GLuint>::iterator it = glTextureCache.find(filename);

        if(it != glTextureCache.end())
        {
            if(fontInfoVec != NULL)
            {
                (*fontInfoVec).insert((*fontInfoVec).begin(), fontInfoCache[filename].begin(), fontInfoCache[filename].end());
            }

            //element found;
            //LOGTOUCH( "PNG %s is already loaded\n", filename.c_str() );
            return it->second;
        }

        std::string filename_stripped;

        if(filename.find('?') != std::string::npos)
        {
            filename_stripped = filename.substr(0, filename.find('?'));
        }
        else //No extra info in the filename
        {
            filename_stripped = filename;
        }

        std::string full_file = graphicsBasePath + filename_stripped + ".png";
        file = fopen(full_file.c_str(), "r");

        if(!file)
        {
            LOGTOUCH_E("Error opening %s from APK\n", full_file.c_str());
            return TEXTURE_LOAD_ERROR;
        }

        //header for testing if it is a png
        png_byte header[8];

        //read the header
        fread(header, 1, 8, file);

        //test if png
        int is_png = !png_sig_cmp(header, 0, 8);

        if(!is_png)
        {
            fclose(file);
            LOGTOUCH_E("Not a png file : %s", full_file.c_str());
            return TEXTURE_LOAD_ERROR;
        }

        //create png struct
        png_structp png_ptr = png_create_read_struct(PNG_LIBPNG_VER_STRING, NULL,
                                                     NULL, NULL);

        if(!png_ptr)
        {
            fclose(file);
            LOGTOUCH_E("Unable to create png struct : %s\n", full_file.c_str());
            return (TEXTURE_LOAD_ERROR);
        }

        //create png info struct
        png_infop info_ptr = png_create_info_struct(png_ptr);

        if(!info_ptr)
        {
            png_destroy_read_struct(&png_ptr, (png_infopp) NULL, (png_infopp) NULL);
            LOGTOUCH_E("Unable to create png info : %s\n", full_file.c_str());
            fclose(file);
            return (TEXTURE_LOAD_ERROR);
        }

        //create png info struct
        png_infop end_info = png_create_info_struct(png_ptr);

        if(!end_info)
        {
            png_destroy_read_struct(&png_ptr, &info_ptr, (png_infopp) NULL);
            LOGTOUCH_E("Unable to create png end info : %s\n", full_file.c_str());
            fclose(file);
            return (TEXTURE_LOAD_ERROR);
        }

        //png error stuff, not sure libpng man suggests this.
        if(setjmp(png_jmpbuf(png_ptr)))
        {
            fclose(file);
            LOGTOUCH_E("Error during setjmp : %s\n", full_file.c_str());
            png_destroy_read_struct(&png_ptr, &info_ptr, &end_info);
            return (TEXTURE_LOAD_ERROR);
        }

        //init png reading
        //png_init_io(png_ptr, fp);
        png_set_read_fn(png_ptr, NULL, png_read);

        //let libpng know you already read the first 8 bytes
        png_set_sig_bytes(png_ptr, 8);

        // read all the info up to the image data
        png_read_info(png_ptr, info_ptr);

        //variables to pass to get info
        int bit_depth, color_type;
        png_uint_32 twidth, theight;

        // get info about png
        png_get_IHDR(png_ptr, info_ptr, &twidth, &theight, &bit_depth, &color_type,
                     NULL, NULL, NULL);

        //update width and height based on png info
        width = twidth;
        height = theight;

        // Update the png info struct.
        png_read_update_info(png_ptr, info_ptr);

        // Row size in bytes.
        int rowbytes = png_get_rowbytes(png_ptr, info_ptr);

        // Allocate the image_data as a big block, to be given to opengl
        png_byte *image_data = new png_byte[rowbytes * height];

        if(!image_data)
        {
            //clean up memory and close stuff
            png_destroy_read_struct(&png_ptr, &info_ptr, &end_info);
            LOGTOUCH_E("Unable to allocate image_data while loading %s\n", full_file.c_str());
            fclose(file);
            return TEXTURE_LOAD_ERROR;
        }

        //row_pointers is for pointing to image_data for reading the png with libpng
        //This is because opengl needs the image data upside-down to normal
        png_bytep *row_pointers = new png_bytep[height];

        if(!row_pointers)
        {
            //clean up memory and close stuff
            png_destroy_read_struct(&png_ptr, &info_ptr, &end_info);
            delete[] image_data;
            LOGTOUCH_E("Unable to allocate row_pointer while loading %s\n ", full_file.c_str());
            fclose(file);
            return TEXTURE_LOAD_ERROR;
        }

        // set the individual row_pointers to point at the correct offsets of image_data
        for(int i = 0; i < height; ++i)
        {
            row_pointers[height - 1 - i] = image_data + i * rowbytes;
        }

        //read the png into image_data through row_pointers
        png_read_image(png_ptr, row_pointers);

        //Now generate the OpenGL texture object
        //GLuint texture = texNumber++;
        GLuint texture;

        texture = getNextTexNum();

        //LOGTOUCH( "Texture ID: %d\n", texture );

        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA,
                     GL_UNSIGNED_BYTE, (GLvoid *) image_data);

        if(filename.find("F=N") != std::string::npos)  // Only option so far
        {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }
        else
        {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        }

        //Add to cache
        glTextureCache[filename] = texture;

        if(fontInfoVec != NULL)
        {
            calcFontSpacing(image_data, width, height, fontInfoVec);
            //Add to cache
            //fontInfoCache[filename].resize(256);
            fontInfoCache[filename].insert(fontInfoCache[filename].begin(), (*fontInfoVec).begin(), (*fontInfoVec).end());
        }

        //clean up memory and close stuff
        png_destroy_read_struct(&png_ptr, &info_ptr, &end_info);
        delete[] image_data;
        delete[] row_pointers;
        fclose(file);


        return texture;
    }

}
