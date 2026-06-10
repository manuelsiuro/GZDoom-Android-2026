//
// Created by emile on 01/01/2021.
//
#include "touch_interface_base.h"

#include "TouchControlsInterface.h"
#include "TouchControlsContainer.h"
#include "Framebuffer.h"
#include "../../SDL_beloko_extra.h"

#ifdef USE_SDL3
#include "SDL3/SDL.h"
#include "SDL3/SDL_keycode.h"
#else
#include "SDL.h"
#include "SDL_keycode.h"
#endif

#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>

#include "UI_TouchDefaultSettings.h"
#include "UI_ButtonListWindow.h"

#include "touch_interface.h"
#include "game_interface.h"

#ifdef CHOC_SETUP
#define DEFAULT_FADE_FRAMES 0
#else
#define DEFAULT_FADE_FRAMES 10
#endif


#define COMMAND_SET_BACKLIGHT      0x8001
#define COMMAND_SHOW_GYRO_OPTIONS  0x8002
#define COMMAND_SHOW_KEYBOARD      0x8003
#define COMMAND_SHOW_GAMEPAD       0x8004
#define COMMAND_VIBRATE            0x8005
#define COMMAND_LOAD_SAVE_CONTROLS 0x8006
#define COMMAND_SHOW_QUICK_COMMANDS 0x8008


extern "C"
{
#ifdef USE_SDL3
void SDL_SendKeyboardText(const char *text);
bool SDL_SendKeyboardKey(Uint64 timestamp, SDL_KeyboardID keyboardID, int rawcode, SDL_Scancode scancode, bool down);
#else
int Android_JNI_SendMessage(int command, int param);
int SDL_SendKeyboardKey(Uint8 state, SDL_Scancode scancode);
int SDL_SendKeyboardText(const char *text);
#endif

extern void setupStatic(void);
extern void frameControlsSDLCallback(void);
extern void showKeyboardCallbackSDLCallback(int show);
extern void showMouseSDLCallback(int show);
extern void moveMouseSDLCallback(float x, float y);

extern char keyGlobal[512];
extern int keyCheck();

extern const char *getFilesPath();
extern const char *nativeLibsPath;

extern int mobile_screen_width;
extern int mobile_screen_height;

extern int game_screen_width;
extern int game_screen_height;

int checkGfx();

extern bool allowGyro; // In android_jni_inc.cpp
}


void TouchInterfaceBase::init(int width, int height, const char *pngPath, const char *touchSettingsPath, int options, int wheelNbr_, int game)
{
    nativeWidth = width;
    nativeHeight = height;

    gameType = game;
    wheelNbr = wheelNbr_;

    if(options & GAME_OPTION_AUTO_HIDE_GAMEPAD)
        gamepadHideTouch = true;

    if(options & GAME_OPTION_TOUCHJOY_MULTITOUCH)
        touchcontrols::setTouchJoyMultiTouch(true);

    if(options & GAME_OPTION_HIDE_MENU_AND_GAME)
        hideGameAndMenu = true;

    if(options & GAME_OPTION_USE_SYSTEM_KEYBOARD)
        useSystemKeyboard = true;

    int glesVersion = 1;

    if(options & GAME_OPTION_GLES2)
        glesVersion = 2;

    if(options & GAME_OPTION_GLES3)
        glesVersion = 3;

    // Set framebuffer rendering mode, needs to be same as the game
    touchcontrols::R_FrameBufferSetRenderer(options & GAME_OPTION_GL4ES, glesVersion);

    if(options & GAME_OPTION_TOUCH_SURFACE_VIEW) // Touch control rendered on
    {
        touchcontrols::setTextureNumberStart(100); // Force it not use glGenTextures
        touchcontrols::gl_setGLESVersion(2); //Force GLES 2 normal for surface view
    }
    else
    {
        touchcontrols::gl_setGLESVersion(glesVersion);

        if(options & GAME_OPTION_GL4ES)
            touchcontrols::gl_useGL4ES();
    }

    touchcontrols::GLScaleWidth = (float) nativeWidth;
    touchcontrols::GLScaleHeight = (float) -nativeHeight;
    touchcontrols::signal_vibrate.connect(sigc::mem_fun(this, &TouchInterfaceBase::vibrate));
    touchcontrols::gl_setGraphicsBasePath(pngPath);
    touchcontrols::getSettingsSignal()->connect(sigc::mem_fun(this, &TouchInterfaceBase::touchSettingsCallback));

    if(!(options & GAME_OPTION_TOUCH_SURFACE_VIEW))
    {
        controlsContainer.openGL_start.connect(sigc::mem_fun(this, &TouchInterfaceBase::openGLStart));
        controlsContainer.openGL_end.connect(sigc::mem_fun(this, &TouchInterfaceBase::openGLEnd));
    }

    SDL_SetSwapBufferCallBack(frameControlsSDLCallback);
    SDL_ShowMouseCallBack(showMouseSDLCallback);
    SDL_MouseMoveCallBack(moveMouseSDLCallback);
    SDL_SetShowKeyboardCallBack(showKeyboardCallbackSDLCallback);

    // Create common Run button. can be used or not
    runButton = new touchcontrols::Button("run_toggle", touchcontrols::RectF(8, 7, 10, 9), "sprint_slow;sprint", PORT_ACT_SMART_TOGGLE_RUN, false, true, "Run (smart toggle)");

    // Call app specific control creation
    createControls(touchSettingsPath);
    //createControls(pngPath);
}


int TouchInterfaceBase::touchActionToAction(int action)
{
    int ret = 0;

    switch(action)
    {
        case 1:
            ret = PORT_ACT_USE;
            break;

        case 2:
            ret = PORT_ACT_JUMP;
            break;

        case 3:
            ret = PORT_ACT_ATTACK;
            break;

        case 4:
            ret = PORT_ACT_ALT_ATTACK;
            break;

        case 5:
            ret = PORT_ACT_TOGGLE_CROUCH;
            break;

        default:
            ret = 0;
    }

    return ret;
}

