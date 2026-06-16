package com.opentouchgaming.androidcore.ui.tutorial;

import java.util.ArrayList;

public class Tutorial
{
    public String title;
    public String icon;

    public ArrayList<Screen> screens = new ArrayList<>();

    public Tutorial(String title, String icon)
    {
        this.title = title;
        this.icon = icon;
    }

    public ArrayList<Screen> getScreens()
    {
        return screens;
    }

    public void addScreen(Screen screen)
    {
        screens.add(screen);
    }

    static public class Screen
    {
        public String title;
        public String description;
        public String image;

        public Screen(String title, String description, String image)
        {
            this.title = title;
            this.description = description;
            this.image = image;
        }
    }
}
