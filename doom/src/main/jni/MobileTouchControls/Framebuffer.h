
#ifndef _Framebuffer_H_
#define _Framebuffer_H_


namespace touchcontrols
{

    typedef struct
    {
        int vidWidth;
        int vidHeight;
        int vidWidthReal;
        int vidHeightReal;

        bool maintainAspect;

        bool isGles2;
        bool npotAvailable;
        bool filter;

        bool depthStencilAvailable;
    } fbConfig;

    void R_FrameBufferSetRenderer(bool useGL4ES, int glesVersion);

    void R_FrameBufferConfig(fbConfig config);

    void R_FrameBufferInit();

    void R_FrameBufferStart();

    void R_FrameBufferEnd();

    int fb_getGLESVersion();

}

#endif