void TouchInterfaceBase::touchSettingsCallback(touchcontrols::tTouchSettings settings)
{
    touchSettings = settings;

    leftDoubleAction = touchActionToAction(touchSettings.dblTapLeft);
    rightDoubleAction = touchActionToAction(touchSettings.dblTapRight);

    volumeUpAction = touchActionToAction(touchSettings.volumeUp);
    volumeDownAction = touchActionToAction(touchSettings.volumeDown);

    controlsContainer.setColour(touchSettings.defaultColor);
    controlsContainer.setAlpha(touchSettings.alpha);

    touchJoyLeft->setCenterAnchor(touchSettings.fixedMoveStick);

    if(isWalking == -1)
        isWalking = !touchSettings.alwaysRunDefault;

    // LOGI("run %d %d", isWalking, touchSettings.alwaysRunDefault);

    if(tcGameMain) tcGameMain->setAlpha(touchSettings.alpha);

    if(tcCustomButtons) tcCustomButtons->setAlpha(touchSettings.alpha);

    if(tcWeaponWheel)
    {
        if(touchSettings.weaponWheelOpaque)
            tcWeaponWheel->setAlpha(0.8);
        else
            tcWeaponWheel->setAlpha(touchSettings.alpha);
    }

    if(tcYesNo) tcYesNo->setColour(touchSettings.defaultColor);

    if(tcGameMain) tcGameMain->setColour(touchSettings.defaultColor);

    if(tcGameWeapons) tcGameWeapons->setColour(touchSettings.defaultColor);

    if(tcWeaponWheel) tcWeaponWheel->setColour(touchSettings.defaultColor);

    if(tcInventory) tcInventory->setColour(touchSettings.defaultColor);

    if(tcAutomap) tcAutomap->setColour(touchSettings.defaultColor);

    if(tcCustomButtons) tcCustomButtons->setColour(touchSettings.defaultColor);

    if(tcGamepadUtility) tcGamepadUtility->setColour(touchSettings.defaultColor);

    if(tcDPadInventory) tcDPadInventory->setColour(touchSettings.defaultColor);
}

extern "C" SDL_Window* window;

void TouchInterfaceBase::gameButton(int state, int code)
{
#ifndef NO_SEC

    if(state)
    {
        if(licTest < 0)
            return;

        licTest++;

        if(licTest == 64)
        {
            if(keyCheck() == 0)
            {
                // Failed
                licTest = -1;
            }
            else
            {
                // Now make it fail
                keyGlobal[4] = keyGlobal[4] ^ 0xAA;

                if(keyCheck() == 1)
                {
                    // Failed, keyCheck always returns valid!!
                    licTest = -1;
                }

                // Put back
                keyGlobal[4] = keyGlobal[4] ^ 0xAA;
            }
        }
    }

#endif

    if(code == KEY_SHOOT)
    {
        isShooting = state;
        PortableAction(state, PORT_ACT_ATTACK);
    }
    else if(code == KEY_SHOW_WEAPONS)
    {
        if(state == 1)
        {
            if(!tcGameWeapons->enabled)
            {
                showWeaponNumbersOn = true;
                tcGameWeapons->setAlpha(touchSettings.alpha);
                tcGameWeapons->animateIn(5);
            }
            else
            {
                showWeaponNumbersOn = false;
                tcGameWeapons->animateOut(5);
            }
        }
    }
    else if(code == KEY_SHOW_INV)
    {
        if(state == 1)
        {
            if(!tcInventory->enabled)
            {
                tcInventory->animateIn(5);
            }
            else
                tcInventory->animateOut(5);
        }
    }
    else if(code == KEY_SHOW_CUSTOM)
    {
        if(state == 1)
        {
            if(!tcCustomButtons->enabled)
            {
                tcCustomButtons->setEnabled(true);
                tcCustomButtons->fade(touchcontrols::FADE_IN, DEFAULT_FADE_FRAMES);
                showCustomOn = true;
            }
            else
            {
                tcCustomButtons->fade(touchcontrols::FADE_OUT, DEFAULT_FADE_FRAMES);
                showCustomOn = false;
            }
        }
    }
    else if(code == KEY_SHOW_KBRD)
    {
        if(state)
        {
            startTextInput();
        }
    }
    else if(code == KEY_BACK_BUTTON)
    {
        if(state)
        {
            mobileBackButton();
        }
    }
    else if(code == KEY_USE_MOUSE)
    {
        if(state)
        {
            useMouse = true;
        }
    }
    else if(code == PORT_ACT_MAP)
    {
        if(state)
        {
            mapState = 0; //TODO FIX FOR DELTA
        }

        PortableAction(state, code);
    }
    else if(code == KEY_QUICK_COMMANDS)
    {
        if(state)
            Android_JNI_SendMessage(COMMAND_SHOW_QUICK_COMMANDS, 0);
    }
    else if(code == PORT_ACT_GYRO_TOGGLE)
    {
        allowGyro = SmartToggleAction(&gyroSmartToggle, state, allowGyro);
    }
    else if(code == PORT_ACT_RELOAD)
    {
        if(enableReloadSniperMode)
        {
            //If holding down the reload button, do not reload
            if(state)  //key down
            {
                reloadDownTime = getMS();
            }
            else //up
            {
                //if less than 0.5 sec, reload
                if((getMS() - reloadDownTime) < 500)
                {
                    PortableAction(1, PORT_ACT_RELOAD);
                    PortableAction(0, PORT_ACT_RELOAD);
                }
            }

            sniperMode = state; //Use reload button for precision aim also
        }
        else
            PortableAction(state, code);
    }
    else if(code == PORT_ACT_SMART_TOGGLE_RUN)
    {
        // Check if getting key UP without a corresponding key DOWN. This happens when changing screens.
        // Don't want to change running state if this happens so ignore
        if(!state && !runSmartToggle.buttonDown)
        {
            return;
        }
        isWalking = SmartToggleAction(&runSmartToggle, state, isWalking);
        PortableSetAlwaysRun(!isWalking);
    }
    else
    {
        PortableAction(state, code);
    }
}

