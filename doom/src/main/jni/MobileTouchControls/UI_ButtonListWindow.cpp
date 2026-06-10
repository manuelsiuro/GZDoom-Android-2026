//
// Created by Emile on 23/07/2017.
//

#include "UI_ButtonListWindow.h"

namespace touchcontrols
{

    static TouchControlsContainer *container = NULL;
    static UI_Controls *rootControls = NULL;

    static std::vector<ControlSuper *> editableButtons; // List of the buttons currently being edited

    static void buttonPress(int state, int code)
    {
        if(state == 0 && code == UI_WINDOW_BUTTON_BACK)
        {
            LOGTOUCH("Window close");
            container->hideUIWindow();
        }
    }

    static void switchChange(uint32_t uid, bool value)
    {
        if(uid < editableButtons.size())
        {
            editableButtons.at(uid)->setHidden(!value);
        }
    }


    static bool colorChange(uint32_t uid, uint32_t color)
    {
        if(uid < editableButtons.size())
        {
            editableButtons.at(uid)->color = color;
        }

        return false;
    }

    void showButtonListWindow(TouchControlsContainer *con)
    {
        container = con;

        if(rootControls == NULL)
        {
            rootControls = new UI_Controls("ui_edit_buttons");
        }

        // Remove all the old stuff first
        rootControls->deleteControls();

        // Clear our list of old buttons
        editableButtons.clear();

        float textSize = 0.07f;
        uint32_t windowLeft = 3;
        uint32_t windowRight = 23;

#define ROW_HEIGHT 2

        // Background window
        UI_Window *window = new UI_Window("bg_window", touchcontrols::RectF(windowLeft, 1, windowRight, 15), "Hide/show buttons", "ui_background");
        rootControls->addControl(window);
        window->signal.connect(sigc::ptr_fun(&buttonPress));


        touchcontrols::TouchControls *editing = container->getEditingControls();

        if(editing != NULL)  //This should not ever be NULL
        {
            // Get all the controls there are, but we only want the buttons
            uint32_t numberControls = editing->getControls()->size();

            float yPos = 1 + 2; //Window + window title. FIX this so window is at zero

            for(int n = 0; n < numberControls; n++)
            {
                if((editing->getControls()->at(n)->type == TC_TYPE_BUTTON) ||
                   (editing->getControls()->at(n)->type == TC_TYPE_QUADSLIDE) ||
                   (editing->getControls()->at(n)->type == TC_TYPE_BUTTONGRID))
                {
                    // This is the button in the touch controls
                    ControlSuper *control = (editing->getControls()->at(n));

                    // We can only edit buttons which have been given a description
                    if(control->description != "")
                    {
                        const char *imagePng;

                        if(editing->getControls()->at(n)->type == TC_TYPE_BUTTON)
                            imagePng = ((Button *) control)->image.c_str();

                        if(editing->getControls()->at(n)->type == TC_TYPE_QUADSLIDE)
                            imagePng = ((QuadSlide *) control)->bgImage.c_str();

                        if(editing->getControls()->at(n)->type == TC_TYPE_BUTTONGRID)
                            imagePng = ((ButtonGrid *) control)->getPrimaryImage().c_str();

                        //This is a button for the UI controls, just used for the image
                        Button *image = new Button("", touchcontrols::RectF(windowLeft, yPos, windowLeft + ROW_HEIGHT, yPos + ROW_HEIGHT), imagePng, -1);

                        rootControls->addControl(image);

                        // Add the text desciption
                        rootControls->addControl(new UI_TextBox("text", touchcontrols::RectF(windowLeft + ROW_HEIGHT, yPos, windowRight - 7, yPos + ROW_HEIGHT),
                                                                "font_dual", 0, UI_TEXT_CENTRE, control->description, 0.08));

                        // Add the color picker
                        UI_ColorPicker *colorPicker = new UI_ColorPicker("color_picker",
                                                                         touchcontrols::RectF(windowRight - 7, yPos + 0.2, windowRight - 4, yPos + ROW_HEIGHT - 0.2),
                                                                         editableButtons.size(), 0);
                        colorPicker->setColor(control->color);
                        rootControls->addControl(colorPicker);
                        colorPicker->signal.connect(sigc::ptr_fun(&colorChange));

                        // Add the switch
                        UI_Switch *swtch = new UI_Switch("switch", touchcontrols::RectF(windowRight - 4, yPos, windowRight, yPos + ROW_HEIGHT), editableButtons.size(),
                                                         "ui_switch_on", "ui_switch_off");
                        swtch->setValue(!control->isHidden());
                        swtch->signal.connect(sigc::ptr_fun(&switchChange));
                        rootControls->addControl(swtch);

                        editableButtons.push_back(control);

                        yPos += 2;
                    }
                }
            }

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
        }

        con->showUIWindow(rootControls);
    }


}