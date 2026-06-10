//
// Created by emile on 01/01/2021.
//

#include "touch_interface.h"
#include "Framebuffer.h"
#include "quake_game_dll.h"
#include "SDL_keycode.h"
#include <fstream>


extern "C"
{

#ifdef DARKPLACES
//These are in gl_backend
    extern int android_reset_vertex;
    extern int android_reset_color;
    extern int android_reset_tex;
#endif

void jwzgles_restore(void);

#ifdef YQUAKE2
extern int yquake2Renderer;
#endif

void nanoPushState();
void nanoPopState();
void BE_FixPointers();
void GL_FixState();

void PortableAutomapControl(float zoom, float x, float y)
{
    // Blank
}
}


void TouchInterface::openGLStart()
{
    touchcontrols::gl_startRender();
#ifdef QUAKE2
    nanoPushState();
#endif
};

void TouchInterface::openGLEnd()
{
    touchcontrols::gl_endRender();

#ifdef FTEQW

    if(touchcontrols::fb_getGLESVersion() == 1)
    {
        BE_FixPointers();
    }

#endif

#ifdef DARKPLACES
    android_reset_vertex = 1;
    android_reset_color = 1;
    android_reset_tex = 1;
#endif

#ifdef QUAKE3
    GL_FixState();
#endif


#ifdef QUAKE2
    nanoPopState();
#endif

#ifdef QUAKESPASM
    jwzgles_restore();
#endif

#ifdef QUAKESPASM_SPIKED
    touchcontrols::gl_resetGL4ES();
#endif
};


