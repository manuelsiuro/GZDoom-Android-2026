package com.opentouchgaming.androidcore.controls;

/**
 * Created by Emile on 03/08/2017.
 */

public class PortActDefs
{
    public static final int ACTION_ANALOG_FWD = 0x8000;
    public static final int ACTION_ANALOG_STRAFE = 0x8001;
    public static final int ACTION_ANALOG_PITCH = 0x8002;
    public static final int ACTION_ANALOG_YAW = 0x8003;


    public static final int PORT_ACT_LEFT = 1;
    public static final int PORT_ACT_RIGHT = 2;
    public static final int PORT_ACT_FWD = 3;
    public static final int PORT_ACT_BACK = 4;
    public static final int PORT_ACT_LOOK_UP = 5;
    public static final int PORT_ACT_LOOK_DOWN = 6;
    public static final int PORT_ACT_MOVE_LEFT = 7;
    public static final int PORT_ACT_MOVE_RIGHT = 8;
    public static final int PORT_ACT_STRAFE = 9;
    public static final int PORT_ACT_SPEED = 10;
    public static final int PORT_ACT_USE = 11;
    public static final int PORT_ACT_JUMP = 12;
    public static final int PORT_ACT_ATTACK = 13;
    public static final int PORT_ACT_UP = 14;
    public static final int PORT_ACT_DOWN = 15;

    public static final int PORT_ACT_NEXT_WEP = 16;
    public static final int PORT_ACT_PREV_WEP = 17;

    //Quake 2
    public static final int PORT_ACT_INVEN = 18;
    public static final int PORT_ACT_INVUSE = 19;
    public static final int PORT_ACT_INVDROP = 20;
    public static final int PORT_ACT_INVPREV = 21;
    public static final int PORT_ACT_INVNEXT = 22;
    public static final int PORT_ACT_HELPCOMP = 23;

    public static final int PORT_ACT_USE_WEAPON_WHEEL = 24;
    public static final int PORT_ACT_CONSOLE = 25;
    public static final int PORT_ACT_SHOW_KBRD = 26;
    public static final int PORT_ACT_SHOW_INV = 27;
    public static final int PORT_ACT_SHOW_GP_UTILS = 28;
    public static final int PORT_ACT_SHOW_DPAD_INV = 29;

    //Doom
    public static final int PORT_ACT_MAP = 30;
    public static final int PORT_ACT_MAP_UP = 31;
    public static final int PORT_ACT_MAP_DOWN = 32;
    public static final int PORT_ACT_MAP_LEFT = 33;
    public static final int PORT_ACT_MAP_RIGHT = 34;
    public static final int PORT_ACT_MAP_ZOOM_IN = 35;
    public static final int PORT_ACT_MAP_ZOOM_OUT = 36;
    public static final int PORT_ACT_ALWAYS_RUN = 37;
    public static final int PORT_ACT_TOGGLE_CROUCH = 38;

    public static final int PORT_ACT_SMART_TOGGLE_RUN = 42;

    //RTCW
    public static final int PORT_ACT_ZOOM_IN = 50;
    public static final int PORT_ACT_ALT_FIRE = 51;
    public static final int PORT_ACT_RELOAD = 52;
    public static final int PORT_ACT_QUICKSAVE = 53;
    public static final int PORT_ACT_QUICKLOAD = 54;
    public static final int PORT_ACT_KICK = 56;
    public static final int PORT_ACT_LEAN_LEFT = 57;
    public static final int PORT_ACT_LEAN_RIGHT = 58;


    //MALICE
    public static final int PORT_MALICE_USE = 59;
    public static final int PORT_MALICE_RELOAD = 60;
    public static final int PORT_MALICE_CYCLE = 61;

    //SMD for Q2
    public static final int PORT_SMD_USE = 62;
    public static final int PORT_WRATH_USE = 63;

    //JK2
    //public static final int   PORT_ACT_FORCE_LIGHTNING = 60;
    //public static final int   PORT_ACT_SABER_BLOCK     = 62;
    //public static final int   PORT_ACT_FORCE_GRIP      = 63;
    public static final int PORT_ACT_ALT_ATTACK = 64;
    public static final int PORT_ACT_NEXT_FORCE = 65;
    public static final int PORT_ACT_PREV_FORCE = 66;
    public static final int PORT_ACT_FORCE_USE = 67;
    public static final int PORT_ACT_DATAPAD = 68;
    public static final int PORT_ACT_FORCE_SELECT = 69;
    public static final int PORT_ACT_WEAPON_SELECT = 70;
    public static final int PORT_ACT_SABER_STYLE = 71;
    public static final int PORT_ACT_FORCE_PULL = 75;
    public static final int PORT_ACT_FORCE_MIND = 76;
    public static final int PORT_ACT_FORCE_LIGHT = 77;
    public static final int PORT_ACT_FORCE_HEAL = 78;
    public static final int PORT_ACT_FORCE_GRIP = 79;
    public static final int PORT_ACT_FORCE_SPEED = 80;
    public static final int PORT_ACT_FORCE_PUSH = 81;
    public static final int PORT_ACT_SABER_SEL = 87; //Just chooses weapon 1 so show/hide saber.