///////////////////////////////////////////
///////////     MENU BUTTONS    ///////////
///////////////////////////////////////////
void TouchInterfaceBase::menuButton(int state, int code)
{
    if(code == KEY_SHOW_KBRD)
    {
        if(state)
        {
            startTextInput();
        }

        return;
    }
    else if(code == KEY_BACK_BUTTON)
    {
        if(state)
            mobileBackButton();
    }
    else if(code == KEY_SHOW_CUSTOM)
    {
        if(state)
            showCustomMenu = true;
    }
    else if(code == SDL_SCANCODE_F10)  //for choc setup f10 to work!
    {
        PortableKeyEvent(state, code, 0);
    }
    else if(code == KEY_SHOW_GYRO)
    {
        // Show gyro options
        if(state)
            Android_JNI_SendMessage(COMMAND_SHOW_GYRO_OPTIONS, 0);
    }
    else if(code == KEY_LOAD_SAVE_CONTROLS)
    {
        // Show load save controls
        if(state)
            Android_JNI_SendMessage(COMMAND_LOAD_SAVE_CONTROLS, 0);
    }
    else if(code == KEY_SHOW_GAMEPAD)
    {
        // Show gamepad options
        if(state)
            Android_JNI_SendMessage(COMMAND_SHOW_GAMEPAD, 0);
    }
    else if(code == KEY_USE_MOUSE)
    {
        if(state)
        {
            useMouse = true;
        }
    }

#if !defined(QUAKESPASM) && !defined(QUAKESPASM_SPIKED) && !defined(FTEQW) && !defined(DARKPLACES) && !defined(QUAKE2) && !defined(YQUAKE2) && !defined(QUAKE3) && !defined(UHEXEN2)
    else if(code == PORT_ACT_CONSOLE)
    {
        PortableKeyEvent(state, SDL_SCANCODE_GRAVE, 0);
    }

#endif
    else if(code == KEY_LEFT_MOUSE)
    {
#if defined(GZDOOM) || defined(ZANDRONUM_30) || defined(D3ES)
        PortableMouseButton(state, BUTTON_PRIMARY, 0, 0);
#endif
    }
    else if(code == KEY_ZDOOM_CLEAR_BIND)
    {
        PortableKeyEvent(state, SDL_SCANCODE_BACKSPACE, 0);
    }
    else
    {
        PortableAction(state, code);
    }

    demoControlsAlpha = DEMO_ALPHA_RESET;
}

void TouchInterfaceBase::leftStick(float joy_x, float joy_y, float mouse_x, float mouse_y)
{
    float deadZoneX = touchSettings.deadzoneSensitivity * 0.05;
    float deadZoneY = deadZoneX * 1.6; // Y is about 1.6 bigger due to screen ratio

    if(abs(joy_x) < deadZoneX)
        joy_x = 0;
    else if(joy_x >= deadZoneX)
        joy_x = joy_x - deadZoneX;
    else
        joy_x = joy_x + deadZoneX;

    if(abs(joy_y) < deadZoneY)
        joy_y = 0;
    else if(joy_y >= deadZoneY)
        joy_y = joy_y - deadZoneY;
    else
        joy_y = joy_y + deadZoneY;

    float fwdback = joy_y * 15 * touchSettings.fwdSensitivity;
    float strafe = -joy_x * 10 * touchSettings.strafeSensitivity;

    //LOGI("fwd = %f, side = %f", fwdback, strafe);

    if(touchSettings.digitalMove)
    {
        if(fwdback > touchSettings.digMoveYSensitivity)
        {
            PortableAction(1, PORT_ACT_FWD);
            PortableAction(0, PORT_ACT_BACK);
        }
        else if(fwdback < -touchSettings.digMoveYSensitivity)
        {
            PortableAction(0, PORT_ACT_FWD);
            PortableAction(1, PORT_ACT_BACK);
        }
        else
        {
            PortableAction(0, PORT_ACT_FWD);
            PortableAction(0, PORT_ACT_BACK);
        }

        if(strafe > touchSettings.digMoveXSensitivity)
        {
            PortableAction(1, PORT_ACT_MOVE_RIGHT);
            PortableAction(0, PORT_ACT_MOVE_LEFT);
        }
        else if(strafe < -touchSettings.digMoveXSensitivity)
        {
            PortableAction(0, PORT_ACT_MOVE_RIGHT);
            PortableAction(1, PORT_ACT_MOVE_LEFT);
        }
        else
        {
            PortableAction(0, PORT_ACT_MOVE_RIGHT);
            PortableAction(0, PORT_ACT_MOVE_LEFT);
        }
    }
    else
    {
        PortableMove(fwdback, strafe);
    }
}

void TouchInterfaceBase::rightStick(float joy_x, float joy_y, float mouse_x, float mouse_y)
{
    //LOGI(" mouse x = %f",mouse_x);
    int invert = touchSettings.invertLook ? -1 : 1;
    float scale = (isShooting && touchSettings.precisionShoot) ? touchSettings.precisionSensitivity : 1;

    // Sniper mode overrides it to 10x slower
    if(sniperMode)
        scale = 0.1;

    float pitchMouse = mouse_y * touchSettings.lookSensitivity * invert * scale;
    float pitchJoystick = joy_y * touchSettings.lookSensitivity * invert * scale * -2;

    float yawMouse = mouse_x * touchSettings.turnSensitivity * scale;
    float yawJoystick = joy_x * touchSettings.turnSensitivity * scale * 10;


    // Disable mouse look for Doom, otherwise this should always be enabled
    if(!touchSettings.mouseLook)
    {
        pitchMouse = 0;
        pitchJoystick = 0;
    }

    if(touchSettings.joystickLookMode)
        PortableLookPitch(LOOK_MODE_JOYSTICK, pitchJoystick);
    else
        PortableLookPitch(LOOK_MODE_MOUSE, pitchMouse);

    if(touchSettings.joystickLookMode)
        PortableLookYaw(LOOK_MODE_JOYSTICK, yawJoystick);
    else
        PortableLookYaw(LOOK_MODE_MOUSE, yawMouse);
}

void TouchInterfaceBase::inventoryButton(int state, int code)
{
    PortableAction(state, code);
}

