package com.opentouchgaming.androidcore.ui.SuperMod;

import com.opentouchgaming.androidcore.GameEngine;
import com.opentouchgaming.androidcore.common.CustomArgs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

public class SuperModItem implements Serializable
{

    static final long serialVersionUID = 1;

    public String title;
    public GameEngine.Engine engine;
    public int version;
    public String subgameTag;
    public CustomArgs customArgs;
    public long lastPlayed;

    public String gameTypeImage;

    public String modImage; // null = none, "zipfile:filename" = image in zip/wad file

    public SuperModItem(GameEngine.Engine engine, int version, String subgameTag, String image, CustomArgs customArgs)
    {
        this.engine = engine;
        this.version = version;
        this.subgameTag = subgameTag;
        this.customArgs = customArgs;
        this.gameTypeImage = image;

        lastPlayed = 0;
        title = "";
    }

    public SuperModItem(SuperModItem item)
    {
        engine = item.engine;
        title = item.title;
        version = item.version;
        subgameTag = item.subgameTag;
        customArgs = new CustomArgs(item.customArgs);
        lastPlayed = item.lastPlayed;
        gameTypeImage = item.gameTypeImage;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        ObjectInputStream.GetField fields = in.readFields();

        title = (String) fields.get("title", "");
        engine = (GameEngine.Engine) fields.get("engine", null);
        version = fields.get("version", 0);
        customArgs = (CustomArgs) fields.get("customArgs", null);
        lastPlayed = fields.get("lastPlayed", 0L);
        modImage = (String) fields.get("modImage", null);
        gameTypeImage = (String) fields.get("gameTypeImage", null);

        if (fields.defaulted("subgameTag"))
        {
            subgameTag = (String) fields.get("iwad", "");
        }
        else
        {
            subgameTag = (String) fields.get("subgameTag", "");
        }
    }
}
