#include "RectF.h"

using namespace touchcontrols;

RectF::RectF()
{

}

RectF::RectF(float left_, float top_, float right_, float bottom_)
{
    left = left_;
    top = top_;
    right = right_;
    bottom = bottom_;
}

bool RectF::contains(float x, float y)
{
    return left < right && top < bottom  // check for empty first
           && x >= left && x < right && y >= top && y < bottom;
}

void RectF::offset(float dx, float dy)
{
    left += dx;
    top += dy;
    right += dx;
    bottom += dy;
}

void RectF::offsetTo(float newLeft, float newTop)
{
    right += newLeft - left;
    bottom += newTop - top;
    left = newLeft;
    top = newTop;
}

void RectF::scale(float width, float height)
{
    left *= width;
    right *= width;
    top *= height;
    bottom *= height;
}

float RectF::width()
{
    return right - left;
}

float RectF::height()
{
    return bottom - top;
}
