#include "SDL_internal.h"

#include "../../SDL_beloko_extra.h"

#include "SDL_atomic.h"
#include "video/SDL_sysvideo.h"
#include "video/android/SDL_androidmouse.h"

extern SDL_mutex *Android_ActivityMutex;
extern SDL_Window *Android_Window;

void SDL_InjectMouse(int button, int action, float x, float y, int relative)
{
  	SDL_LockMutex(Android_ActivityMutex);

    Android_OnMouse(Android_Window, button, action, x, y, relative ? SDL_TRUE : SDL_FALSE);

    SDL_UnlockMutex(Android_ActivityMutex);
}