void TouchInterface::createControls(std::string filesPath)
{
    tcMenuMain = new touchcontrols::TouchControls("menu", false, true, 10, false);
    tcYesNo = new touchcontrols::TouchControls("yes_no", false, false);
    tcGameMain = new touchcontrols::TouchControls("game", false, true, 1, true);
    tcGameWeapons = new touchcontrols::TouchControls("weapons", false, true, 1, false);
    tcInventory = new touchcontrols::TouchControls("inventory", false, true, 2, false); //Different edit group
    tcWeaponWheel = new touchcontrols::TouchControls("weapon_wheel", false, true, 1, false);
    // Hide the cog because when using the gamepad and weapon wheel is enabled, the cog will show otherwise
    tcWeaponWheel->hideEditButton = true;

    //tcAutomap = new touchcontrols::TouchControls("automap",false,false);
    tcBlank = new touchcontrols::TouchControls("blank", true, false);
    tcCustomButtons = new touchcontrols::TouchControls("custom_buttons", false, true, 1, true);
    tcKeyboard = new touchcontrols::TouchControls("keyboard", false, false);
    //tcDemo = new touchcontrols::TouchControls("demo_playback",false,false);
    tcGamepadUtility = new touchcontrols::TouchControls("gamepad_utility", false, false);
    tcDPadInventory = new touchcontrols::TouchControls("dpad_inventory", false, false);
    tcMouse = new touchcontrols::TouchControls("mouse", false, false);

    // Show main game controls when editing custom buttons
    tcCustomButtons->setEditBackgroundControl(tcGameMain);

    //Menu -------------------------------------------
    //------------------------------------------------------
    tcMenuMain->setFixAspect(true);
    tcMenuMain->addControl(new touchcontrols::Button("back", touchcontrols::RectF(0, 0, 2, 2), "back_button", KEY_BACK_BUTTON));
#ifndef QUAKE3

#if !defined(WRATH) // No keyboard in menu for Wrath
    tcMenuMain->addControl(new touchcontrols::Button("down_arrow", touchcontrols::RectF(20, 13, 23, 16), "arrow_down", PORT_ACT_MENU_DOWN));
    tcMenuMain->addControl(new touchcontrols::Button("up_arrow", touchcontrols::RectF(20, 10, 23, 13), "arrow_up", PORT_ACT_MENU_UP));
    tcMenuMain->addControl(new touchcontrols::Button("left_arrow", touchcontrols::RectF(17, 13, 20, 16), "arrow_left", PORT_ACT_MENU_LEFT));
    tcMenuMain->addControl(new touchcontrols::Button("right_arrow", touchcontrols::RectF(23, 13, 26, 16), "arrow_right", PORT_ACT_MENU_RIGHT));

    tcMenuMain->addControl(new touchcontrols::Button("enter", touchcontrols::RectF(0, 10, 6, 16), "enter", PORT_ACT_MENU_SELECT));
#endif

    tcMenuMain->addControl(new touchcontrols::Button("keyboard", touchcontrols::RectF(2, 0, 4, 2), "keyboard", KEY_SHOW_KBRD));
    tcMenuMain->addControl(new touchcontrols::Button("console", touchcontrols::RectF(6, 0, 8, 2), "tild", PORT_ACT_CONSOLE));
    tcMenuMain->addControl(new touchcontrols::Button("show_custom", touchcontrols::RectF(9, 0, 11, 2), "custom_show", KEY_SHOW_CUSTOM));

    tcMenuMain->addControl(new touchcontrols::Button("gamepad", touchcontrols::RectF(22, 0, 24, 2), "gamepad", KEY_SHOW_GAMEPAD));
    tcMenuMain->addControl(new touchcontrols::Button("gyro", touchcontrols::RectF(24, 0, 26, 2), "gyro", KEY_SHOW_GYRO));
    tcMenuMain->addControl(new touchcontrols::Button("load_save_touch", touchcontrols::RectF(20, 0, 22, 2), "touchscreen_save", KEY_LOAD_SAVE_CONTROLS));

    touchcontrols::Mouse *brightnessSlide = new touchcontrols::Mouse("slide_mouse", touchcontrols::RectF(24, 3, 26, 11), "brightness_slider");
    brightnessSlide->signal_action.connect(sigc::mem_fun(this, &TouchInterface::brightnessSlideMouse));
    tcMenuMain->addControl(brightnessSlide);

    touchcontrols::Mouse *mouseMainMenu = new touchcontrols::Mouse("mouse", touchcontrols::RectF(0, 2, 26, 16), "");
    mouseMainMenu->setHideGraphics(true);
    mouseMainMenu->setEditable(false);
    tcMenuMain->addControl(mouseMainMenu);
    mouseMainMenu->signal_action.connect(sigc::mem_fun(this, &TouchInterface::mouseMove));
#else // QUAKE 3 menu is different

    tcMenuMain->addControl(new touchcontrols::Button("keyboard", touchcontrols::RectF(2, 0, 4, 2), "keyboard", KEY_SHOW_KBRD));
    tcMenuMain->addControl(new touchcontrols::Button("console", touchcontrols::RectF(6, 0, 8, 2), "tild", PORT_ACT_CONSOLE));

    tcMenuMain->addControl(new touchcontrols::Button("gamepad", touchcontrols::RectF(22, 0, 24, 2), "gamepad", KEY_SHOW_GAMEPAD));
    tcMenuMain->addControl(new touchcontrols::Button("gyro", touchcontrols::RectF(24, 0, 26, 2), "gyro", KEY_SHOW_GYRO));
    tcMenuMain->addControl(new touchcontrols::Button("load_save_touch", touchcontrols::RectF(20, 0, 22, 2), "touchscreen_save", KEY_LOAD_SAVE_CONTROLS));

    // Stop buttons passing though control, otherwise goes wierd with mouse
    touchcontrols::Button *b;

    b = new touchcontrols::Button("left_arrow", touchcontrols::RectF(1, 10, 3, 12), "arrow_left", PORT_ACT_MENU_LEFT);
    b->setAllowPassThrough(false);
    tcMenuMain->addControl(b);

    b = new touchcontrols::Button("right_arrow", touchcontrols::RectF(3, 10, 5, 12), "arrow_right", PORT_ACT_MENU_RIGHT);
    b->setAllowPassThrough(false);
    tcMenuMain->addControl(b);

    b = new touchcontrols::Button("up_arrow", touchcontrols::RectF(3, 12, 5, 14), "arrow_up", PORT_ACT_MENU_UP);
    b->setAllowPassThrough(false);
    tcMenuMain->addControl(b);

    b = new touchcontrols::Button("down_arrow", touchcontrols::RectF(3, 14, 5, 16), "arrow_down", PORT_ACT_MENU_DOWN);
    b->setAllowPassThrough(false);
    tcMenuMain->addControl(b);

    b = new touchcontrols::Button("enter", touchcontrols::RectF(0, 12, 3, 16), "enter", PORT_ACT_MENU_SELECT);
    b->setAllowPassThrough(false);
    tcMenuMain->addControl(b);

    b = new touchcontrols::Button("left_mouse", touchcontrols::RectF(0, 6, 2, 9), "left_mouse", PORT_ACT_MOUSE_LEFT);
    b->setAllowPassThrough(false);
    tcMenuMain->addControl(b);

    touchcontrols::Mouse *brightnessSlide = new touchcontrols::Mouse("slide_mouse", touchcontrols::RectF(24, 3, 26, 11), "brightness_slider");
    brightnessSlide->signal_action.connect(sigc::mem_fun(this, &TouchInterface:: brightnessSlideMouse));
    brightnessSlide->setAllowPassThrough(false);
    tcMenuMain->addControl(brightnessSlide);

    // Mouse at end
    touchcontrols::Mouse *mouse = new touchcontrols::Mouse("mouse", touchcontrols::RectF(0, 2, 26, 16), "");
    mouse->setHideGraphics(true);
    mouse->setEditable(false);
    tcMenuMain->addControl(mouse);
    mouse->signal_action.connect(sigc::mem_fun(this, &TouchInterface::mouseMove));
#endif

    tcMenuMain->signal_button.connect(sigc::mem_fun(this, &TouchInterface::menuButton));
    tcMenuMain->setAlpha(0.8);

    //Game -------------------------------------------
    //------------------------------------------------------
    tcGameMain->setAlpha(touchSettings.alpha);
    tcGameMain->addControl(new touchcontrols::Button("back", touchcontrols::RectF(0, 0, 2, 2), "back_button", KEY_BACK_BUTTON, false, false, "Show menu"));
    tcGameMain->addControl(new touchcontrols::Button("attack", touchcontrols::RectF(21, 5, 24, 8), "shoot", KEY_SHOOT, false, false, "Attack!"));
    tcGameMain->addControl(new touchcontrols::Button("attack2", touchcontrols::RectF(3, 5, 6, 8), "shoot", KEY_SHOOT, false, true, "Attack! (duplicate)"));

#if defined(WRATH)
    tcGameMain->addControl(new touchcontrols::Button("use", touchcontrols::RectF(24, 6, 26, 8), "use", PORT_WRATH_USE, false, false, "Use/Open"));
#endif

#ifdef QUAKE3
    tcGameMain->addControl(new touchcontrols::Button("use_inventory", touchcontrols::RectF(0, 9, 2, 11), "inventory", PORT_ACT_USE, false, false, "Use item"));
    tcGameMain->addControl(new touchcontrols::Button("zoom", touchcontrols::RectF(24, 0, 26, 2), "binocular", PORT_ACT_ZOOM_IN, false, false, "Weapon zoom"));
#else
    tcGameMain->addControl(new touchcontrols::Button("quick_save", touchcontrols::RectF(24, 0, 26, 2), "save", PORT_ACT_QUICKSAVE, false, false, "Quick save"));
    tcGameMain->addControl(new touchcontrols::Button("quick_load", touchcontrols::RectF(20, 0, 22, 2), "load", PORT_ACT_QUICKLOAD, false, false, "Quick load"));
#endif
    tcGameMain->addControl(new touchcontrols::Button("keyboard", touchcontrols::RectF(8, 0, 10, 2), "keyboard", KEY_SHOW_KBRD, false, false, "Show Keyboard"));
    tcGameMain->addControl(new touchcontrols::Button("showscores", touchcontrols::RectF(17, 0, 19, 2), "scores", PORT_ACT_MP_SCORES, false, true, "Show Scores"));
    tcGameMain->addControl(new touchcontrols::Button("jump", touchcontrols::RectF(24, 3, 26, 5), "jump", PORT_ACT_JUMP, false, false, "Jump/Swim up"));
    tcGameMain->addControl(new touchcontrols::Button("quick_command", touchcontrols::RectF(21, 3, 23, 5), "star", KEY_QUICK_COMMANDS, false, true, "Quick Commands"));

    if(gameType == Q1HEXEN2)
        tcGameMain->addControl(new touchcontrols::Button("crouch", touchcontrols::RectF(24, 14, 26, 16), "crouch", PORT_ACT_CROUCH, false, false, "Crouch/Swim down"));
    else
        tcGameMain->addControl(new touchcontrols::Button("crouch", touchcontrols::RectF(24, 14, 26, 16), "crouch", PORT_ACT_DOWN, false, false, "Crouch/Swim down"));

#if defined(WRATH) // Move alt attack to the left and unhide
    tcGameMain->addControl(new touchcontrols::Button("attack_alt", touchcontrols::RectF(3, 7, 5, 9), "shoot_alt", PORT_ACT_ALT_ATTACK, false, false, "Alt attack"));
    tcGameMain->addControl(new touchcontrols::Button("attack_alt2", touchcontrols::RectF(17, 7, 19, 9), "shoot_alt", PORT_ACT_ALT_ATTACK, false, false, "Alt attack (duplicate)"));
#else
    tcGameMain->addControl(new touchcontrols::Button("attack_alt", touchcontrols::RectF(21, 5, 23, 7), "shoot_alt", PORT_ACT_ALT_ATTACK, false, true, "Alt attack (Mouse 2)"));
#endif

    tcGameMain->addControl(runButton); // Common run button created in touch_interface_base

    bool hideQ2 = true;

#if defined(QUAKE2) || defined(YQUAKE2)
    hideQ2 = false;
#endif

    // If Hexen 2, also show these
    if(gameType == Q1HEXEN2)
        hideQ2 = false;

    tcGameMain->addControl(new touchcontrols::Button("use_inventory", touchcontrols::RectF(0, 9, 2, 11), "inventory", KEY_SHOW_INV, false, hideQ2, "Show Inventory"));
    tcGameMain->addControl(new touchcontrols::Button("help_comp", touchcontrols::RectF(2, 0, 4, 2), "gamma", PORT_ACT_HELPCOMP, false, hideQ2, "PDA"));

    tcGameMain->addControl(new touchcontrols::Button("show_custom", touchcontrols::RectF(0, 2, 2, 4), "custom_show", KEY_SHOW_CUSTOM, false, true, "Show custom"));
    tcGameMain->addControl(new touchcontrols::Button("show_weapons", touchcontrols::RectF(12, 14, 14, 16), "show_weapons", KEY_SHOW_WEAPONS, false, false, "Show numbers"));
    tcGameMain->addControl(new touchcontrols::Button("console", touchcontrols::RectF(6, 0, 8, 2), "tild", PORT_ACT_CONSOLE, false, true, "Console"));

#if defined(WRATH)
    tcGameMain->addControl(new touchcontrols::Button("notebook", touchcontrols::RectF(4, 0, 6, 2), "notebook", PORT_ACT_HELPCOMP, false, false, "Notebook"));
    tcGameMain->addControl(new touchcontrols::Button("open_runes", touchcontrols::RectF(17, 0, 19, 2), "rune", PORT_ACT_INVEN, false, false, "Artifact (smart toggle)"));
    tcGameMain->addControl(new touchcontrols::Button("use_rune", touchcontrols::RectF(21, 3, 23, 5), "fist", PORT_ACT_INVUSE, false, false, "Use Artifact"));
#endif

    if(gameType != Q1MALICE)
    {
        tcGameMain->addControl(new touchcontrols::Button("next_weapon", touchcontrols::RectF(0, 3, 3, 5), "next_weap", PORT_ACT_NEXT_WEP, false, false, "Next weapon"));
        tcGameMain->addControl(new touchcontrols::Button("prev_weapon", touchcontrols::RectF(0, 5, 3, 7), "prev_weap", PORT_ACT_PREV_WEP, false, false, "Prev weapon"));
    }
    else // MALICE
    {
        tcGameMain->addControl(new touchcontrols::Button("next_weapon", touchcontrols::RectF(0, 4, 3, 6), "next_weap", PORT_ACT_NEXT_WEP));
        tcGameMain->addControl(new touchcontrols::Button("malice_reload", touchcontrols::RectF(0, 6, 3, 9), "reload", PORT_MALICE_RELOAD));
        tcGameMain->addControl(new touchcontrols::Button("malice_use", touchcontrols::RectF(22, 3, 24, 5), "use", PORT_MALICE_USE));
        tcGameMain->addControl(new touchcontrols::Button("malice_cycle", touchcontrols::RectF(15, 0, 17, 2), "cycle", PORT_MALICE_CYCLE));
    }

    // Slight mechanical destruction for YQ2
    if(gameType == Q2DLL_SMD)
    {
        tcGameMain->addControl(new touchcontrols::Button("use", touchcontrols::RectF(22, 3, 24, 5), "use", PORT_SMD_USE, false, false, "Use"));
    }

    touchcontrols::ButtonGrid *dpad = new touchcontrols::ButtonGrid("dpad_move", touchcontrols::RectF(6, 3, 12, 7), "", 3, 2, true, "Movement btns (WASD)");

    dpad->addCell(0, 1, "direction_left", PORT_ACT_MOVE_LEFT);
    dpad->addCell(2, 1, "direction_right", PORT_ACT_MOVE_RIGHT);
    dpad->addCell(1, 0, "direction_up", PORT_ACT_FWD);
    dpad->addCell(1, 1, "direction_down", PORT_ACT_BACK);
    tcGameMain->addControl(dpad);

    touchJoyRight = new touchcontrols::TouchJoy("touch", touchcontrols::RectF(17, 4, 26, 16), "look_arrow", "fixed_stick_circle");
    tcGameMain->addControl(touchJoyRight);
    touchJoyRight->signal_move.connect(sigc::mem_fun(this, &TouchInterface::rightStick));
    touchJoyRight->signal_double_tap.connect(sigc::mem_fun(this, &TouchInterface::rightDoubleTap));

    touchJoyLeft = new touchcontrols::TouchJoy("stick", touchcontrols::RectF(0, 7, 8, 16), "strafe_arrow", "fixed_stick_circle");
    tcGameMain->addControl(touchJoyLeft);
    touchJoyLeft->signal_move.connect(sigc::mem_fun(this, &TouchInterface::leftStick));
    touchJoyLeft->signal_double_tap.connect(sigc::mem_fun(this, &TouchInterface::leftDoubleTap));

    // SWAPFIX
    touchJoyLeft->registerTouchJoySWAPFIX(touchJoyRight);
    touchJoyRight->registerTouchJoySWAPFIX(touchJoyLeft);

    tcGameMain->signal_button.connect(sigc::mem_fun(this, &TouchInterface::gameButton));
    tcGameMain->signal_settingsButton.connect(sigc::mem_fun(this, &TouchInterface::gameSettingsButton));

    // Also now allow menu to enter the settings
    tcMenuMain->signal_settingsButton.connect(sigc::mem_fun(this, &TouchInterface::gameSettingsButton));


    //Weapons -------------------------------------------
    //------------------------------------------------------
    tcGameWeapons->addControl(new touchcontrols::Button("weapon1", touchcontrols::RectF(1, 14, 3, 16), "key_1", 1));
    tcGameWeapons->addControl(new touchcontrols::Button("weapon2", touchcontrols::RectF(3, 14, 5, 16), "key_2", 2));
    tcGameWeapons->addControl(new touchcontrols::Button("weapon3", touchcontrols::RectF(5, 14, 7, 16), "key_3", 3));
    tcGameWeapons->addControl(new touchcontrols::Button("weapon4", touchcontrols::RectF(7, 14, 9, 16), "key_4", 4));
    tcGameWeapons->addControl(new touchcontrols::Button("weapon5", touchcontrols::RectF(9, 14, 11, 16), "key_5", 5));

    tcGameWeapons->addControl(new touchcontrols::Button("weapon6", touchcontrols::RectF(15, 14, 17, 16), "key_6", 6));
    tcGameWeapons->addControl(new touchcontrols::Button("weapon7", touchcontrols::RectF(17, 14, 19, 16), "key_7", 7));
    tcGameWeapons->addControl(new touchcontrols::Button("weapon8", touchcontrols::RectF(19, 14, 21, 16), "key_8", 8));
    tcGameWeapons->addControl(new touchcontrols::Button("weapon9", touchcontrols::RectF(21, 14, 23, 16), "key_9", 9));
    tcGameWeapons->addControl(new touchcontrols::Button("weapon0", touchcontrols::RectF(23, 14, 25, 16), "key_0", 0));

    tcGameWeapons->signal_button.connect(sigc::mem_fun(this, &TouchInterface::selectWeaponButton));
    tcGameWeapons->setAlpha(0.8);

    //Weapon wheel -------------------------------------------
    //------------------------------------------------------
    wheelSelect = new touchcontrols::WheelSelect("weapon_wheel", touchcontrols::RectF(7, 2, 19, 14), "weapon_wheel_%d", wheelNbr);
    wheelSelect->signal_selected.connect(sigc::mem_fun(this, &TouchInterface::weaponWheel));
    wheelSelect->signal_enabled.connect(sigc::mem_fun(this, &TouchInterface::weaponWheelSelected));
    tcWeaponWheel->addControl(wheelSelect);

    if(touchSettings.weaponWheelOpaque)
        tcWeaponWheel->setAlpha(0.8);
    else
        tcWeaponWheel->setAlpha(touchSettings.alpha);

    // Inventory -------------------------------------------
    //------------------------------------------------------

    uiInventoryButtonGrid = new touchcontrols::ButtonGrid("inventory_grid", touchcontrols::RectF(3, 9, 11, 11), "inventory_bg", 4, 1);
    uiInventoryButtonGrid->addCell(0, 0, "inventory_left", PORT_ACT_INVPREV);
    uiInventoryButtonGrid->addCell(1, 0, "inventory_right", PORT_ACT_INVNEXT);
    uiInventoryButtonGrid->addCell(2, 0, "inventory_use", PORT_ACT_INVUSE);
    uiInventoryButtonGrid->addCell(3, 0, "inventory_drop", PORT_ACT_INVDROP);


    uiInventoryButtonGrid->signal_outside.connect(sigc::mem_fun(this, &TouchInterface::inventoryOutside));

    tcInventory->addControl(uiInventoryButtonGrid);
    tcInventory->setPassThroughTouch(touchcontrols::TouchControls::PassThrough::NO_CONTROL);
    tcInventory->signal_button.connect(sigc::mem_fun(this, &TouchInterface::inventoryButton));
    tcInventory->setAlpha(0.9);

    //Blank -------------------------------------------
    //------------------------------------------------------
    tcBlank->addControl(new touchcontrols::Button("enter", touchcontrols::RectF(0, 0, 26, 16), "", 0x123));
    tcBlank->signal_button.connect(sigc::mem_fun(this, &TouchInterface::blankButton));

    //Keyboard -------------------------------------------
    //------------------------------------------------------
    uiKeyboard = new touchcontrols::UI_Keyboard("keyboard", touchcontrols::RectF(0, 8, 26, 16), "font_dual", 0, 0, 0);
    uiKeyboard->signal.connect(sigc::mem_fun(this, &TouchInterface::keyboardKeyPressed));
    tcKeyboard->addControl(uiKeyboard);
    // We want touch to pass through only where there is no keyboard
    tcKeyboard->setPassThroughTouch(touchcontrols::TouchControls::PassThrough::NO_CONTROL);

    //Yes No -------------------------------------------
    //------------------------------------------------------
    tcYesNo->addControl(new touchcontrols::Button("yes", touchcontrols::RectF(8, 12, 11, 15), "key_y", PORT_ACT_MENU_CONFIRM));
    tcYesNo->addControl(new touchcontrols::Button("no", touchcontrols::RectF(15, 12, 18, 15), "key_n", PORT_ACT_MENU_ABORT));
    tcYesNo->signal_button.connect(sigc::mem_fun(this, &TouchInterface::menuButton));
    tcYesNo->setAlpha(0.8);

    //Custom Controls -------------------------------------------
    //------------------------------------------------------
    tcCustomButtons->addControl(new touchcontrols::Button("A", touchcontrols::RectF(5, 5, 7, 7), "Custom_1", PORT_ACT_CUSTOM_0, false, false, "Custom 1 (H)", touchcontrols::COLOUR_RED2));
    tcCustomButtons->addControl(new touchcontrols::Button("B", touchcontrols::RectF(7, 5, 9, 7), "Custom_2", PORT_ACT_CUSTOM_1, false, false, "Custom 2 (I)", touchcontrols::COLOUR_RED2));
    tcCustomButtons->addControl(new touchcontrols::Button("C", touchcontrols::RectF(5, 7, 7, 9), "Custom_3", PORT_ACT_CUSTOM_2, false, false, "Custom 3 (J)", touchcontrols::COLOUR_BLUE1));

    tcCustomButtons->addControl(new touchcontrols::Button("D", touchcontrols::RectF(7, 7, 9, 9), "Custom_4", PORT_ACT_CUSTOM_3, false, false, "Custom 4 (K)", touchcontrols::COLOUR_BLUE1));
    tcCustomButtons->addControl(new touchcontrols::Button("E", touchcontrols::RectF(5, 9, 7, 11), "Custom_5", PORT_ACT_CUSTOM_4, false, false, "Custom 5 (L)", touchcontrols::COLOUR_GREEN2));
    tcCustomButtons->addControl(new touchcontrols::Button("F", touchcontrols::RectF(7, 9, 9, 11), "Custom_6", PORT_ACT_CUSTOM_5, false, false, "Custom 6 (M)", touchcontrols::COLOUR_GREEN2));

    tcCustomButtons->addControl(new touchcontrols::Button("G", touchcontrols::RectF(5, 11, 7, 13), "custom_a", PORT_ACT_CUSTOM_6, false, true, "Custom 7 (N)", touchcontrols::COLOUR_NONE));
    tcCustomButtons->addControl(new touchcontrols::Button("H", touchcontrols::RectF(7, 11, 9, 13), "custom_b", PORT_ACT_CUSTOM_7, false, true, "Custom 8 (O)", touchcontrols::COLOUR_NONE));
    tcCustomButtons->addControl(new touchcontrols::Button("I", touchcontrols::RectF(5, 13, 7, 15), "custom_c", PORT_ACT_CUSTOM_8, false, true, "Custom 9 (P)", touchcontrols::COLOUR_NONE));
    tcCustomButtons->addControl(new touchcontrols::Button("J", touchcontrols::RectF(7, 13, 9, 15), "custom_d", PORT_ACT_CUSTOM_9, false, true, "Custom 10 (Q)", touchcontrols::COLOUR_NONE));

    touchcontrols::QuadSlide *qs1 = new touchcontrols::QuadSlide("quad_slide_1", touchcontrols::RectF(10, 7, 12, 9), "quad_slide", "slide_arrow", PORT_ACT_CUSTOM_10, PORT_ACT_CUSTOM_11,
                                                                 PORT_ACT_CUSTOM_12, PORT_ACT_CUSTOM_13, false, "Quad Slide 1 (R - U)");
    tcCustomButtons->addControl(qs1);

    touchcontrols::QuadSlide *qs2 = new touchcontrols::QuadSlide("quad_slide_2", touchcontrols::RectF(14, 7, 16, 9), "quad_slide", "slide_arrow", PORT_ACT_CUSTOM_14, PORT_ACT_CUSTOM_15,
                                                                 PORT_ACT_CUSTOM_16, PORT_ACT_CUSTOM_17, false, "Quad Slide 2 (V - Y)");
    tcCustomButtons->addControl(qs2);

    //tcCustomButtons->setColor(0.7,0.7,1.f);

    qs1->signal.connect(sigc::mem_fun(this, &TouchInterface::customButton));
    qs2->signal.connect(sigc::mem_fun(this, &TouchInterface::customButton));

    tcCustomButtons->signal_button.connect(sigc::mem_fun(this, &TouchInterface::customButton));
    tcCustomButtons->signal_settingsButton.connect(sigc::mem_fun(this, &TouchInterface::customSettingsButton));
    tcCustomButtons->setAlpha(touchSettings.alpha);

    //Gamepad utility -------------------------------------------
    //------------------------------------------------------
    touchcontrols::ButtonGrid *gamepadUtils = new touchcontrols::ButtonGrid("gamepad_grid", touchcontrols::RectF(8, 5, 18, 11), "gamepad_utils_bg", 3, 2);

    gamepadUtils->addCell(0, 0, "back_button", KEY_BACK_BUTTON);
    gamepadUtils->addCell(0, 1, "scores", PORT_ACT_MP_SCORES);
    gamepadUtils->addCell(1, 0, "keyboard", KEY_SHOW_KBRD);
    gamepadUtils->addCell(1, 1, "tild", PORT_ACT_CONSOLE);
    gamepadUtils->addCell(2, 0, "save", PORT_ACT_QUICKSAVE);
    gamepadUtils->addCell(2, 1, "load", PORT_ACT_QUICKLOAD);

    gamepadUtils->signal_outside.connect(sigc::mem_fun(this, &TouchInterface::gameUtilitiesOutside));

    tcGamepadUtility->addControl(gamepadUtils);
    tcGamepadUtility->setAlpha(0.9);
    tcGamepadUtility->signal_button.connect(sigc::mem_fun(this, &TouchInterface::gameUtilitiesButton));

    // DPad inventory -------------------------------------------
    //------------------------------------------------------
    touchcontrols::DPadSelect *dpadInventory = new touchcontrols::DPadSelect("dpad_inventory", touchcontrols::RectF(9, 4, 17, 12), "dpad", PORT_ACT_INVUSE);

    dpadInventory->addCell(touchcontrols::DPAD_LEFT, "inventory_left", PORT_ACT_INVPREV);
    dpadInventory->addCell(touchcontrols::DPAD_RIGHT, "inventory_right", PORT_ACT_INVNEXT);
    dpadInventory->addCell(touchcontrols::DPAD_DOWN, "inventory_drop", PORT_ACT_INVDROP);
    //dpadInventory->addCell(touchcontrols::DPAD_LEFT,"inventory_left",PORT_ACT_INVPREV);
    //
    dpadInventory->signal_button.connect(sigc::mem_fun(this, &TouchInterface::dPadInventoryButton));
    dpadInventory->signal_outside.connect(sigc::mem_fun(this, &TouchInterface::dPadInventoryOutside));

    tcDPadInventory->addControl(dpadInventory);
    tcDPadInventory->setAlpha(0.9);

    // Mouse -------------------------------------------
    //------------------------------------------------------
    touchcontrols::Mouse *mouseMouseMenu = new touchcontrols::Mouse("mouse", touchcontrols::RectF(0, 2, 26, 16), "");
    mouseMouseMenu->setHideGraphics(true);
    mouseMouseMenu->setEditable(false);
    tcMouse->addControl(mouseMouseMenu);
    mouseMouseMenu->signal_action.connect(sigc::mem_fun(this, &TouchInterface::mouseMove));

    tcMouse->addControl(new touchcontrols::Button("back", touchcontrols::RectF(0, 0, 2, 2), "back_button", KEY_BACK_BUTTON, false, false, "Back"));
    tcMouse->addControl(new touchcontrols::Button("hide_mouse", touchcontrols::RectF(4, 0, 6, 2), "mouse2", KEY_USE_MOUSE, false, false, "Hide mouse"));
    tcMouse->setAlpha(0.9);
    tcMouse->signal_button.connect(sigc::mem_fun(this, &TouchInterface::mouseButton));


    //---------------------------------------------------------------
    //---------------------------------------------------------------
    controlsContainer.addControlGroup(tcKeyboard);
    controlsContainer.addControlGroup(tcInventory); // before gamemain so touches don't go through
    controlsContainer.addControlGroup(tcGamepadUtility); // before gamemain so touches don't go through
    controlsContainer.addControlGroup(tcDPadInventory);
    controlsContainer.addControlGroup(tcCustomButtons);
    controlsContainer.addControlGroup(tcGameMain);
    controlsContainer.addControlGroup(tcYesNo);
    controlsContainer.addControlGroup(tcGameWeapons);

    controlsContainer.addControlGroup(tcMenuMain);
    controlsContainer.addControlGroup(tcWeaponWheel);
    controlsContainer.addControlGroup(tcBlank);
    controlsContainer.addControlGroup(tcMouse);

    std::string
    newSettings = (std::string) filesPath + "/touch_settings_"
    ENGINE_NAME
    ".xml";

    UI_tc = touchcontrols::createDefaultSettingsUI(&controlsContainer, newSettings);
    UI_tc->setAlpha(1);

    if(gameType == Q1MALICE)
        touchcontrols::setGlobalXmlAppend(".malice");
    else if(gameType == Q1HEXEN2)
        touchcontrols::setGlobalXmlAppend(".hexen2");
    else if(gameType == Q2DLL_SMD)
        touchcontrols::setGlobalXmlAppend(".smd");

#ifdef QUAKE3
    tcMenuMain->setXMLFile((std::string)filesPath +  "/quake3_menu.xml");
#else
    tcMenuMain->setXMLFile((std::string) filesPath + "/menu.xml");
#endif
    tcGameMain->setXMLFile((std::string) filesPath + "/game_"
    ENGINE_NAME
    ".xml");
    tcInventory->setXMLFile((std::string) filesPath + "/inventory_"
    ENGINE_NAME
    ".xml");
    tcWeaponWheel->setXMLFile((std::string) filesPath + "/weaponwheel_"
    ENGINE_NAME
    ".xml");
    tcGameWeapons->setXMLFile((std::string) filesPath + "/weapons_"
    ENGINE_NAME
    ".xml");
    tcCustomButtons->setXMLFile((std::string) filesPath + "/custom_buttons_0_"
    ENGINE_NAME
    ".xml");
}


