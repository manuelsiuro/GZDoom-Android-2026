
#ifndef _TouchControlsConfig_H_
#define _TouchControlsConfig_H_
//#include <string>
#include <sys/time.h>

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

namespace touchcontrols
{
const int ScaleX = 26;
const int ScaleY = 16;

    inline double getMS()
    {
        struct timeval tv;
        gettimeofday(&tv, NULL);
        return  (tv.tv_sec) * 1000 + (tv.tv_usec) / 1000;
    }
}


#endif
