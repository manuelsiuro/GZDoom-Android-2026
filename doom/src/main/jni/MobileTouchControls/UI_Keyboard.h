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

#define UI_KEYBOARD_HIDE    1
#define UI_KEYBOARD_SHIFT   2
#define UI_KEYBOARD_SYMBOLS 3
#define UI_KEYBOARD_MOVE    4

namespace touchcontrols
{

#define MAX_ROW_KEYS 13
#define NBR_ROWS 5

    struct KeyboardKey
    {
        uint32_t keyPrim;
        uint32_t keyPrimScanCode;
        uint32_t keyAlt;
        const char *primImg;
        const char *altImg;
        GLuint glPrim;
        GLuint glAlt;
        float width;
        float padLeft;
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

        void setKey(uint32_t row, uint32_t key, uint32_t keyPrim, uint32_t keyAlt, float width, float padLeft, const char *primImg = NULL, const char *altImg = NULL);

        float findXCenter(uint32_t row, uint32_t key);

        void keyDown(KeyboardKey *key);

        void keyUp();

        uint32_t shiftKey(uint32_t key);

        int touchId;
        int moveTouchId;

        float keyboardYPos;
        float moveYAnchor;

        KeyboardKey *pressedKey;
        double timeDown;
        bool useAltKey;

        //Gamepad
        int32_t selectedX;
        int32_t selectedRow;

        float fadePos;

        bool shiftActive;

        bool symbolActive;

    public:
        UI_Keyboard(std::string tag, RectF pos, std::string font_filename, int fontSet, uint32_t params, float textSize);

        sigc::signal<void, uint32_t> signal;

        bool processPointer(int action, int pid, float x, float y);

        bool gamepadInput(bool down, GamePadKey key);

        void resetOutput();

        bool drawGL(bool forEditor = false);

        bool initGL();

        void updateSize();

        void saveXML(TiXmlDocument &doc);

        void loadXML(TiXmlDocument &doc);
    };

}
#endif //OPENTOUCH_UI_KEYBOARD_H