    //Choloate
    public static final int PORT_ACT_GAMMA = 90;
    public static final int PORT_ACT_SHOW_WEAPONS = 91;
    public static final int PORT_ACT_SHOW_KEYS = 92;
    public static final int PORT_ACT_FLY_UP = 93;
    public static final int PORT_ACT_FLY_DOWN = 94;
    public static final int PORT_ACT_FLY_CENTER = 95;
    public static final int PORT_ACT_GYRO_TOGGLE = 96;

    public static final int PORT_ACT_WEAP0 = 100;
    public static final int PORT_ACT_WEAP1 = 101;
    public static final int PORT_ACT_WEAP2 = 102;
    public static final int PORT_ACT_WEAP3 = 103;
    public static final int PORT_ACT_WEAP4 = 104;
    public static final int PORT_ACT_WEAP5 = 105;
    public static final int PORT_ACT_WEAP6 = 106;
    public static final int PORT_ACT_WEAP7 = 107;
    public static final int PORT_ACT_WEAP8 = 108;
    public static final int PORT_ACT_WEAP9 = 109;
    public static final int PORT_ACT_WEAP10 = 110;
    public static final int PORT_ACT_WEAP11 = 111;
    public static final int PORT_ACT_WEAP12 = 112;
    public static final int PORT_ACT_WEAP13 = 113;

    public static final int PORT_ACT_WEAP_ALT = 114;
    public static final int PORT_ACT_MEDKIT = 115;
    public static final int PORT_ACT_RADAR = 116;
    //Custom
    public static final int PORT_ACT_CUSTOM_0 = 150;
    public static final int PORT_ACT_CUSTOM_1 = 151;
    public static final int PORT_ACT_CUSTOM_2 = 152;
    public static final int PORT_ACT_CUSTOM_3 = 153;
    public static final int PORT_ACT_CUSTOM_4 = 154;
    public static final int PORT_ACT_CUSTOM_5 = 155;
    public static final int PORT_ACT_CUSTOM_6 = 156;
    public static final int PORT_ACT_CUSTOM_7 = 157;


    public static final int PORT_ACT_CUSTOM_8 = 158;
    public static final int PORT_ACT_CUSTOM_9 = 159;
    public static final int PORT_ACT_CUSTOM_10 = 160;
    public static final int PORT_ACT_CUSTOM_11 = 161;
    public static final int PORT_ACT_CUSTOM_12 = 162;
    public static final int PORT_ACT_CUSTOM_13 = 163;
    public static final int PORT_ACT_CUSTOM_14 = 164;
    public static final int PORT_ACT_CUSTOM_15 = 165;
    public static final int PORT_ACT_CUSTOM_16 = 166;
    public static final int PORT_ACT_CUSTOM_17 = 167;
    public static final int PORT_ACT_CUSTOM_18 = 168;
    public static final int PORT_ACT_CUSTOM_19 = 169;
    public static final int PORT_ACT_CUSTOM_20 = 170;
    public static final int PORT_ACT_CUSTOM_21 = 171;
    public static final int PORT_ACT_CUSTOM_22 = 172;
    public static final int PORT_ACT_CUSTOM_23 = 173;
    public static final int PORT_ACT_CUSTOM_24 = 174;
    public static final int PORT_ACT_CUSTOM_25 = 175;

    //Doom 3
    public static final int PORT_ACT_FLASH_LIGHT = 200;
    public static final int PORT_ACT_SPRINT = 201;

    //Menu
    public static final int PORT_ACT_MENU_UP = 0x200;
    public static final int PORT_ACT_MENU_DOWN = 0x201;
    public static final int PORT_ACT_MENU_LEFT = 0x202;
    public static final int PORT_ACT_MENU_RIGHT = 0x203;
    public static final int PORT_ACT_MENU_SELECT = 0x204;
    public static final int PORT_ACT_MENU_BACK = 0x205;
    public static final int PORT_ACT_MENU_CONFIRM = 0x206;
    public static final int PORT_ACT_MENU_ABORT = 0x207;


    public static final int PORT_ACT_MENU_SHOW = 0x208;

    public static final int PORT_ACT_VOLUME_UP = 0x242;
    public static final int PORT_ACT_VOLUME_DOWN = 0x243;
}