void TouchInterfaceBase::inventoryOutside(bool fromGamepad)
{
    if(touchSettings.autoHideInventory || fromGamepad)
    {
        if(tcInventory->enabled)
            tcInventory->animateOut(5);
    }
}

void TouchInterfaceBase::gameUtilitiesButton(int state, int code)
{
    // Auto hide the gamepad utilitie, except if showing consol
    if((code != PORT_ACT_CONSOLE) && (tcGamepadUtility->isEnabled()))
    {
        tcGamepadUtility->setEnabled(false);
    }

    TouchInterfaceBase::gameButton(state, code);
}

void TouchInterfaceBase::gameUtilitiesOutside(bool fromGamepad)
{
    if(tcGamepadUtility->isEnabled())
    {
        tcGamepadUtility->setEnabled(false);
    }
}

void TouchInterfaceBase::customButton(int state, int code)
{
    // LOGI("Cust %d, %d", state, code);
    PortableAction(state, code);

    // If we are binding keys hide the custom buttons again
    if(state == 1)
        showCustomMenu = false;
}

//Weapon wheel callbacks
void TouchInterfaceBase::weaponWheelSelected(int enabled)
{
    if(enabled)
        tcWeaponWheel->fade(touchcontrols::FADE_IN, 5); //fade in

    /*
        if(enabled)
            PortableCommand("i_timescale 0.1"); //doom3 timescale
        else
            PortableCommand("i_timescale 1");
    */
}

void TouchInterfaceBase::weaponWheel(int segment)
{
    LOGI("weaponWheel %d", segment);
    int number = 0;

    if(segment == 9)   // Press '0'
        number = 0;
    else
        number = 1 + segment;

    PortableAction(1, PORT_ACT_WEAP0 + number);
    waitFrames(1);
    PortableAction(0, PORT_ACT_WEAP0 + number);
}

void TouchInterfaceBase::selectWeaponButton(int state, int code)
{
    PortableAction(state, PORT_ACT_WEAP0 + code);

    if(touchSettings.autoHideNumbers && state == 0)
    {
        showWeaponNumbersOn = false;
        tcGameWeapons->animateOut(5);
    }
}

void TouchInterfaceBase::mouseMove(int action, float x, float y, float mouse_x, float mouse_y)
{
#if defined(GZDOOM) || defined(ZANDRONUM_30) || defined(D3ES) || defined(EDUKE32)// todo

    if(action == TOUCHMOUSE_MOVE)
    {
        PortableMouse(mouse_x, mouse_y);
    }
    else if(action == TOUCHMOUSE_TAP)
    {
        PortableMouseButton(1, BUTTON_PRIMARY, 0, 0);
        //usleep(200 * 1000); // Need this for the PDA to work in D3, needs a frame to react..
        waitFrames(3);
        PortableMouseButton(0, BUTTON_PRIMARY, 0, 0);
    }

#endif

#if defined(QUAKE3) || defined(QUAKESPASM_SPIKED) || defined(QUAKESPASM) || defined(DARKPLACES) || defined(FTEQW)

    if(action == TOUCHMOUSE_MOVE)
    {
        MouseMove(mouse_x * game_screen_width, mouse_y * game_screen_height);
    }
    else if(action == TOUCHMOUSE_TAP)
    {
        MouseButton(1, BUTTON_PRIMARY);
        waitFrames(3);
        MouseButton(0, BUTTON_PRIMARY);
    }

#endif
}

void TouchInterfaceBase::mouseButton(int state, int code)
{
#if defined(GZDOOM) || defined(ZANDRONUM_30) || defined(D3ES) || defined(QUAKESPASM_SPIKED) || defined(QUAKESPASM) || defined(DARKPLACES) || defined(FTEQW) || defined(EDUKE32)
    LOGI("mouseButton %d, %d", state, code);

    // Hide the mouse
    if((code == KEY_USE_MOUSE) && state)
    {
        useMouse = false;
    }
    else if(code == KEY_LEFT_MOUSE)
    {
#if defined(QUAKESPASM_SPIKED) || defined(QUAKESPASM) || defined(DARKPLACES) || defined(FTEQW)
        MouseButton(state, BUTTON_PRIMARY);
#else
        PortableMouseButton(state, BUTTON_PRIMARY, 0, 0);
#endif
    }
    else if(code == KEY_RIGHT_MOUSE)
    {
#if defined(QUAKESPASM_SPIKED) || defined(QUAKESPASM) || defined(DARKPLACES) || defined(FTEQW)
        MouseButton(state, BUTTON_SECONDARY);
#else
        PortableMouseButton(state, BUTTON_SECONDARY, 0, 0);
#endif
    }
    else if((code == KEY_BACK_BUTTON) && state)
    {
        mobileBackButton();
    }

    LOGI("useMouse = %d", useMouse);
#endif
}


void TouchInterfaceBase::AutomapMultitouchMouseMove(int action, float x, float y, float dx, float dy)
{
    if(action == MULTITOUCHMOUSE_MOVE)
    {
        PortableAutomapControl(0, dx, dy);
    }
    else if(action == MULTITOUCHMOUSE_ZOOM)
    {
        PortableAutomapControl(x, 0, 0);
    }
}


void TouchInterfaceBase::brightnessSlideMouse(int action, float x, float y, float dx, float dy)
{
    y = 1 - y;
    Android_JNI_SendMessage(COMMAND_SET_BACKLIGHT, y * 255);
}

void TouchInterfaceBase::dPadInventoryButton(int state, int code)
{
    PortableAction(state, code);
}

void TouchInterfaceBase::dPadInventoryOutside(bool fromGamepad)
{
    if(tcDPadInventory->enabled)
        tcDPadInventory->animateOut(5);
}

void TouchInterfaceBase::leftDoubleTap(int state)
{
    if(leftDoubleAction)
        PortableAction(state, leftDoubleAction);
}

void TouchInterfaceBase::rightDoubleTap(int state)
{
    if(rightDoubleAction)
        PortableAction(state, rightDoubleAction);
}

void TouchInterfaceBase::vibrate(int duration)
{
    Android_JNI_SendMessage(COMMAND_VIBRATE, duration);
}

