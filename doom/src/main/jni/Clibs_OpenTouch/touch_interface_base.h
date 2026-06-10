//
// Created by emile on 01/01/2021.
//

#ifndef OPENTOUCH_TOUCH_INTERFACE_BASE_H
#define OPENTOUCH_TOUCH_INTERFACE_BASE_H


#include "TouchControlsInterface.h"
#include "TouchControlsContainer.h"

#include "game_interface.h"

#include "UI_TouchDefaultSettings.h"

#include "SmartToggle.h"

#define KEY_SHOW_WEAPONS 0x1000
#define KEY_SHOOT        0x1001

#define KEY_SHOW_INV     0x1006
#define KEY_QUICK_CMD    0x1007
#define KEY_SHOW_KBRD    0x1008
#define KEY_SHOW_CUSTOM  0x1009

#define KEY_F10  0x100A
#define KEY_BACK_BUTTON  0x100B
#define KEY_SHOW_GYRO    0x100C
#define KEY_SHOW_GAMEPAD 0x100D
#define KEY_USE_MOUSE    0x100E
#define KEY_LEFT_MOUSE   0x100F
#define KEY_LOAD_SAVE_CONTROLS 0x1010
#define KEY_QUICK_COMMANDS     0x1011
#define KEY_ZDOOM_CLEAR_BIND     0x1012
#define KEY_RIGHT_MOUSE   0x10013

#define DEMO_ALPHA_RESET      1
#define DEMO_ALPFA_DEC        0.001f

class TouchInterfaceBase
{
    int licTest = 0;
public:

    int gameType = 0;
    int wheelNbr = 10;

    int nativeWidth = 0;
    int nativeHeight = 0;

    bool gamepadHideTouch = false;
    bool hideGameAndMenu = false;
    bool useSystemKeyboard = false;

    bool weaponWheelMoveStick = true;
    touchcontrols::WheelSelectMode weaponWheelGamepadMode = touchcontrols::WHEELSELECT_GP_MODE_HOLD;
    int weaponWheelAutoTimout = 0;

    int leftDoubleAction = 0;
    int rightDoubleAction = 0;
    int volumeUpAction = 0;
    int volumeDownAction = 0;

    touchscreemode_t currentScreenMode = TS_BLANK;
    touchcontrols::tTouchSettings touchSettings;
    bool useMouse = false;
    bool gameShowMouse = false;
    bool isShooting = false;
    bool showWeaponNumbersOn = false;
    bool showCustomOn = false;
    bool showCustomMenu = false;
    bool gotMouseMove = false; // Set to true if we ever got a mouse mouse from SDL, so we can choose if to show the mouse icon

    // For Doom
    int mapState = 0;
    float demoControlsAlpha = 0; // Used to fade out demo controls
    SmartToggle_t gyroSmartToggle;

    bool enableReloadSniperMode = false; // Set to true to enable sniper mdoe for the reload button
    bool sniperMode = false; // Reduce look sensitivity when reload being held down
    uint64_t reloadDownTime = 0;

    int isWalking = -1; // Run toggle. Initial -1 so first time settings update gets called it can be set to its default
    SmartToggle_t runSmartToggle;

    int framecount = 0;

    touchcontrols::TouchControlsContainer controlsContainer;

    touchcontrols::UI_Controls *UI_tc = 0;

    touchcontrols::TouchControls *tcMenuMain = 0;
    touchcontrols::TouchControls *tcYesNo = 0;
    touchcontrols::TouchControls *tcGameMain = 0;
    touchcontrols::TouchControls *tcGameWeapons = 0;
    touchcontrols::TouchControls *tcWeaponWheel = 0;
    touchcontrols::TouchControls *tcInventory = 0;
    touchcontrols::TouchControls *tcAutomap = 0;
    touchcontrols::TouchControls *tcBlank = 0;
    touchcontrols::TouchControls *tcKeyboard = 0;
    touchcontrols::TouchControls *tcCustomButtons = 0;
    touchcontrols::TouchControls *tcDemo = 0;
    touchcontrols::TouchControls *tcGamepadUtility = 0;
    touchcontrols::TouchControls *tcDPadInventory = 0;
    touchcontrols::TouchControls *tcMouse = 0;
    touchcontrols::TouchControls *tcPda = 0;


    // So can hide and show these buttons
    touchcontrols::TouchJoy *touchJoyLeft;
    touchcontrols::TouchJoy *touchJoyRight;

    // So gampad can control keyboard
    touchcontrols::UI_Keyboard *uiKeyboard;

    // So gamepad can access it
    touchcontrols::WheelSelect *wheelSelect;
    touchcontrols::ButtonGrid *uiInventoryButtonGrid;

    // Common 'Run/Sprint' button
    touchcontrols::Button *runButton;

    virtual void createControls(std::string filesPath) = 0;

    virtual void openGLEnd() = 0;

    virtual void openGLStart() = 0;

    virtual void blankButton(int state, int code) = 0;

    virtual void newFrame() = 0;

    virtual void newGLContext() = 0;

    void init(int width, int height, const char *pngPath, const char *touchSettingsPath, int options, int wheelNbr_, int game);

    int touchActionToAction(int action);

    void touchSettingsCallback(touchcontrols::tTouchSettings settings);

    void gameButton(int state, int code);

    void menuButton(int state, int code);

    void leftStick(float joy_x, float joy_y, float mouse_x, float mouse_y);

    void rightStick(float joy_x, float joy_y, float mouse_x, float mouse_y);

    void inventoryButton(int state, int code);

    void inventoryOutside(bool fromGamepad);

    void gameUtilitiesButton(int state, int code);

    void gameUtilitiesOutside(bool fromGamepad);

    void customButton(int state, int code);

    //Weapon wheel callbacks
    void weaponWheelSelected(int enabled);

    void weaponWheel(int segment);

    void selectWeaponButton(int state, int code);

    void mouseMove(int action, float x, float y, float mouse_x, float mouse_y);

    void mouseButton(int state, int code);

    void AutomapMultitouchMouseMove(int action, float x, float y, float dx, float dy);

    void brightnessSlideMouse(int action, float x, float y, float dx, float dy);

    void dPadInventoryButton(int state, int code);

    void dPadInventoryOutside(bool fromGamepad);

    void leftDoubleTap(int state);

    void rightDoubleTap(int state);

    void vibrate(int duration);

    void gameSettingsButton(int state);

    void customSettingsButton(int state);

    void mobileBackButton(void);

    void updateTouchScreenModeOut(touchscreemode_t mode);

    void updateTouchScreenModeIn(touchscreemode_t mode);

    void createCustomControls(touchcontrols::TouchControls *customControls);

    void keyboardKeyPressed(uint32_t key);

    void hideControls(void);

    void processPointer(int action, int pid, float x, float y);

    void gamepadAction(int state, int action);

    void axisValue(int axis, float value);

    int blockGamepad(void);

    void weaponWheelSettings(bool useMoveStick, int mode, int autoTimeout);

    int volumeKey(int state, bool volumeUp);

    bool saveControlSettings(std::string path);

    bool loadControlSettings(std::string path);

    void executeCommand(const char *cmd);

    void frameControls(bool REND_FRAMEBUFFER, bool REND_TOUCH);

    void newGameFrame();

    void showKeyboardCallback(int show);

    void showMouseCallback(int show);

    void moveMouseCallback(float x, float y);

    void waitFrames(int nbrFrames);

    int isPlayerRunning()
    {
        return (isWalking == 0);
    }

    void startTextInput();
};


#endif //OPENTOUCH_TOUCH_INTERFACE_BASE_H
