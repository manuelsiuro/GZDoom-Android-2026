#include "UI_TouchDefaultSettings.h"
#include "UI_ButtonListWindow.h"
#include "vector"

namespace touchcontrols
{

#define BUTTON_CLOSE 1
#define BUTTON_RESET 2
#define BUTTON_EDIT_BUTTONS 3
#define BUTTON_RESET_POS 4

#define SLIDER_ALPHA 10
#define SLIDER_LOOK  11
#define SLIDER_FWD   12
#define SLIDER_TURN  13
#define SLIDER_STRAFE  14

#define SLIDER_PRECISION  15
#define SLIDER_DEADZONE   16
#define SLIDER_DIG_MOVE_Y   18
#define SLIDER_DIG_MOVE_X   19

#define SWITCH_DIGITAL_MOVE   19
#define SWITCH_INVERT_LOOK   20
#define SWITCH_JOYSTICKS     22
#define SWITCH_JOYSTICK_MODE 23
#define SWITCH_HIDE_INV      24
#define SWITCH_HIDE_NBRS     25
#define SWITCH_WEAP_WHEEL    26
#define SWITCH_FIXED_MOVE    27
#define SWITCH_PRECISION_SHOOT    28
#define SWITCH_SHOW_CUSTOM        29

#define DROPDOWN_DBL_TAP_LEFT 30
#define DROPDOWN_DBL_TAP_RIGHT 31

#define DEFAULT_COLOR 32

#define DROPDOWN_VOLUME_UP 33
#define DROPDOWN_VOLUME_DOWN 34

    static uint32_t windowLeft = 3;
    static uint32_t windowRight = 24;

    static TouchControlsContainer *container = NULL;
    static UI_Controls *rootControls = NULL;
    static std::string settingsFilename;
    static tTouchSettings settings;

    static sigc::signal<void, tTouchSettings> signal_settingChange;

    static void applyControlValues();


    class SwitchOption
    {
    public:
        std::string title;
        std::string description;
        std::string xmlTag;
        bool defaultValue;

        bool *settingPtr;

        SwitchOption(std::string title, std::string description, std::string xmlTag, bool defaultValue, bool *settingPtr) :
                title(title), description(description), xmlTag(xmlTag), defaultValue(defaultValue), settingPtr(settingPtr)
        {

        }
    };

    static std::vector <SwitchOption> switchOptions;

    bool touchSettings_save(std::string filename)
    {
        TiXmlDocument doc;
        TiXmlDeclaration *decl = new TiXmlDeclaration("1.0", "", "");
        doc.LinkEndChild(decl);

        TiXmlElement *root = new TiXmlElement("settings");
        doc.LinkEndChild(root);

        root->SetDoubleAttribute("alpha", settings.alpha);
        root->SetDoubleAttribute("look_sens", settings.lookSensitivity);
        root->SetDoubleAttribute("turn_sens", settings.turnSensitivity);
        root->SetDoubleAttribute("fwd_sens", settings.fwdSensitivity);
        root->SetDoubleAttribute("strafe_sens", settings.strafeSensitivity);
        root->SetDoubleAttribute("deadzone_sens", settings.deadzoneSensitivity);

        root->SetDoubleAttribute("precision_sens", settings.precisionSensitivity);

        root->SetDoubleAttribute("dig_move_y_sens", settings.digMoveYSensitivity);
        root->SetDoubleAttribute("dig_move_x_sens", settings.digMoveXSensitivity);

        root->SetAttribute("dbl_tap_left", settings.dblTapLeft);
        root->SetAttribute("dbl_tap_right", settings.dblTapRight);

        root->SetAttribute("volume_up", settings.volumeUp);
        root->SetAttribute("volume_down", settings.volumeDown);

        root->SetAttribute("default_color", settings.defaultColor);

        for(SwitchOption &switchOption: switchOptions)
        {
            LOGTOUCH("touchSettings_save %s = %d", switchOption.xmlTag.c_str(), *switchOption.settingPtr);
            root->SetAttribute(switchOption.xmlTag.c_str(), *switchOption.settingPtr);
        }

        LOGTOUCH("Saving settings to %s\n", filename.c_str());
        return doc.SaveFile(filename);
    }

