

#ifndef _UI_TouchDefaultSettings_H_
#define _UI_TouchDefaultSettings_H_

#include "UI_Controls.h"
#include "TouchControlsContainer.h"

namespace touchcontrols
{

    typedef struct
    {
        float alpha;
        float lookSensitivity;
        float turnSensitivity;
        float fwdSensitivity;
        float strafeSensitivity;
        float deadzoneSensitivity;

        float precisionSensitivity;

        float digMoveYSensitivity;
        float digMoveXSensitivity;

        bool mouseLook;
        bool invertLook;
        bool showLeftStick;
        bool showRightStick;
        bool joystickLookMode;
        bool autoHideInventory;
        bool autoHideNumbers;
        bool weaponWheelEnabled;
        bool fixedMoveStick;
        bool precisionShoot;
        bool digitalMove;
        bool alwaysShowCust;
        bool weaponWheelOpaque;
        bool alwaysRunDefault;

        uint32_t dblTapLeft;
        uint32_t dblTapRight;

        uint32_t volumeUp;
        uint32_t volumeDown;

        uint32_t defaultColor;

    } tTouchSettings;


    typedef struct
    {
        bool mouseLookVisible;
    } tTouchSettingsModifier;

    UI_Controls *createDefaultSettingsUI(TouchControlsContainer *con, std::string settingsFile, tTouchSettingsModifier *modifier = nullptr);

    sigc::signal<void, tTouchSettings> *getSettingsSignal();

    bool touchSettings_save();

    bool touchSettings_save(std::string filename);

    bool touchSettings_load(std::string filename);


}

#endif