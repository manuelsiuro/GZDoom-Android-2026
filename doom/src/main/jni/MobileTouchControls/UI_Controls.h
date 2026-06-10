//
// Created by Emile on 18/04/2017.
//

#ifndef OPENTOUCH_UI_CONTROLS_H
#define OPENTOUCH_UI_CONTROLS_H


#include <string>
#include <stdbool.h>
#include <vector>
#include "sigc++/sigc++.h"

#include "ControlSuper.h"
#include "PointF.h"

namespace touchcontrols
{


    class UI_Controls
    {
    public:
        UI_Controls(std::string t);

        sigc::signal<void, bool> signalEnable; // This signal gets called when the control is enabled or disabled


        bool processPointer(int action, int pid, float x, float y);

        int draw();

        void addControl(ControlSuper *cntrl);

        void deleteControls();

        void setEnabled(bool v);

        bool getEnabled();

        void initGL();

        void setAlpha(float a);

        void fade(fadedir_t dir, int steps);

        void *getControl(std::string name); //Get control by name, obviously you must cast to correct type!

        void scaleAllControls(float x, float y);

        void translateAllControls(float x, float y);

    private:
        std::string tag;
        std::vector<ControlSuper *> controls;
        bool enabled;

        ControlSuper *controlActive; // If a control wants to show something over the screen use this

        float fadePos; //current fade
        fadedir_t fadeDir;
        float fadeStep;
        bool fading;

        float yOffset; // Used to scroll up/down
        void offsetYOffset(float offset);

        PointF finger1;
        bool scrolling;

        // Used to calculate velocity for fling
        uint64_t lastMoveTime;
        float lastYpos;
        float yMoveSpeed;

        // Used to decelerate in the draw from a fling
        uint64_t lastDrawTime;
        float flingMomentum = 0;
    };

}
#endif //OPENTOUCH_UI_CONTROLS_H
