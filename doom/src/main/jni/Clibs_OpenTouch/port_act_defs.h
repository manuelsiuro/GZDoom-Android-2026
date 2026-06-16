#define PORT_ACT_LEFT       1
#define PORT_ACT_RIGHT      2
#define PORT_ACT_FWD        3
#define PORT_ACT_BACK       4
#define PORT_ACT_LOOK_UP    5
#define PORT_ACT_LOOK_DOWN  6
#define PORT_ACT_MOVE_LEFT  7
#define PORT_ACT_MOVE_RIGHT 8
#define PORT_ACT_STRAFE     9
#define PORT_ACT_SPEED      10
#define PORT_ACT_USE        11
#define PORT_ACT_JUMP       12
#define PORT_ACT_ATTACK     13
#define PORT_ACT_UP         14
#define PORT_ACT_DOWN       15

#define PORT_ACT_NEXT_WEP   16
#define PORT_ACT_PREV_WEP   17

//Quake2
#define PORT_ACT_INVEN     18
#define PORT_ACT_INVUSE    19
#define PORT_ACT_INVDROP   20
#define PORT_ACT_INVPREV   21
#define PORT_ACT_INVNEXT   22
#define PORT_ACT_HELPCOMP  23 //also RTCW, also Strife


#define PORT_ACT_USE_WEAPON_WHEEL   24
#define PORT_ACT_CONSOLE            25
#define PORT_ACT_SHOW_KBRD          26
#define PORT_ACT_SHOW_INV           27
#define PORT_ACT_SHOW_GP_UTILS      28
#define PORT_ACT_SHOW_DPAD_INV      29

//Doom
#define PORT_ACT_MAP         30
#define PORT_ACT_MAP_UP      31
#define PORT_ACT_MAP_DOWN    32
#define PORT_ACT_MAP_LEFT    33
#define PORT_ACT_MAP_RIGHT   34
#define PORT_ACT_MAP_ZOOM_IN 35
#define PORT_ACT_MAP_ZOOM_OUT 36
#define PORT_ACT_ALWAYS_RUN  37
#define PORT_ACT_TOGGLE_CROUCH  38
#define PORT_ACT_TOGGLE_ALT_ATTACK  39

// Hexen 2
#define PORT_ACT_CROUCH    40

//Multiplayer
#define PORT_ACT_MP_SAY    41

// Common 'Run' button
#define PORT_ACT_SMART_TOGGLE_RUN 42

//RTCW
#define PORT_ACT_ZOOM_IN    50 //Also used for Quake 3 zoom
#define PORT_ACT_ALT_FIRE   51
#define PORT_ACT_RELOAD     52
#define PORT_ACT_QUICKSAVE  53
#define PORT_ACT_QUICKLOAD  54
#define PORT_ACT_KICK       56
#define PORT_ACT_LEAN_LEFT  57
#define PORT_ACT_LEAN_RIGHT 58

//MALICE
#define PORT_MALICE_USE     59
#define PORT_MALICE_RELOAD  60
#define PORT_MALICE_CYCLE   61

//SMD for Q2
#define PORT_SMD_USE        62

// WRATH
#define PORT_WRATH_USE      63

//JK2
#define PORT_ACT_ALT_ATTACK      64
#define PORT_ACT_NEXT_FORCE      65
#define PORT_ACT_PREV_FORCE      66
#define PORT_ACT_FORCE_USE       67
#define PORT_ACT_DATAPAD         68
#define PORT_ACT_FORCE_SELECT    69
#define PORT_ACT_WEAPON_SELECT   70
#define PORT_ACT_SABER_STYLE     71
#define PORT_ACT_SAVE_MENU       72 //NOT USED
#define PORT_ACT_LOAD_MENU       73 //NOT USED
#define PORT_ACT_THIRD_PERSON    74

#define PORT_ACT_FORCE_PULL      75
#define PORT_ACT_FORCE_MIND      76
#define PORT_ACT_FORCE_LIGHT     77
#define PORT_ACT_FORCE_HEAL      78
#define PORT_ACT_FORCE_GRIP      79
#define PORT_ACT_FORCE_SPEED     80
#define PORT_ACT_FORCE_PUSH      81

//JK3
#define PORT_ACT_FORCE_DRAIN     82
#define PORT_ACT_FORCE_RAGE      83
#define PORT_ACT_FORCE_PROTECT   84
#define PORT_ACT_FORCE_ABSORB    85
#define PORT_ACT_FORCE_SIGHT     86

#define PORT_ACT_SABER_SEL       87 //Just chooses weapon 1 so show/hide saber.
#define PORT_ACT_MP_CHAT         88
#define PORT_ACT_MP_SCORES       89
#define PORT_ACT_MP_DUEL         90


//Chocolate
#define PORT_ACT_GAMMA              90
#define PORT_ACT_SHOW_WEAPONS       91
#define PORT_ACT_SHOW_KEYS          92
#define PORT_ACT_FLY_UP             93
#define PORT_ACT_FLY_DOWN           94
#define PORT_ACT_FLY_CENTER         95

#define PORT_ACT_GYRO_TOGGLE        96

#define PORT_ACT_HOLSTER_WEAPON     97

