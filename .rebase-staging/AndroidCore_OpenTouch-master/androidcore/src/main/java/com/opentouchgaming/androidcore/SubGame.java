package com.opentouchgaming.androidcore;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.opentouchgaming.androidcore.ui.FileSelectDialog;
import com.opentouchgaming.saffal.FileSAF;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Emile on 20/05/2018.
 */

public class SubGame
{
    public boolean selected;
    String title;
    int image;
    String imagePng;
    String detail1;
    String detail2;
    String rootPath;
    String name;
    int gameType;
    String tag; // unique tag for each sub-game
    String downloadPath;
    String downloadFilename;
    String extraArgs;
    int wheelNbr;
    boolean runFromHere = false;

    public SubGame(String tag, String title, String name, String rootPath, int gameType, int image, String detail1, String detail2, int wheelNbr)
    {
        this.tag = tag;
        this.name = name;
        this.rootPath = rootPath;
        this.title = title;
        this.image = image;
        this.detail1 = detail1;
        this.detail2 = detail2;
        this.imagePng = null;
        this.gameType = gameType;
        this.wheelNbr = wheelNbr;
    }

    static public SubGame addGame(ArrayList<SubGame> availableSubGames, String rootPath1, String rootPath2, String tag, String subDir, int gameType,
                                  int weaponWheel, String[] files, int defaultIconRes, String title, String installDetails, String mkdirFilename)
    {
        String fullPath1 = rootPath1 + "/" + subDir;
        String fullPath2 = rootPath2 != null ? rootPath2 + "/" + subDir : null;

        // Always create the directories
        Utils.mkdirs(AppInfo.getContext(), fullPath1, mkdirFilename);

        if (fullPath2 != null)
            Utils.mkdirs(AppInfo.getContext(), fullPath2, mkdirFilename);

        String inPath = Utils.checkFilesInPaths(rootPath1, rootPath2, files);

        if (inPath != null)
        {
            String pathInfo = inPath + "/" + subDir;
            String fileInfo = Utils.filesInfoString(pathInfo, null, 3);
            SubGame subGame = new SubGame(tag, title, subDir, inPath, gameType, defaultIconRes, pathInfo, fileInfo, weaponWheel);
            availableSubGames.add(subGame);
            return subGame;
        }
        else
        {
            String pathText = fullPath1;

            if (fullPath2 != null)
            {
                pathText += "\nOR\n" + fullPath2;
            }

            SubGame subGame = new SubGame(null,
                                          title + " not yet installed",
                                          null,
                                          fullPath1,
                                          gameType,
                                          R.drawable.questionmark,
                                          installDetails,
                                          pathText,
                                          weaponWheel);

            if (!AppSettings.getBoolOption(AppInfo.getContext(), "hide_install_hints", false))
            {
                availableSubGames.add(subGame);
            }
            return subGame;
        }
    }

    public String imageCacheFilename()
    {
        return AppInfo.getFilesDir() + "/imagecache/" + tag + ".png";
    }

    private void replaceImageIfPresent(String imageFilename)
    {
        FileSAF iconFile = new FileSAF(imageFilename);
        File cacheFile = new File(imageCacheFilename());

        // Check if icon.png exists
        if (iconFile.exists())
        {
            boolean validImage = true;
            // Check if not already copied over
            if (iconFile.length() != cacheFile.length())
            {
                new File(imageCacheFilename()).getParentFile().mkdirs();
                try
                {
                    Utils.copyFile(iconFile.getInputStream(), new FileOutputStream(cacheFile));
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    validImage = false;
                }
            }

            if (validImage)
                setImagePng(imageCacheFilename());
        }
    }

    public void load(Context ctx)
    {
        if (tag != null)
        {
            String title = AppSettings.getStringOption(ctx, tag + "title", null);
            if (title != null)
                setTitle(title);

            String args = AppSettings.getStringOption(ctx, tag + "extraArgs", null);
            if (args != null)
                setExtraArgs(args);

            String imageOverride = AppSettings.getStringOption(ctx, tag + "imageOverride", null);

            if (imageOverride != null)
            {
                replaceImageIfPresent(imageOverride);
            }
            else // Else check for icon.png in the folder
            {
                // Check for icon.png file
                if (rootPath != null && name != null)
                {
                    String icon = rootPath + "/" + name + "/icon.png";
                    replaceImageIfPresent(icon);
                }
            }

            int weaponWheelNbr = AppSettings.getIntOption(ctx, tag + "wheel_nbr", -1);
            if (weaponWheelNbr != -1)
                setWheelNbr(weaponWheelNbr);

            boolean runFromHereSaved = AppSettings.getBoolOption(ctx, tag + "run_from_here", runFromHere);
            setRunFromHere(runFromHereSaved);
        }
    }

    public void save(Context ctx)
    {
        if (tag != null)
        {
            AppSettings.setStringOption(ctx, tag + "title", title);
            AppSettings.setStringOption(ctx, tag + "extraArgs", extraArgs);
            AppSettings.setBoolOption(ctx, tag + "run_from_here", runFromHere);
        }
    }

    public String getExtraArgs()
    {
        if (extraArgs == null)
            return "";
        else
            return extraArgs;
    }