    bool touchSettings_load(std::string filename)
    {
        LOGTOUCH("Loading settings from %s\n", filename.c_str());

        bool ok = false;

        TiXmlDocument *doc = new TiXmlDocument(filename.c_str());

        if(!doc->LoadFile())
        {
            LOGTOUCH("Failed to open settings file\n");
        }
        else
        {
            TiXmlHandle hDoc(doc);
            TiXmlElement *root = hDoc.FirstChild("settings").Element();

            root->QueryFloatAttribute("alpha", &settings.alpha);
            root->QueryFloatAttribute("look_sens", &settings.lookSensitivity);
            root->QueryFloatAttribute("turn_sens", &settings.turnSensitivity);
            root->QueryFloatAttribute("fwd_sens", &settings.fwdSensitivity);
            root->QueryFloatAttribute("strafe_sens", &settings.strafeSensitivity);
            root->QueryFloatAttribute("deadzone_sens", &settings.deadzoneSensitivity);

            root->QueryFloatAttribute("dig_move_y_sens", &settings.digMoveYSensitivity);
            root->QueryFloatAttribute("dig_move_x_sens", &settings.digMoveXSensitivity);

            root->QueryUnsignedAttribute("dbl_tap_left", &settings.dblTapLeft);
            root->QueryUnsignedAttribute("dbl_tap_right", &settings.dblTapRight);

            root->QueryUnsignedAttribute("volume_up", &settings.volumeUp);
            root->QueryUnsignedAttribute("volume_down", &settings.volumeDown);

            root->QueryUnsignedAttribute("default_color", &settings.defaultColor);

            for(SwitchOption &switchOption: switchOptions)
            {
                root->QueryBoolAttribute(switchOption.xmlTag.c_str(), switchOption.settingPtr);
                LOGTOUCH("touchSettings_load %s = %d", switchOption.xmlTag.c_str(), *switchOption.settingPtr);
            }

            ok = true;
        }

        signal_settingChange.emit(settings);

        applyControlValues();

        delete doc;

        return ok;
    }

    bool touchSettings_save()
    {
        return touchSettings_save(settingsFilename);
    }

    static void resetDefaults()
    {
        settings.alpha = 0.5;
        settings.mouseLook = true;
        settings.invertLook = false;
        settings.lookSensitivity = 1;
        settings.turnSensitivity = 1;
        settings.fwdSensitivity = 1;
        settings.strafeSensitivity = 1;
        settings.deadzoneSensitivity = 0.1;

        settings.digitalMove = false;
        settings.showLeftStick = true;
        settings.showRightStick = true;
        settings.joystickLookMode = false;
        settings.autoHideInventory = true;
        settings.autoHideNumbers = true;
        settings.weaponWheelEnabled = true;
        settings.fixedMoveStick = false;
        settings.precisionShoot = false;
        settings.precisionSensitivity = 0.3;
        settings.digMoveYSensitivity = 0.5;
        settings.digMoveXSensitivity = 0.5;
        settings.alwaysShowCust = false;
        settings.weaponWheelOpaque = false;
        settings.alwaysRunDefault = true;

        settings.dblTapLeft = 0;
        settings.dblTapRight = 0;
        settings.volumeUp = 0;
        settings.volumeDown = 0;

        settings.defaultColor = COLOUR_WHITE;
    }

