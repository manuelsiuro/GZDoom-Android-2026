LOCAL_PATH := $(call my-dir)/../src


include $(CLEAR_VARS)

LOCAL_MODULE    := gzdoom

LOCAL_CFLAGS   := -DNO_FMOD -DNO_CLOCK_GETTIME -D__MOBILE__ -DGZDOOM1_9 -DGZDOOM -D__STDINT_LIMITS -DENGINE_NAME=\"gzdoom_1.9\"
LOCAL_CPPFLAGS := -DNOASM -DFMOD_STUDIO -DDISABLE_SSE -DHAVE_FLUIDSYNTH  -DHAVE_MPG123 -DHAVE_SNDFILE -DONLY_GPL -DHAVE_JWZGLES -DUSE_GLES  -Werror=format-security  -fexceptions -fpermissive -Dstricmp=strcasecmp -Dstrnicmp=strncasecmp -D__forceinline=inline -DNO_GTK -DNO_SSE -fsigned-char


LOCAL_C_INCLUDES := \
 $(TOP_DIR)/ \
 $(TOP_DIR)/AudioLibs_OpenTouch/fluidsynth-lite/include \
 $(GZDOOM_TOP_PATH)/src/  \
 $(GZDOOM_TOP_PATH)/mobile/src/extrafiles  \
 $(GZDOOM_TOP_PATH)/game-music-emu/ \
 $(GZDOOM_TOP_PATH)/gdtoa \
 $(GZDOOM_TOP_PATH)/lzma/C \
 $(GZDOOM_TOP_PATH)/bzip2 \
 $(GZDOOM_TOP_PATH)/src/sound \
 $(GZDOOM_TOP_PATH)/src/textures \
 $(GZDOOM_TOP_PATH)/src/thingdef \
 $(GZDOOM_TOP_PATH)/src/posix \
 $(GZDOOM_TOP_PATH)/src/posix/sdl \
 $(GZDOOM_TOP_PATH)/src/g_doom \
 $(GZDOOM_TOP_PATH)/src/g_heretic \
 $(GZDOOM_TOP_PATH)/src/g_hexen \
 $(GZDOOM_TOP_PATH)/src/g_raven \
 $(GZDOOM_TOP_PATH)/src/g_strife \
 $(GZDOOM_TOP_PATH)/src/g_shared \
  $(TOP_DIR)/SDL/include  \
 $(TOP_DIR)/AudioLibs_OpenTouch/openal/include/AL \
 $(TOP_DIR)/AudioLibs_OpenTouch/libsndfile-android/jni/ \
 $(TOP_DIR)/AudioLibs_OpenTouch/libmpg123 \
 $(TOP_DIR)/AudioLibs_OpenTouch/FMOD_studio/api/lowlevel/inc \
 $(TOP_DIR)/jpeg8d \
 $(TOP_DIR)/jwzgles \
 $(TOP_DIR)/MobileTouchControls  \
 $(GZDOOM_TOP_PATH)/mobile/src


#############################################################################
# CLIENT/SERVER
#############################################################################


ANDROID_SRC_FILES = \
    ../mobile/src/in_android.cpp \
    ../mobile/src/android-jni.cpp \
    ../mobile/src/fts.c \

#ANDROID_SRC_FILES =

PLAT_POSIX_SOURCES = \
	posix/i_cd.cpp \
	posix/i_movie.cpp \
	posix/i_steam.cpp

PLAT_SDL_SOURCES = \
	posix/sdl/crashcatcher.c \
	posix/sdl/hardware.cpp \
	posix/sdl/i_gui.cpp \
	posix/sdl/i_input.cpp \
	posix/sdl/i_joystick.cpp \
	posix/sdl/i_main.cpp \
	posix/sdl/i_system.cpp \
	posix/sdl/i_timer.cpp \
	posix/sdl/sdlvideo.cpp \
	posix/sdl/sdlglvideo.cpp \
	posix/sdl/st_start.cpp

