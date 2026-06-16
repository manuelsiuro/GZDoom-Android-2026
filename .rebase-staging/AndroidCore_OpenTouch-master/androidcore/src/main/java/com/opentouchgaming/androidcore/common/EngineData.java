package com.opentouchgaming.androidcore.common;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Emile on 30/10/2017.
 */

public class EngineData implements Serializable
{
    static final long serialVersionUID = 1;

    public ArrayList<CustomArgs> argsHistory = new ArrayList<CustomArgs>();
    public CustomArgs currentCustomArgs = new CustomArgs();
    public int selectedSubGamePos = 0;

    public void addArgsHistory()
    {
        if (!currentCustomArgs.isEmpty())
        {
            // Remove duplicates
            Iterator<CustomArgs> it = getArgsHistory().iterator();
            while (it.hasNext())
            {
                CustomArgs c = it.next();
                if (c.getFinalArgs().contentEquals(currentCustomArgs.getFinalArgs()))
                    it.remove();
            }

            //Limit size
            while (argsHistory.size() > 50)
                getArgsHistory().remove(getArgsHistory().size() - 1);

            // This creates a copy, otherwise the reference will go in
            CustomArgs args = new CustomArgs(currentCustomArgs);
            getArgsHistory().add(0, args);
        }
    }

    public ArrayList<CustomArgs> getArgsHistory()
    {
        if (argsHistory == null)
        {
            argsHistory = new ArrayList<CustomArgs>();
        }
        return argsHistory;
    }

    public CustomArgs getCurrentCustomArgs()
    {
        if (currentCustomArgs == null)
        {
            currentCustomArgs = new CustomArgs();
        }
        return currentCustomArgs;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        ObjectInputStream.GetField fields = in.readFields();

        argsHistory = (ArrayList<CustomArgs>) fields.get("argsHistory", new ArrayList<CustomArgs>());
        currentCustomArgs = (CustomArgs) fields.get("currentCustomArgs", new CustomArgs());

        if (fields.defaulted("selectedSubGamePos"))
        {
            // If selectedSubGamePos is missing, try to read selectedIWadPos (the old name)
            selectedSubGamePos = fields.get("selectedIWadPos", 0);
        }
        else
        {
            selectedSubGamePos = fields.get("selectedSubGamePos", 0);
        }
    }
}
