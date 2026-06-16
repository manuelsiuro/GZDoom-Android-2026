#ifndef touch_interface_h
#define touch_interface_h

//#define NO_SEC

#include "touch_interface_base.h"

class TouchInterface : public TouchInterfaceBase
{
    void createControlsDoom(std::string filesPath);

    void createControlsDoom3(std::string filesPath);

    touchcontrols::Button *doom3SprintButton = 0; // To enable changing of the graphic
public:
    void createControls(std::string filesPath);

    void openGLEnd();

    void openGLStart();

    void blankButton(int state, int code);

    void newFrame();

    void automapButton(int state, int code);

    void newGLContext();
};

#endif /* touch_interface_h */