FASTMATH_SOURCES = \
	swrenderer/r_all.cpp \
	polyrenderer/poly_all.cpp \
	sound/oplsynth/opl_mus_player.cpp \
	sound/mpg123_decoder.cpp \
	sound/music_midi_base.cpp \
	sound/oalsound.cpp \
	sound/sndfile_decoder.cpp \
	sound/mididevices/music_timiditypp_mididevice.cpp \
	gl/data/gl_matrix.cpp \
	gl/utility/gl_clock.cpp \
	gl/renderer/gl_2ddrawer.cpp \
	gl/hqnx/init.cpp \
	gl/hqnx/hq2x.cpp \
	gl/hqnx/hq3x.cpp \
	gl/hqnx/hq4x.cpp \
	gl/xbr/xbrz.cpp \
	gl/xbr/xbrz_old.cpp \
	gl/scene/gl_bsp.cpp \
	gl/scene/gl_fakeflat.cpp \
	gl/scene/gl_clipper.cpp \
	gl/scene/gl_decal.cpp \
	gl/scene/gl_drawinfo.cpp \
	gl/scene/gl_flats.cpp \
	gl/scene/gl_walls.cpp \
	gl/scene/gl_sprite.cpp \
	gl/scene/gl_skydome.cpp \
	gl/scene/gl_renderhacks.cpp \
	gl/scene/gl_weapon.cpp \
	gl/scene/gl_scene.cpp \
	gl/scene/gl_sky.cpp \
	gl/scene/gl_portal.cpp \
	gl/scene/gl_walls_draw.cpp \
	gl/scene/gl_vertex.cpp \
	gl/scene/gl_spritelight.cpp \
	gl/dynlights/gl_dynlight1.cpp \
	gl/system/gl_load.c \
	gl/models/gl_models.cpp \


