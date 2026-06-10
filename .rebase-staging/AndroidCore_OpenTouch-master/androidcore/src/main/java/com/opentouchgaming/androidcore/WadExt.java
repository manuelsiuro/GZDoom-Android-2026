package com.opentouchgaming.androidcore;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Emile on 04/02/2018.
 */

public class WadExt
{
    public static final int NO_CONVERT_GFX = 1;
    public static final int DO_HERETIC_PAL = 2;
    public static final int DO_HEXEN_PAL = 4;
    public static final int DO_STRIFE = 8;
    public static final int DO_STRIP = 16;    // strip map lumps ZDoom does not need (nodes, blockmap, reject)
    public static final int NO_CONVERT_SND = 32;
    static Map<String, TitlePicOptions> titlePicOptions;

    static
    {
        Map<String, TitlePicOptions> aMap = new HashMap<String, TitlePicOptions>();
        aMap.put("hexen.wad", new TitlePicOptions("TITLE", WadExt.DO_HEXEN_PAL, true));
        aMap.put("hexdd.wad", new TitlePicOptions("TITLE", WadExt.DO_HEXEN_PAL, true));
        aMap.put("heretic.wad", new TitlePicOptions("TITLE", WadExt.DO_HERETIC_PAL, true));
        aMap.put("strife.wad", new TitlePicOptions("TITLEPIC", WadExt.DO_STRIFE, true));
        aMap.put("strife1.wad", new TitlePicOptions("TITLEPIC", WadExt.DO_STRIFE, true));
        aMap.put("doom2bfg.wad", new TitlePicOptions("DMENUPIC", 0, true));
        aMap.put("bfgdoom2.wad", new TitlePicOptions("DMENUPIC", 0, true));

        titlePicOptions = Collections.unmodifiableMap(aMap);
    }

    public static native int extract(String wadFile, String lumpName, int options, int isFlat, String outFilename);

    public static TitlePicOptions getOptions(String wadName)
    {
        TitlePicOptions ret = titlePicOptions.get(wadName.toLowerCase());

        if (ret == null)
        {
            // Default for doom and the rest
            ret = new TitlePicOptions("TITLEPIC", 0, false);
        }

        return ret;
    }

    static public class TitlePicOptions
    {
        public String lumpName;
        public int options;
        public boolean isFlat;

        TitlePicOptions(String n, int o, boolean f)
        {
            lumpName = n;
            options = o;
            isFlat = f;
        }
    }

}