#define PORT_ACT_WEAP0              100
#define PORT_ACT_WEAP1              101
#define PORT_ACT_WEAP2              102
#define PORT_ACT_WEAP3              103
#define PORT_ACT_WEAP4              104
#define PORT_ACT_WEAP5              105
#define PORT_ACT_WEAP6              106
#define PORT_ACT_WEAP7              107
#define PORT_ACT_WEAP8              108
#define PORT_ACT_WEAP9              109
#define PORT_ACT_WEAP10             110
#define PORT_ACT_WEAP11             111
#define PORT_ACT_WEAP12             112
#define PORT_ACT_WEAP13             113

#define PORT_ACT_WEAP_ALT           114

// Perfect Dark N64  - Reuses CUSTOMX button values
#define PORT_ACT_N64_BUTTON_A        150
#define PORT_ACT_N64_BUTTON_B        151


// IONFURY
#define PORT_ACT_MEDKIT            115
#define PORT_ACT_RADAR             116

//Descent
#define PORT_ACT_CYCLE_PRIM        120
#define PORT_ACT_CYCLE_SEC         121
#define PORT_ACT_FIRE_FLARE        122
#define PORT_ACT_FIRE_BOMB         123
#define PORT_ACT_AFTERBURNER       124
#define PORT_ACT_HEADLIGHT         125
#define PORT_ACT_ENERGY_SHIELD     126
#define PORT_ACT_COCKPIT_VIEW      127
#define PORT_ACT_VERT_UP           128
#define PORT_ACT_VERT_DOWN         129
#define PORT_ACT_BANK_LEFT         130
#define PORT_ACT_BANK_RIGHT        131

//Generic custom buttons
#define PORT_ACT_CUSTOM_0          150
#define PORT_ACT_CUSTOM_1          151
#define PORT_ACT_CUSTOM_2          152
#define PORT_ACT_CUSTOM_3          153
#define PORT_ACT_CUSTOM_4          154
#define PORT_ACT_CUSTOM_5          155
#define PORT_ACT_CUSTOM_6          156
#define PORT_ACT_CUSTOM_7          157


#define PORT_ACT_CUSTOM_8          158
#define PORT_ACT_CUSTOM_9          159
#define PORT_ACT_CUSTOM_10         160
#define PORT_ACT_CUSTOM_11         161
#define PORT_ACT_CUSTOM_12         162
#define PORT_ACT_CUSTOM_13         163
#define PORT_ACT_CUSTOM_14         164
#define PORT_ACT_CUSTOM_15         165
#define PORT_ACT_CUSTOM_16         166
#define PORT_ACT_CUSTOM_17         167
#define PORT_ACT_CUSTOM_18         168
#define PORT_ACT_CUSTOM_19         169
#define PORT_ACT_CUSTOM_20         170
#define PORT_ACT_CUSTOM_21         171
#define PORT_ACT_CUSTOM_22         172
#define PORT_ACT_CUSTOM_23         173
#define PORT_ACT_CUSTOM_24         174
#define PORT_ACT_CUSTOM_25         175



// Demo playback
#define PORT_ACT_DEMO_SPD_UP      180
#define PORT_ACT_DEMO_SPD_DWN     181
#define PORT_ACT_DEMO_SPD_DEF     182
#define PORT_ACT_DEMO_JOIN        183
#define PORT_ACT_DEMO_CAMERA      184


//Doom 3
#define PORT_ACT_FLASH_LIGHT      200
#define PORT_ACT_SPRINT           201


#define PORT_ACT_MENU_UP                  0x200 //512
#define PORT_ACT_MENU_DOWN                0x201 //513
#define PORT_ACT_MENU_LEFT                0x202 //514
#define PORT_ACT_MENU_RIGHT               0x203 //515
#define PORT_ACT_MENU_SELECT              0x204 //516
#define PORT_ACT_MENU_BACK                0x205 //517
#define PORT_ACT_MENU_CONFIRM             0x206 //518
#define PORT_ACT_MENU_ABORT               0x207 //519
#define PORT_ACT_MENU_SHOW                0x208 //520 (back button)

#define PORT_ACT_MOUSE_LEFT               0x240 // Left mouse button
#define PORT_ACT_MOUSE_RIGHT              0x241 // Right mouse button
#define PORT_ACT_VOLUME_UP                0x242
#define PORT_ACT_VOLUME_DOWN              0x243


#define LOOK_MODE_MOUSE    0
#define LOOK_MODE_ABSOLUTE 1
#define LOOK_MODE_JOYSTICK 2


#define ANALOGUE_AXIS_FWD   1
#define ANALOGUE_AXIS_SIDE  2
#define ANALOGUE_AXIS_PITCH 4
#define ANALOGUE_AXIS_YAW   8


#define GAME_OPTION_AUTO_HIDE_GAMEPAD   0x1
#define GAME_OPTION_HIDE_MENU_AND_GAME  0x2
#define GAME_OPTION_USE_SYSTEM_KEYBOARD 0x4
#define GAME_OPTION_GLES2               0x8
#define GAME_OPTION_GLES3               0x10
#define GAME_OPTION_SDL_OLD_AUDIO       0x20
#define GAME_OPTION_GL4ES               0x40
#define GAME_OPTION_SDL_AAUDIO_AUDIO    0x80
#define GAME_OPTION_SDL_MIDI_FLUIDSYNTH 0x100
#define GAME_OPTION_TOUCH_SURFACE_VIEW  0x200
#define GAME_OPTION_VULKAN              0x400
#define GAME_OPTION_DOOM_SET_DOOMWADDIR 0x800
#define GAME_OPTION_OPENAL_USE_OBOE         0x1000
#define GAME_OPTION_TOUCHJOY_MULTITOUCH     0x2000