PCH_SOURCES = \
	x86.cpp \
	actorptrselect.cpp \
	am_map.cpp \
	b_bot.cpp \
	b_func.cpp \
	b_game.cpp \
	b_move.cpp \
	b_think.cpp \
	bbannouncer.cpp \
	c_bind.cpp \
	c_cmds.cpp \
	c_console.cpp \
	c_consolebuffer.cpp \
	c_cvars.cpp \
	c_dispatch.cpp \
	c_expr.cpp \
	cmdlib.cpp \
	colormatcher.cpp \
	compatibility.cpp \
	configfile.cpp \
	ct_chat.cpp \
	d_dehacked.cpp \
	d_iwad.cpp \
	d_main.cpp \
	d_net.cpp \
	d_netinfo.cpp \
	d_protocol.cpp \
	decallib.cpp \
	dobject.cpp \
	dobjgc.cpp \
	dobjtype.cpp \
	doomdef.cpp \
	doomstat.cpp \
	dsectoreffect.cpp \
	dthinker.cpp \
	edata.cpp \
	f_wipe.cpp \
	farchive.cpp \
	files.cpp \
	g_doomedmap.cpp \
	g_game.cpp \
	g_hub.cpp \
	g_level.cpp \
	g_mapinfo.cpp \
	g_skill.cpp \
	gameconfigfile.cpp \
	gi.cpp \
	gitinfo.cpp \
	hu_scores.cpp \
	i_net.cpp \
	info.cpp \
	keysections.cpp \
	lumpconfigfile.cpp \
	m_alloc.cpp \
	m_argv.cpp \
	m_bbox.cpp \
	m_cheat.cpp \
	m_joy.cpp \
	m_misc.cpp \
	m_png.cpp \
	m_random.cpp \
	m_specialpaths.cpp \
	memarena.cpp \
	md5.cpp \
	name.cpp \
	nodebuild.cpp \
	nodebuild_classify_nosse2.cpp \
	nodebuild_events.cpp \
	nodebuild_extract.cpp \
	nodebuild_gl.cpp \
	nodebuild_utility.cpp \
	pathexpander.cpp \
	p_3dfloors.cpp \
	p_3dmidtex.cpp \
	p_acs.cpp \
	p_buildmap.cpp \
	p_ceiling.cpp \
	p_conversation.cpp \
	p_doors.cpp \
	p_effect.cpp \
	p_enemy.cpp \
	p_floor.cpp \
	p_glnodes.cpp \
	p_interaction.cpp \
	p_lights.cpp \
	p_linkedsectors.cpp \
	p_lnspec.cpp \
	p_map.cpp \
	p_maputl.cpp \
	p_mobj.cpp \
	p_pillar.cpp \
	p_plats.cpp \
	p_portals.cpp \
	p_pspr.cpp \
	p_saveg.cpp \
	p_sectors.cpp \
	p_setup.cpp \
	p_sight.cpp \
	p_slopes.cpp \
	p_spec.cpp \
	p_states.cpp \
	p_switch.cpp \
	p_tags.cpp \
	p_teleport.cpp \
	p_terrain.cpp \
	p_things.cpp \
	p_tick.cpp \
	p_trace.cpp \
	p_udmf.cpp \
	p_usdf.cpp \
	p_user.cpp \
	p_writemap.cpp \
	p_xlat.cpp \
	parsecontext.cpp \
	po_man.cpp \
	r_swrenderer.cpp \
	r_utility.cpp \
	r_3dfloors.cpp \
	r_bsp.cpp \
	r_draw.cpp \
	r_drawt.cpp \
	r_main.cpp \
	r_plane.cpp \
	r_segs.cpp \
	r_sky.cpp \
	r_things.cpp \
	s_advsound.cpp \
	s_environment.cpp \
	s_playlist.cpp \
	s_sndseq.cpp \
	s_sound.cpp \
	sc_man.cpp \
	st_stuff.cpp \
	statistics.cpp \
	stats.cpp \
	stringtable.cpp \
	strnatcmp.c \
	tables.cpp \
	teaminfo.cpp \
	tempfiles.cpp \
	v_blend.cpp \
	v_collection.cpp \
	v_draw.cpp \
	v_font.cpp \
	v_palette.cpp \
	v_pfx.cpp \
	v_text.cpp \
	v_video.cpp \
	w_wad.cpp \
	wi_stuff.cpp \
	zstrformat.cpp \
	zstring.cpp \
	GuillotineBinPack.cpp \
	SkylineBinPack.cpp \
	g_doom/a_doommisc.cpp \
	g_heretic/a_hereticmisc.cpp \
	g_hexen/a_hexenmisc.cpp \
	g_raven/a_artitele.cpp \
	g_raven/a_minotaur.cpp \
	g_strife/a_strifestuff.cpp \
	g_strife/strife_sbar.cpp \
	g_shared/a_action.cpp \
	g_shared/a_armor.cpp \
	g_shared/a_artifacts.cpp \
	g_shared/a_bridge.cpp \
	g_shared/a_camera.cpp \
	g_shared/a_debris.cpp \
	g_shared/a_decals.cpp \
	g_shared/a_fastprojectile.cpp \
	g_shared/a_flashfader.cpp \
	g_shared/a_fountain.cpp \
	g_shared/a_hatetarget.cpp \
	g_shared/a_keys.cpp \
	g_shared/a_lightning.cpp \
	g_shared/a_mapmarker.cpp \
	g_shared/a_morph.cpp \
	g_shared/a_movingcamera.cpp \
	g_shared/a_pickups.cpp \
	g_shared/a_puzzleitems.cpp \
	g_shared/a_quake.cpp \
	g_shared/a_randomspawner.cpp \
	g_shared/a_secrettrigger.cpp \
	g_shared/a_sectoraction.cpp \
	g_shared/a_setcolor.cpp \
	g_shared/a_skies.cpp \
	g_shared/a_soundenvironment.cpp \
	g_shared/a_soundsequence.cpp \
	g_shared/a_spark.cpp \
	g_shared/a_specialspot.cpp \
	g_shared/a_waterzone.cpp \
	g_shared/a_weaponpiece.cpp \
	g_shared/a_weapons.cpp \
	g_shared/hudmessages.cpp \
	g_shared/sbarinfo.cpp \
	g_shared/sbar_mugshot.cpp \
	g_shared/shared_hud.cpp \
	g_shared/shared_sbar.cpp \
	intermission/intermission.cpp \
	intermission/intermission_parse.cpp \
	menu/colorpickermenu.cpp \
	menu/joystickmenu.cpp \
	menu/listmenu.cpp \
	menu/loadsavemenu.cpp \
	menu/menu.cpp \
	menu/menudef.cpp \
	menu/menuinput.cpp \
	menu/messagebox.cpp \
	menu/optionmenu.cpp \
	menu/playerdisplay.cpp \
	menu/playermenu.cpp \
	menu/readthis.cpp \
	menu/videomenu.cpp \
	gl/data/gl_data.cpp \
	gl/data/gl_portaldata.cpp \
	gl/data/gl_setup.cpp \
	gl/data/gl_vertexbuffer.cpp \
	gl/dynlights/a_dynlight.cpp \
	gl/utility/gl_clock.cpp \
	gl/utility/gl_cycler.cpp \
	gl/utility/gl_geometric.cpp \
	gl/renderer/gl_renderer.cpp \
	gl/renderer/gl_renderstate.cpp \
	gl/renderer/gl_lightdata.cpp \
	gl/hqnx/init.cpp \
	gl/hqnx/hq2x.cpp \
	gl/hqnx/hq3x.cpp \
	gl/hqnx/hq4x.cpp \
	gl/textures/gl_hwtexture.cpp \
	gl/textures/gl_texture.cpp \
	gl/textures/gl_material.cpp \
	gl/textures/gl_hirestex.cpp \
	gl/textures/gl_bitmap.cpp \
	gl/textures/gl_translate.cpp \
	gl/textures/gl_hqresize.cpp \
	gl/textures/gl_skyboxtexture.cpp \
	gl/scene/gl_bsp.cpp \
	gl/scene/gl_fakeflat.cpp \
	gl/scene/gl_clipper.cpp \
	gl/scene/gl_decal.cpp \
	gl/scene/gl_drawinfo.cpp \
	gl/scene/gl_flats.cpp \
	gl/scene/gl_walls.cpp \
	gl/scene/gl_sprite.cpp \
	gl/scene/gl_skydome.cpp \
	gl/scene/gl_renderhacks.cpp \
	gl/scene/gl_weapon.cpp \
	gl/scene/gl_scene.cpp \
	gl/scene/gl_sky.cpp \
	gl/scene/gl_portal.cpp \
	gl/scene/gl_walls_draw.cpp \
	gl/scene/gl_vertex.cpp \
	gl/scene/gl_spritelight.cpp \
	gl/stereo3d/gl_stereo3d.cpp \
	gl/stereo3d/gl_stereo_cvars.cpp \
	gl/stereo3d/gl_stereo_leftright.cpp \
	gl/stereo3d/scoped_view_shifter.cpp \
	gl/stereo3d/gl_anaglyph.cpp \
	gl/dynlights/gl_dynlight.cpp \
	gl/dynlights/gl_glow.cpp \
	gl/dynlights/gl_dynlight1.cpp \
	gl/dynlights/gl_lightbuffer.cpp \
	gl/shaders/gl_shader.cpp \
	gl/shaders/gl_texshader.cpp \
	gl/system/gl_interface.cpp \
	gl/system/gl_framebuffer.cpp \
	gl/system/gl_menu.cpp \
	gl/system/gl_wipe.cpp \
	gl/system/gl_load.c \
	gl/models/gl_models_md3.cpp \
	gl/models/gl_models_md2.cpp \
	gl/models/gl_models.cpp \
	gl/models/gl_voxels.cpp \
	oplsynth/fmopl.cpp \
	oplsynth/mlopl.cpp \
	oplsynth/mlopl_io.cpp \
	oplsynth/music_opldumper_mididevice.cpp \
	oplsynth/music_opl_mididevice.cpp \
	oplsynth/opl_mus_player.cpp \
	oplsynth/dosbox/opl.cpp \
	oplsynth/OPL3.cpp \
	oplsynth/nukedopl3.cpp \
	resourcefiles/ancientzip.cpp \
	resourcefiles/file_7z.cpp \
	resourcefiles/file_grp.cpp \
	resourcefiles/file_lump.cpp \
	resourcefiles/file_rff.cpp \
	resourcefiles/file_wad.cpp \
	resourcefiles/file_zip.cpp \
	resourcefiles/file_pak.cpp \
	resourcefiles/file_directory.cpp \
	resourcefiles/resourcefile.cpp \
	sfmt/SFMT.cpp \
	sound/fmodsound_studio.cpp \
	sound/i_music.cpp \
	sound/i_sound.cpp \
	sound/mpg123_decoder.cpp \
	sound/music_cd.cpp \
	sound/music_dumb.cpp \
	sound/music_gme.cpp \
	sound/music_mus_midiout.cpp \
	sound/music_smf_midiout.cpp \
	sound/music_hmi_midiout.cpp \
	sound/music_xmi_midiout.cpp \
	sound/music_midistream.cpp \
	sound/music_midi_base.cpp \
	sound/music_midi_timidity.cpp \
	sound/music_mus_opl.cpp \
	sound/music_stream.cpp \
	sound/music_fluidsynth_mididevice.cpp \
	sound/music_softsynth_mididevice.cpp \
	sound/music_timidity_mididevice.cpp \
	sound/music_wildmidi_mididevice.cpp \
	sound/music_win_mididevice.cpp \
	sound/oalsound.cpp \
	sound/sndfile_decoder.cpp \
	sound/music_pseudo_mididevice.cpp \
	textures/animations.cpp \
	textures/anim_switches.cpp \
	textures/automaptexture.cpp \
	textures/bitmap.cpp \
	textures/buildtexture.cpp \
	textures/canvastexture.cpp \
	textures/ddstexture.cpp \
	textures/flattexture.cpp \
	textures/imgztexture.cpp \
	textures/jpegtexture.cpp \
	textures/multipatchtexture.cpp \
	textures/patchtexture.cpp \
	textures/pcxtexture.cpp \
	textures/pngtexture.cpp \
	textures/rawpagetexture.cpp \
	textures/emptytexture.cpp \
	textures/texture.cpp \
	textures/texturemanager.cpp \
	textures/tgatexture.cpp \
	textures/warptexture.cpp \
	thingdef/olddecorations.cpp \
	thingdef/thingdef.cpp \
	thingdef/thingdef_codeptr.cpp \
	thingdef/thingdef_data.cpp \
	thingdef/thingdef_exp.cpp \
	thingdef/thingdef_expression.cpp \
	thingdef/thingdef_function.cpp \
	thingdef/thingdef_parse.cpp \
	thingdef/thingdef_properties.cpp \
	thingdef/thingdef_states.cpp \
	timidity/common.cpp \
	timidity/instrum.cpp \
	timidity/instrum_dls.cpp \
	timidity/instrum_font.cpp \
	timidity/instrum_sf2.cpp \
	timidity/mix.cpp \
	timidity/playmidi.cpp \
	timidity/resample.cpp \
	timidity/timidity.cpp \
	wildmidi/file_io.cpp \
	wildmidi/gus_pat.cpp \
	wildmidi/reverb.cpp \
	wildmidi/wildmidi_lib.cpp \
	wildmidi/wm_error.cpp \
	xlat/parse_xlat.cpp \
	fragglescript/t_fspic.cpp \
	fragglescript/t_func.cpp \
	fragglescript/t_load.cpp \
	fragglescript/t_oper.cpp \
	fragglescript/t_parse.cpp \
	fragglescript/t_prepro.cpp \
	fragglescript/t_script.cpp \
	fragglescript/t_spec.cpp \
	fragglescript/t_variable.cpp \
	fragglescript/t_cmd.cpp \
	r_data/colormaps.cpp \
	r_data/sprites.cpp \
	r_data/voxels.cpp \
	r_data/renderstyle.cpp \
	r_data/r_interpolate.cpp \
	r_data/r_translate.cpp \
	zzautozend.cpp \

LOCAL_SRC_FILES = \
    __autostart.cpp \
    $(ANDROID_SRC_FILES) \
    $(PLAT_POSIX_SOURCES) \
    $(PLAT_SDL_SOURCES) \
    $(PCH_SOURCES)


# Turn down optimisation of this file so clang doesnt produce ldrd instructions which are missaligned
p_acs.cpp_CFLAGS := -O1


LOCAL_LDLIBS := -ldl -llog -lOpenSLES -lGLESv1_CM
LOCAL_LDLIBS +=  -lEGL

LOCAL_STATIC_LIBRARIES := sndfile mpg123 fluidsynth-static libjpeg zlib_1.9 lzma_1.9 gdtoa_1.9 dumb_1.9 gme_1.9 bzip2_1.9
LOCAL_SHARED_LIBRARIES := touchcontrols openal SDL jwzgles_shared

LOCAL_STATIC_LIBRARIES +=

include $(BUILD_SHARED_LIBRARY)








