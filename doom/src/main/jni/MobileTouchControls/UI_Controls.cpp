//
// Created by Emile on 18/04/2017.
//
#include "UI_Controls.h"
#include "UI_Window.h"
#include "OpenGLUtils.h"

using namespace touchcontrols;

UI_Controls::UI_Controls( std::string t ):
tag(t)
{
    yOffset = 0;
    scrolling = false;
}

void UI_Controls::setEnabled(bool v)
{
    enabled = v;
    signalEnable( enabled );
}

bool UI_Controls::getEnabled()
{
    return enabled;
}


void UI_Controls::addControl(ControlSuper *cntrl)
{
	controls.push_back((cntrl));
}

void UI_Controls::deleteControls()
{
    int size = controls.size();
    for (int n=0;n<size;n++)
    {
        delete( controls.at(n) );
    }
    controls.clear();
    yOffset = 0;
}

bool UI_Controls::processPointer(int action, int pid, float x, float y)
{
    int size = controls.size();

    float totalHeight = 0; //Find to limit scroll
    float windowHeight = 0;

    bool eventUsed = false;
    for (int n=0;n<size;n++)
    {
        ControlSuper *cs = controls.at(size - 1 - n); // Start from top of stack
        if (cs->isEnabled())
        {
            float ys;
            ys = (cs->type == TC_TYPE_UI_WINDOW) ? y : y + yOffset; //Dont apply scroll offset to window

            if( cs->processPointer(action,pid, x, ys ))
            {
                eventUsed = true;
            }
        }

        if( cs->type == TC_TYPE_UI_WINDOW )
        {
            windowHeight = cs->controlPos.bottom - cs->controlPos.top;
        }

        if( cs->controlPos.bottom > totalHeight )
        {
            totalHeight = cs->controlPos.bottom;
        }
    }

    if (action == P_UP)
    {
         scrolling = false;
    }

    if( !eventUsed ) // Finger not over a usable control
    {
        if (action == P_DOWN)
        {
            finger1.x = x;
            finger1.y = y;
            scrolling = true;
        }
        else if(action == P_MOVE) //Scroll controls
        {
            if( scrolling )
            {
                yOffset += finger1.y - y;

                if( yOffset > ( totalHeight - windowHeight ) )
                {
                    yOffset = ( totalHeight - windowHeight );
                }

                if( yOffset < 0 )
                    yOffset = 0;

                finger1.x = x;
                finger1.y = y;
            }
        }
    }


    return true;
}

int UI_Controls::draw ()
{
	if (fading)
	{
		if (fadeDir == FADE_IN) //Fading in
		{
			fadePos += fadeStep;
			if (fadePos >= 1)
				fading = false;

		}
		else //Fading out
		{
			fadePos -= fadeStep;
			if (fadePos <= 0)
			{
				fading = false;
				setEnabled(false); //now also disable the control
			}
		}
		//LOGTOUCH("fadePos = %f",fadePos);

		glColor4f(1, 1, 1, fadePos);

	}
	else
		glColor4f(1, 1, 1,1 );

    int size = controls.size();
    for (int n=0;n<size;n++) //draw
    {
        ControlSuper *c = controls.at(n);
        if (c->isEnabled())
        {
            glLoadIdentity();

            glScalef(GLScaleWidth, GLScaleHeight, 1);

            // Scroll, but don't scroll the background window
            if( c->type != TC_TYPE_UI_WINDOW )
            {
                glTranslatef( 0, yOffset, 0 );
            }
            c->drawGL();
        }
    }
    // The UI window enables this
    glDisable( GL_SCISSOR_TEST );

    return 0;
}

void UI_Controls::initGL ()
{
	int size = controls.size();
	for (int n=0;n<size;n++) //draw
	{
		ControlSuper *c = controls.at(size-1-n);
		c->initGL();
	}
}

void UI_Controls::setAlpha(float a)
{

}

void UI_Controls::fade(fadedir_t dir,int steps)
{
    if (dir == FADE_IN) //Fade in
	{
		fadePos = 0;
	}
	else //Fade out
	{
		fadePos = 1;
	}
	fadeDir = dir;
	fadeStep = (float)1/(float)steps;
	fading = true;
}