//
// Created by Emile on 03/05/2017.
//

#ifndef OPENTOUCH_UI_KEYBOARD_H
#define OPENTOUCH_UI_KEYBOARD_H

#include "sigc++/sigc++.h"
#include "ControlSuper.h"
#include "GLRect.h"
#include "OpenGLUtils.h"
#include "TextDraw.h"

namespace touchcontrols
{

#define MAX_ROW_KEYS 10
#define NBR_ROWS 4
struct KeyboardKey
{
    unsigned char keyPrim;
    uint32_t keyPrimScanCode;
    unsigned char keyAlt;
    const char *primImg;
    const char *altImg;
    GLuint glPrim;
    GLuint glAlt;
    float width;
};

struct KeyboardRow
{
     KeyboardKey keys[MAX_ROW_KEYS];
};

struct KeyboardLayout
{
    KeyboardRow rows[NBR_ROWS];
};


class UI_Keyboard : public ControlSuper
{
    std::string font_filename;
    TextDraw textDrawer;
    int fontSet;
    KeyboardLayout layout;
    GLuint glKeyBg;

    void setKey( uint32_t row, uint32_t key, char keyPrim, char keyAlt, float width, const char* primImg = NULL, const char * altImg = NULL );

    int touchId;
    KeyboardKey *pressedKey;
    double timeDown;
    bool useAltKey;
public:

	UI_Keyboard( std::string tag, RectF pos, std::string font_filename, int fontSet, uint32_t params, float textSize );

    sigc::signal< void, uint32_t > signal;

	bool processPointer(int action, int pid, float x, float y);

 	void resetOutput();

	bool drawGL(bool forEditor = false);

	bool initGL();

	void updateSize();

	void saveXML(TiXmlDocument &doc);

	void loadXML(TiXmlDocument &doc);
};

}
#endif //OPENTOUCH_UI_KEYBOARD_H
