#include "ControlSuper.h"
#include "TouchControls.h"

using namespace touchcontrols;

ControlSuper::ControlSuper(int type_, std::string t, RectF pos)
{
    controlPos = pos;
    controlPos.left /= ScaleX;
    controlPos.right /= ScaleX;
    controlPos.top /= ScaleY;
    controlPos.bottom /= ScaleY;

    type = type_;
    tag = t;
    enabled = true;
    hidden = false;
    editable = true;
    allowPassThrough = true;
    color = COLOUR_NONE; // 0 means use default
}

bool ControlSuper::gamepadInput(bool down, GamePadKey key)
{
    // By default controls do no handle a gamepad
    return false;
}

ControlSuper::~ControlSuper()
{

}

bool ControlSuper::UI_drawGL(bool UIActive)
{
    return drawGL(UIActive);
}

void ControlSuper::setEnabled(bool v)
{
    enabled = v;
}

void ControlSuper::setHidden(bool v)
{
    hidden = v;
}

bool ControlSuper::isEnabled()
{
    return enabled;
}

bool ControlSuper::isHidden()
{
    return hidden;
}

void ControlSuper::setAllowPassThrough(bool v)
{
    allowPassThrough = v;
}

bool ControlSuper::isAllowPassThrough()
{
    return allowPassThrough;
}

void ControlSuper::setEditable(bool v)
{
    editable = v;
}

bool ControlSuper::isEditable()
{
    return editable;
}

void ControlSuper::scaleSize(float x, float y)
{
    controlPos.scale(x, y);
}

void ControlSuper::positionTranslate(float dx, float dy)
{
    controlPos.offset(dx, dy);
}

void ControlSuper::saveXML(TiXmlElement &root)
{
    root.SetAttribute("enabled", enabled);
    root.SetAttribute("hidden", hidden);
    root.SetDoubleAttribute("left", controlPos.left);
    root.SetDoubleAttribute("right", controlPos.right);
    root.SetDoubleAttribute("top", controlPos.top);
    root.SetDoubleAttribute("bottom", controlPos.bottom);
    root.SetAttribute("color", color);
}


void ControlSuper::loadXML(TiXmlElement &root)
{
    root.QueryBoolAttribute("enabled", &enabled);
    root.QueryBoolAttribute("hidden", &hidden);
    root.QueryFloatAttribute("left", &controlPos.left);
    root.QueryFloatAttribute("right", &controlPos.right);
    root.QueryFloatAttribute("top", &controlPos.top);
    root.QueryFloatAttribute("bottom", &controlPos.bottom);
    root.QueryUnsignedAttribute("color", &color);
}