void TouchInterfaceBase::gameSettingsButton(int state)
{
    if(state == 1)
    {
        controlsContainer.showUIWindow(UI_tc);
    }
}

void TouchInterfaceBase::customSettingsButton(int state)
{
    if(state == 1)
    {
        showButtonListWindow(&controlsContainer);
    }
}

void TouchInterfaceBase::mobileBackButton(void)
{
    LOGI("mobileBackButton");

    if(tcKeyboard->isEnabled())
    {
#ifdef USE_SDL3
        SDL_StopTextInput(window);
#else
        SDL_StopTextInput();
#endif
    }
    else if(tcInventory && tcInventory->isEnabled())
    {
        tcInventory->animateOut(5);
    }
    else if(tcGamepadUtility && tcGamepadUtility->isEnabled())
    {
        tcGamepadUtility->animateOut(5);
    }
    else if(tcDPadInventory && tcDPadInventory->isEnabled())
    {
        tcDPadInventory->animateOut(5);
    }
    else if(controlsContainer.isEditing())
    {
        controlsContainer.finishEditing();
        return;
    }
    else if(wheelSelect->blockGamepad())
    {
        wheelSelect->reset();
        return;
    }
    else if(showCustomMenu == true)
    {
        showCustomMenu = false;
    }
    else
    {
        PortableBackButton();
    }
}

void TouchInterfaceBase::updateTouchScreenModeOut(touchscreemode_t mode)
{
    if(mode != currentScreenMode)
    {
        //first disable the last screen and fade out if necessary
        switch(currentScreenMode)
        {
            case TS_BLANK:
                tcBlank->resetOutput();
                tcBlank->setEnabled(false);
                break;

            case TS_MENU:
                tcMenuMain->resetOutput();
                tcMenuMain->fade(touchcontrols::FADE_OUT, DEFAULT_FADE_FRAMES);
                break;

            case TS_Y_N:
                tcYesNo->resetOutput();
                tcYesNo->fade(touchcontrols::FADE_OUT, DEFAULT_FADE_FRAMES);
                break;

            case TS_GAME:
                tcGameMain->resetOutput();
                tcGameMain->fade(touchcontrols::FADE_OUT, DEFAULT_FADE_FRAMES);
                tcGameWeapons->setEnabled(false);
                tcWeaponWheel->setEnabled(false);

                if(tcCustomButtons)
                {
                    tcCustomButtons->resetOutput();
                    tcCustomButtons->setEnabled(false);
                }

                if(tcInventory)
                {
                    tcInventory->resetOutput();
                    tcInventory->setEnabled(false);
                }

                break;

            case TS_MAP:
                tcAutomap->resetOutput();
                tcAutomap->fade(touchcontrols::FADE_OUT, DEFAULT_FADE_FRAMES);
                break;

            case TS_CUSTOM:
                tcCustomButtons->resetOutput();
                tcCustomButtons->fade(touchcontrols::FADE_OUT, DEFAULT_FADE_FRAMES);
                break;

            case TS_DEMO:
                tcDemo->resetOutput();
                tcDemo->fade(touchcontrols::FADE_OUT, DEFAULT_FADE_FRAMES);
                break;

            case TS_MOUSE:
                tcMouse->resetOutput();
                tcMouse->fade(touchcontrols::FADE_OUT, DEFAULT_FADE_FRAMES);
                break;

            case TS_PDA:
                tcPda->resetOutput();
                tcPda->fade(touchcontrols::FADE_OUT, DEFAULT_FADE_FRAMES);
                break;

            case TS_CONSOLE:
                break;
        }
    }
}

void TouchInterfaceBase::updateTouchScreenModeIn(touchscreemode_t mode)
{
    if(mode != currentScreenMode)
    {
        //Enable the current new screen
        switch(mode)
        {
            case TS_BLANK:
                tcBlank->setEnabled(true);
                break;

            case TS_MENU:

                if(!hideGameAndMenu)
                {
                    tcMenuMain->setEnabled(true);
                    tcMenuMain->fade(touchcontrols::FADE_IN, DEFAULT_FADE_FRAMES);
                }

                break;

            case TS_Y_N:
                tcYesNo->setEnabled(true);
                tcYesNo->fade(touchcontrols::FADE_IN, DEFAULT_FADE_FRAMES);
                break;

            case TS_GAME:

                // Always set these so they are never wrong
                if(tcGameMain) tcGameMain->setAlpha(touchSettings.alpha);

                if(tcCustomButtons) tcCustomButtons->setAlpha(touchSettings.alpha);

                if(!hideGameAndMenu)
                {
                    tcGameMain->setEnabled(true);
                    tcGameMain->fade(touchcontrols::FADE_IN, DEFAULT_FADE_FRAMES);

                    if(showCustomOn || touchSettings.alwaysShowCust)   // Also remember if custom buttons were shown
                    {
                        tcCustomButtons->setEnabled(true);
                        tcCustomButtons->fade(touchcontrols::FADE_IN, DEFAULT_FADE_FRAMES);
                    }

                    // Bring back weapon numbers
                    if(showWeaponNumbersOn)
                    {
                        tcGameWeapons->animateIn(5);
                    }
                }

                // Always enable wheel
                if(touchSettings.weaponWheelEnabled)
                    tcWeaponWheel->setEnabled(true);

                // Alway enable the gyro when going back to the game
                allowGyro = true;

                break;

            case TS_MAP:
            {
                // This is a bit of hack. We want the map button to be in the same place as the game map button
                // So move it
                touchcontrols::Button *mapGame = (touchcontrols::Button *) tcGameMain->getControl("map");
                touchcontrols::Button *map = (touchcontrols::Button *) tcAutomap->getControl("map");
                map->controlPos = mapGame->controlPos;
                map->updateSize();

                tcAutomap->setEnabled(true);
                tcAutomap->fade(touchcontrols::FADE_IN, DEFAULT_FADE_FRAMES);
            }
                break;

            case TS_CUSTOM:
                tcCustomButtons->setEnabled(true);
                tcCustomButtons->setAlpha(1.0);
                tcCustomButtons->fade(touchcontrols::FADE_IN, DEFAULT_FADE_FRAMES);
                break;

            case TS_DEMO:
                tcDemo->setEnabled(true);
                tcDemo->fade(touchcontrols::FADE_IN, DEFAULT_FADE_FRAMES);
                break;

            case TS_MOUSE:
                tcMouse->setEnabled(true);
                tcMouse->fade(touchcontrols::FADE_IN, DEFAULT_FADE_FRAMES);
                break;

            case TS_PDA:
            {
                // Copy position from the game screen
                touchcontrols::Button *pdaGame = (touchcontrols::Button *) tcGameMain->getControl("pda");
                touchcontrols::Button *pda = (touchcontrols::Button *) tcPda->getControl("pda");
                pda->controlPos = pdaGame->controlPos;
                pda->updateSize();

                tcPda->setEnabled(true);
                tcPda->fade(touchcontrols::FADE_IN, DEFAULT_FADE_FRAMES);
            }
                break;

            case TS_CONSOLE:
                break;
        }
    }
}

