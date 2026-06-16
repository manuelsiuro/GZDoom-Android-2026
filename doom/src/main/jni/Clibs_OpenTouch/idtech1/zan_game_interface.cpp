

#include "game_interface.h"

#include <signal.h>
#include <stdlib.h>
#include <limits.h>
#include <sys/types.h>
#include <stdarg.h>
#include <stdio.h>
#include <sys/stat.h>
#include <string.h>
#include <ctype.h>
#include <errno.h>

#include "SDL.h"
#include "SDL_keycode.h"

#include "doomerrors.h"

#include "d_gui.h"
#include "m_random.h"
#include "doomdef.h"
#include "doomstat.h"
#include "gstrings.h"
#include "w_wad.h"
#include "s_sound.h"
#include "v_video.h"
#include "intermission/intermission.h"
#include "f_wipe.h"
#include "m_argv.h"
#include "m_misc.h"
#include "menu/menu.h"
#include "c_console.h"
#include "c_dispatch.h"
#include "i_sound.h"
#include "i_video.h"
#include "g_game.h"
#include "hu_stuff.h"
#include "wi_stuff.h"
#include "st_stuff.h"
#include "am_map.h"
#include "p_setup.h"
#include "r_utility.h"
#include "r_sky.h"
#include "d_main.h"
#include "d_dehacked.h"
#include "cmdlib.h"
#include "s_sound.h"
#include "m_swap.h"
#include "v_text.h"
#include "gi.h"
#include "stats.h"
#include "gameconfigfile.h"
#include "sbar.h"
#include "decallib.h"
#include "version.h"
#include "v_text.h"
#include "st_start.h"
#include "templates.h"
#include "teaminfo.h"
#include "sbarinfo.h"
#include "d_net.h"
#include "g_level.h"
#include "d_event.h"
#include "d_netinf.h"
#include "v_palette.h"
#include "m_cheat.h"
//#include "compatibility.h"
#include "m_joy.h"
#include "sc_man.h"
#include "po_man.h"
#include "resourcefiles/resourcefile.h"
#include "r_renderer.h"
#include "p_local.h"
#include "autosegs.h"

#include "SDL_beloko_extra.h"

extern "C"
{
extern int SDL_SendKeyboardKey(Uint8 state, SDL_Scancode scancode);
#include "SmartToggle.h"
#include "CStringFifo.h"
}

//Move left/right fwd/back
static float forwardmove_android = 0;
static float sidemove_android = 0;

//Look up and down
static float look_pitch_mouse = 0;
static float look_pitch_joy = 0;

//left right
static float look_yaw_mouse = 0;
static float look_yaw_joy = 0;

static CStringFIFO m_CmdFifo;

int PortableKeyEvent(int state, int code, int unicode)
{
    //LOGI("PortableKeyEvent %d %d %d\n", state, code, unicode);

    if(state)
        SDL_SendKeyboardKey(SDL_PRESSED, (SDL_Scancode) code);
    else
        SDL_SendKeyboardKey(SDL_RELEASED, (SDL_Scancode) code);

    return 0;

}

void PortableBackButton()
{
    PortableKeyEvent(1, SDL_SCANCODE_ESCAPE, 0);
    PortableKeyEvent(0, SDL_SCANCODE_ESCAPE, 0);
}


static void buttonChange(int state, FButtonStatus *button)
{
    if(state)
    {
        button->bDown = true;
        button->bWentDown = true;
    }
    else
    {
        button->bWentUp = true;
        button->bDown = false;
    }
}

#define ACTION_DOWN 0
#define ACTION_UP 1
#define ACTION_MOVE 2
#define ACTION_MOVE_REL 3
#define ACTION_HOVER_MOVE 7
#define ACTION_SCROLL 8
#define BUTTON_PRIMARY 1
#define BUTTON_SECONDARY 2
#define BUTTON_TERTIARY 4
#define BUTTON_BACK 8
#define BUTTON_FORWARD 16

void PortableMouse(float dx, float dy)
{
    static float mx = 0;
    static float my = 0;
    //LOGI("%f %f",dx,dy);
    mx += -dx * screen->GetWidth();
    my += -dy * screen->GetHeight();

    if((fabs(mx) > 1) || (fabs(my) > 1))
    {
        SDL_InjectMouse(0, ACTION_MOVE, mx, my, SDL_TRUE);
    }

    if(fabs(mx) > 1)
        mx = 0;

    if(fabs(my) > 1)
        my = 0;
}

void PortableMouseButton(int state, int button, float dx, float dy)
{
    if(state)
        SDL_InjectMouse(BUTTON_PRIMARY, ACTION_DOWN, 0, 0, SDL_TRUE);
    else
        SDL_InjectMouse(0, ACTION_UP, 0, 0, SDL_TRUE);
}

