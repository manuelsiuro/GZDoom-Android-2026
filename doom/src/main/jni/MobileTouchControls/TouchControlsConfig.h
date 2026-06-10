
#ifndef _TouchControlsConfig_H_
#define _TouchControlsConfig_H_

#include <string>
#include <sys/time.h>
#include <time.h>
#include <inttypes.h>
#include <math.h>
#include "sigc++/sigc++.h"

#ifdef __ANDROID__
#include <android/log.h>
#define LOGTOUCH(...)  __android_log_print(ANDROID_LOG_INFO,"TouchCntrl",__VA_ARGS__)
#define LOGTOUCH_E(...)  __android_log_print(ANDROID_LOG_ERROR,"TouchCntrl",__VA_ARGS__)
#endif

#ifdef __IOS__
#define LOGTOUCH printf
#define LOGTOUCH_E printf
#endif

#define P_DOWN 1
#define P_UP   2
#define P_MOVE 3
#define P_ALLUP 4

#define SHORT_VIBRATE 50

namespace touchcontrols
{
    const int ScaleX = 26;
    const int ScaleY = 16;

    inline uint64_t getMS()
    {
        uint64_t ms; // Milliseconds
        time_t s;  // Seconds
        struct timespec spec;

        clock_gettime(CLOCK_REALTIME, &spec);

        s = spec.tv_sec;
        ms = round(spec.tv_nsec / 1.0e6); // Convert nanoseconds to milliseconds

        if(ms > 999)
        {
            s++;
            ms = 0;
        }

        return (s * 1000ull) + ms;
    }

    extern sigc::signal<void, int> signal_vibrate;

    // When false (default), TouchJoy only accepts P_DOWN when the control is inactive
    extern bool g_touchJoyMultiTouch;
    void setTouchJoyMultiTouch(bool enable);

}


#endif
