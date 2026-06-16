
LOCAL_PATH := $(call my-dir)/../thirdparty/adlmidi

include $(CLEAR_VARS)

LOCAL_MODULE    := adlmidi_zmd

LOCAL_CFLAGS := -DADLMIDI_DISABLE_MIDI_SEQUENCER -fsigned-char

LOCAL_CPPFLAGS :=  -fexceptions

LOCAL_C_INCLUDES :=

LOCAL_SRC_FILES =  	\
	adlmidi.cpp \
	adlmidi_load.cpp \
	adlmidi_midiplay.cpp \
	adlmidi_opl3.cpp \
	adlmidi_private.cpp \
	chips/dosbox/dbopl.cpp \
	chips/dosbox_opl3.cpp \
	chips/java_opl3.cpp \
	chips/nuked_opl3.cpp \
	chips/nuked_opl3_v174.cpp \
	chips/opal_opl3.cpp \
    chips/ym3812_lle.cpp  \
    chips/ym3812_lle/nopl2.c  \
    chips/ym3812_lle/nuked_fmopl2.c  \
    chips/ymf262_lle.cpp  \
    chips/ymf262_lle/nopl3.c  \
    chips/ymf262_lle/nuked_fmopl3.c  \
	inst_db.cpp \
	chips/opal/opal.c \
	chips/nuked/nukedopl3_174.c \
	chips/nuked/nukedopl3.c \
    chips/esfmu_opl3.cpp  \
    chips/esfmu/esfm.c  \
    chips/esfmu/esfm_registers.c  \
    chips/mame_opl2.cpp  \
    chips/ymfm_opl2.cpp \
    chips/ymfm_opl3.cpp \
    chips/ymfm/ymfm_adpcm.cpp \
    chips/ymfm/ymfm_misc.cpp \
    chips/ymfm/ymfm_opl.cpp \
    chips/ymfm/ymfm_pcm.cpp \
    chips/ymfm/ymfm_ssg.cpp \
	wopl/wopl_file.c \

LOCAL_CFLAGS += -fvisibility=hidden -fdata-sections -ffunction-sections

include $(BUILD_STATIC_LIBRARY)








