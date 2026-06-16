#include "sigc++/sigc++.h"
#include "ControlSuper.h"
#include "GLRect.h"
#include "OpenGLUtils.h"

#ifndef _Button_H_
#define _Button_H_


namespace touchcontrols
{

    class Button : public ControlSuper
    {
        bool pressed;

        int id;

        // Two possible images to draw
        GLuint glTex[2];

        // glText above to draw
        int32_t glTexDraw = 0;

        GLRect glRect;

        int value;

        bool repeat;
        double repeatTime;

        bool flash;
        bool flashDir;
        int long long flashCount;

        // Per-button draw alpha multiplier. 1.0 (default) leaves the alpha set by
        // the owning TouchControls unchanged; e.g. 0.5 draws this button at 50% of
        // that alpha (used to dim it).
        float drawAlpha;

    public:
        std::string image;
        sigc::signal<void, int> signal_buttonDown;
        sigc::signal<void, int> signal_buttonUp;
        sigc::signal<void, int, int> signal_button;

        Button(std::string tag, RectF pos, std::string image_filename, int value, bool repeat = false, bool hidden = false, std::string description = "",
               uint32_t color = COLOUR_NONE);
        //Button(std::string tag,RectF pos,std::string image_filename,int value,bool repeat,bool hidden);
        //Button(std::string tag,RectF pos,std::string image_filename,int value,bool repeat,bool hidden);

        void setRepeat(bool v);

        void setFlash(bool v);

        void setImage(int32_t i);

        // Set this button's draw alpha multiplier (1.0 = unchanged, 0.5 = half).
        void setAlpha(float a);

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
