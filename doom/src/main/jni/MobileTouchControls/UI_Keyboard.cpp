//
// Created by Emile on 03/05/2017.
//

#include "UI_Keyboard.h"
#include "TouchControlsConfig.h"

using namespace touchcontrols;

#define Y_START_POS  0.55
#define ROW_HEIGHT   0.1
#define ROW_GAP      0.01

UI_Keyboard::UI_Keyboard( std::string tag, RectF pos, std::string font_filename, int fontSet, uint32_t params, float textSize ):
    ControlSuper( TC_TYPE_UI_KEYBOARD, tag, pos ),
    font_filename(font_filename),
    fontSet(fontSet)
{
    setKey( 0, 0, 'Q', '1', 0.1f );
    setKey( 0, 1, 'W', '2', 0.1f );
    setKey( 0, 2, 'E', '3', 0.1f );
    setKey( 0, 3, 'R', '4', 0.1f );
    setKey( 0, 4, 'T', '5', 0.1f );
    setKey( 0, 5, 'Y', '6', 0.1f );
    setKey( 0, 6, 'U', '7', 0.1f );
    setKey( 0, 7, 'I', '8', 0.1f );
    setKey( 0, 8, 'O', '9', 0.1f );
    setKey( 0, 9, 'P', '0', 0.1f );

    setKey( 1, 0, 0, 0,     0.05f ); //Blank
    setKey( 1, 1, 'A', '@', 0.1f );
    setKey( 1, 2, 'S', '#', 0.1f );
    setKey( 1, 3, 'D', '&', 0.1f );
    setKey( 1, 4, 'F', '*', 0.1f );
    setKey( 1, 5, 'G', '-', 0.1f );
    setKey( 1, 6, 'H', '+', 0.1f );
    setKey( 1, 7, 'J', '=', 0.1f );
    setKey( 1, 8, 'K', '(', 0.1f );
    setKey( 1, 9, 'L', ')', 0.1f );

    setKey( 2, 0, 0, 0,     0.15f ); //Blank
    setKey( 2, 1, 'Z', '_', 0.1f );
    setKey( 2, 2, 'X', '$', 0.1f );
    setKey( 2, 3, 'C', '"', 0.1f );
    setKey( 2, 4, 'V', '\'', 0.1f );
    setKey( 2, 5, 'B', ':', 0.1f );
    setKey( 2, 6, 'N', ';', 0.1f );
    setKey( 2, 7, 'M', '/', 0.1f );
    setKey( 2, 8, 8, 0, 0.15f, "key_backspace" ); //Backspace
    setKey( 2, 9, 0, 0, 0.1f );

    setKey( 3, 0, 1, 0, 0.10f, "hide_key" ); //Hide keyboard
    setKey( 3, 1, '.', 0, 0.1f );
    setKey( 3, 2, ',', 0, 0.1f );
    setKey( 3, 3, ' ', 0, 0.5f );
    setKey( 3, 4, 13, 0, 0.2f, "enter_key" ); //Enter
    setKey( 3, 5, 0, 0, 0.1f );
    setKey( 3, 6, 0, 0, 0.1f );
    setKey( 3, 7, 0, 0, 0.1f );
    setKey( 3, 8, 0, 0, 0.1f );
    setKey( 3, 9, 0, 0, 0.1f );

    pressedKey = NULL;
    touchId = -1;
}

void UI_Keyboard::setKey( uint32_t row, uint32_t key, char keyPrim, char keyAlt, float width, const char* primImg, const char * altImg )
{
    layout.rows[row].keys[key].keyPrim = keyPrim;
    layout.rows[row].keys[key].keyAlt  = keyAlt;
    layout.rows[row].keys[key].width   = width;
    layout.rows[row].keys[key].primImg = primImg;
    layout.rows[row].keys[key].altImg  = altImg;
    layout.rows[row].keys[key].glPrim  = 0;
    layout.rows[row].keys[key].glAlt   = 0;
}