void PortableAction(int state, int action)
{
    LOGI("PortableAction %d   %d", state, action);

    if((action >= PORT_ACT_CUSTOM_0) && (action <= PORT_ACT_CUSTOM_25))
    {
        if(action <= PORT_ACT_CUSTOM_9)
            PortableKeyEvent(state, SDL_SCANCODE_KP_1 + action - PORT_ACT_CUSTOM_0, 0);
        else if(action <= PORT_ACT_CUSTOM_25)
            PortableKeyEvent(state, SDL_SCANCODE_A + action - PORT_ACT_CUSTOM_10, 0);
    }
    else if((PortableGetScreenMode() == TS_MENU) || (PortableGetScreenMode() == TS_BLANK))
    {
        if(action >= PORT_ACT_MENU_UP && action <= PORT_ACT_MENU_BACK)
        {

            int sdl_code[] = {SDL_SCANCODE_UP, SDL_SCANCODE_DOWN, SDL_SCANCODE_LEFT,
                              SDL_SCANCODE_RIGHT, SDL_SCANCODE_RETURN, SDL_SCANCODE_ESCAPE
            };
            PortableKeyEvent(state, sdl_code[action - PORT_ACT_MENU_UP], 0);
            return;
        }
        else if(action == PORT_ACT_USE)   // This is sent from the blank screen
        {
            buttonChange(state, &Button_Use);
        }
    }
    else
    {
        switch(action)
        {
            case PORT_ACT_LEFT:
                buttonChange(state, &Button_Left);
                break;

            case PORT_ACT_RIGHT:
                buttonChange(state, &Button_Right);
                break;

            case PORT_ACT_FWD:
                buttonChange(state, &Button_Forward);
                break;

            case PORT_ACT_BACK:
                buttonChange(state, &Button_Back);
                break;

            case PORT_ACT_MOVE_LEFT:
                buttonChange(state, &Button_MoveLeft);
                break;

            case PORT_ACT_MOVE_RIGHT:
                buttonChange(state, &Button_MoveRight);
                break;

            case PORT_ACT_FLY_UP:
                buttonChange(state, &Button_MoveUp);
                break;

            case PORT_ACT_FLY_DOWN:
                buttonChange(state, &Button_MoveDown);
                break;

            case PORT_ACT_USE:
                buttonChange(state, &Button_Use);
                break;

            case PORT_ACT_ATTACK:
                buttonChange(state, &Button_Attack);
                break;

            case PORT_ACT_ALT_ATTACK:
                buttonChange(state, &Button_AltAttack);
                break;

            case PORT_ACT_TOGGLE_ALT_ATTACK:
                if(state)
                {
                    if(Button_AltAttack.bDown)
                        buttonChange(0, &Button_AltAttack);
                    else
                        buttonChange(1, &Button_AltAttack);
                }

                break;

            case PORT_ACT_JUMP:
                buttonChange(state, &Button_Jump);
                break;

            case PORT_ACT_DOWN:
                buttonChange(state, &Button_Crouch);
                break;

            case PORT_ACT_TOGGLE_CROUCH:
            {
                static SmartToggle_t smartToggle;
                int activate = SmartToggleAction(&smartToggle, state, Button_Crouch.bDown);
                buttonChange(activate, &Button_Crouch);
            }
                break;

            case PORT_ACT_NEXT_WEP:
                if(state)
                    PortableCommand("weapnext");

                break;

            case PORT_ACT_PREV_WEP:
                if(state)
                    PortableCommand("weapprev");

                break;

            case PORT_ACT_MAP:
                if(state)
                    PortableCommand("togglemap");

                break;

            case PORT_ACT_QUICKLOAD:
                if(state)
                    PortableCommand("quickload");

                break;

            case PORT_ACT_QUICKSAVE:
                if(state)
                    PortableCommand("quicksave");

                break;

            case PORT_ACT_WEAP0:
                if(state)
                    PortableCommand("slot 0");

                break;

            case PORT_ACT_WEAP1:
                if(state)
                    PortableCommand("slot 1");

                break;

            case PORT_ACT_WEAP2:
                if(state)
                    PortableCommand("slot 2");

                break;

            case PORT_ACT_WEAP3:
                if(state)
                    PortableCommand("slot 3");

                break;

            case PORT_ACT_WEAP4:
                if(state)
                    PortableCommand("slot 4");

                break;

            case PORT_ACT_WEAP5:
                if(state)
                    PortableCommand("slot 5");

                break;

            case PORT_ACT_WEAP6:
                if(state)
                    PortableCommand("slot 6");

                break;

            case PORT_ACT_WEAP7:
                if(state)
                    PortableCommand("slot 7");

                break;

            case PORT_ACT_WEAP8:
                if(state)
                    PortableCommand("slot 8");

                break;

            case PORT_ACT_WEAP9:
                if(state)
                    PortableCommand("slot 9");

                break;

            case PORT_ACT_CONSOLE:
                if(state)
                    PortableCommand("toggleconsole");

                break;

            case PORT_ACT_INVUSE:
                if(state)
                    PortableCommand("invuse");

                break;

            case PORT_ACT_INVDROP:
                if(state)
                    PortableCommand("invdrop");

                break;

            case PORT_ACT_INVPREV:
                if(state)
                    PortableCommand("invprev");

                break;

            case PORT_ACT_INVNEXT:
                if(state)
                    PortableCommand("invnext");

                break;

            case PORT_ACT_HELPCOMP:
                if(state)
                    PortableCommand("showpop 1");

                break;

            case PORT_ACT_SHOW_WEAPONS:
                if(state)
                    PortableCommand("showpop 3");

                break;

            case PORT_ACT_SHOW_KEYS:
                if(state)
                    PortableCommand("showpop 2");

                break;

            case PORT_ACT_MP_SAY:
                if(state)
                    PortableCommand("messagemode");

                break;
        }
    }
}