void TouchInterfaceBase::createCustomControls(touchcontrols::TouchControls *customControls)
{
    customControls->addControl(new touchcontrols::Button("A", touchcontrols::RectF(5, 5, 7, 7), "Custom_1", PORT_ACT_CUSTOM_0, false, false, "Custom 1 (KP1)", touchcontrols::COLOUR_RED2));
    customControls->addControl(new touchcontrols::Button("B", touchcontrols::RectF(7, 5, 9, 7), "Custom_2", PORT_ACT_CUSTOM_1, false, false, "Custom 2 (KP2)", touchcontrols::COLOUR_RED2));
    customControls->addControl(new touchcontrols::Button("C", touchcontrols::RectF(5, 7, 7, 9), "Custom_3", PORT_ACT_CUSTOM_2, false, false, "Custom 3 (KP3)", touchcontrols::COLOUR_BLUE1));

    customControls->addControl(new touchcontrols::Button("D", touchcontrols::RectF(7, 7, 9, 9), "Custom_4", PORT_ACT_CUSTOM_3, false, false, "Custom 4 (KP4)", touchcontrols::COLOUR_BLUE1));
    customControls->addControl(new touchcontrols::Button("E", touchcontrols::RectF(5, 9, 7, 11), "Custom_5", PORT_ACT_CUSTOM_4, false, false, "Custom 5 (KP5)", touchcontrols::COLOUR_GREEN2));
    customControls->addControl(new touchcontrols::Button("F", touchcontrols::RectF(7, 9, 9, 11), "Custom_6", PORT_ACT_CUSTOM_5, false, false, "Custom 6 (KP6)", touchcontrols::COLOUR_GREEN2));

    customControls->addControl(new touchcontrols::Button("G", touchcontrols::RectF(5, 11, 7, 13), "custom_a", PORT_ACT_CUSTOM_6, false, true, "Custom 7 (KP7)", touchcontrols::COLOUR_NONE));
    customControls->addControl(new touchcontrols::Button("H", touchcontrols::RectF(7, 11, 9, 13), "custom_b", PORT_ACT_CUSTOM_7, false, true, "Custom 8 (KP8)", touchcontrols::COLOUR_NONE));
    customControls->addControl(new touchcontrols::Button("I", touchcontrols::RectF(5, 13, 7, 15), "custom_c", PORT_ACT_CUSTOM_8, false, true, "Custom 9 (KP9)", touchcontrols::COLOUR_NONE));
    customControls->addControl(new touchcontrols::Button("J", touchcontrols::RectF(7, 13, 9, 15), "custom_d", PORT_ACT_CUSTOM_9, false, true, "Custom 10 (KP0)", touchcontrols::COLOUR_NONE));

    touchcontrols::QuadSlide *qs1 = new touchcontrols::QuadSlide("quad_slide_1", touchcontrols::RectF(10, 7, 12, 9), "quad_slide", "slide_arrow", PORT_ACT_CUSTOM_10, PORT_ACT_CUSTOM_11,
                                                                 PORT_ACT_CUSTOM_12, PORT_ACT_CUSTOM_13, false, "Quad Slide 1 (A - D)");
    customControls->addControl(qs1);

    touchcontrols::QuadSlide *qs2 = new touchcontrols::QuadSlide("quad_slide_2", touchcontrols::RectF(14, 7, 16, 9), "quad_slide", "slide_arrow", PORT_ACT_CUSTOM_14, PORT_ACT_CUSTOM_15,
                                                                 PORT_ACT_CUSTOM_16, PORT_ACT_CUSTOM_17, false, "Quad Slide 2 (E - H)");
    customControls->addControl(qs2);

    touchcontrols::QuadSlide *qs3 = new touchcontrols::QuadSlide("quad_slide_3", touchcontrols::RectF(10, 11, 12, 13), "quad_slide_2", "slide_arrow", PORT_ACT_CUSTOM_18, PORT_ACT_CUSTOM_19,
                                                                 PORT_ACT_CUSTOM_20, PORT_ACT_CUSTOM_21, true, "Quad Slide 3 (I - L)");
    customControls->addControl(qs3);

    touchcontrols::QuadSlide *qs4 = new touchcontrols::QuadSlide("quad_slide_4", touchcontrols::RectF(14, 11, 16, 13), "quad_slide_2", "slide_arrow", PORT_ACT_CUSTOM_22, PORT_ACT_CUSTOM_23,
                                                                 PORT_ACT_CUSTOM_24, PORT_ACT_CUSTOM_25, true, "Quad Slide 4 (M - P)");
    customControls->addControl(qs4);
    //customControls->setColor(0.7,0.7,1.f);

    qs1->signal.connect(sigc::mem_fun(this, &TouchInterfaceBase::customButton));
    qs2->signal.connect(sigc::mem_fun(this, &TouchInterfaceBase::customButton));
    qs3->signal.connect(sigc::mem_fun(this, &TouchInterfaceBase::customButton));
    qs4->signal.connect(sigc::mem_fun(this, &TouchInterfaceBase::customButton));
    customControls->signal_button.connect(sigc::mem_fun(this, &TouchInterfaceBase::customButton));
    customControls->signal_settingsButton.connect(sigc::mem_fun(this, &TouchInterfaceBase::customSettingsButton));
    customControls->setAlpha(touchSettings.alpha);
}


