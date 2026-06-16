package com.opentouchgaming.androidcore.ui;

import java.io.Serializable;

class TouchSettingSaveInfo implements Serializable
{

    // WARNING! DO NOT MOVE THIS CLASS!!!

    private static final long serialVersionUID = 1L;

    // Saved
    String name;
    long timeSaved;

    // Gets updated when read, the folder number
    long folder;

}