// =================== FORWARD and SIDE MOVMENT ==============


void PortableMoveFwd(float fwd)
{
    if(fwd > 1)
        fwd = 1;
    else if(fwd < -1)
        fwd = -1;

    forwardmove_android = fwd;
}

void PortableMoveSide(float strafe)
{
    if(strafe > 1)
        strafe = 1;
    else if(strafe < -1)
        strafe = -1;

    sidemove_android = strafe;
}

void PortableMove(float fwd, float strafe)
{
    PortableMoveFwd(fwd);
    PortableMoveSide(strafe);
}

//======================================================================


void PortableLookPitch(int mode, float pitch)
{
    switch(mode)
    {
        case LOOK_MODE_MOUSE:
            look_pitch_mouse += pitch;
            break;

        case LOOK_MODE_JOYSTICK:
            look_pitch_joy = pitch;
            break;
    }
}


void PortableLookYaw(int mode, float yaw)
{
    switch(mode)
    {
        case LOOK_MODE_MOUSE:
            look_yaw_mouse += yaw;
            break;

        case LOOK_MODE_JOYSTICK:
            look_yaw_joy = yaw;
            break;
    }
}


// Start game, does not return!
void PortableInit(int argc, const char **argv)
{

    extern int main_android(int argc, char **argv);
    main_android(argc, (char **) argv);

}

extern bool automapactive;
bool g_bindingbutton = false;

touchscreemode_t PortableGetScreenMode()
{
    if(menuactive != MENU_Off)
    {
        if(g_bindingbutton)
            return TS_CUSTOM;
        else
            return TS_MENU;
    }
    else if(gamestate == GS_LEVEL)  // In a game
    {
        if(automapactive)
            return TS_MAP;
        else
            return TS_GAME;
    }
    else
        return TS_BLANK;
}


int PortableShowKeyboard(void)
{
    return 0;
}

void PortableCommand(const char *cmd)
{
    LOGI("PortableCommand: %s", cmd);
    cstr_fifo_push(&m_CmdFifo, cmd);
}


static float am_zoom = 0;
static float am_pan_x = 0;
static float am_pan_y = 0;

void PortableAutomapControl(float zoom, float x, float y)
{
    am_zoom += zoom * 5;
    am_pan_x += x * 400;
    am_pan_y += y * 400;
    //LOGI("am_pan_x = %f",am_pan_x);
}

EXTERN_CVAR (Bool, cl_run)

bool PortableSetAlwaysRun(bool run)
{
    if(run)
        PortableCommand("cl_run 1");
    else
        PortableCommand("cl_run  0");

    return false;
}

void Mobile_AM_controls(double *zoom, double *pan_x, double *pan_y)
{
    if(am_zoom)
    {
        if(am_zoom > 0)
            *zoom = 1 + am_zoom;

        if(am_zoom < 0)
            *zoom = -1 + am_zoom;

        am_zoom = 0;
    }

    *pan_x += am_pan_x;
    *pan_y += -am_pan_y;
    am_pan_x = am_pan_y = 0;
    //LOGI("zoom = %f",*zoom);
}

extern fixed_t forwardmove[2], sidemove[2];

extern "C" int blockGamepad(void);

void Mobile_IN_Move(ticcmd_t *cmd)
{
    int blockMove = blockGamepad() & ANALOGUE_AXIS_FWD;
    int blockLook = blockGamepad() & ANALOGUE_AXIS_PITCH;

    if(!blockMove)
    {
        float fwdSpeed = forwardmove_android;
        float sideSpeed = sidemove_android;

        if(!isPlayerRunning())
        {
            fwdSpeed = fwdSpeed / 2;
            sideSpeed = sideSpeed / 2;
        }

        cmd->ucmd.forwardmove += fwdSpeed * forwardmove[1];
        cmd->ucmd.sidemove += sideSpeed * sidemove[1];
    }

    if(!blockLook)
    {
        // Add pitch
        G_AddViewPitch(look_pitch_mouse * 30000);
        look_pitch_mouse = 0;
        G_AddViewPitch(-look_pitch_joy * 800);

        // Add yaw
        G_AddViewAngle(-look_yaw_mouse * 100000);
        look_yaw_mouse = 0;
        G_AddViewAngle(-look_yaw_joy * 1000);
    }

    char *consoleCmd;
    while((consoleCmd = cstr_fifo_pop(&m_CmdFifo)))
    {
        AddCommandString((char *) consoleCmd, 0);
        free(consoleCmd);
    }
}



