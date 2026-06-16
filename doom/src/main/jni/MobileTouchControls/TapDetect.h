
#ifndef _TapDetect_H_
#define _TapDetect_H_

#include "PointF.h"

namespace touchcontrols
{

    class TapDetect
    {
        bool tapped;
        bool moved;
        int touchId;
        uint64_t timeDown;
        PointF anchor;
    public:
        TapDetect();

        void reset();

        void processPointer(int action, int pid, float x, float y);

        bool didTap();
    };

}
#endif
