package com.sentaroh.android.TextFileBrowser;

/*
The MIT License (MIT)
Copyright (c) 2020 Sentaroh

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
import android.app.usage.UsageStatsManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Spannable;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.fragment.app.FragmentManager;

import com.sentaroh.android.TextFileBrowser.Log.LogUtil;
import com.sentaroh.android.Utilities3.Base64Compat;
import com.sentaroh.android.Utilities3.Dialog.CommonDialog;
import com.sentaroh.android.Utilities3.Dialog.MessageDialogFragment;
import com.sentaroh.android.Utilities3.EncryptUtilV3;
import com.sentaroh.android.Utilities3.MiscUtil;
import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.ShellCommandUtil;
import com.sentaroh.android.Utilities3.StringUtil;
import com.sentaroh.android.Utilities3.SystemInfo;

import org.markdownj.MarkdownProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Locale;

import static android.content.Context.USAGE_STATS_SERVICE;
import static com.sentaroh.android.Utilities3.SafFile3.SAF_FILE_PRIMARY_UUID;

public final class CommonUtilities {
    private static Logger log= LoggerFactory.getLogger(CommonUtilities.class);
    private Context mContext = null;
    private LogUtil mLog = null;
    private GlobalParameters mGp = null;
    private String mLogIdent = "";

    private FragmentManager mFragMgr=null;

    public CommonUtilities(Context c, String li, GlobalParameters gp, FragmentManager fm) {
        mContext = c;// ContextはApplicationContext
        mLog = new LogUtil(c, li);
        mLogIdent = li;
        mGp = gp;
        mFragMgr=fm;
    }

    public int getLogLevel() {
        return mLog.getLogLevel();
    }

    public LogUtil getLogUtil() {
        return mLog;
    }

    static public void setViewEnabled(Activity a, View v, boolean enabled) {
//        Thread.dumpStack();
        GlobalParameters gp=GlobalWorkArea.getGlobalParameters(a);
        CommonDialog.setViewEnabled(gp.themeColorList.theme_is_light, v, enabled);
    }

    final public SharedPreferences getSharedPreference() {
        return getSharedPreference(mContext);
    }

    final static public SharedPreferences getSharedPreference(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c);
    }

    public static void setSpinnerBackground(Context c, Spinner spinner, boolean theme_is_light) {
        if (theme_is_light) spinner.setBackground(c.getDrawable(R.drawable.spinner_color_background_light));
        else spinner.setBackground(c.getDrawable(R.drawable.spinner_color_background));
    }

    public void showCommonDialog(final boolean negative, String type, String title, String msgtext, Object listener) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, type, title, msgtext);
        cdf.showDialog(mFragMgr,cdf, listener);
    };
    public void showCommonDialogInfo(final boolean negative, String title, String msgtext, Object listener) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, MessageDialogFragment.CATEGORY_INFO, title, msgtext);
        cdf.showDialog(mFragMgr,cdf, listener);
    };
    public void showCommonDialogWarn(final boolean negative, String title, String msgtext, Object listener) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, MessageDialogFragment.CATEGORY_WARN, title, msgtext);
        cdf.showDialog(mFragMgr,cdf, listener);
    };
    public void showCommonDialogError(final boolean negative, String title, String msgtext, Object listener) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, MessageDialogFragment.CATEGORY_ERROR, title, msgtext);
        cdf.showDialog(mFragMgr,cdf, listener);
    };
    public void showCommonDialogDanger(final boolean negative, String title, String msgtext, Object listener) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, MessageDialogFragment.CATEGORY_DANGER, title, msgtext);
        cdf.showDialog(mFragMgr,cdf, listener);
    };

    static public void showCommonDialog(FragmentManager fm, final boolean negative, String type, String title, String msgtext, Object listener) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, type, title, msgtext);
        cdf.showDialog(fm, cdf, listener);
    };

    public void showCommonDialog(final boolean negative, String type, String title, Spannable msgtext, Object listener) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, type, title, "");
        cdf.setMessageText(msgtext);
        cdf.showDialog(mFragMgr,cdf,listener);
    };

    public void showCommonDialog(final boolean negative, String type, String title, String msgtext, int text_color, Object listener) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, type, title, msgtext);
        cdf.setTextColor(text_color);
        cdf.showDialog(mFragMgr,cdf,listener);
    };

    public void showCommonDialog(final boolean negative, String type, String title, String msgtext, String ok_text, String cancel_text, Object listener) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, type, title, msgtext, ok_text, cancel_text);
        cdf.showDialog(mFragMgr,cdf,listener);
    };

    public void showCommonDialogWarn(final boolean negative, String title, String msgtext, String ok_text, String cancel_text, Object listener) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, MessageDialogFragment.CATEGORY_WARN, title, msgtext, ok_text, cancel_text);
        cdf.showDialog(mFragMgr,cdf,listener);
    };

    public void showCommonDialogDanger(boolean negative, String title, String msgtext, String ok_text, String cancel_text, Object listener) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, MessageDialogFragment.CATEGORY_DANGER, title, msgtext, ok_text, cancel_text);
        cdf.showDialog(mFragMgr,cdf,listener);
    };

    public static String convertMakdownToHtml(Context c, String mark_down_fp) {
//        long b_time=System.currentTimeMillis();
        String html ="";
        try {
            InputStream is = c.getAssets().open(mark_down_fp);
            MarkdownProcessor processor = new MarkdownProcessor();
            html=processor.markdown(true, is);
        } catch(Exception e) {
            log.error("MarkDown conversion error.", e);
            e.printStackTrace();
        }
//        Log.v(APPLICATION_TAG, "convertMakdownToHtml elapsed time="+(System.currentTimeMillis()-b_time));
        return html;
    }

    public String getStringWithLangCode(Activity c, String lang_code, int res_id) {
        Configuration config = new Configuration(c.getResources().getConfiguration());
        config.setLocale(new Locale(lang_code));
        String result = c.createConfigurationContext(config).getText(res_id).toString();
        return result;
    }

    public String getStringWithLangCode(Activity c, String lang_code, int res_id, Object... value) {
        String text = getStringWithLangCode(c, lang_code, res_id);
        String result=text;
        if (value!=null && value.length>0) result=String.format(text, value);
        return result;
    }

    static public String getRootFilePath(String fp) {
        String reform_fp=StringUtil.removeRedundantDirectorySeparator(fp);
        if (reform_fp.startsWith("/storage/emulated/0")) return "/storage/emulated/0";
        else {
            String[] fp_parts=reform_fp.startsWith("/")?reform_fp.substring(1).split("/"):reform_fp.split("/");
            String rt_fp="/"+fp_parts[0]+"/"+fp_parts[1];
            return rt_fp;
        }
    }

    public static boolean isAllFileAccessAvailable() {
        return SafFile3.isAllFileAccessAvailable();
    }

    final public void setLogId(String li) {
        mLog.setLogId(li);
    }

    final static public String getExecutedMethodName() {
        String name = Thread.currentThread().getStackTrace()[3].getMethodName();
        return name;
    }

    final public void resetLogReceiver() {
        mLog.resetLogReceiver();
    }

    final public void flushLog() {
        mLog.flushLog();
    }

    final public void rotateLogFile() {
        mLog.rotateLogFile();
    }

    final public void deleteLogFile() {
        mLog.deleteLogFile();
    }

    final public void addLogMsg(String cat, String... msg) {
        mLog.addLogMsg(cat, msg);
    }

    final public void addDebugMsg(int lvl, String cat, String... msg) {
        mLog.addDebugMsg(lvl, cat, msg);
    }

    final public boolean isLogFileExists() {
        boolean result = false;
        result = mLog.isLogFileExists();
        if (mLog.getLogLevel() >= 3) addDebugMsg(3, "I", "Log file exists=" + result);
        return result;
    }

    final public String getLogFilePath() {
        return mLog.getLogFilePath();
    }

    public boolean isDebuggable() {
        PackageManager manager = mContext.getPackageManager();
        ApplicationInfo appInfo = null;
        try {
            appInfo = manager.getApplicationInfo(mContext.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            return false;
        }
        if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE)
            return true;
        return false;
    }

    static public void setCheckedTextViewListener(final CheckedTextView ctv) {
        ctv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ctv.toggle();
            }
        });
    }

    static public void setDialogBoxOutline(Context c, LinearLayout ll) {
        setDialogBoxOutline(c, ll, 3, 5);
    }

    static public void setDialogBoxOutline(Context c, LinearLayout ll, int padding_dp, int margin_dp) {
        ll.setBackgroundResource(R.drawable.dialog_box_outline);
        int padding=(int)toPixel(c.getResources(),padding_dp);
        ll.setPadding(padding, padding, padding, padding);

        ViewGroup.LayoutParams lp = ll.getLayoutParams();
        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams)lp;
        int margin=(int)toPixel(c.getResources(), margin_dp);
        mlp.setMargins(margin, mlp.topMargin, margin, mlp.bottomMargin);
        ll.setLayoutParams(mlp);
    }

    final static public float toPixel(Resources res, int dip) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, res.getDisplayMetrics());
        return px;
    }

    static public boolean isExfatFileSystem(String uuid) {
        boolean result=false;
        String fs=getExternalStorageFileSystemName(uuid);
        if (fs.toLowerCase().contains("exfat")) result=true;
        return result;
    }

    static public String getExternalStorageFileSystemName(String uuid) {
        String result="";
        try {
            String resp= ShellCommandUtil.executeShellCommand(new String[]{"/bin/sh", "-c", "mount | grep -e ^/dev.*/mnt/media_rw/"+uuid});
            if (resp!=null && !resp.equals("")) {
                String[] fs_array=resp.split(" ");
                for(int i=0;i<fs_array.length;i++) {
                    if (fs_array[i].equals("type")) {
                        result=fs_array[i+1];
                    }
                }
            }
            log.debug("getExternalStorageFileSystemName result="+result+", uuid="+uuid);
        } catch (Exception e) {
            log.debug("getExternalStorageFileSystemName error="+e.toString()+MiscUtil.getStackTraceString(e));
        }
        return result;
    }

}

