//
// Created by emile on 01/01/2021.
//

#include "touch_interface.h"
#include "SDL_keycode.h"

#include "gameTypes.h"


void TouchInterface::openGLStart()
{
    touchcontrols::gl_startRender();
};

void TouchInterface::openGLEnd()
{
    touchcontrols::gl_endRender();
#ifdef EDUKE32
    touchcontrols::gl_resetGL4ES();
#endif
};


void TouchInterface::createControls(std::string filesPath)
{
    createControlsDoom(filesPath);
}

void TouchInterface::createControlsDoom(std::string filesPath)
{
    LOGI("creating controls");

    tcMenuMain = new touchcontrols::TouchControls("menu", false, true, 10, false);
    tcYesNo = new touchcontrols::TouchControls("yes_no", false, false);
    tcGameMain = new touchcontrols::TouchControls("game", false, true, 1, true);
    tcGameWeapons = new touchcontrols::TouchControls("weapons", false, true, 1, false);
    tcInventory = new touchcontrols::TouchControls("inventory", false, true, 2, false); //Different edit group
    tcWeaponWheel = new touchcontrols::TouchControls("weapon_wheel", false, true, 1, false);
    tcAutomap = new touchcontrols::TouchControls("automap", false, false);
    tcBlank = new touchcontrols::TouchControls("blank", true, false);
    tcCustomButtons = new touchcontrols::TouchControls("custom_buttons", false, true, 1, true);
    tcKeyboard = new touchcontrols::TouchControls("keyboard", false, false);
    tcDemo = new touchcontrols::TouchControls("demo_playback", false, false);
    tcGamepadUtility = new touchcontrols::TouchControls("gamepad_utility", false, false);
    tcDPadInventory = new touchcontrols::TouchControls("dpad_inventory", false, false);
    tcMouse = new touchcontrols::TouchControls("mouse", false, false);
    // Hide the cog because when using the gamepad and weapon wheel is enabled, the cog will show otherwise
    tcWeaponWheel->hideEditButton = true;

    // Show main game controls when editing custom buttons
    tcCustomButtons->setEditBackgroundControl(tcGameMain);

    //Menu -------------------------------------------
    //------------------------------------------------------
    tcMenuMain->addControl(new touchcontrols::Button("back", touchcontrols::RectF(0, 0, 2, 2), "back_button", KEY_BACK_BUTTON));
    tcMenuMain->addControl(new touchcontrols::Button("down_arrow", touchcontrols::RectF(20, 13, 23, 16), "arrow_down", PORT_ACT_MENU_DOWN));
    tcMenuMain->addControl(new touchcontrols::Button("up_arrow", touchcontrols::RectF(20, 10, 23, 13), "arrow_up", PORT_ACT_MENU_UP));
    tcMenuMain->addControl(new touchcontrols::Button("left_arrow", touchcontrols::RectF(17, 13, 20, 16), "arrow_left", PORT_ACT_MENU_LEFT));
    tcMenuMain->addControl(new touchcontrols::Button("right_arrow", touchcontrols::RectF(23, 13, 26, 16), "arrow_right", PORT_ACT_MENU_RIGHT));
    tcMenuMain->addControl(new touchcontrols::Button("enter", touchcontrols::RectF(0, 10, 6, 16), "enter", PORT_ACT_MENU_SELECT));
    tcMenuMain->addControl(new touchcontrols::Button("keyboard", touchcontrols::RectF(2, 0, 4, 2), "keyboard", KEY_SHOW_KBRD));

    tcMenuMain->addControl(new touchcontrols::Button("gamepad", touchcontrols::RectF(22, 0, 24, 2), "gamepad", KEY_SHOW_GAMEPAD));
    tcMenuMain->addControl(new touchcontrols::Button("gyro", touchcontrols::RectF(24, 0, 26, 2), "gyro", KEY_SHOW_GYRO));
    tcMenuMain->addControl(new touchcontrols::Button("load_save_touch", touchcontrols::RectF(20, 0, 22, 2), "touchscreen_save", KEY_LOAD_SAVE_CONTROLS));

    touchcontrols::Mouse *brightnessSlide = new touchcontrols::Mouse("slide_mouse", touchcontrols::RectF(24, 3, 26, 11), "brightness_slider");
    brightnessSlide->signal_action.connect(sigc::mem_fun(this, &TouchInterface::brightnessSlideMouse));
    tcMenuMain->addControl(brightnessSlide);

    tcMenuMain->signal_button.connect(sigc::mem_fun(this, &TouchInterface::menuButton));
    tcMenuMain->setAlpha(0.8);
    tcMenuMain->setFixAspect(true);

    //Game -------------------------------------------
    //------------------------------------------------------

    bool hideAltAttack = true;
    bool hideAltWeapon = false;
    bool hideInventory = false;

    if(gameType == RAZE_GAME_BLOOD || gameType == RAZE_GAME_IONFURY || gameType == RAZE_GAME_AMC)
    {
        hideAltAttack = false;
    }

    if(gameType == RAZE_GAME_IONFURY || gameType == RAZE_GAME_AWOL) // Does not use traditional inventory, nor alt weapon
    {
        hideInventory = true;
        hideAltWeapon = true;
    }

    if(gameType == RAZE_GAME_AMC)
    {
        hideAltWeapon = true;
    }

    tcGameMain->setAlpha(touchSettings.alpha);
    tcGameMain->addControl(new touchcontrols::Button("back", touchcontrols::RectF(0, 0, 2, 2), "back_button", KEY_BACK_BUTTON, false, false, "Show menu"));
    tcGameMain->addControl(new touchcontrols::Button("attack", touchcontrols::RectF(20, 7, 23, 10), "shoot", KEY_SHOOT, false, false, "Attack!"));
    tcGameMain->addControl(new touchcontrols::Button("attack2", touchcontrols::RectF(3, 5, 6, 8), "shoot", KEY_SHOOT, false, true, "Attack! (duplicate)"));

    tcGameMain->addControl(new touchcontrols::Button("use", touchcontrols::RectF(23, 6, 26, 9), "use", PORT_ACT_USE, false, false, "Use/Open"));
    tcGameMain->addControl(new touchcontrols::Button("quick_save", touchcontrols::RectF(24, 0, 26, 2), "save", PORT_ACT_QUICKSAVE, false, false, "Quick save"));
    tcGameMain->addControl(new touchcontrols::Button("quick_load", touchcontrols::RectF(20, 0, 22, 2), "load", PORT_ACT_QUICKLOAD, false, false, "Quick load"));
    tcGameMain->addControl(new touchcontrols::Button("map", touchcontrols::RectF(2, 0, 4, 2), "map", PORT_ACT_MAP, false, false, "Show map"));
    tcGameMain->addControl(new touchcontrols::Button("keyboard", touchcontrols::RectF(8, 0, 10, 2), "keyboard", KEY_SHOW_KBRD, false, false, "Show keyboard"));
    tcGameMain->addControl(new touchcontrols::Button("show_mouse", touchcontrols::RectF(4, 0, 6, 2), "mouse2", KEY_USE_MOUSE, false, true, "Use mouse"));
    tcGameMain->addControl(new touchcontrols::Button("quick_command", touchcontrols::RectF(21, 3, 23, 5), "star", KEY_QUICK_COMMANDS, false, true, "Quick Commands"));


    tcGameMain->addControl(new touchcontrols::Button("jump", touchcontrols::RectF(24, 3, 26, 5), "jump", PORT_ACT_JUMP, false, false, "Jump"));
    tcGameMain->addControl(new touchcontrols::Button("use_inventory", touchcontrols::RectF(0, 9, 2, 11), "inventory", KEY_SHOW_INV, false, hideInventory, "Show Inventory"));
    tcGameMain->addControl(new touchcontrols::Button("activate_inventory", touchcontrols::RectF(22, 3, 24, 5), "inventory_use_fade", PORT_ACT_INVUSE, false, true, "Use Inventory"));
    tcGameMain->addControl(new touchcontrols::Button("crouch", touchcontrols::RectF(24, 14, 26, 16), "crouch", PORT_ACT_DOWN, false, false, "Crouch"));
    tcGameMain->addControl(new touchcontrols::Button("crouch_toggle", touchcontrols::RectF(24, 14, 26, 16), "crouch", PORT_ACT_TOGGLE_CROUCH, false, true, "Crouch (toggle)"));
    tcGameMain->addControl(new touchcontrols::Button("attack_alt", touchcontrols::RectF(21, 5, 23, 7), "shoot_alt", PORT_ACT_ALT_ATTACK, false, hideAltAttack, "Alt attack"));
    tcGameMain->addControl(new touchcontrols::Button("attack_alt2", touchcontrols::RectF(4, 3, 6, 5), "shoot_alt", PORT_ACT_ALT_ATTACK, false, true, "Alt attack (duplicate)"));
    tcGameMain->addControl(new touchcontrols::Button("attack_alt_toggle", touchcontrols::RectF(20, 5, 22, 7), "shoot_alt", PORT_ACT_TOGGLE_ALT_ATTACK, false, true, "Alt attack (toggle)"));

    tcGameMain->addControl(runButton); // Common run button created in touch_interface_base

    tcGameMain->addControl(new touchcontrols::Button("show_custom", touchcontrols::RectF(0, 7, 2, 9), "custom_show", KEY_SHOW_CUSTOM, false, true, "Show custom"));
    tcGameMain->addControl(new touchcontrols::Button("show_weapons", touchcontrols::RectF(12, 14, 14, 16), "show_weapons", KEY_SHOW_WEAPONS, false, false, "Show numbers"));
    tcGameMain->addControl(new touchcontrols::Button("next_weapon", touchcontrols::RectF(0, 3, 3, 5), "next_weap", PORT_ACT_NEXT_WEP, false, false, "Next weapon"));
    tcGameMain->addControl(new touchcontrols::Button("prev_weapon", touchcontrols::RectF(0, 5, 3, 7), "prev_weap", PORT_ACT_PREV_WEP, false, false, "Prev weapon"));
    tcGameMain->addControl(new touchcontrols::Button("console", touchcontrols::RectF(6, 0, 8, 2), "tild", PORT_ACT_CONSOLE, false, true, "Console"));
    tcGameMain->addControl(new touchcontrols::Button("swap_weapon", touchcontrols::RectF(3, 3, 5, 5), "swap", PORT_ACT_WEAP_ALT, false, hideAltWeapon, "Weapon alternative"));

    if(gameType != RAZE_GAME_AWOL)
        tcGameMain->addControl(new touchcontrols::Button("kick", touchcontrols::RectF(19, 3, 21, 5), "kick", PORT_ACT_KICK, false, true, "Kick"));

    tcGameMain->addControl(new touchcontrols::Button("gyro_toggle", touchcontrols::RectF(17, 3, 19, 5), "gyro", PORT_ACT_GYRO_TOGGLE, false, true, "Gyro Smart Toggle"));

    // Add ion fury specific buttons
    if(gameType == RAZE_GAME_IONFURY)
    {
        tcGameMain->addControl(new touchcontrols::Button("reload", touchcontrols::RectF(3, 4, 5, 6), "reload", PORT_ACT_RELOAD, false, false, "Reload"));
        tcGameMain->addControl(new touchcontrols::Button("medikit", touchcontrols::RectF(15, 0, 17, 2), "medikit", PORT_ACT_MEDKIT, false, false, "Medkit"));
        tcGameMain->addControl(new touchcontrols::Button("radar", touchcontrols::RectF(17, 0, 19, 2), "radar", PORT_ACT_RADAR, false, false, "Radar"));
    }

    // Add AWOL specific buttons
    if(gameType == RAZE_GAME_AWOL)
    {
        tcGameMain->addControl(new touchcontrols::Button("reload", touchcontrols::RectF(3, 4, 5, 6), "reload", PORT_ACT_RELOAD, false, false, "Reload"));
        tcGameMain->addControl(new touchcontrols::Button("medikit", touchcontrols::RectF(15, 0, 17, 2), "medikit", PORT_ACT_MEDKIT, false, false, "Medkit"));
        tcGameMain->addControl(new touchcontrols::Button("give_command", touchcontrols::RectF(17, 0, 19, 2), "chat", PORT_ACT_MP_SAY, false, false, "Give command"));
        tcGameMain->addControl(new touchcontrols::Button("zoom", touchcontrols::RectF(22, 3, 24, 5), "zoom", PORT_ACT_ZOOM_IN, false, false, "Aim"));
    }

    // Add AMC specific buttons
    if(gameType == RAZE_GAME_AMC)
    {
        tcGameMain->addControl(new touchcontrols::Button("reload", touchcontrols::RectF(3, 4, 5, 6), "reload", PORT_ACT_RELOAD, false, false, "Reload"));
        tcGameMain->addControl(new touchcontrols::Button("medikit", touchcontrols::RectF(15, 0, 17, 2), "medikit", PORT_ACT_MEDKIT, false, false, "Medkit"));
        tcGameMain->addControl(new touchcontrols::Button("pda", touchcontrols::RectF(4, 0, 6, 2), "notebook", PORT_ACT_HELPCOMP, false, false, "PDA"));
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

#if 1
    // SWAPFIX
    touchJoyLeft->registerTouchJoySWAPFIX(touchJoyRight);
    touchJoyRight->registerTouchJoySWAPFIX(touchJoyLeft);
#endif

    tcGameMain->signal_button.connect(sigc::mem_fun(this, &TouchInterface::gameButton));
    tcGameMain->signal_settingsButton.connect(sigc::mem_fun(this, &TouchInterface::gameSettingsButton));



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


    //Auto Map -------------------------------------------
    //------------------------------------------------------
    touchcontrols::MultitouchMouse *multimouse = new touchcontrols::MultitouchMouse("gamemouse", touchcontrols::RectF(0, 0, 26, 16), "");
    multimouse->setHideGraphics(true);
    tcAutomap->addControl(multimouse);
    multimouse->signal_action.connect(sigc::mem_fun(this, &TouchInterface::AutomapMultitouchMouseMove));
    tcAutomap->addControl(new touchcontrols::Button("map", touchcontrols::RectF(2, 0, 4, 2), "map", PORT_ACT_MAP));
    tcAutomap->signal_button.connect(sigc::mem_fun(this, &TouchInterface::automapButton));


    //Blank -------------------------------------------
    //------------------------------------------------------
    tcBlank->addControl(new touchcontrols::Button("enter", touchcontrols::RectF(0, 0, 26, 16), "", 0x123));
    tcBlank->signal_button.connect(sigc::mem_fun(this, &TouchInterface::blankButton));

    //Demo -------------------------------------------
    //------------------------------------------------------
    tcDemo->addControl(new touchcontrols::Button("pressed", touchcontrols::RectF(0, 2, 26, 16), "", 0));
    tcDemo->addControl(new touchcontrols::Button("keyboard", touchcontrols::RectF(2, 0, 4, 2), "keyboard", KEY_SHOW_KBRD));
    tcDemo->addControl(new touchcontrols::Button("speed_down", touchcontrols::RectF(20, 14, 22, 16), "key_-", PORT_ACT_DEMO_SPD_DWN));
    tcDemo->addControl(new touchcontrols::Button("speed_default", touchcontrols::RectF(22, 14, 24, 16), "key_0", PORT_ACT_DEMO_SPD_DEF));
    tcDemo->addControl(new touchcontrols::Button("speed_up", touchcontrols::RectF(24, 14, 26, 16), "key_+", PORT_ACT_DEMO_SPD_UP));
    tcDemo->addControl(new touchcontrols::Button("camera", touchcontrols::RectF(0, 14, 2, 16), "camera", PORT_ACT_DEMO_CAMERA));
    tcDemo->signal_button.connect(sigc::mem_fun(this, &TouchInterface::menuButton));
    tcDemo->setAlpha(1);

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
    createCustomControls(tcCustomButtons);

    //Gamepad utility -------------------------------------------
    //------------------------------------------------------
    touchcontrols::ButtonGrid *gamepadUtils = new touchcontrols::ButtonGrid("gamepad_grid", touchcontrols::RectF(8, 5, 18, 11), "gamepad_utils_bg", 3, 2);

    gamepadUtils->addCell(0, 0, "back_button", KEY_BACK_BUTTON);
    gamepadUtils->addCell(0, 1, "map", PORT_ACT_MAP);
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

    // Mouse for GZDoom
    touchcontrols::Mouse *mouse = new touchcontrols::Mouse("mouse", touchcontrols::RectF(0, 2, 26, 16), "");
    mouse->setHideGraphics(true);
    mouse->setEditable(false);
    tcMouse->addControl(mouse);
    mouse->signal_action.connect(sigc::mem_fun(this, &TouchInterface::mouseMove));
    tcMouse->addControl(new touchcontrols::Button("back", touchcontrols::RectF(0, 0, 2, 2), "back_button", KEY_BACK_BUTTON, false, false, "Back"));
    // Hide the mouse button because we can now use a tap
    //tcMouse->addControl(new touchcontrols::Button("left_button", touchcontrols::RectF(0, 6, 3, 10), "mouse2", KEY_LEFT_MOUSE, false, false, "Back"));
    tcMouse->addControl(new touchcontrols::Button("hide_mouse", touchcontrols::RectF(4, 0, 6, 2), "mouse2", KEY_USE_MOUSE, false, false, "Hide mouse"));
    tcMouse->setAlpha(0.9);
    tcMouse->signal_button.connect(sigc::mem_fun(this, &TouchInterface::mouseButton));

    std::string gameTypeString = std::to_string(gameType);

    UI_tc = touchcontrols::createDefaultSettingsUI(&controlsContainer, (std::string) filesPath + "/touch_settings" + gameTypeString + ".xml");
    UI_tc->setAlpha(1);

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
    controlsContainer.addControlGroup(tcAutomap);
    controlsContainer.addControlGroup(tcBlank);
    controlsContainer.addControlGroup(tcDemo);
    controlsContainer.addControlGroup(tcMouse);

    tcMenuMain->setXMLFile((std::string) filesPath + "/menu.xml");
    tcGameMain->setXMLFile((std::string) filesPath + "/game_"
    ENGINE_NAME + gameTypeString + ".xml");
    tcInventory->setXMLFile((std::string) filesPath + "/inventory_"
    ENGINE_NAME + gameTypeString + ".xml");
    tcWeaponWheel->setXMLFile((std::string) filesPath + "/weaponwheel_"
    ENGINE_NAME + gameTypeString + ".xml");
    tcGameWeapons->setXMLFile((std::string) filesPath + "/weapons_"
    ENGINE_NAME + gameTypeString + ".xml");
    tcCustomButtons->setXMLFile((std::string) filesPath + "/custom_buttons_0_"
    ENGINE_NAME + gameTypeString + ".xml");
}


void TouchInterface::blankButton(int state, int code)
{
    PortableAction(state, PORT_ACT_USE);
    PortableKeyEvent(state, SDL_SCANCODE_RETURN, 0);
}

void TouchInterface::automapButton(int state, int code)
{
    if(state && code == PORT_ACT_MAP && mapState == 0)
    {
        mapState = 1;
    }
}

void TouchInterface::newFrame()
{
    touchscreemode_t screenMode = PortableGetScreenMode();

    // Hack to show custom buttons while in the menu to bind keys
    if(screenMode == TS_MENU && showCustomMenu == true)
    {
        screenMode = TS_CUSTOM;
    }

    if((screenMode == TS_MAP) && (mapState == 1))
    {
        screenMode = TS_GAME;
    }

    if(screenMode == TS_DEMO)   // Fade out demo buttons if not touched for a while
    {
        if(demoControlsAlpha > 0)
        {
            demoControlsAlpha -= DEMO_ALPFA_DEC;

            if(demoControlsAlpha < 0)
                demoControlsAlpha = 0;
        }

        tcDemo->setAlpha(demoControlsAlpha);
    }

    if(((screenMode == TS_GAME) || (screenMode == TS_MENU)) & useMouse)   // Show mouse screen
    {
        screenMode = TS_MOUSE;
    }

    if(gameShowMouse && useMouse)
        controlsContainer.showMouse(true);
    else
        controlsContainer.showMouse(false);

    if(screenMode != currentScreenMode && (screenMode == TS_GAME || screenMode == TS_MENU))
    {
        useMouse = false;
    }

    updateTouchScreenModeOut(screenMode);
    updateTouchScreenModeIn(screenMode);

    currentScreenMode = screenMode;
}

void TouchInterface::newGLContext()
{

}