package com.sentaroh.android.TextFileBrowser;

/*
The MIT License (MIT)
Copyright (c) 2013 Sentaroh

Permission is hereby granted, free of charge, to any person obtaining a copy of 
this software and associated documentation files (the "Software"), to deal 
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to 
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or 
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE 
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

*/

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.core.util.LogWriter;

import com.sentaroh.android.TextFileBrowser.Log.LogUtil;
import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.SafManager3;
import com.sentaroh.android.Utilities3.StringUtil;
import com.sentaroh.android.Utilities3.ThemeColorList;
import com.sentaroh.android.Utilities3.ThemeUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.LoggerWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import static com.sentaroh.android.TextFileBrowser.Constants.*;

public class GlobalParameters {
    public boolean debugEnabled = false;

    private static Logger log = LoggerFactory.getLogger(GlobalParameters.class);

    public ArrayList<ViewedFileListItem> viewedFileList = new ArrayList<ViewedFileListItem>();
    public SafFile3 currentViewedFile = null;

    public boolean disableFileSelector=false;

    public NotificationCommonParms commonNotification = new NotificationCommonParms();

    public int settingLineBreak = CustomTextView.LINE_BREAK_NO_WORD_WRAP;
    public int settingBrowseMode =
            FileViewerAdapter.TEXT_BROWSER_BROWSE_MODE_CHAR;
    public String settingFontFamily = "SANS",
            settingFontStyle = "NORMAL", settingFontSize = "Medium",
            settingIndexCache = "2";
    public boolean settingShowLineno = true;
    public boolean settingShowAllFileAsText = false;
    public String settingMimeTypeToOpenAsText =TEXT_MODE_MIME_TYPE;

    public boolean settingExitCleanly = false;
//    public boolean settingAlwayDeterminCharCode = true;
    public int settingTabStop = 4;
//    public String settingEncodeName = "";
    public String settingDefaultEncodeName = "";
    public int settingBufferCharIndexSize = 8;
    public int settingBufferHexIndexSize = 2;
    public int settingBufferPoolSize = 128;

    public boolean settingUseLightTheme = false;

//    public boolean settingCorrectApi21TextSizeCalculation = false;

    public boolean settingConfirmExit = true;

    public boolean settingShowDivederLine = true;

    public boolean settingUseNoWordWrapTextView=false;

    public int screenTheme = -1;

    public ThemeColorList themeColorList = null;

    public String settingTextAreaBackgroundColor= TEXT_AREA_BACKGROUND_COLOR_DARK;

    public SafManager3 safMgr=null;

    public void initThemeColorList(Activity c) {
        themeColorList= ThemeUtil.getThemeColorList(c);
        applyTextAreaBackGroundColor();
    }

