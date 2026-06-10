#include "sigc++/sigc++.h"
#include "ControlSuper.h"
#include "GLRect.h"
#include "OpenGLUtils.h"
#include "Button.h"
#include "UI_TextBox.h"

#ifndef _UI_Window_H_
#define _UI_Window_H_

#define UI_WINDOW_BUTTON_BACK 0x2200001

namespace touchcontrols
{

class UI_Window : public ControlSuper
{
	bool pressed;

	int id;

	GLuint glTex;

	GLRect glRect;

	Button *backButton;
    UI_TextBox *titleText;

public:
	std::string image;

	UI_Window(std::string tag,RectF pos,std::string title, std::string image_filename);

     sigc::signal<void, uint32_t, uint32_t> signal;

    float getScrollOffsetY();

	bool processPointer(int action, int pid, float x, float y);

 	void resetOutput();

    void setScissor( void );

	bool drawGL(bool forEditor = false);

	bool initGL();

	void updateSize();

	void saveXML(TiXmlDocument &doc);

	void loadXML(TiXmlDocument &doc);
};

}

#endif
