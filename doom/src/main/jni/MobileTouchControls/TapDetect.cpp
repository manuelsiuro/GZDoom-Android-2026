#include "TouchControlsConfig.h"

#include "TapDetect.h"

using namespace touchcontrols;


#define MAX_MOVE 0.01
#define MAX_DOWN_TIME 250

TapDetect::TapDetect()
{
    reset();
}

void TapDetect::reset()
{
    tapped = false;
    moved = false;
}

void TapDetect::processPointer(int action, int pid, float x, float y)
{
    if(action == P_DOWN)
    {
        timeDown = getMS();
        touchId = pid;
        anchor.x = x;
        anchor.y = y;
        reset();
    }
    else if(action == P_UP)
    {
        if(touchId == pid)
        {
            uint64_t timeNow = getMS();

            if(((timeNow - timeDown) < MAX_DOWN_TIME) && !moved)
            {
                tapped = true;
            }
        }
    }
    else if(action == P_MOVE)
    {
        if(touchId == pid)
        {
            if((abs(anchor.x - x) > MAX_MOVE) || (abs(anchor.y - y) > MAX_MOVE))
            {
                moved = true;
            }
        }
    }
}

bool TapDetect::didTap()
{
    return tapped;
}