void TouchInterfaceBase::keyboardKeyPressed(uint32_t key)
{
    LOGI("Keyboard press: %d , %c", key, (char) key);

    if(key == UI_KEYBOARD_HIDE)
    {
#ifdef USE_SDL3
        SDL_StopTextInput(window);
#else
        SDL_StopTextInput();
#endif
        return;
    }

    char text[2] = {0, 0};

    // Only send printable chars
    if(key >= 32 && key <= 125)
    {
        text[0] = key;
        text[1] = 0;
    }

    // Change upper case to lower case to get scan code
    if(key >= 65 && key <= 90)
    {
        key = key + 32;
    }
#ifdef USE_SDL3
    #define SDL_DEFAULT_KEYBOARD_ID    1

    SDL_Scancode sc = SDL_GetScancodeFromKey(key, NULL);
    if(sc != SDL_SCANCODE_UNKNOWN)
    {
        SDL_SendKeyboardKey(0, SDL_DEFAULT_KEYBOARD_ID, key, sc, 1);
        waitFrames(1); // Some games (EDuke) need a frame to register
        SDL_SendKeyboardKey(0, SDL_DEFAULT_KEYBOARD_ID, key, sc, 0);
    }
#else
    SDL_Scancode sc = SDL_GetScancodeFromKey(key);

    // Send scancode
    if(sc != SDL_SCANCODE_UNKNOWN)
    {
        SDL_SendKeyboardKey(SDL_PRESSED, sc);
        waitFrames(1); // Some games (EDuke) need a frame to register
        SDL_SendKeyboardKey(SDL_RELEASED, sc);
    }
#endif
    // Send text if avaliable
    if(text[0])
    {
#ifdef USE_SDL3
        SDL_SendKeyboardText(text);
#else
        SDL_SendKeyboardText(text);
#endif
    }
}

void TouchInterfaceBase::hideControls(void)
{
    if(tcGameMain)
        if(tcGameMain->isEnabled())
            tcGameMain->animateOut(30);
}


void TouchInterfaceBase::processPointer(int action, int pid, float x, float y)
{
    controlsContainer.processPointer(action, pid, x, y);
}

void TouchInterfaceBase::gamepadAction(int state, int action)
{
    LOGI("gamepadAction, %d  %d", state, action);

    bool used = false;

    switch(action)
    {
        case PORT_ACT_MENU_UP:
            used = controlsContainer.gamepadInput(state, touchcontrols::GamePadKey::UP);
            break;

        case PORT_ACT_MENU_DOWN:
            used = controlsContainer.gamepadInput(state, touchcontrols::GamePadKey::DOWN);
            break;

        case PORT_ACT_MENU_LEFT:
            used = controlsContainer.gamepadInput(state, touchcontrols::GamePadKey::LEFT);
            break;

        case PORT_ACT_MENU_RIGHT:
            used = controlsContainer.gamepadInput(state, touchcontrols::GamePadKey::RIGHT);
            break;

        case PORT_ACT_MENU_SELECT:
            used = controlsContainer.gamepadInput(state, touchcontrols::GamePadKey::SELECT);
            break;

        case PORT_ACT_MENU_BACK:
            used = controlsContainer.gamepadInput(state, touchcontrols::GamePadKey::BACK);
            break;
    }

    if(used)
        return;

    if(tcKeyboard->isEnabled())
    {
        // Do nothing for now, stops multiple controls opening
    }
    else if(tcInventory && tcInventory->isEnabled())
    {
        // Do nothing for now, stops multiple controls opening
    }
    else if(tcGamepadUtility && tcGamepadUtility->isEnabled())
    {
        // Do nothing for now, stops multiple controls opening
    }
    else if(tcDPadInventory && tcDPadInventory->isEnabled())
    {
        // Do nothing for now, stops multiple controls opening
    }
    else if(action == PORT_ACT_SHOW_KBRD)
    {
        if(state)
        {
            startTextInput();
        }
    }
    else if(action == PORT_ACT_SHOW_GP_UTILS)
    {
        if(state)
        {
            if(!tcGamepadUtility->isEnabled())
            {
                tcGamepadUtility->animateIn(5);
            }
        }
    }
    else if(tcDPadInventory && action == PORT_ACT_SHOW_DPAD_INV && currentScreenMode == TS_GAME)
    {
        if(state)
        {
            if(!tcDPadInventory->isEnabled())
            {
                //tcDPadInventory->animateIn(5);
                tcDPadInventory->setEnabled(true);
            }
        }
    }
    else if(tcInventory && action == PORT_ACT_SHOW_INV && currentScreenMode == TS_GAME)
    {
        if(state)
        {
            if(!tcInventory->isEnabled())
            {
                tcInventory->animateIn(5);
            }
        }
    }
    else if(action == PORT_ACT_USE_WEAPON_WHEEL && currentScreenMode == TS_GAME)
    {
        wheelSelect->setGamepadMode(weaponWheelGamepadMode, weaponWheelAutoTimout);
        wheelSelect->gamepadActionButton(state);
    }
    else if(action == PORT_ACT_MENU_SHOW)
    {
        if(state)
        {
            mobileBackButton();
        }
    }
    else if(action == PORT_ACT_GYRO_TOGGLE)
    {
        allowGyro = SmartToggleAction(&gyroSmartToggle, state, allowGyro);
    }
    else
    {
        PortableAction(state, action);

        if(gamepadHideTouch)
            hideControls();
    }
}

void TouchInterfaceBase::axisValue(int axis, float value)
{
    static float x = 0;
    static float y = 0;

    if(weaponWheelMoveStick)
    {
        if(axis == ANALOGUE_AXIS_SIDE)
            x = value;
        else if(axis == ANALOGUE_AXIS_FWD)
            y = value;
    }
    else
    {
        if(axis == ANALOGUE_AXIS_YAW)
            x = value;
        else if(axis == ANALOGUE_AXIS_PITCH)
            y = value;
    }

    if(wheelSelect)
        wheelSelect->processGamepad(x, y);   // return true is axis value was used
}

