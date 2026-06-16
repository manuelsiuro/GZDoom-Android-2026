package com.opentouchgaming.androidcore.ui.widgets;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.opentouchgaming.androidcore.AppInfo;
import com.opentouchgaming.androidcore.AppSettings;
import com.opentouchgaming.androidcore.databinding.WidgetViewResolutionSelectBinding;

import java.util.ArrayList;
import java.util.List;

public class ResolutionOptionsWidget
{
    static List<ResolutionOptions> resolutionListDefault = new ArrayList<>();

    static
    {
        resolutionListDefault.add(new ResolutionOptions("Screen (100%)", ResolutionType.FULL, "$W", "$H"));
        resolutionListDefault.add(new ResolutionOptions("Screen / 2 (50%)", ResolutionType.FULL_0_5, "$W2", "$H2"));
        resolutionListDefault.add(new ResolutionOptions("Screen / 3 (33%)", ResolutionType.FULL_0_3, "$W3", "$H3"));
        resolutionListDefault.add(new ResolutionOptions("Screen / 4 (25%)", ResolutionType.FULL_0_25, "$W4", "$H4"));
        resolutionListDefault.add(new ResolutionOptions("568 x 320 (16:9)", ResolutionType.SET, "568", "320"));
        resolutionListDefault.add(new ResolutionOptions("850 x 480 (16:9)", ResolutionType.SET, "850", "480"));
        resolutionListDefault.add(new ResolutionOptions("769 x 480 (16:10)", ResolutionType.SET, "769", "480"));
        resolutionListDefault.add(new ResolutionOptions("1280 x 675 (16:9)", ResolutionType.SET, "1280", "675"));
        resolutionListDefault.add(new ResolutionOptions("Custom", ResolutionType.CUSTOM, "0", "0"));
    }

    List<ResolutionOptions> resolutionList;

    WidgetViewResolutionSelectBinding binding;

    String prefix;
    int def;

    boolean allowMaintainAspect = true;

    public ResolutionOptionsWidget(Context context, View view, String prefix, List<ResolutionOptions> resolutions)
    {
        Init(context, view, prefix, resolutions, 0);
    }

    public ResolutionOptionsWidget(Context context, View view, String prefix)
    {
        Init(context, view, prefix, resolutionListDefault, 0);
    }

    public ResolutionOptionsWidget(Context context, View view, String prefix, List<ResolutionOptions> resolutions, int def)
    {
        Init(context, view, prefix, resolutions, def);
    }

    public ResolutionOptionsWidget(Context context, View view, String prefix, int def)
    {
        Init(context, view, prefix, resolutionListDefault, def);
    }

    public static ResolutionOptions getResOption(String prefix, List<ResolutionOptions> resolutionList)
    {
        return getResOption(prefix, resolutionList, 0);
    }

    public static ResolutionOptions getResOption(String prefix, List<ResolutionOptions> resolutionList, int def)
    {
        int selected = AppSettings.getIntOption(AppInfo.getContext(), prefix + "_resolution_spinner", def);
        ResolutionOptions option = resolutionList.get(selected);

        option.position = selected; // Save this if we want to know which spinner position it was

        option.maintainAspect = AppSettings.getBoolOption(AppInfo.getContext(), prefix + "_resolution_maint_aspect", false);

        if (option.type == ResolutionType.CUSTOM)
        {
            option.w = AppSettings.getStringOption(AppInfo.getContext(), prefix + "_resolution_cust_w", "640");
            option.h = AppSettings.getStringOption(AppInfo.getContext(), prefix + "_resolution_cust_h", "480");
        }
        return option;
    }

    public static ResolutionOptions getResOption(String prefix)
    {
        return getResOption(prefix, resolutionListDefault, 0);
    }

    public static ResolutionOptions getResOption(String prefix, int def)
    {
        return getResOption(prefix, resolutionListDefault, def);
    }

