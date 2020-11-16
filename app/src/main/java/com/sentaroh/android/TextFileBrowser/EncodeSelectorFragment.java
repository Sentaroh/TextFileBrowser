package com.sentaroh.android.TextFileBrowser;
/*
The MIT License (MIT)
Copyright (c) 2011 Sentaroh

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

import java.util.ArrayList;

import com.sentaroh.android.Utilities3.Dialog.CommonDialog;
import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.ThemeUtil;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncodeSelectorFragment extends DialogFragment {
    private static Logger log = LoggerFactory.getLogger(EncodeSelectorFragment.class);
	private final static String APPLICATION_TAG="EncodeSelectorFragment";

	private Dialog mDialog=null;
	private EncodeSelectorFragment mFragment=null;

	private GlobalParameters mGp =null;

	private boolean mTerminateSelf=false;
	
//	private ViewedFileListItem mViewedFileListItem=null;
//	private FileViewerFragment mFileViewerFragment=null;
	
	private SavedViewValues mSavedViewValues=null;

	private boolean mIsLightThemeUsed=false;

	@SuppressWarnings("unused")
	private Context mContext=null;

	public static EncodeSelectorFragment newInstance() {
//		Log.v(APPLICATION_TAG,"newInstance");
		EncodeSelectorFragment frag = new EncodeSelectorFragment();
        Bundle bundle = new Bundle();
        frag.setArguments(bundle);
        return frag;
    }

	public EncodeSelectorFragment() {
		log.debug("Constructor(Default)");
	}

	@Override
	public void onAttach(Activity activity) {
	    super.onAttach(activity);
	    mContext=getActivity().getApplicationContext();
    	mGp =GlobalWorkArea.getGlobalParameters(getActivity());

	    if (mGp.debugEnabled) log.debug("onAttach");
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {  
		super.onSaveInstanceState(outState);
		if (mGp.debugEnabled) log.debug("onSaveInstanceState");
		if(outState.isEmpty()){
	        outState.putBoolean("WORKAROUND_FOR_BUG_19917_KEY", true);
	    }
		outState.putBoolean("Restart", true);
    	saveViewContents();
	}

	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
	    // Ignore orientation change to keep activity from restarting
	    super.onConfigurationChanged(newConfig);
	    if (mGp.debugEnabled) log.debug("onConfigurationChanged");
	    reInitViewWidget();
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	if (mGp.debugEnabled) log.debug("onCreateView");
    	View view=super.onCreateView(inflater, container, savedInstanceState);
//    	CommonDialog.setDlgBoxSizeLimit(mDialog,true);
    	return view;
    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);
	    if (mGp.debugEnabled) log.debug("onActivityCreated");
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        mContext=getActivity().getApplicationContext();
        mGp =GlobalWorkArea.getGlobalParameters(getActivity());
        if (mGp.debugEnabled) log.debug("onCreate");
        mFragment=this;
        mIsLightThemeUsed=ThemeUtil.isLightThemeUsed(mContext);
       	if (savedInstanceState!=null) {
            if (savedInstanceState.getBoolean("Restart", false)) {
//    			Restart occurred, terminate self
                if (mGp.debugEnabled) log.debug("Application restart is detected, terminate self issued.");
            	mTerminateSelf=true;
            	mFragment.dismiss();
            }
       	}
        this.setRetainInstance(true);
    }

	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	if (mGp.debugEnabled) log.debug("onCreateDialog");
    	mDialog=new Dialog(getActivity(), mGp.screenTheme);
		mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		mDialog.setCanceledOnTouchOutside(false);

		if (!mTerminateSelf) {
    		initViewWidget();
    		restoreViewContents();
            CommonDialog.setDlgBoxSizeLimit(mDialog,true);
        }

        return mDialog;
    }

	@Override
	public void onStart() {
	    super.onStart();
	    if (mGp.debugEnabled) log.debug("onStart");
	}

	@Override
	public void onCancel(DialogInterface di) {
		if (mGp.debugEnabled) log.debug("onCancel");
		mFragment.dismiss();
		super.onCancel(di);
	}

	@Override
	public void onDismiss(DialogInterface di) {
		if (mGp.debugEnabled) log.debug("onDismiss");
		super.onDismiss(di);
	}

	@Override
	public void onStop() {
	    super.onStop();
	    if (mGp.debugEnabled) log.debug("onStop");
	}

	@Override
	public void onDestroyView() {
		if (mGp.debugEnabled) log.debug("onDestroyView");
	    if (getDialog() != null && getRetainInstance())
	        getDialog().setDismissMessage(null);
	    super.onDestroyView();
	}

	@Override
	public void onDetach() {
	    super.onDetach();
	    if (mGp.debugEnabled) log.debug("onDetach");
	}

	private void reInitViewWidget() {
    	if (mGp.debugEnabled) log.debug("reInitViewWidget");
    	saveViewContents();
    	initViewWidget();
    	restoreViewContents();
    }

	public void showDialog(FragmentManager fm, Fragment frag) {
//    	if (mGp.debugEnabled)
    		log.debug("showDialog");
//    	mViewedFileListItem=vfli;
	    FragmentTransaction ft = fm.beginTransaction();
	    ft.add(frag,null);
	    ft.commitAllowingStateLoss();
//    	show(fm,APPLICATION_TAG);
    }

	class SavedViewValues {
    	
    }

	private void saveViewContents() {
    	mSavedViewValues=new SavedViewValues();
    }

	private void restoreViewContents() {
    	if (mSavedViewValues!=null) {
    		
    	}
    	mSavedViewValues=null;
    }

	private void initViewWidget() {
    	if (mGp.debugEnabled) log.debug("initViewWidget");

    	mDialog.setContentView(R.layout.encode_name_selection_dlg);
    	
//    	mDialog.setTitle(R.string.msgs_tb_encode_dialog_title);
    	
    	LinearLayout title_view=(LinearLayout)mDialog.findViewById(R.id.encode_name_selection_dlg_title_view);
    	title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
    	TextView title=(TextView)mDialog.findViewById(R.id.encode_name_selection_dlg_title);
    	title.setTextColor(mGp.themeColorList.title_text_color);
    	
		final Button btn_ok=(Button)mDialog.findViewById(R.id.encode_name_selection_dlg_btn_ok);
		final Button btn_cancel=(Button)mDialog.findViewById(R.id.encode_name_selection_dlg_btn_cancel);
		final ListView lv_encode=(ListView)mDialog.findViewById(R.id.encode_name_selection_dlg_listview);
		//		lv_encode.setBackgroundColor(mGp.themeColorList.window_background_color_content);
		
		ArrayList<EncodeListItem>encode_list=new ArrayList<EncodeListItem>();
		
		final String[] tmp_enc_array_list=getResources().getStringArray(R.array.settings_tb_default_encode_name_list_entries);
		final String[] tmp_enc_array_value=getResources().getStringArray(R.array.settings_tb_default_encode_name_list_values);

        final String[] enc_array_list=new String[tmp_enc_array_list.length+1];
        final String[] enc_array_value=new String[tmp_enc_array_value.length+1];
        enc_array_list[0]=getResources().getString(R.string.settings_tb_encode_name_display_automatic);
        enc_array_value[0]=getResources().getString(R.string.settings_tb_encode_name_value_automatic);

        for(int i=0;i<tmp_enc_array_list.length;i++) enc_array_list[i+1]=tmp_enc_array_list[i];
        for(int i=0;i<tmp_enc_array_value.length;i++) enc_array_value[i+1]=tmp_enc_array_value[i];

		EncodeListItem item=new EncodeListItem();
		FragmentManager fm=getActivity().getSupportFragmentManager();
		final FileViewerFragment fvf=(FileViewerFragment)fm.findFragmentById(R.id.container);
		if (fvf.getEncodeName().equals("")) item.isChecked=true;
		item.encode_name=enc_array_list[0];
		encode_list.add(item);
		for (int i=1;i<enc_array_list.length;i++) {
			item=new EncodeListItem();
			item.encode_name=enc_array_list[i];
//			byte[] utf_8=ENCODE_NAME_UTF8.getBytes();
//            byte[] enc=fvf.getEncodeName().getBytes();
//            byte[] lst=enc_array_list[i].getBytes();
//            log.info("name="+ENCODE_NAME_UTF8+", enc="+StringUtil.getHexString(utf_8,0,utf_8.length)+
//                    ", name="+fvf.getEncodeName()+", enc="+StringUtil.getHexString(enc,0,enc.length)+
//                    ", name="+enc_array_list[i]+", lst="+StringUtil.getHexString(lst,0,lst.length));
			if (fvf.getEncodeName().equals(enc_array_list[i])) {
                item.isChecked = true;
                lv_encode.setSelection(i);
            }
			encode_list.add(item);
		}
		final EncodeSelectorAdapter adapter=new EncodeSelectorAdapter(getActivity(), R.layout.encode_name_selection_list_item, encode_list);
		lv_encode.setAdapter(adapter);
		lv_encode.setScrollbarFadingEnabled(false);
        btn_ok.setEnabled(false);
        btn_ok.setAlpha(0.3f);

        NotifyEvent ntfy_click=new NotifyEvent(mContext);
        ntfy_click.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                String sel_name=(String)objects[0];
                if (sel_name.equals(fvf.getEncodeName())) {
                    btn_ok.setEnabled(false);
                    btn_ok.setAlpha(0.3f);
                } else {
                    btn_ok.setEnabled(true);
                    btn_ok.setAlpha(1.0f);
                }
            }

            @Override
            public void negativeResponse(Context context, Object[] objects) {

            }
        });
        adapter.setClickListener(ntfy_click);

		btn_cancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				mFragment.dismiss();
			}
		});
		btn_ok.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				int pos=0;
				for (int i=0;i<adapter.getCount();i++) {
					if (adapter.getItem(i).isChecked) {
						pos=i;
						if (pos==0) fvf.setEncodeName("");
						else fvf.setEncodeName(enc_array_value[pos]);
						fvf.reloadFile();
						mFragment.dismiss();
						break;
					}
				}
			}
		});

    }

}