    public void setExtraArgs(String extraArgs)
    {
        this.extraArgs = extraArgs;
    }

    public void setDownloadInfo(String path, String filename)
    {
        downloadPath = path;
        downloadFilename = filename;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getRootPath()
    {
        return rootPath;
    }

    public void setRootPath(String rootPath)
    {
        this.rootPath = rootPath;
    }

    public String getDownloadPath()
    {
        return downloadPath;
    }

    public String getDownloadFilename()
    {
        return downloadFilename;
    }

    public String getImagePng()
    {
        return imagePng;
    }

    public void setImagePng(String imagePng)
    {
        this.imagePng = imagePng;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public int getImage()
    {
        return image;
    }

    public void setImage(int image)
    {
        this.image = image;
    }

    public String getDetail1()
    {
        return detail1;
    }

    public void setDetail1(String detail1)
    {
        this.detail1 = detail1;
    }

    public String getDetail2()
    {
        return detail2;
    }

    public void setDetail2(String detail2)
    {
        this.detail2 = detail2;
    }

    public int getGameType()
    {
        return gameType;
    }

    public void setGameType(int gameType)
    {
        this.gameType = gameType;
    }

    public int getWheelNbr()
    {
        return wheelNbr;
    }

    public void setWheelNbr(int wheelNbr)
    {
        this.wheelNbr = wheelNbr;
    }

    public boolean isRunFromHere()
    {
        return runFromHere;
    }

    public void setRunFromHere(boolean runFromHere)
    {
        this.runFromHere = runFromHere;
    }

    public String getFullPath()
    {
        return rootPath + "/" + name;
    }

    public String getTag()
    {
        return tag;
    }

    public void edit(final Activity act, final DialogCallback callback)
    {
        if (tag != null)
        {
            Dialog dialog = new Dialog(act);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_subgame_options);
            dialog.setCancelable(true);

            final EditText title = dialog.findViewById(R.id.subgame_title_edittext);
            title.setText(getTitle());

            final EditText args = dialog.findViewById(R.id.subgame_args_edittext);
            args.setText(getExtraArgs());

            final Button imageChoose = dialog.findViewById(R.id.subgame_image_choose_button);
            final TextView imagePath = dialog.findViewById(R.id.subgame_image_path);
            final CheckBox runFromHere = dialog.findViewById(R.id.run_from_here_checkBox);
            final TextView dirTextView = dialog.findViewById(R.id.run_dir_textView);

            String imageOverride = AppSettings.getStringOption(act, tag + "imageOverride", null);

            if (imageOverride != null)
                imagePath.setText(imageOverride);
            else
                imagePath.setText("[default]");

            imageChoose.setOnClickListener(v ->
                                           {

                                               FileSelectDialog.FileSelectCallback callback1 = filesArray ->
                                               {
                                                   String imageOverride1;

                                                   if (filesArray == null || filesArray.size() == 0)
                                                   {
                                                       imageOverride1 = null;
                                                   }
                                                   else
                                                   {
                                                       imageOverride1 = filesArray.get(0);
                                                   }

                                                   if (imageOverride1 != null)
                                                       imagePath.setText(imageOverride1);
                                                   else
                                                       imagePath.setText("[default]");

                                                   AppSettings.setStringOption(act, tag + "imageOverride", imageOverride1);
                                               };

                                               new FileSelectDialog(act, callback1, rootPath + "/" + name, new String[]{".png", ".jpg"}, false);
                                           });

            Spinner wheelSpinner = dialog.findViewById(R.id.weapon_wheel_spinner);
            List<String> list = new ArrayList<String>();
            list.add("4");
            list.add("8");
            list.add("10");

            ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(act, android.R.layout.simple_spinner_item, list);
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            wheelSpinner.setAdapter(dataAdapter);
            wheelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                {
                    setWheelNbr(Integer.parseInt(list.get(position)));
                    AppSettings.setIntOption(act, tag + "wheel_nbr", getWheelNbr());
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent)
                {
                }
            });

            int pos = 0;
            for (int n = 0; n < list.size(); n++)
            {
                if (getWheelNbr() == Integer.parseInt(list.get(n)))
                {
                    pos = n;
                    break;
                }
            }

            wheelSpinner.setSelection(pos);

            runFromHere.setOnCheckedChangeListener((buttonView, isChecked) ->
                                                   {
                                                       if (isChecked)
                                                       {
                                                           dirTextView.setText("Run directory: " + AppInfo.replaceRootPaths(getFullPath()));
                                                           dirTextView.setVisibility(View.VISIBLE);
                                                       }
                                                       else
                                                       {
                                                           dirTextView.setVisibility(View.GONE);
                                                       }
                                                   });

            runFromHere.setChecked(isRunFromHere());

            dialog.setOnDismissListener(dialog1 ->
                                        {
                                            setTitle(title.getText().toString());
                                            setExtraArgs(args.getText().toString());
                                            setRunFromHere(runFromHere.isChecked());

                                            save(act);

                                            if (callback != null)
                                            {
                                                callback.dismiss();
                                            }
                                        });


            // Delete the image cache
            new File(imageCacheFilename()).delete();
            Glide.get(act).clearMemory();

            dialog.show();
        }
    }

    public interface DialogCallback
    {
        void dismiss();
    }
}