    public void setTextAreaBackGroundColor(Context c, String bgc) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        prefs.edit().putString(c.getString(R.string.settings_tb_text_area_background_color), bgc).commit();
    }

    public void applyTextAreaBackGroundColor() {
        try {
            themeColorList.text_background_color=(int)Long.parseLong(settingTextAreaBackgroundColor.substring(1), 16);
//            log.info("bgc="+String.format("%8h",themeColorList.text_background_color));
        } catch(Exception e) {
            e.printStackTrace();
        }

    }

    public void setLogEnabled(boolean enabled) {
        log.setLogOption(enabled, true, true, enabled, true);
    }

    public void init(Activity a) {
        if (Build.VERSION.SDK_INT>=30) disableFileSelector=true;
        initSettingParms(a.getApplicationContext());
        loadSettingParms(a.getApplicationContext());
        initLogger(a.getApplicationContext());
        initThemeColorList(a);
        safMgr=new SafManager3(a.getApplicationContext());
    }

    public void initSettingParms(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        if (!prefs.contains(c.getString(R.string.settings_tb_confirm_exit))) {
            prefs.edit().putBoolean(c.getString(R.string.settings_tb_confirm_exit), true).commit();
        }
        if (!prefs.contains(c.getString(R.string.settings_tb_show_divider_line))) {
            prefs.edit().putBoolean(c.getString(R.string.settings_tb_show_divider_line), true).commit();
        }

        if (!prefs.contains(c.getString(R.string.settings_tb_text_area_background_color))) {
            prefs.edit().putString(c.getString(R.string.settings_tb_text_area_background_color), TEXT_AREA_BACKGROUND_COLOR_DARK).commit();
        }
        if (!prefs.contains(c.getString(R.string.settings_tb_default_encode_name))) {
            prefs.edit().putString(c.getString(R.string.settings_tb_default_encode_name), ENCODE_NAME_UTF8).commit();
        }

        if (!prefs.contains(c.getString(R.string.settings_tb_show_all_file_as_text))) {
            prefs.edit().putBoolean(c.getString(R.string.settings_tb_show_all_file_as_text), false).commit();
        }

        if (!prefs.contains(c.getString(R.string.settings_tb_mime_type_to_open_text_mode))) {
            prefs.edit().putString(c.getString(R.string.settings_tb_mime_type_to_open_text_mode), TEXT_MODE_MIME_TYPE).commit();
        }

        settingFontFamily=prefs.getString(c.getString(R.string.settings_tb_font_family), "0");
        if (settingFontFamily.equals("0")) {
            prefs.edit().putString(c.getString(R.string.settings_tb_line_break), "1").commit();
            prefs.edit().putString(c.getString(R.string.settings_tb_font_family), DEFAULT_FONT_FAMILY).commit();
            settingFontFamily=DEFAULT_FONT_FAMILY;
            prefs.edit().putString(c.getString(R.string.settings_tb_font_style), DEFAULT_FONT_STYLE).commit();
            prefs.edit().putString(c.getString(R.string.settings_tb_font_size), DEFAULT_FONT_SIZE).commit();
            prefs.edit().putString(c.getString(R.string.settings_tb_index_cache),
                    Constants.INDEX_CACHE_UP_TO_50MB).commit();
            prefs.edit().putBoolean(c.getString(R.string.settings_tb_show_lineno),true).commit();
            prefs.edit().putBoolean(c.getString(R.string.settings_tb_debug_enable),false).commit();
            prefs.edit().putString(c.getString(R.string.settings_tb_tab_stop),DEFAULT_TAB_STOP).commit();
        }

        if (!prefs.contains(c.getString(R.string.settings_tb_use_no_word_wrap_text_view))) {
            prefs.edit().putBoolean(c.getString(R.string.settings_tb_use_no_word_wrap_text_view), true).commit();
        }
    }
    
    public void loadSettingParms(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        settingFontFamily=prefs.getString(c.getString(R.string.settings_tb_font_family), "0");

        settingDefaultEncodeName=prefs.getString(c.getString(R.string.settings_tb_default_encode_name), ENCODE_NAME_UTF8);

        settingFontStyle=prefs.getString(c.getString(R.string.settings_tb_font_style), DEFAULT_FONT_STYLE);
        settingLineBreak=Integer.valueOf(prefs.getString(c.getString(R.string.settings_tb_line_break), "1"));
        settingFontSize=prefs.getString(c.getString(R.string.settings_tb_font_size), DEFAULT_FONT_SIZE);
        settingIndexCache=prefs.getString(c.getString(R.string.settings_tb_index_cache), Constants.INDEX_CACHE_UP_TO_50MB);
        settingShowLineno=prefs.getBoolean(c.getString(R.string.settings_tb_show_lineno),true);
        settingTabStop=Integer.valueOf(prefs.getString(c.getString(R.string.settings_tb_tab_stop),DEFAULT_TAB_STOP));
        settingBufferCharIndexSize=Integer.valueOf(prefs.getString(c.getString(R.string.settings_tb_buffer_char_index_size),"8"));
        settingBufferHexIndexSize=Integer.valueOf(prefs.getString(c.getString(R.string.settings_tb_buffer_hex_index_size),"2"));
        settingBufferPoolSize=Integer.valueOf(prefs.getString(c.getString(R.string.settings_tb_buffer_pool_size),"32"));
        if (settingBufferPoolSize<128) settingBufferPoolSize=256;
        debugEnabled=prefs.getBoolean(c.getString(R.string.settings_tb_debug_enable),false);
        setLogEnabled(debugEnabled);

        settingExitCleanly=prefs.getBoolean(c.getString(R.string.settings_tb_exit_cleanly),false);

        settingConfirmExit=prefs.getBoolean(c.getString(R.string.settings_tb_confirm_exit), true);

        settingShowDivederLine=prefs.getBoolean(c.getString(R.string.settings_tb_show_divider_line), true);


        settingUseLightTheme=prefs.getBoolean(c.getString(R.string.settings_tb_use_light_theme),false);
        if (settingUseLightTheme) screenTheme=R.style.MainLight;
        else screenTheme=R.style.Main;

        settingTextAreaBackgroundColor=prefs.getString(c.getString(R.string.settings_tb_text_area_background_color), TEXT_AREA_BACKGROUND_COLOR_DARK);

        settingShowAllFileAsText=prefs.getBoolean(c.getString(R.string.settings_tb_show_all_file_as_text), false);
        settingMimeTypeToOpenAsText=prefs.getString(c.getString(R.string.settings_tb_mime_type_to_open_text_mode), TEXT_MODE_MIME_TYPE);

        settingUseNoWordWrapTextView=prefs.getBoolean(c.getString(R.string.settings_tb_use_no_word_wrap_text_view), true);
//        if (debugEnabled) log.debug("Init Setting parms: "+
//                "Line break="+ settingLineBreak+
//                ", Font family="+ settingFontFamily+
//                ", Font style="+ settingFontStyle+
//                ", Font size="+ settingFontSize+
//                ", Tab stop="+ settingTabStop+
//                ", use light theme="+ settingUseLightTheme);

    }

    public boolean isDebuggable(Context c) {
        boolean result=false;
        PackageManager manager = c.getPackageManager();
        ApplicationInfo appInfo = null;
        try {
            appInfo = manager.getApplicationInfo(c.getPackageName(), 0);
        } catch (Exception e) {
            result=false;
        }
        if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE)
            result=true;
//        Log.v("","debuggable="+result);
        return result;
    };

    private void initLogger(Context c) {
        final LogUtil log_util = new LogUtil(c, "SLF4J");

        Slf4jLogWriter log_writer=new Slf4jLogWriter(log_util);
        log.setWriter(log_writer);
    }

    class Slf4jLogWriter extends LoggerWriter {
        private LogUtil mLu =null;
        public Slf4jLogWriter(LogUtil lu) {
            mLu =lu;
        }
        @Override
        public void write(String msg) {
            mLu.addDebugMsg(1,"I", msg);
        }
    }

}