bool UI_Keyboard::processPointer(int action, int pid, float x, float y)
{
	if (action == P_DOWN)
	{
		if (touchId == -1) //Only process if not active
		{
			if( y > Y_START_POS )
			{
				touchId = pid;

				int row = (y - Y_START_POS) / (ROW_HEIGHT + ROW_GAP);
                if(row < 4)
                {
                    float xPos = 0;

                    for(int n = 0; n < MAX_ROW_KEYS; n++ )
                    {
                        KeyboardKey *key = NULL;
                        key = &(layout.rows[row].keys[n]);
                        if( x < xPos + key->width)
                        {
                            pressedKey = key;
                            timeDown = getMS();
                            useAltKey = false;
                            break;
                        }

                        xPos = xPos + key->width;
                    }
                }

				return true;
			}
		}
		return false;
	}
	else if (action == P_UP)
	{
		if ( pid == touchId )
		{
		    if( pressedKey != NULL )
		    {
		        if( useAltKey )
		            signal.emit( pressedKey->keyAlt );
		        else
		            signal.emit( pressedKey->keyPrim );

                pressedKey = NULL;
		    }

		    touchId = -1;
			return true;
		}
		return false;
	}
	else if(action == P_MOVE)
	{
		return false;
	}

    return false;
}

void UI_Keyboard::resetOutput()
{

}

bool UI_Keyboard::drawGL(bool forEditor)
{
    float rowXPos = 0;
    float rowYPos = 0.55;
    float rowHeight = 0.1;
    float rowGap = 0.01;

    setFixAspect( false );

    GLRect glRect;

    // Draw background
    glRect.resize( 1, 0.5 );
    drawRect( 0, 0, 0, 0.8, 0, rowYPos, glRect );

    glColor4f(1,1,1,0.8);

    for( int row = 0; row < 4; row++ )
    {
        rowXPos = 0;
        for(int n = 0; n < MAX_ROW_KEYS; n++ )
        {
            KeyboardKey *key = &(layout.rows[row].keys[n]);

            if( key->keyPrim != 0 )
            {
                // Draw key background
                if( key == pressedKey ) //Make dark if pressed
                {
                    glColor4f( 0.5, 0.5, 0.5, 1 );

                    // Check if button is held down
                    if( key->keyAlt && getMS() > timeDown + 500 )
                    {
                        useAltKey = true;
                    }
                }
                glRect.resize( key->width, rowHeight );
                drawRect( glKeyBg, rowXPos, rowYPos, glRect );

                glColor4f(1,1,1,0.8);

                // Check for custom image, otherwise use font
                if( key->glPrim != 0 )
                {
                    drawRect( key->glPrim , rowXPos, rowYPos, glRect );
                }
                else
                {
                    textDrawer.drawChar( key->keyPrim, fontSet, rowXPos +  (key->width * 0.5), rowYPos, 0.1, TEXT_DRAW_X_CENTER );
                }

                if( key->keyAlt )
                    textDrawer.drawChar( key->keyAlt, fontSet, rowXPos +  (key->width * 0.8), rowYPos, 0.05, TEXT_DRAW_X_CENTER );

                // Finger has been press for a while so show the altkey above the finger
                if( key == pressedKey && useAltKey )
                {
                    textDrawer.drawChar( key->keyAlt, fontSet, rowXPos +  (key->width * 0.5), rowYPos - rowHeight, 0.1, TEXT_DRAW_X_CENTER );
                }
            }

            rowXPos = rowXPos + key->width;
        }
        rowYPos = rowYPos + rowHeight + rowGap;
    }
    glColor4f(1,1,1,1);

    setFixAspect( true );

    return false;
}

bool UI_Keyboard::initGL()
{
    textDrawer.initGL(font_filename);
    int x, y;
    glKeyBg = loadTextureFromPNG( "key_background", x, y );

    // Load any custom image for the keys
    for( int row = 0; row < 4; row++ )
    {
        for(int n = 0; n < MAX_ROW_KEYS; n++ )
        {
            KeyboardKey *key = &(layout.rows[row].keys[n]);
            if(key->primImg != NULL)
                key->glPrim = loadTextureFromPNG( key->primImg, x, y );

            if(key->altImg != NULL)
                key->glAlt = loadTextureFromPNG( key->altImg, x, y );
        }
    }
    return false;
}

void UI_Keyboard::updateSize()
{

}

void UI_Keyboard::saveXML(TiXmlDocument &doc)
{

}

void UI_Keyboard::loadXML(TiXmlDocument &doc)
{

}