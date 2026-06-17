Changelog
=========

## Version 0.50.0

_2026-06-17_

A ground-up modernization of the fork — new engine, a Kotlin/Compose launcher, an
in-app map editor, and a full WAD browser/manager. Published under the new
application id **`com.msa.freedoom`**.

### Engine
* Rebased the native stack onto **UZDoom 5.0.0-pre** (emileb's `uz_5.0_pre`, a maintained
  GZDoom 4.x fork) — brings SDL2, the modern GL ES 3 renderer, ZMusic, and **ZScript**
  support, so modern idgames releases actually run.
* **64-bit**: native libraries build for `arm64-v8a` and `armeabi-v7a` (Google Play 64-bit
  requirement); **16 KB page-size** support for Android 15+.
* Removed the proprietary **FMOD** dependency — audio is now open **OpenAL + FluidSynth**;
  readded OPL synth emulation.
* Portrait gameplay via aspect-fit letterboxing.

### Launcher (Jetpack Compose Material 3)
* Whole launcher rewritten in **Kotlin + Compose M3** (dark Doom theme); 100% Kotlin app code.
* Material 3 **NavigationBar** (Play / Browse / Editor / Settings); responsive two-pane
  launch screen with game-aware add-on filtering.
* **Themed system bars**: white status-bar icons; navigation bar matches the bottom navigation.
* Add-on picker: compatibility badges, an **"only compatible"** filter, **favorites** (star
  WADs/mods/folders + favorites filter, persisted), puzzle-piece icons, and the selected
  game shown in the title.
* New **Settings → About** screen (app version, engine, developer, and credit links).

### WAD browser & downloader (new "Browse" tab)
* Search the Doomworld **/idgames archive** by title/filename/author with one-tap, mirror-failover
  downloads that unzip straight into the add-on folder.
* Curated **Featured classics**, downloadable **Classic games** (shareware/freeware IWADs), and a
  **bring-your-own-copy** import flow for commercial IWADs.
* **Compatibility badges** (Doom/Doom II/Heretic/Hexen/Strife/Chex + map slot), a **rich detail
  sheet** (text file, base, credits, editors, bugs, reviews/ratings, "play with" hint), an
  **Archive** tree browser, **`idgames://` deep links**, **per-game download folders**, and an
  **Installed** view with multi-select **bulk delete**.

### In-app map editor (PNG2WAD)
* New **Editor** tab: draw a level on a tile grid and generate a playable Doom-format PWAD via a
  bundled native converter, then launch it. Shape tools, eyedropper, symmetry, brush size,
  replace-tile, validation, templates, thumbnails, and share/sandbox.

## Version 0.2.1

_2017-06-15_

* Added the soundfont from GZDoom 3.1.0. Music should *just work*.
* Dependency and SDK updates

## Version 0.2.0

_2017-04-16_

* Removed SoundFont download capabilities. Previous builds were downloading
  from someone's website and eating up their bandwidth. For now, place
  WeedsGM3.sf2 in GZDoom/soundfont/
* Moved GZDoom resolution divider to options tab
* Cleanup

## Version 0.1

_2017-02-25_

* Initial release.