    static void applyControlValues()
    {
        if(rootControls != NULL)
        {
            ((UI_Slider *) rootControls->getControl("slider_alpha"))->setValue(settings.alpha);
            ((UI_Slider *) rootControls->getControl("slider_look"))->setValue(settings.lookSensitivity / 2);
            ((UI_Slider *) rootControls->getControl("slider_turn"))->setValue(settings.turnSensitivity / 2);
            ((UI_Slider *) rootControls->getControl("slider_fwd"))->setValue(settings.fwdSensitivity / 2);
            ((UI_Slider *) rootControls->getControl("slider_strafe"))->setValue(settings.strafeSensitivity / 2);
            ((UI_Slider *) rootControls->getControl("slider_deadzone"))->setValue(settings.deadzoneSensitivity);

            ((UI_ColorPicker *) rootControls->getControl("color_picker"))->setColor(settings.defaultColor);

            ((UI_Slider *) rootControls->getControl("slider_precision"))->setValue((settings.precisionSensitivity - 0.2f) / 0.5f);
            ((UI_Slider *) rootControls->getControl("slider_dig_move_y"))->setValue((1.0 - settings.digMoveYSensitivity));
            ((UI_Slider *) rootControls->getControl("slider_dig_move_x"))->setValue((1.0 - settings.digMoveXSensitivity));

            ((UI_DropDown *) rootControls->getControl("dbl_tap_left"))->setSelected(settings.dblTapLeft);
            ((UI_DropDown *) rootControls->getControl("dbl_tap_right"))->setSelected(settings.dblTapRight);

            ((UI_DropDown *) rootControls->getControl("volume_up"))->setSelected(settings.volumeUp);
            ((UI_DropDown *) rootControls->getControl("volume_down"))->setSelected(settings.volumeDown);

            for(SwitchOption &switchOption: switchOptions)
            {
                ((UI_Switch *) rootControls->getControl(switchOption.xmlTag))->setValue(*switchOption.settingPtr);
            }
        }
    }

    static void buttonPress(int state, int code)
    {
        if(state == 0 && code == UI_WINDOW_BUTTON_BACK)
        {
            rootControls->setEnabled(false);    // controlsEnabled will get called automatically
            LOGTOUCH("Window close");
        }

        if(code == BUTTON_RESET)
        {
            resetDefaults();
            signal_settingChange.emit(settings);
            applyControlValues();
        }
        else if(code == BUTTON_RESET_POS)
        {
            container->resetDefaults();
        }
        else if(code == BUTTON_EDIT_BUTTONS)
        {
            showButtonListWindow(container);
        }
    }

    static void sliderChange(uint32_t uid, float value)
    {
        if(uid == SLIDER_ALPHA)
        {
            settings.alpha = value;
        }
        else if(uid == SLIDER_LOOK)
        {
            settings.lookSensitivity = value * 2;
        }
        else if(uid == SLIDER_TURN)
        {
            settings.turnSensitivity = value * 2;
        }
        else if(uid == SLIDER_FWD)
        {
            settings.fwdSensitivity = value * 2;
        }
        else if(uid == SLIDER_STRAFE)
        {
            settings.strafeSensitivity = value * 2;
        }
        else if(uid == SLIDER_DEADZONE)
        {
            settings.deadzoneSensitivity = value;
        }
        else if(uid == SLIDER_PRECISION)
        {
            settings.precisionSensitivity = 0.2 + (value * 0.5);
        }
        else if(uid == SLIDER_DIG_MOVE_Y)
        {
            settings.digMoveYSensitivity = 1.0 - value;
            if(settings.digMoveYSensitivity > 0.95) // Clamp
                settings.digMoveYSensitivity = 0.95;
        }
        else if(uid == SLIDER_DIG_MOVE_X)
        {
            settings.digMoveXSensitivity = 1.0 - value;
            if(settings.digMoveXSensitivity > 0.95) // Clamp
                settings.digMoveXSensitivity = 0.95;
        }
    }

    static void switchChange(uint32_t uid, bool value)
    {
        LOGTOUCH("switchChange %d, %d", uid, value);

        // The uid is the position  in the vector
        SwitchOption switchOption = switchOptions.at(uid);

        // Update the settings via the pointer
        *switchOption.settingPtr = value;
    }

    static void dropDownChange(uint32_t uid, uint32_t value)
    {
        if(uid == DROPDOWN_DBL_TAP_LEFT)
        {
            settings.dblTapLeft = value;
        }
        else if(uid == DROPDOWN_DBL_TAP_RIGHT)
        {
            settings.dblTapRight = value;
        }
        else if(uid == DROPDOWN_VOLUME_UP)
        {
            settings.volumeUp = value;
        }
        else if(uid == DROPDOWN_VOLUME_DOWN)
        {
            settings.volumeDown = value;
        }
    }

