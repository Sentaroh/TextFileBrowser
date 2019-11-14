package com.sentaroh.android.TextFileBrowser;

/*
The MIT License (MIT)
Copyright (c) 2011-2013 Sentaroh

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

import static com.sentaroh.android.TextFileBrowser.Constants.*;

import java.util.List;
import java.util.prefs.PreferenceChangeEvent;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;

import com.sentaroh.android.Utilities2.ThemeUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressLint("NewApi")
public class SettingsActivity extends PreferenceActivity{
    private static Logger log = LoggerFactory.getLogger(SettingsActivity.class);
//	private static Context mContext=null;
//	private static PreferenceFragment mPrefFrag=null;
	
	private static GlobalParameters mGp =null;
	
    /** Called when the activity is first created. */
    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
//        mContext=getApplicationContext();
        mGp =GlobalWorkArea.getGlobalParameters(this);
        log.debug("onCreate entered");
        setTheme(mGp.screenTheme);
        super.onCreate(savedInstanceState);
        
	};

    @Override
    protected boolean isValidFragment(String fragmentName) {
        // 使用できる Fragment か確認する

        return true;
    }

    @Override
    public void onStart(){
        super.onStart();
        log.debug("onStart entered");
    };
 
    @SuppressWarnings("deprecation")
	@Override
    public void onResume(){
        super.onResume();
        log.debug("onResume entered");
		setTitle(R.string.settings_main_title);

    };
 
    @Override
    public void onBuildHeaders(List<Header> target) {
    	log.debug("onBuildHeaders entered");
        loadHeadersFromResource(R.xml.tb_settings_frag, target);
    };

    @Override
    public boolean onIsMultiPane () {
//        mContext=getApplicationContext();
        mGp =GlobalWorkArea.getGlobalParameters(this);
    	log.debug("onIsMultiPane entered");
        return isTablet(getApplicationContext());
    };

    public static boolean isTablet(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        final int pixels = Math.min(metrics.heightPixels, metrics.widthPixels);
        boolean sz_mp=pixels >= 1200;
        int orientation = context.getResources().getConfiguration().orientation;
        boolean sc_or= orientation == Configuration.ORIENTATION_LANDSCAPE;

        return sz_mp||sc_or;
    }

	@Override
	protected void onPause() {  
	    super.onPause();  
	    log.debug("onPause entered");
	};

	@Override
	final public void onStop() {
		super.onStop();
		log.debug("onStop entered");
	};

	@Override
	final public void onDestroy() {
		super.onDestroy();
		log.debug("onDestroy entered");
	};


    static public void setPreferenceEnabled(Activity a, Preference menu_item, boolean enabled) {
        if (ThemeUtil.isLightThemeUsed(a)) {
            menu_item.setEnabled(enabled);
            SpannableString title = new SpannableString(menu_item.getTitle());
            if (enabled) title.setSpan(new ForegroundColorSpan(Color.BLACK), 0, title.length(), 0);
            else title.setSpan(new ForegroundColorSpan(Color.LTGRAY), 0, title.length(), 0);
            menu_item.setTitle(title);
        } else {
            menu_item.setEnabled(enabled);
        }
    }

    public static class SettingsUi extends PreferenceFragment {
        private static Logger log = LoggerFactory.getLogger(SettingsUi.class);
        private SharedPreferences.OnSharedPreferenceChangeListener listenerAfterHc =new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences shared_pref, String key_string) {
                checkSettings(log, shared_pref, key_string);
            }
        };

        private PreferenceFragment mPrefFrag=null;
        private Context mContext=null;
        @Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
        	log.debug("onCreate entered");
            
    		addPreferencesFromResource(R.xml.tb_settings_frag_ui);

            mPrefFrag=this;
    		mContext=getActivity().getApplicationContext();

    		SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);

    		checkSettings(log, shared_pref, getString(R.string.settings_tb_show_lineno));
            checkSettings(log, shared_pref,getString(R.string.settings_tb_default_encode_name));
            checkSettings(log, shared_pref,getString(R.string.settings_tb_show_divider_line));
            checkSettings(log, shared_pref,getString(R.string.settings_tb_line_break));
            checkSettings(log, shared_pref,getString(R.string.settings_tb_tab_stop));

            checkSettings(log, shared_pref,getString(R.string.settings_tb_font_family));
            checkSettings(log, shared_pref,getString(R.string.settings_tb_font_style));
            checkSettings(log, shared_pref,getString(R.string.settings_tb_font_size));

            checkSettings(log, shared_pref,getString(R.string.settings_tb_use_light_theme));
            checkSettings(log, shared_pref,getString(R.string.settings_tb_text_area_background_color));

            checkSettings(log, shared_pref,getString(R.string.settings_tb_show_all_file_as_text));
            checkSettings(log, shared_pref,getString(R.string.settings_tb_mime_type_to_open_text_mode));

        };

        private void checkSettings(Logger log, SharedPreferences shared_pref, String key_string) {
            Preference pref_key=mPrefFrag.findPreference(key_string);
            setPreferenceEnabled(mPrefFrag.getActivity(), pref_key, true);
            String show_value="";
            if (key_string.equals(mContext.getString(R.string.settings_tb_show_lineno))) {
                show_value=""+shared_pref.getBoolean(key_string,false);
            } else if (key_string.equals(mContext.getString(R.string.settings_tb_default_encode_name))) {
                String enc_name=shared_pref.getString(key_string,"");
                String[] sa=mContext.getResources().getStringArray(R.array.settings_tb_default_encode_name_list_entries);
                if (enc_name.equals("")) enc_name=sa[17];
                pref_key.setSummary(enc_name);
                show_value=enc_name;
            } else if (key_string.equals(mContext.getString(R.string.settings_tb_line_break))) {
                String[] sa=mContext.getResources().getStringArray(R.array.settings_tb_line_break_list_entries);
                if (shared_pref.getString(key_string, "1").equals("0")) {
                    pref_key.setSummary(sa[0]);
                } else if (shared_pref.getString(key_string, "1").equals("1")) {
                    pref_key.setSummary(sa[1]);
                } else if (shared_pref.getString(key_string, "1").equals("2")) {
                    pref_key.setSummary(sa[2]);
                }
                show_value=pref_key.getSummary().toString();
            } else if (key_string.equals(mContext.getString(R.string.settings_tb_show_divider_line))) {
                show_value=""+shared_pref.getBoolean(key_string,false);
            } else if (key_string.equals(mContext.getString(R.string.settings_tb_tab_stop))) {
                pref_key.setSummary(shared_pref.getString(key_string, DEFAULT_TAB_STOP));
                show_value=pref_key.getSummary().toString();
            } else if (key_string.equals(mContext.getString(R.string.settings_tb_font_family))) {
                String ff=shared_pref.getString(key_string, DEFAULT_FONT_FAMILY);
                pref_key.setSummary(ff);
                show_value=ff;
            } else if (key_string.equals(mContext.getString(R.string.settings_tb_font_style))) {
                pref_key.setSummary(shared_pref.getString(key_string, DEFAULT_FONT_STYLE));
                show_value=pref_key.getSummary().toString();
            } else if (key_string.equals(mContext.getString(R.string.settings_tb_font_size))) {
                pref_key.setSummary(shared_pref.getString(key_string, DEFAULT_FONT_SIZE));
                show_value=pref_key.getSummary().toString();
            } else if (key_string.equals(mContext.getString(R.string.settings_tb_use_light_theme))) {
                show_value=""+shared_pref.getBoolean(key_string,false);
            } else if (key_string.equals(mContext.getString(R.string.settings_tb_text_area_background_color))) {
                String color=shared_pref.getString(key_string,"");
                pref_key.setSummary(color);
                show_value=pref_key.getSummary().toString();
            } else if (key_string.equals(mContext.getString(R.string.settings_tb_show_all_file_as_text))) {
                boolean value=shared_pref.getBoolean(key_string,false);
                Preference pf=mPrefFrag.findPreference(mContext.getString(R.string.settings_tb_mime_type_to_open_text_mode));
                if (value) setPreferenceEnabled(mPrefFrag.getActivity(), pf, false);
                else setPreferenceEnabled(mPrefFrag.getActivity(), pf, true);
                show_value=""+value;
            } else if (key_string.equals(mContext.getString(R.string.settings_tb_mime_type_to_open_text_mode))) {
                String value=shared_pref.getString(key_string,"");
                pref_key.setSummary(value);
                show_value=value;
            }
            checkUiCombination(log, pref_key, shared_pref, key_string, mContext);
            log.debug("checkSettings entered, key="+key_string+", value="+show_value);
        }

        private void checkUiCombination(Logger log, Preference pref_key, SharedPreferences shared_pref, String key_string, Context c) {
            Preference pf_ts=mPrefFrag.findPreference(c.getString(R.string.settings_tb_tab_stop));
            Preference pf_ff=mPrefFrag.findPreference(c.getString(R.string.settings_tb_font_family));
            String val_ff=shared_pref.getString(c.getString(R.string.settings_tb_font_family), DEFAULT_FONT_FAMILY);
            Preference pf_fs=mPrefFrag.findPreference(c.getString(R.string.settings_tb_font_style));
            String val_fs=shared_pref.getString(c.getString(R.string.settings_tb_font_style), DEFAULT_FONT_STYLE);
            Preference pf_lb=mPrefFrag.findPreference(c.getString(R.string.settings_tb_line_break));
            String val_lb=shared_pref.getString(c.getString(R.string.settings_tb_line_break), DEFAULT_LINE_BREAK);
            if (val_lb.equals(String.valueOf(CustomTextView.LINE_BREAK_NOTHING))) {
                setPreferenceEnabled(mPrefFrag.getActivity(), pf_ff, false);
                setPreferenceEnabled(mPrefFrag.getActivity(), pf_fs, false);
                setPreferenceEnabled(mPrefFrag.getActivity(), pf_ts, true);
            } else {
                setPreferenceEnabled(mPrefFrag.getActivity(), pf_ff, true);
                if (val_ff.equals("NORMAL")) {
                    setPreferenceEnabled(mPrefFrag.getActivity(), pf_fs, true);
                    setPreferenceEnabled(mPrefFrag.getActivity(), pf_ts, false);
                } else {
                    setPreferenceEnabled(mPrefFrag.getActivity(), pf_fs, false);
                    if (val_ff.equals("MONOSPACE")) {
                        setPreferenceEnabled(mPrefFrag.getActivity(), pf_ts, true);
                    } else {
                        setPreferenceEnabled(mPrefFrag.getActivity(), pf_ts, false);
                    }
                }
            }
        }

        @Override
        public void onStart() {
        	super.onStart();
        	log.debug("onStart entered");
    	    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(listenerAfterHc);
//    		getActivity().setTitle(R.string.settings_main_title);
        };
        @Override
        public void onStop() {
        	super.onStop();
        	log.debug("onStop entered");
    	    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listenerAfterHc);
        };
    };

    public static class SettingsCache extends PreferenceFragment {
        private static Logger log = LoggerFactory.getLogger(SettingsCache.class);
        private SharedPreferences.OnSharedPreferenceChangeListener listenerAfterHc = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences shared_pref, String key_string) {
                checkSettings(log, shared_pref, key_string);
            }
        };

        private PreferenceFragment mPrefFrag=null;
        private Context mContext=null;

        @Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
        	log.debug("onCreate entered");
            
    		addPreferencesFromResource(R.xml.tb_settings_frag_cache);

            mPrefFrag=this;
    		mContext=getActivity().getApplicationContext();

    		SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);

            checkSettings(log, shared_pref,getString(R.string.settings_tb_index_cache));
        };

        private void checkSettings(Logger log, SharedPreferences shared_pref, String key_string) {
            Preference pref_key=mPrefFrag.findPreference(key_string);
            if (key_string.equals(mContext.getString(R.string.settings_tb_index_cache))) {
                String[] sa=mContext.getResources().getStringArray(R.array.settings_tb_index_cache_list_entries);
                if (shared_pref.getString(key_string, "0").equals("0")) {
                    pref_key.setSummary(sa[0]);
                } else if (shared_pref.getString(key_string, "1").equals("1")) {
                    pref_key.setSummary(sa[1]);
                } else if (shared_pref.getString(key_string, "2").equals("2")) {
                    pref_key.setSummary(sa[2]);
                } else if (shared_pref.getString(key_string, "3").equals("3")) {
                    pref_key.setSummary(sa[3]);
                }
                log.debug("checkSettings entered, key="+key_string+", value="+pref_key.getSummary());
            }

        }

        @Override
        public void onStart() {
        	super.onStart();
        	log.debug("onStart entered");
    	    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(listenerAfterHc);
//    		getActivity().setTitle(R.string.settings_main_title);
        };
        @Override
        public void onStop() {
        	super.onStop();
        	log.debug("onStop entered");
    	    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listenerAfterHc);
        };
    };

    public static class SettingsBuffer extends PreferenceFragment {
        private static Logger log = LoggerFactory.getLogger(SettingsBuffer.class);
        private SharedPreferences.OnSharedPreferenceChangeListener listenerAfterHc = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences shared_pref, String key_string) {
                checkSettings(log, shared_pref, key_string);
            }
        };

        private PreferenceFragment mPrefFrag=null;
        private Context mContext=null;

        @Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
        	log.debug("onCreate entered");
            
    		addPreferencesFromResource(R.xml.tb_settings_frag_buffer);

            mPrefFrag=this;
    		mContext=getActivity().getApplicationContext();

    		SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);

            checkSettings(log, shared_pref,getString(R.string.settings_tb_buffer_char_index_size));
            checkSettings(log, shared_pref,getString(R.string.settings_tb_buffer_hex_index_size));
            checkSettings(log, shared_pref,getString(R.string.settings_tb_buffer_pool_size));
        };

        private void checkSettings(Logger log, SharedPreferences shared_pref, String key_string) {
            Preference pref_key=mPrefFrag.findPreference(key_string);
            if (key_string.equals(mContext.getString(R.string.settings_tb_buffer_char_index_size))) {
                pref_key.setSummary(shared_pref.getString(key_string, "8")+"KB");
                log.debug("checkSettings entered, key="+key_string+", value="+pref_key.getSummary());
            } else if (key_string.equals(mContext.getString(R.string.settings_tb_buffer_hex_index_size))) {
                pref_key.setSummary(shared_pref.getString(key_string, "2")+"KB");
                log.debug("checkSettings entered, key="+key_string+", value="+pref_key.getSummary());
            } else if (key_string.equals(mContext.getString(R.string.settings_tb_buffer_pool_size))) {
                pref_key.setSummary(shared_pref.getString(key_string, "256")+"KB");
                log.debug("checkSettings entered, key="+key_string+", value="+pref_key.getSummary());
            }

        }

        @Override
        public void onStart() {
        	super.onStart();
        	log.debug("onStart entered");
    	    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(listenerAfterHc);
//    		getActivity().setTitle(R.string.settings_main_title);
        };
        @Override
        public void onStop() {
        	super.onStop();
        	log.debug("onStop entered");
    	    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listenerAfterHc);
        };
    };

    public static class SettingsMisc extends PreferenceFragment {
        private static Logger log = LoggerFactory.getLogger(SettingsMisc.class);
        private SharedPreferences.OnSharedPreferenceChangeListener listenerAfterHc = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences shared_pref, String key_string) {
                checkSettings(log, shared_pref, key_string);
            }
        };

        private PreferenceFragment mPrefFrag=null;
        private Context mContext=null;

        @Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
        	log.debug("onCreate entered");
            
    		addPreferencesFromResource(R.xml.tb_settings_frag_misc);

            mPrefFrag=this;
    		mContext=getActivity().getApplicationContext();

    		SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);

            checkSettings(log, shared_pref,getString(R.string.settings_tb_debug_enable));
            checkSettings(log, shared_pref,getString(R.string.settings_tb_exit_cleanly));
            checkSettings(log, shared_pref,getString(R.string.settings_tb_confirm_exit));

            if (!mGp.debugEnabled) {
                mPrefFrag.findPreference(getString(R.string.settings_tb_use_no_word_wrap_text_view)).setEnabled(false);
            } else {
                mPrefFrag.findPreference(getString(R.string.settings_tb_use_no_word_wrap_text_view)).setEnabled(true);
            }
            checkSettings(log, shared_pref, getString(R.string.settings_tb_use_no_word_wrap_text_view));
        };

        private void checkSettings(Logger log, SharedPreferences shared_pref, String key_string) {
            Preference pref_key=mPrefFrag.findPreference(key_string);
            if (key_string.equals(mContext.getString(R.string.settings_tb_debug_enable))) {
                log.debug("checkSettings entered, key="+key_string+", value="+shared_pref.getBoolean(key_string, false));
            } else if (key_string.equals(mContext.getString(R.string.settings_tb_exit_cleanly))) {
                log.debug("checkSettings entered, key="+key_string+", value="+shared_pref.getBoolean(key_string, false));
            } else if (key_string.equals(mContext.getString(R.string.settings_tb_confirm_exit))) {
                log.debug("checkSettings entered, key="+key_string+", value="+shared_pref.getBoolean(key_string, false));
            } else if (key_string.equals(mContext.getString(R.string.settings_tb_use_no_word_wrap_text_view))) {
                log.debug("checkSettings entered, key="+key_string+", value="+shared_pref.getBoolean(key_string, false));
            }

        }

        @Override
        public void onStart() {
        	super.onStart();
        	log.debug("onStart entered");
    	    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(listenerAfterHc);
//    		getActivity().setTitle(R.string.settings_main_title);
        };
        @Override
        public void onStop() {
        	super.onStop();
        	log.debug("onStop entered");
    	    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listenerAfterHc);
        };
    };

}