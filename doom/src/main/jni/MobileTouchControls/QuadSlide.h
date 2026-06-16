#include "sigc++/sigc++.h"
#include "ControlSuper.h"
#include "GLRect.h"
#include "OpenGLUtils.h"
#include "PointF.h"

#ifndef _QuadSlide_H_
#define _QuadSlide_H_


namespace touchcontrols
{

    class QuadSlide : public ControlSuper
    {
        int32_t pid;

        GLuint glBgTex;
        GLuint glArrowTex;
        GLRect glRect;

        int32_t valueUp, valueDown, valueLeft, valueRight;
        int32_t valueCurrent;


        std::string arrowImage;

        float arrowSlidePos;
        uint64_t arrowSlideTime;

        uint32_t showDemoArrowsTime;

        PointF anchorPoint;
        uint32_t slideState;

        void valueOutput(bool down, int32_t value);

    public:
        std::string bgImage; // Public so button list can access

        sigc::signal<void, int32_t, int32_t> signal;

        QuadSlide(std::string tag, RectF pos, std::string bgImage, std::string arrowImage, int32_t valueUp, int32_t valueRight, int32_t valueDown, int32_t valueLeft,
                  bool hidden = false, std::string description = "");

        void resetOutput();

        bool processPointer(int action, int pid, float x, float y);

        bool drawGL(bool forEditor = false);

        bool initGL();

        void updateSize();

        void saveXML(TiXmlDocument &doc);

        void loadXML(TiXmlDocument &doc);
    };

}

#endif