void TouchInterface::blankButton(int state, int code)
{
    PortableAction(state, PORT_ACT_USE);
#if DARKPLACES_NONO // Needed because demos need esc
    PortableKeyEvent(state, SDL_SCANCODE_ESCAPE, 0);
#else
    PortableKeyEvent(state, SDL_SCANCODE_RETURN, 0);
#endif
}

void TouchInterface::automapButton(int state, int code)
{

}

void TouchInterface::newFrame()
{
    touchscreemode_t screenMode = PortableGetScreenMode();

    // Hack to show custom buttons while in the menu to bind keys
    if(screenMode == TS_MENU && showCustomMenu == true)
    {
        screenMode = TS_CUSTOM;
    }

    /*
        if(((screenMode == TS_GAME) || (screenMode == TS_MENU)) & useMouse)   // Show mouse screen
        {
            screenMode = TS_MOUSE;
        }

        if(gameShowMouse && useMouse)
            controlsContainer.showMouse(true);
        else
            controlsContainer.showMouse(false);
    */

    if((screenMode == TS_MENU) && (useMouse || gotMouseMove))
        controlsContainer.showMouse(true);
    else
    {
        useMouse = false;
        gotMouseMove = false;
        controlsContainer.showMouse(false);
    }

    updateTouchScreenModeOut(screenMode);
    updateTouchScreenModeIn(screenMode);

    currentScreenMode = screenMode;
}

void TouchInterface::newGLContext()
{
#if defined(YQUAKE2) // When using GLES1 on YQ2, does not use glGenTextures

    if(yquake2Renderer == 1)
    {
        touchcontrols::setTextureNumberStart(10000);
    }

#endif

#if defined(QUAKE2)
    touchcontrols::setTextureNumberStart(5000);
#endif
}