    public void Init(Context context, View view, String prefix, List<ResolutionOptions> resolutions, int def)
    {
        this.prefix = prefix;
        this.def = def;
        this.resolutionList = resolutions;

        binding = WidgetViewResolutionSelectBinding.bind(view);

        ArrayAdapter<ResolutionOptions> dataAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, resolutionList);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.resolutionSpinner.setAdapter(dataAdapter);
        binding.resolutionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                updateSelected(context, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });

        // Stupid hack to remove the old TextWatcher if already added
        Object oldTw = binding.resolutionWidthEditText.getTag();
        if (oldTw != null)
        {
            binding.resolutionWidthEditText.removeTextChangedListener((TextWatcher) oldTw);
            binding.resolutionHeightEditText.removeTextChangedListener((TextWatcher) oldTw);
        }

        TextWatcher tw = new TextWatcher()
        {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                if (binding.resolutionWidthEditText.isEnabled())
                {
                    AppSettings.setStringOption(AppInfo.getContext(), prefix + "_resolution_cust_w", binding.resolutionWidthEditText.getText().toString());
                    AppSettings.setStringOption(AppInfo.getContext(), prefix + "_resolution_cust_h", binding.resolutionHeightEditText.getText().toString());
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        };

        binding.resolutionWidthEditText.addTextChangedListener(tw);
        binding.resolutionHeightEditText.addTextChangedListener(tw);

        binding.maintainAspect.setOnCheckedChangeListener((compoundButton, b) ->
                                                          {
                                                              AppSettings.setBoolOption(AppInfo.getContext(), prefix + "_resolution_maint_aspect", b);
                                                          });

        // Save to tag so it can removed later if necessary
        binding.resolutionWidthEditText.setTag(tw);

        int selected = AppSettings.getIntOption(context, prefix + "_resolution_spinner", def);
        if (selected < resolutionList.size())
        {
            binding.resolutionSpinner.setSelection(selected);
            //updateSelected(context, selected);
        }
        else
        {
            binding.resolutionSpinner.setSelection(def);
            //updateSelected(context, def);
        }
    }

    public ResolutionOptionsWidget disableMaintainAspect()
    {
        allowMaintainAspect = false;
        return this;
    }

    private void updateSelected(Context context, int selected)
    {
        AppSettings.setIntOption(context, prefix + "_resolution_spinner", selected);
        ResolutionOptions option = getResOption(prefix, resolutionList, def);

        if (option.type == ResolutionType.CUSTOM)
        {
            binding.resolutionWidthEditText.setEnabled(true);
            binding.resolutionHeightEditText.setEnabled(true);
        }
        else
        {
            binding.resolutionWidthEditText.setEnabled(false);
            binding.resolutionHeightEditText.setEnabled(false);
        }

        binding.resolutionWidthEditText.setText(option.w);
        binding.resolutionHeightEditText.setText(option.h);

        if (allowMaintainAspect && ((option.type == ResolutionType.CUSTOM) || (option.type == ResolutionType.SET)))
        {
            binding.aspectLayout.setVisibility(View.VISIBLE);
            binding.maintainAspect.setChecked(option.maintainAspect);
        }
        else
        {
            binding.aspectLayout.setVisibility(View.GONE);
        }
    }

    public void setEnabled(boolean enabled)
    {
        if (enabled)
        {
            binding.resolutionSpinner.setEnabled(true);

            ResolutionOptions option = getResOption(prefix, resolutionList, def);
            if (option.type == ResolutionType.CUSTOM)
            {
                binding.resolutionWidthEditText.setEnabled(true);
                binding.resolutionHeightEditText.setEnabled(true);
            }
            else
            {
                binding.resolutionWidthEditText.setEnabled(false);
                binding.resolutionHeightEditText.setEnabled(false);
            }
        }
        else
        {
            binding.resolutionSpinner.setEnabled(false);
            binding.resolutionWidthEditText.setEnabled(false);
            binding.resolutionHeightEditText.setEnabled(false);
        }
    }

    public void setHidden(boolean hidden)
    {
        if (hidden)
        {
            binding.resolutionLayoutTop.setVisibility(View.GONE);
        }
        else
        {
            binding.resolutionLayoutTop.setVisibility(View.VISIBLE);
        }
    }

    public void save()
    {
        if (getResOption(prefix, def).type == ResolutionType.CUSTOM)
        {
            AppSettings.setStringOption(AppInfo.getContext(), prefix + "_resolution_cust_w", binding.resolutionWidthEditText.getText().toString());
            AppSettings.setStringOption(AppInfo.getContext(), prefix + "_resolution_cust_h", binding.resolutionHeightEditText.getText().toString());
        }
    }

    public enum ResolutionType
    {
        FULL, FULL_0_5, FULL_0_3, FULL_0_25, SET, CUSTOM
    }

    public static class ResolutionOptions
    {
        public String w;
        public String h;
        public int position; // Spinner position
        public boolean maintainAspect;
        String title;
        ResolutionType type;

        public ResolutionOptions(String title, ResolutionType t, String w, String h)
        {
            this.title = title;
            this.type = t;
            this.w = w;
            this.h = h;
        }

        //Override
        public String toString()
        {
            return title;
        }
    }

}