    static bool colorChange(uint32_t uid, uint32_t color)
    {
        if(color != COLOUR_NONE)
        {
            settings.defaultColor = color;
            ((UI_ColorPicker *) rootControls->getControl("color_picker"))->setColor(settings.defaultColor);
            return false; //OK color
        }
        else
            return true; //return true so picker does not close
    }

// Gets called by a signal when the controls are enabled or disabled
    static void controlsEnabled(bool enabled)
    {
        if(!enabled)
        {
            touchSettings_save(settingsFilename);
            signal_settingChange.emit(settings);
        }
    }


    sigc::signal<void, tTouchSettings> *getSettingsSignal()
    {
        return &signal_settingChange;
    }

    static float divider(UI_Controls *root, float y)
    {
        const float divHeight = 0.05;

        root->addControl(new Button("", touchcontrols::RectF(windowLeft, y, windowRight, y + divHeight), "ui_grey_box", -1));

        return divHeight;
    }

    static float
    addSwitch(UI_Controls *root, float y, std::string title, std::string description, std::string xmlTag, bool defaultValue, bool *settingsPtr, bool showDiv = true)
    {
        const float textSizeTitle = 0.07;
        const float textSizeDesc = 0.05;

        const float titleTextHeight = 1.3;
        const float descTextHeight = 1.2;

        SwitchOption sw(title, description, xmlTag, defaultValue, settingsPtr);

        root->addControl(
                new UI_TextBox("text", touchcontrols::RectF(windowLeft, y, 21, y + titleTextHeight), "font_dual", 0, UI_TEXT_LEFT, title, textSizeTitle, COLOUR_WHITE));
        root->addControl(
                new UI_TextBox("text", touchcontrols::RectF(windowLeft, y + titleTextHeight, 21, y + titleTextHeight + descTextHeight), "font_dual", 0, UI_TEXT_LEFT,
                               description, textSizeDesc, COLOUR_GREY2));

        UI_Switch *swtch = new UI_Switch(xmlTag, touchcontrols::RectF(21, y + 0.4, windowRight, y + 2), switchOptions.size(), "ui_switch_on", "ui_switch_off");
        swtch->signal.connect(sigc::ptr_fun(&switchChange));
        root->addControl(swtch);

        // Use button to show divider line
        if(showDiv)
            divider(root, y + titleTextHeight + descTextHeight);

        switchOptions.push_back(sw);

        return titleTextHeight + descTextHeight + (showDiv ? 0.1 : 0.0);
    }


