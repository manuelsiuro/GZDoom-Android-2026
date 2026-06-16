package com.opentouchgaming.androidcore.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

public class CompatibleObjectInputStream extends ObjectInputStream
{
    public CompatibleObjectInputStream(InputStream in) throws IOException
    {
        super(in);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException
    {
        String name = desc.getName();
        if ("com.opentouchgaming.deltatouch.CustomArgs".equals(name))
        {
            return CustomArgs.class;
        }
        if ("com.opentouchgaming.deltatouch.EngineData".equals(name))
        {
            return EngineData.class;
        }
        if ("com.opentouchgaming.deltatouch.SuperMod.SuperModItem".equals(name))
        {
            return com.opentouchgaming.androidcore.ui.SuperMod.SuperModItem.class;
        }
        if ("com.opentouchgaming.deltatouch.AppData".equals(name)) {
            return com.opentouchgaming.androidcore.common.AppData.class;
        }
        return super.resolveClass(desc);
    }
}
