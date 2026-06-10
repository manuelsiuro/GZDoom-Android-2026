
//#include "SDL.h"

#ifdef __cplusplus
extern "C"
{
#endif

//set if swap buffers actually does an egl swap
void SDL_SwapBufferPerformsSwap(int value);

//Set option function to call before swap buffer
void SDL_SetSwapBufferCallBack(void (*pt2Func)(void));

//Callback to hide/show keyboard
void SDL_SetShowKeyboardCallBack(void (*pt2Func)(int));

// True if a new egl was created in the last time this was called
int SDL_NewEGLCreated(void);

void SDL_ShowMouseCallBack(void (*pt2Func)(int));

void SDL_MouseMoveCallBack(void (*pt2Func)(float, float));

#define ACTION_DOWN 0
#define ACTION_UP 1
#define ACTION_MOVE 2
#define ACTION_HOVER_MOVE 7
#define ACTION_SCROLL 8
#define BUTTON_PRIMARY 1
#define BUTTON_SECONDARY 2
#define BUTTON_TERTIARY 4
#define BUTTON_BACK 8
#define BUTTON_FORWARD 16

void SDL_InjectMouse(int button, int action, float x, float y, int relative);


#ifdef __cplusplus
}
#endif