int TouchInterfaceBase::blockGamepad(void)
{
    if(wheelSelect)
    {
        if(wheelSelect->blockGamepad())
        {
            if(weaponWheelMoveStick)
                return ANALOGUE_AXIS_SIDE | ANALOGUE_AXIS_FWD;
            else
                return ANALOGUE_AXIS_YAW | ANALOGUE_AXIS_PITCH;
        }
        else
            return 0;
    }
    else
        return 0;
}

void TouchInterfaceBase::weaponWheelSettings(bool useMoveStick, int mode, int autoTimeout)
{
    weaponWheelMoveStick = useMoveStick;
    weaponWheelGamepadMode = (touchcontrols::WheelSelectMode) mode;
    weaponWheelAutoTimout = autoTimeout;
}

int TouchInterfaceBase::volumeKey(int state, bool volumeUp)
{
    if(currentScreenMode == TS_GAME) // Allow real volume to change when not in game
    {
        if(volumeUp)
        {
            if(volumeUpAction)
            {
                PortableAction(state, volumeUpAction);
                return 1;
            }
        }
        else
        {
            if(volumeDownAction)
            {
                PortableAction(state, volumeDownAction);
                return 1;
            }
        }
    }

    return 0;
}

bool TouchInterfaceBase::saveControlSettings(std::string path)
{
    std::string settings = path + "/settings.xml";
    touchcontrols::touchSettings_save(settings);

    tcGameMain->saveXML(path + "/tcGameMain.xml");
    tcWeaponWheel->saveXML(path + "/tcWeaponWheel.xml");
    tcGameWeapons->saveXML(path + "/tcGameWeapons.xml");

    if(tcInventory)
        tcInventory->saveXML(path + "/tcInventory.xml");

    if(tcCustomButtons)
        tcCustomButtons->saveXML(path + "/tcCustomButtons.xml");

    return false;
}

bool TouchInterfaceBase::loadControlSettings(std::string path)
{
    std::string settings = path + "/settings.xml";
    touchcontrols::touchSettings_load(settings);
    touchcontrols::touchSettings_save(); // Save over current settings

    tcGameMain->loadXML(path + "/tcGameMain.xml");
    tcGameMain->save(); // Save the newly loaded

    tcWeaponWheel->loadXML(path + "/tcWeaponWheel.xml");
    tcWeaponWheel->save();

    tcGameWeapons->loadXML(path + "/tcGameWeapons.xml");
    tcGameWeapons->save();

    if(tcInventory)
    {
        tcInventory->loadXML(path + "/tcInventory.xml");
        tcInventory->save();
    }

    if(tcCustomButtons)
    {
        tcCustomButtons->loadXML(path + "/tcCustomButtons.xml");
        tcCustomButtons->save();
    }

    return false;
}

void TouchInterfaceBase::newGameFrame()
{
    framecount++;

    // Update run button image
    if(!isWalking)
    {
        runButton->setImage(1);
    }
    else
    {
        runButton->setImage(0);
    }

    // Update hide/show joysticks
    if(touchJoyLeft)
        touchJoyLeft->setHideGraphics(!touchSettings.showLeftStick);

    if(touchJoyRight)
        touchJoyRight->setHideGraphics(!touchSettings.showRightStick);

    newFrame();
}


void TouchInterfaceBase::frameControls(bool REND_FRAMEBUFFER, bool REND_TOUCH)
{
    if(REND_FRAMEBUFFER)
    {
        if(SDL_NewEGLCreated())
        {
            LOGI("NEW EGL CONTEXT");

            if(REND_TOUCH)
            {
                newGLContext();
                controlsContainer.initGL();
            }

            touchcontrols::R_FrameBufferInit();
        }

        if(!REND_TOUCH)
        {
            openGLStart();
            openGLEnd();
        }
    }

    // Coming from surface view
    if(!REND_FRAMEBUFFER && REND_TOUCH)
    {
        static bool first = true;
        if(first)
        {
            controlsContainer.initGL();
            first = false;
        }
    }

    if(REND_TOUCH)
    {
        if(checkGfx())
            return;
#if 1
        //openGLStart();
        //openGLEnd();
        controlsContainer.draw();
#endif
    }
}


void TouchInterfaceBase::showKeyboardCallback(int show)
{
    //LOGI("showKeyboardCallback %d", show);

    if(useSystemKeyboard)
    {
        if(show)
            Android_JNI_SendMessage(COMMAND_SHOW_KEYBOARD, 0);
    }
    else
    {
        tcKeyboard->setEnabled(show);
    }
}

void TouchInterfaceBase::executeCommand(const char *cmd)
{
    PortableCommand(cmd);
}


void TouchInterfaceBase::showMouseCallback(int show)
{
    // LOGI("showMouseCallback = %d", show);
    gameShowMouse = show;
}

void TouchInterfaceBase::moveMouseCallback(float x, float y)
{
    //LOGI("moveMouseCallback = %f, %f", x, y);
    gotMouseMove = x || y; // Got mouse if it's not zero
    controlsContainer.mousePos(x, y);
}

#ifdef DSDA_DOOM // DSDA separates rendering and game tick, we want waitFrames to act on gameticks
extern "C" uint32_t DSDA_GAME_TICKER;
#define framecount DSDA_GAME_TICKER
#endif

//Wait for N number of frame to pass. This MUST NOT be called from the game thread
void TouchInterfaceBase::waitFrames(int nbrFrames)
{
    uint64_t startTime = touchcontrols::getMS();

    int frameNow = framecount;

    // Wait for n Frames, OR 500ms has passed for safety
    while((frameNow + nbrFrames + 1 > framecount) && ((touchcontrols::getMS() - startTime) < 500))
    {
        usleep(1000); // wait 1ms
    }
}

void TouchInterfaceBase::startTextInput()
{
#ifdef USE_SDL3
    SDL_StartTextInput(window);
#else
    SDL_StartTextInput();
#endif
}