    UI_Controls *createDefaultSettingsUI(TouchControlsContainer *con, std::string settingsFile, tTouchSettingsModifier *modifier)
    {
        settingsFilename = settingsFile;
        container = con;

        if(rootControls == NULL)
        {
            //Defaults
            resetDefaults();

            rootControls = new UI_Controls("ui_settings");

            float textSize = 0.07f;

            float y = 0.2;

            // Draws backwards so need background last
            UI_Window *window = new UI_Window("bg_window", touchcontrols::RectF(windowLeft, y, windowRight, 15), "Touch settings", "ui_background");
            rootControls->addControl(window);
            window->signal.connect(sigc::ptr_fun(&buttonPress));

            y += 2;

            rootControls->addControl(
                    new UI_TextBox("text", touchcontrols::RectF(windowLeft, y, 12, y + 2), "font_dual", 0, UI_TEXT_RIGHT, "Transparency:", textSize, COLOUR_ORANGE));
            UI_Slider *slider = new UI_Slider("slider_alpha", touchcontrols::RectF(13, y, windowRight - 1, y + 2), SLIDER_ALPHA, "ui_slider_bg", "ui_slider_handle");
            slider->signal.connect(sigc::ptr_fun(&sliderChange));
            rootControls->addControl(slider);

            y += 1.2;

            rootControls->addControl(
                    new UI_TextBox("text", touchcontrols::RectF(windowLeft, y, windowRight, y + 3), "font_dual", 0, UI_TEXT_CENTRE, "Stick sensitivity", textSize * 1.5,
                                   COLOUR_GREY1));

            y += 2.2;

            rootControls->addControl(new UI_TextBox("text", touchcontrols::RectF(windowLeft, y, 12, y + 2), "font_dual", 0, UI_TEXT_RIGHT, "Look Up/Down:", textSize));
            slider = new UI_Slider("slider_look", touchcontrols::RectF(13, y, windowRight - 1, y + 2), SLIDER_LOOK, "ui_slider_bg", "ui_slider_handle");
            slider->signal.connect(sigc::ptr_fun(&sliderChange));
            rootControls->addControl(slider);

            y += 1.5;

            rootControls->addControl(
                    new UI_TextBox("text", touchcontrols::RectF(windowLeft, y, 12, y + 2), "font_dual", 0, UI_TEXT_RIGHT, "Look Left/Right:", textSize));
            slider = new UI_Slider("slider_turn", touchcontrols::RectF(13, y, windowRight - 1, y + 2), SLIDER_TURN, "ui_slider_bg", "ui_slider_handle");
            slider->signal.connect(sigc::ptr_fun(&sliderChange));
            rootControls->addControl(slider);

            y += 2.2;

            rootControls->addControl(
                    new UI_TextBox("text", touchcontrols::RectF(windowLeft, y, 12, y + 2), "font_dual", 0, UI_TEXT_RIGHT, "Move Fwd/Back:", textSize, COLOUR_GREEN3));
            slider = new UI_Slider("slider_fwd", touchcontrols::RectF(13, y, windowRight - 1, y + 2), SLIDER_FWD, "ui_slider_bg", "ui_slider_handle");
            slider->signal.connect(sigc::ptr_fun(&sliderChange));
            rootControls->addControl(slider);

            y += 1.5;

            rootControls->addControl(
                    new UI_TextBox("text", touchcontrols::RectF(windowLeft, y, 12, y + 2), "font_dual", 0, UI_TEXT_RIGHT, "Move Strafe:", textSize, COLOUR_GREEN3));
            slider = new UI_Slider("slider_strafe", touchcontrols::RectF(13, y, windowRight - 1, y + 2), SLIDER_STRAFE, "ui_slider_bg", "ui_slider_handle");
            slider->signal.connect(sigc::ptr_fun(&sliderChange));
            rootControls->addControl(slider);

            y += 1.5;

            rootControls->addControl(
                    new UI_TextBox("text", touchcontrols::RectF(windowLeft, y, 12, y + 2), "font_dual", 0, UI_TEXT_RIGHT, "Move dead-zone:", textSize, COLOUR_GREEN3));
            slider = new UI_Slider("slider_deadzone", touchcontrols::RectF(13, y, windowRight - 1, y + 2), SLIDER_DEADZONE, "ui_slider_bg", "ui_slider_handle");
            slider->signal.connect(sigc::ptr_fun(&sliderChange));
            rootControls->addControl(slider);

            y += 2.5;

            y += divider(rootControls, y);

            y += 0.5;

            UI_Switch *swtch;

            UI_Button *buttonEdit = new UI_Button("edit_buttons", touchcontrols::RectF(windowLeft + 1, y, windowRight - 1, y + 2), BUTTON_EDIT_BUTTONS, "font_dual", 0,
                                                  UI_TEXT_CENTRE, "Hide/Show buttons", textSize, "ui_button_bg");
            buttonEdit->signal.connect(sigc::ptr_fun(&buttonPress));
            rootControls->addControl(buttonEdit);

            y += 2;

            rootControls->addControl(new UI_TextBox("text", touchcontrols::RectF(windowLeft, y, 9.5, y + 2), "font_dual", 0, UI_TEXT_LEFT, "Default color:", textSize));
            UI_ColorPicker *colorPicker = new UI_ColorPicker("color_picker", touchcontrols::RectF(windowRight - 3, y + 0.2, windowRight, y + 1.8), DEFAULT_COLOR, 0);
            rootControls->addControl(colorPicker);
            colorPicker->signal.connect(sigc::ptr_fun(&colorChange));

            y += 2;

            y += divider(rootControls, y);

            y += addSwitch(rootControls, y, "Digital move (WASD)", "Map the left move stick to digital keys W,A,S,D", "digital_move", false, &settings.digitalMove,
                           false);

            // Add sliders for Digital Move sensitivity
            rootControls->addControl(
                    new UI_TextBox("text", touchcontrols::RectF(windowLeft, y, 8, y + 2), "font_dual", 0, UI_TEXT_RIGHT, "Fwd/Back sens:", textSize * 0.8,
                                   COLOUR_GREEN3));
            slider = new UI_Slider("slider_dig_move_y", touchcontrols::RectF(9, y, windowRight - 1, y + 2), SLIDER_DIG_MOVE_Y, "ui_slider_bg", "ui_slider_handle");
            slider->signal.connect(sigc::ptr_fun(&sliderChange));
            rootControls->addControl(slider);
            y += 1.8;

            rootControls->addControl(
                    new UI_TextBox("text", touchcontrols::RectF(windowLeft, y, 8, y + 2), "font_dual", 0, UI_TEXT_RIGHT, "Strafe sens:", textSize * 0.8,
                                   COLOUR_GREEN3));
            slider = new UI_Slider("slider_dig_move_x", touchcontrols::RectF(9, y, windowRight - 1, y + 2), SLIDER_DIG_MOVE_X, "ui_slider_bg", "ui_slider_handle");
            slider->signal.connect(sigc::ptr_fun(&sliderChange));
            rootControls->addControl(slider);
            y += 1.8;

            y += divider(rootControls, y);

            // Mouse look option disabled by default
            if((modifier != nullptr) && (modifier->mouseLookVisible == true))
                y += addSwitch(rootControls, y, "Mouse look", "Enable vertical look", "mouse_look", false, &settings.mouseLook);

            y += addSwitch(rootControls, y, "Always Run Default", "Set default value of Run when starting the engine", "always_run_default", true,
                           &settings.alwaysRunDefault);

            y += addSwitch(rootControls, y, "Invert look", "Invert the direction of the vertical look stick", "invert_switch", false, &settings.invertLook);

            y += addSwitch(rootControls, y, "Joystick look", "Look stick operates like a joystick", "joystick_look_switch", false, &settings.joystickLookMode);

            y += addSwitch(rootControls, y, "Fixed Move", "Fix the centre of the Move stick (disable floating)", "fixed_move_stick", false, &settings.fixedMoveStick);

            y += addSwitch(rootControls, y, "Always show Custom buttons", "Force the Custom buttons to always be visible", "show_custom", false,
                           &settings.alwaysShowCust);

            y += addSwitch(rootControls, y, "Auto-hide inventory UI", "Automatically hide the inventory buttons when selected", "auto_hide_inventory", false,
                           &settings.autoHideInventory);

            y += addSwitch(rootControls, y, "Auto-hide numbers UI", "Automatically hide the number buttons when selected", "auto_hide_numbers", false,
                           &settings.autoHideNumbers);

            y += addSwitch(rootControls, y, "Show left joystick", "Show the left stick graphic", "show_left_stick", true, &settings.showLeftStick);

            y += addSwitch(rootControls, y, "Show right joystick", "Show the right stick graphic", "show_right_stick", true, &settings.showRightStick);

            y += addSwitch(rootControls, y, "Weapon wheel enable", "Enable the weapon wheel, press centre of screen to use", "weapon_wheel_enabled", false,
                           &settings.weaponWheelEnabled);

            y += addSwitch(rootControls, y, "Weapon wheel opaque", "Weapon wheel alpha value fixed", "weapon_wheel_opaque", false, &settings.weaponWheelOpaque);

//////////
            y += addSwitch(rootControls, y, "Precision shoot", "Reduce look sensitivity while shooting, set amount below", "precision_shoot", false,
                           &settings.precisionShoot, false);

            rootControls->addControl(
                    new UI_TextBox("text", touchcontrols::RectF(windowLeft, y, 8, y + 2), "font_dual", 0, UI_TEXT_RIGHT, "Sensitivity:", textSize * 0.8,
                                   COLOUR_GREEN3));

            slider = new UI_Slider("slider_precision", touchcontrols::RectF(9, y, windowRight - 1, y + 2), SLIDER_PRECISION, "ui_slider_bg", "ui_slider_handle");
            slider->signal.connect(sigc::ptr_fun(&sliderChange));
            rootControls->addControl(slider);

            y += 2;

            y += divider(rootControls, y);
///////////

            rootControls->addControl(
                    new UI_TextBox("text", touchcontrols::RectF(windowLeft, y, 8, y + 2), "font_dual", 0, UI_TEXT_LEFT, "Double tap left/right:", textSize));

            UI_DropDown *dblTapLeft = new UI_DropDown("dbl_tap_left", touchcontrols::RectF(12, y, 18, y + 2), DROPDOWN_DBL_TAP_LEFT, "font_dual", 0, "Left : ",
                                                      "None:Use:Jump:Fire:Alt-fire:Crouch", textSize, "ui_dropdown_bg");
            rootControls->addControl(dblTapLeft);
            dblTapLeft->signal.connect(sigc::ptr_fun(&dropDownChange));

            UI_DropDown *dblTapRight = new UI_DropDown("dbl_tap_right", touchcontrols::RectF(18, y, windowRight, y + 2), DROPDOWN_DBL_TAP_RIGHT, "font_dual", 0,
                                                       "Right : ", "None:Use:Jump:Fire:Alt-fire:Crouch", textSize, "ui_dropdown_bg");
            rootControls->addControl(dblTapRight);
            dblTapRight->signal.connect(sigc::ptr_fun(&dropDownChange));

            y += 2;
            y += divider(rootControls, y);

            rootControls->addControl(new UI_TextBox("text", touchcontrols::RectF(windowLeft, y, 8, y + 2), "font_dual", 0, UI_TEXT_LEFT, "Volume buttons:", textSize));

            UI_DropDown *volumeUp = new UI_DropDown("volume_up", touchcontrols::RectF(12, y, 18, y + 2), DROPDOWN_VOLUME_UP, "font_dual", 0, "Up : ",
                                                    "None:Use:Jump:Fire:Alt-fire:Crouch", textSize, "ui_dropdown_bg");
            rootControls->addControl(volumeUp);
            volumeUp->signal.connect(sigc::ptr_fun(&dropDownChange));

            UI_DropDown *volumeDown = new UI_DropDown("volume_down", touchcontrols::RectF(18, y, windowRight, y + 2), DROPDOWN_VOLUME_DOWN, "font_dual", 0, "Down : ",
                                                      "None:Use:Jump:Fire:Alt-fire:Crouch", textSize, "ui_dropdown_bg");
            rootControls->addControl(volumeDown);
            volumeDown->signal.connect(sigc::ptr_fun(&dropDownChange));


            y += 2;
            y += divider(rootControls, y);

            UI_Button *buttonReset = new UI_Button("reset_button_pos", touchcontrols::RectF(windowLeft + 1, y, 13 - 1, y + 2), BUTTON_RESET_POS, "font_dual", 0,
                                                   UI_TEXT_CENTRE, "Reset positions", textSize, "ui_button_bg");
            buttonReset->signal.connect(sigc::ptr_fun(&buttonPress));
            rootControls->addControl(buttonReset);

            UI_Button *buttonResetSettings = new UI_Button("reset_button_set", touchcontrols::RectF(13 + 1, y, windowRight - 1, y + 2), BUTTON_RESET, "font_dual", 0,
                                                           UI_TEXT_CENTRE, "Reset settings", textSize, "ui_button_bg");
            buttonResetSettings->signal.connect(sigc::ptr_fun(&buttonPress));
            rootControls->addControl(buttonResetSettings);

            // Scale all the controls for wide screen device
            float nominal = (float) ScaleX / (float) ScaleY;
            float actual = GLScaleWidth / -GLScaleHeight;
            float yScale = nominal / actual;
            rootControls->scaleAllControls(yScale, 1);

            // Translate to put back in the middle
            float originalWidth = (windowRight - windowLeft) / ScaleY;
            float newWidth = originalWidth * yScale;
            float translateX = (windowLeft / ScaleY) * yScale; // Find distance moved left
            translateX += (originalWidth - newWidth) / 2; // Add on half the reduction in size
            rootControls->translateAllControls(translateX, 0);

            // Load the settings file
            touchSettings_load(settingsFilename);

            applyControlValues();

            rootControls->signalEnable.connect(sigc::ptr_fun(&controlsEnabled));
        }

        return rootControls;
    }

}