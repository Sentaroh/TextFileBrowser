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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.storage.StorageManager;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;


import com.google.android.material.tabs.TabLayout;
import com.sentaroh.android.TextFileBrowser.Log.LogManagementFragment;
import com.sentaroh.android.TextFileBrowser.Log.LogUtil;
import com.sentaroh.android.Utilities3.AppUncaughtExceptionHandler;
import com.sentaroh.android.Utilities3.BuildConfig;
import com.sentaroh.android.Utilities3.CallBackListener;
import com.sentaroh.android.Utilities3.Dialog.CommonDialog;
import com.sentaroh.android.Utilities3.Dialog.CommonFileSelector2;
import com.sentaroh.android.Utilities3.Dialog.MessageDialogFragment;
import com.sentaroh.android.Utilities3.Dialog.ProgressBarDialogFragment;
import com.sentaroh.android.Utilities3.LocalMountPoint;
import com.sentaroh.android.Utilities3.MiscUtil;
import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.SafManager3;
import com.sentaroh.android.Utilities3.SafStorage3;
import com.sentaroh.android.Utilities3.SystemInfo;
import com.sentaroh.android.Utilities3.ThemeUtil;
import com.sentaroh.android.Utilities3.ThreadCtrl;
import com.sentaroh.android.Utilities3.Widget.CustomTabLayout;
import com.sentaroh.android.Utilities3.Widget.CustomViewPager;
import com.sentaroh.android.Utilities3.Widget.CustomViewPagerAdapter;
import com.sentaroh.android.Utilities3.Zip.ZipUtil;

import static com.sentaroh.android.TextFileBrowser.Constants.*;
import static com.sentaroh.android.Utilities3.Dialog.CommonFileSelector2.DIALOG_SELECT_CATEGORY_FILE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.markdownj.MarkdownProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressLint("NewApi")
public class MainActivity extends AppCompatActivity {
	 
	private GlobalParameters mGp =null;

    private static Logger log = LoggerFactory.getLogger(MainActivity.class);

	private boolean mTerminateApplication=false;
	private int mRestartStatus=0;

	private Spinner mViewedFileListSpinner=null;
	private int mSavedViewedFileListSpinnerPosition=-1;
	private ViewedFileListAdapter mViewedFileListAdapter=null;
	private CommonDialog mCommonDlg=null;

	private FragmentManager mFragmentManager=null;
	
	private Context mContext;
	private MainActivity mActivity;

    private CommonUtilities mUtil = null;

	@Override  
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
        mUtil.addDebugMsg(1, "I", "onSaveInstanceState entered");
        saveViewContents(outState);
	};

	@Override
	protected void onRestoreInstanceState(Bundle savedState) {  
		super.onRestoreInstanceState(savedState);
        mUtil.addDebugMsg(1, "I", "onRestoreInstanceState entered");
		mRestartStatus=2;
        restoreViewContents(savedState);
	};

	final static String SAVE_KEY_SPINNER_POS="SpinnerPos";
    final static String SAVE_KEY_VFL_SIZE="FAH_Size";
    final static String SAVE_KEY_VFL_LIST="FAH_List";
    private void saveViewContents(Bundle outState) {
        outState.putInt(SAVE_KEY_SPINNER_POS,mViewedFileListSpinner.getSelectedItemPosition());
        outState.putInt(SAVE_KEY_VFL_SIZE, mGp.viewedFileList.size());
        try {
            ByteArrayOutputStream bos=new ByteArrayOutputStream(1024*32);
            ObjectOutputStream oos=new ObjectOutputStream(bos);

            for (int i = 0; i< mGp.viewedFileList.size(); i++) {
                ViewedFileListItem vfli= mGp.viewedFileList.get(i);
                vfli.writeExternal(oos);
            }
            oos.flush();
            byte[] buf=bos.toByteArray();
            outState.putByteArray(SAVE_KEY_VFL_LIST, buf);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    };

    private void restoreViewContents(Bundle savedState) {
        mSavedViewedFileListSpinnerPosition=savedState.getInt(SAVE_KEY_SPINNER_POS);
        byte[] buf=savedState.getByteArray(SAVE_KEY_VFL_LIST);
        int list_size=savedState.getInt(SAVE_KEY_VFL_SIZE);
        try {
            ByteArrayInputStream bis=new ByteArrayInputStream(buf);
            ObjectInputStream ois=new ObjectInputStream(bis);
            mGp.viewedFileList=new ArrayList<ViewedFileListItem>();
            for (int i=0;i<list_size;i++) {
                ViewedFileListItem vfli=new ViewedFileListItem();
                vfli.readExternal(ois);
                if (vfli.viewed_file_uri_string!=null) vfli.viewd_file=new SafFile3(mContext, Uri.parse(vfli.viewed_file_uri_string));
                mGp.viewedFileList.add(vfli);
            }

            ois.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
        mUtil.addDebugMsg(1, "I", "onNewIntent entered, restartStatus="+mRestartStatus);
		if (mRestartStatus==2) return;
		if (mFileSelectorDialogFragment!=null && mRestartStatus==1) return;
		if (intent!=null && intent.getData()!=null) {
		    prepareShowFile(intent);
	        refreshOptionMenu();
		}
	};

    @Override
    public void onCreate(Bundle savedInstanceState) {
//        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
//        StrictMode.setVmPolicy(builder.build());

        mContext=MainActivity.this;
        mActivity=MainActivity.this;
        mFragmentManager=getSupportFragmentManager();
        mRestartStatus=0;
        mGp =GlobalWorkArea.getGlobalParameters(mActivity);
        if (mGp.commonNotification==null) mGp.commonNotification=new NotificationCommonParms();
        setTheme(mGp.screenTheme);
        super.onCreate(savedInstanceState);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.text_browser_activity);

        mUtil=new CommonUtilities(mActivity, "Main", mGp, getSupportFragmentManager());
        mUtil.addDebugMsg(1, "I", "onCreate entered, SDK="+ Build.VERSION.SDK_INT+", Appl="+getApplVersionName());

        MyUncaughtExceptionHandler myUncaughtExceptionHandler = new MyUncaughtExceptionHandler();
        myUncaughtExceptionHandler.init(mContext, myUncaughtExceptionHandler);

        mCommonDlg=new CommonDialog(mActivity, mFragmentManager);

        if (mGp.viewedFileList==null) mGp.viewedFileList=new ArrayList<ViewedFileListItem>();
        initFileSelectionSpinner();

        cleanupCacheFile();
    };

    private void initFileSelectionSpinner() {
        mViewedFileListSpinner=(Spinner)findViewById(R.id.text_browser_activity_file_view_selector);
        setSpinnerBackground(mActivity, mViewedFileListSpinner, ThemeUtil.isLightThemeUsed(mActivity));
        mViewedFileListAdapter=new ViewedFileListAdapter(this, android.R.layout.simple_spinner_item, mGp.viewedFileList);
        mViewedFileListAdapter.setDropDownViewResource(R.layout.viewed_file_list_item);
        mViewedFileListSpinner.setPrompt(mContext.getString(R.string.msgs_text_browser_select_view_file));
        mViewedFileListSpinner.setAdapter(mViewedFileListAdapter);
    };

    public static void setSpinnerBackground(Context c, Spinner spinner, boolean theme_is_light) {
        if (theme_is_light) {
            spinner.setBackground(c.getDrawable(R.drawable.spinner_color_background_light));
        } else {
            spinner.setBackground(c.getDrawable(R.drawable.spinner_color_background));
        }
    }

    private class MyUncaughtExceptionHandler extends AppUncaughtExceptionHandler {
        @Override
        public void appUniqueProcess(Throwable ex, String strace) {
            log.error("UncaughtException detected, error="+ex);
            log.error(strace);
            mUtil.flushLog();
        }
    };

    @Override
	public void onStart() {
		super.onStart();
        mUtil.addDebugMsg(1, "I", "onStart entered");
	};

	@Override
	public void onRestart() {
		super.onStart();
        mUtil.addDebugMsg(1, "I", "onRestart entered");
        mGp.commonNotification.notificationManager.cancelAll();
	};

	@Override
	public void onResume() {
		super.onResume();
        mUtil.addDebugMsg(1, "I", "onResume entered, restartStatus="+mRestartStatus);
		if (mRestartStatus==0) {
	    	Intent in=getIntent();
            if (in==null || in.getData()==null) {
		    	NotificationUtil.initNotification(mContext, mGp.commonNotification);
			} else {
                NotificationUtil.initNotification(mContext, mGp.commonNotification);
                prepareShowFile(in);
			}
		} else if (mRestartStatus==1) {
        } else if (mRestartStatus==2) {
            NotificationUtil.initNotification(mContext, mGp.commonNotification);
            for (int i = 0; i< mGp.viewedFileList.size(); i++) {
                ViewedFileListItem vfli= mGp.viewedFileList.get(i);

                vfli.viewerParmsInitRequired=false;
                vfli.viewerParmsRestoreRequired=true;
                vfli.encodeName="";

                vfli.file_view_fragment=FileViewerFragment.newInstance();
                vfli.file_view_fragment.setFileViewerParameter(vfli.viewd_file);

                vfli.tc_view=new ThreadCtrl();
                vfli.ix_reader_view=new IndexedFileReader(mContext, mCommonDlg,
                        vfli.tc_view,
                        mGp.settingDefaultEncodeName,
                        mGp.settingIndexCache,
                        mGp.settingBufferCharIndexSize,
                        mGp.settingBufferHexIndexSize,
                        mGp.settingBufferPoolSize,
                        mActivity);
                if (i==mSavedViewedFileListSpinnerPosition) mGp.currentViewedFile=vfli.viewd_file;
            }
            initFileSelectionSpinner();

            mCommonDlg.showCommonDialog(false, "W", getString(R.string.msgs_tb_application_restarted),"",null);

            showFileByViewedFileList(mGp.currentViewedFile);

            mViewedFileListSpinner.setSelection(mSavedViewedFileListSpinnerPosition);

        }
		mRestartStatus=1;

        setViewedFilleSelectorListener();

    	Handler hndl=new Handler();
		hndl.postDelayed(new Runnable(){
			@Override
			public void run() {
		        mEnableFileSelection=true;
			}
		},500);

	};

	private void prepareShowFile(Intent in) {
        int flag=in.getFlags();
        mUtil.addDebugMsg(1, "I", "received flag="+String.format("0x%8x", flag)+", intent="+in.getData().toString());
        try {
            final SafFile3 in_file=new SafFile3(mContext, in.getData());
            InputStream is=in_file.getInputStreamByUri();
            if (!isFileAlreadyViewed(in_file)) {
                addFileToViewedFileList(true, in.getType(), in_file);
                showFileByViewedFileList(in_file);
            } else {
                showFileByViewedFileList(in_file);
            }
        } catch (Exception e) {
            e.printStackTrace();
            NotifyEvent ntfy=new NotifyEvent(mContext);
            ntfy.setListener(new NotifyEventListener() {
                @Override
                public void positiveResponse(Context context, Object[] objects) {
                    if (mGp.viewedFileList.size()==0) finish();
                }
                @Override
                public void negativeResponse(Context context, Object[] objects) {}
            });
            mCommonDlg.showCommonDialog(false, "W", "File read error", in.getData().toString()+"\n"+e.getMessage(), ntfy);
        }

    }

	private boolean isFileAlreadyViewed(SafFile3 in_file) {
		boolean result=false;
		for (int i=0;i<getViewedFileListCount();i++) {
            if (mGp.viewedFileList.get(i).viewd_file.getPath().equals(in_file.getPath())) {
				result=true;
				break;
			}
		}
        mUtil.addDebugMsg(1, "I", "isFileAlreadyViewed, Uri="+in_file.getPath()+", result="+result);
		return result;
	};

	private int getViewedFileListCount() {
		int result=0;
		if (mGp.viewedFileList!=null) result= mGp.viewedFileList.size();
        mUtil.addDebugMsg(1, "I", "getViewedFileListCount, result="+result);
		return result;
	};

	private boolean isTextMimeTypex(String mime_type) {
	    boolean result=false;
	    String[] mt_array=mGp.settingMimeTypeToOpenAsText.split(";");
	    for(String item:mt_array) {
	        if (item.equals("*")) {
                result=true;
                break;
            } else {
	            if (mime_type!=null) {
                    String reg_exp= MiscUtil.convertRegExp(item);
                    Pattern pattern=Pattern.compile("^"+reg_exp);
                    Matcher matcher=pattern.matcher(mime_type);
                    boolean matched=matcher.find();
                    if (matched) {
                        result=true;
                        break;
                    }
                }
            }
        }
        mUtil.addDebugMsg(1, "I", "isTextMimeType result="+result+", MimeType="+mime_type);
	    return result;
    }
	private void addFileToViewedFileList(boolean set_selection, String mime_type, SafFile3 in_file) {
        mUtil.addDebugMsg(1, "I", "addFileToViewedFileList, fp="+in_file.getPath());
		ViewedFileListItem vfli=new ViewedFileListItem();

		if (mGp.settingShowAllFileAsText || (isTextMimeTypex(mime_type))) vfli.browseMode=FileViewerAdapter.TEXT_BROWSER_BROWSE_MODE_CHAR;
		else vfli.browseMode=FileViewerAdapter.TEXT_BROWSER_BROWSE_MODE_HEX;

		vfli.tc_view=new ThreadCtrl();
		vfli.viewd_file=in_file;
        mGp.viewedFileList.add(vfli);

        vfli.ix_reader_view=new IndexedFileReader(mContext, mCommonDlg,
        		vfli.tc_view,
//        		mGp.settingEncodeName,
        		mGp.settingDefaultEncodeName,
        		mGp.settingIndexCache,
        		mGp.settingBufferCharIndexSize,
        		mGp.settingBufferHexIndexSize,
        		mGp.settingBufferPoolSize,
        		mActivity);
        mViewedFileListAdapter.notifyDataSetChanged();
        vfli.file_view_fragment=FileViewerFragment.newInstance();
        vfli.file_view_fragment.setFileViewerParameter(in_file);

        if (set_selection)
        	mViewedFileListSpinner.setSelection(mGp.viewedFileList.size()-1);
	};

	private void removeFileFromViewedFileList(String fp) {
		ViewedFileListItem vfli=null;
		int rem_pos=-1;
		for (int i=0;i<getViewedFileListCount();i++) {
			if (mViewedFileListAdapter.getItem(i).viewd_file.getPath().equals(fp)) {
				vfli= mGp.viewedFileList.get(i);
				if (vfli.file_view_fragment!=null) {
					mFragmentManager.beginTransaction()
					.setTransition(FragmentTransaction.TRANSIT_NONE)
					.remove(vfli.file_view_fragment)
					.commit();
				}
				mGp.viewedFileList.remove(i);
				mViewedFileListAdapter.notifyDataSetChanged();
				rem_pos=i;
				mGp.currentViewedFile=null;
				break;
			}
		}
		if (rem_pos!=-1) {
			if (rem_pos>1) {
				showFileByViewedFileList(mViewedFileListAdapter.getItem(rem_pos-1).viewd_file);
//				NotificationUtil.showOngoingNotificationMsg(mContext, mGp.commonNotification, mGp.currentViewedFile.uri.getPath());
			} else {
				if (getViewedFileListCount()>0) {
					showFileByViewedFileList(mViewedFileListAdapter.getItem(0).viewd_file);
//					NotificationUtil.showOngoingNotificationMsg(mContext, mGp.commonNotification, mGp.currentViewedFile.uri.getPath());
				}
			}
		}
		mUtil.addDebugMsg(1, "I", "removeFileFromViewedFileList, fp="+fp+", result="+rem_pos);
	};

	private ViewedFileListItem getViewedFileListItem(SafFile3 in_file) {
		ViewedFileListItem vfli=null;
        int pos=-1;
		if (in_file!=null) {
            for (int i=0;i<getViewedFileListCount();i++) {
                if (mGp.viewedFileList.get(i).viewd_file.getPath().equals(in_file.getPath())) {
                    vfli= mGp.viewedFileList.get(i);
                    pos=i;
                    break;
                }
            }
        }
		if (in_file!=null) mUtil.addDebugMsg(1, "I", "getViewedFileListItem, fp="+in_file.getPath()+", result="+pos);
		else mUtil.addDebugMsg(1, "I", "getViewedFileListItem, fp=null");
//		Thread.dumpStack();
		return vfli;
	};

	private void showFileByViewedFileList(SafFile3 in_file) {
		int pos=-1;
		for (int i=0;i<getViewedFileListCount();i++) {
			ViewedFileListItem vfli= mGp.viewedFileList.get(i);
            if (vfli.viewd_file.getPath().equals(in_file.getPath())) {
				if (vfli.file_view_fragment!=null) {
					FragmentTransaction ft=mFragmentManager.beginTransaction();
					ft.setTransition(FragmentTransaction.TRANSIT_NONE);
					ft.replace(R.id.container, vfli.file_view_fragment);
					ft.commit();
				} else {
			        vfli.file_view_fragment=FileViewerFragment.newInstance();
                    vfli.file_view_fragment.setFileViewerParameter(in_file);
					FragmentTransaction ft=mFragmentManager.beginTransaction();
					ft.setTransition(FragmentTransaction.TRANSIT_NONE);
					ft.replace(R.id.container, vfli.file_view_fragment);
					ft.commit();
				}
				mGp.currentViewedFile= mGp.viewedFileList.get(i).viewd_file;
				pos=i;
				mViewedFileListSpinner.setSelection(i, false);

				TextView tv_blank=(TextView)mActivity.findViewById(R.id.blank);
				tv_blank.setVisibility(TextView.GONE);

				break;
			}
		}
		mUtil.addDebugMsg(1, "I", "showFileByViewedFileList, fp="+in_file.getPath()+", result="+pos);
	};

	@Override
	public void onPause() {
		super.onPause();
        mUtil.addDebugMsg(1, "I", "onPause entered");
        mUtil.flushLog();
        // Application process is follow
	};

	@Override
	public void onStop() {
		super.onStop();
        mUtil.addDebugMsg(1, "I", "onStop entered");
        // Application process is follow
        if (mGp.currentViewedFile!=null)
            NotificationUtil.showOngoingNotificationMsg(mContext, mGp.commonNotification, mGp.currentViewedFile.getPath());
        mUtil.flushLog();
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
        mUtil.addDebugMsg(1, "I", "onDestroy entered");
        // Application process is follow
		if (mTerminateApplication) {
	    	NotificationUtil.clearNotification(mContext, mGp.commonNotification);
//			deleteTaskData();
            cleanupCacheFile();
            mUtil.flushLog();
			if (mGp.settingExitCleanly) {
				Handler hndl=new Handler();
				hndl.postDelayed(new Runnable(){
					@Override
					public void run() {
						android.os.Process.killProcess(android.os.Process.myPid());
					}
				}, 200);
			} else {
//				mGp.viewedFileList=null;
//		    	mGp.commonNotification=null;
//		    	mGp=null;
                GlobalWorkArea.clearGp();
				System.gc();
			}
		} else {
			
		}
	};
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if (getViewedFileListCount()>1) {
				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						confirmCloseFile();
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {}
				});
                if (mGp.settingConfirmExit) {
                    mCommonDlg.showCommonDialog(true, "W", getString(R.string.msgs_tb_menu_close_confirm_msg),"",ntfy);
                } else {
                    ntfy.notifyToListener(true, null);
                }
			} else {
                final ViewedFileListItem vfli=getViewedFileListItem(mGp.currentViewedFile);
                if (vfli!=null) {
                    if (vfli.searchEnabled && vfli.file_view_fragment!=null) {
                        vfli.file_view_fragment.switchFindWidget();
                    } else {
                        confirmExit();
                    }
                } else {
                    confirmExit();
                }
			}
			return true;
			// break;
		default:
			return super.onKeyDown(keyCode, event);
			// break;
		}
	};
	
	private void confirmCloseFile() {
		final ViewedFileListItem vfli=getViewedFileListItem(mGp.currentViewedFile);
		if (vfli.tc_view.isThreadActive()) {
			NotifyEvent ntfy=new NotifyEvent(mContext);
			ntfy.setListener(new NotifyEventListener(){
				@Override
				public void positiveResponse(Context c, Object[] o) {
					if (vfli.tc_view.isThreadActive()) vfli.tc_view.setDisabled();
					removeFileFromViewedFileList(mGp.currentViewedFile.getPath());
					if (getViewedFileListCount()<1) {
						mTerminateApplication=true;
						finish();
					}
				}
				@Override
				public void negativeResponse(Context c, Object[] o) {}
			});
			mCommonDlg.showCommonDialog(true, "W", getString(R.string.msgs_text_browser_file_reading_cancel_confirm),"", ntfy);
		} else {
		    if (vfli.searchEnabled) {
                vfli.file_view_fragment.switchFindWidget();
            } else {
                removeFileFromViewedFileList(mGp.currentViewedFile.getPath());
                if (getViewedFileListCount()<1) {
                    mTerminateApplication=true;
                    finish();
                }
            }
		}
	};
	
	@SuppressLint("NewApi")
	public void refreshOptionMenu() {
		invalidateOptionsMenu();
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		mUtil.addDebugMsg(1, "I", "onCreateOptionsMenu Entered");
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_tb_setting, menu);
		return true;
	};
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		mUtil.addDebugMsg(1, "I", "onPrepareOptionsMenu Entered");
        super.onPrepareOptionsMenu(menu);
		ViewedFileListItem vf=getViewedFileListItem(mGp.currentViewedFile);
        CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_clear_cache), true);

        if (vf!=null) {
	        if (vf.browseMode==FileViewerAdapter.TEXT_BROWSER_BROWSE_MODE_CHAR) {
	        	menu.findItem(R.id.menu_tb_mode_swicth)
	        	.setTitle(getString(R.string.msgs_tb_menu_mode_switch_hex))
	        	.setIcon(R.drawable.ic_32_hex);
				menu.findItem(R.id.menu_tb_settings).setVisible(true);
				menu.findItem(R.id.menu_tb_mode_swicth).setVisible(true);
				menu.findItem(R.id.menu_tb_reload).setVisible(true);
				menu.findItem(R.id.menu_tb_find).setVisible(true);
				menu.findItem(R.id.menu_tb_about).setVisible(true);
	        } else {
	        	menu.findItem(R.id.menu_tb_mode_swicth)
	        	.setTitle(getString(R.string.msgs_tb_menu_mode_switch_char))
	        	.setIcon(R.drawable.ic_32_text);
				menu.findItem(R.id.menu_tb_settings).setVisible(true);
				menu.findItem(R.id.menu_tb_mode_swicth).setVisible(true);
				menu.findItem(R.id.menu_tb_reload).setVisible(true);
				menu.findItem(R.id.menu_tb_find).setVisible(false);
				menu.findItem(R.id.menu_tb_about).setVisible(true);
	        }
			if (vf.tc_view.isThreadActive()) {
				CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_settings), false);
				CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_mode_swicth), false);
				CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_reload), false);
				CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_find), false);
				CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_about), false);
			} else {
				CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_settings), true);
				CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_mode_swicth), true);
				CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_reload), true);
				CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_find), true);
				CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_about), true);
			}
			if (!vf.viewd_file.exists()) {
                menu.findItem(R.id.menu_tb_mode_swicth).setVisible(false);
                menu.findItem(R.id.menu_tb_find).setVisible(false);
            }
		} else {
            menu.findItem(R.id.menu_tb_mode_swicth).setVisible(false);
            menu.findItem(R.id.menu_tb_find).setVisible(false);
        }

        return true;
	};

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		mUtil.addDebugMsg(1, "I", "onOptionsItemSelected Entered");
		ViewedFileListItem vf=getViewedFileListItem(mGp.currentViewedFile);
		if (vf!=null) {
            FileViewerFragment fvf=(FileViewerFragment)vf.file_view_fragment;
            if (item.getItemId()== R.id.menu_tb_settings) {
                invokeSettings();
                refreshOptionMenu();
            } else if (item.getItemId()== R.id.menu_tb_reload) {
                fvf.reloadFile();
            } else if (item.getItemId()== R.id.menu_tb_mode_swicth) {
                fvf.switchDisplayMode();
                refreshOptionMenu();
            } else if (item.getItemId()== R.id.menu_tb_find) {
                fvf.switchFindWidget();
                refreshOptionMenu();
            } else if (item.getItemId()== R.id.menu_tb_about) {
                aboutTextFileBrowser();
            } else if (item.getItemId()== R.id.menu_tb_exit) {
                confirmExit();
            } else if (item.getItemId()== R.id.menu_tb_clear_cache) {
                clearCache();
            } else if (item.getItemId()== R.id.menu_tb_log_management) {
                invokeLogManagement();
            }
        } else {
            if (item.getItemId()== R.id.menu_tb_settings) {
                invokeSettings();
                refreshOptionMenu();
            } else if (item.getItemId()== R.id.menu_tb_log_management) {
                invokeLogManagement();
            } else if (item.getItemId()== R.id.menu_tb_about) {
                aboutTextFileBrowser();
            } else if (item.getItemId()== R.id.menu_tb_exit) {
                confirmExit();
            } else if (item.getItemId()== R.id.menu_tb_clear_cache) {
                clearCache();
            }
        }
		return false;
	};

	private void clearCache() {
        IndexedFileReader.removeIndexCache(mContext);
	    mCommonDlg.showCommonDialog(false, "W", "File index cache file was removed, restart the app.", "",null);
    }

    private void invokeLogManagement() {
        LogUtil.flushLog(mContext);
        LogManagementFragment lfm = LogManagementFragment.newInstance(mContext, false, getString(R.string.msgs_log_file_list_title));
        lfm.showDialog(mActivity, getSupportFragmentManager(), lfm, null);
    }

    public boolean isApplicationTerminating() {return mTerminateApplication;}
	
	private void confirmExit() {
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				ViewedFileListItem vfli=getViewedFileListItem(mGp.currentViewedFile);

				for (int i = 0; i< mGp.viewedFileList.size(); i++) {
					vfli= mGp.viewedFileList.get(i);
					if (vfli.tc_view.isThreadActive()) {
						vfli.tc_view.setDisabled();
					}
				}
				mTerminateApplication=true;
				mGp.viewedFileList.clear();
				finish();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
		});
        if (mGp.settingConfirmExit) {
            mCommonDlg.showCommonDialog(true, "W", mContext.getString(R.string.msgs_tb_menu_exit_confirm_msg), "", ntfy);
        } else {
            ntfy.notifyToListener(true, null);
        }
	};

    private void aboutTextFileBrowser() {
        final Dialog dialog = new Dialog(mActivity, mGp.screenTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.about_dialog);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.about_dialog_title_view);
        final TextView title = (TextView) dialog.findViewById(R.id.about_dialog_title);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        title.setTextColor(mGp.themeColorList.title_text_color);
        title.setText(getString(R.string.msgs_dlg_title_about) + " (Ver" + SystemInfo.getApplVersionName(mContext) + ")");

        final CustomTabLayout tab_layout = (CustomTabLayout) dialog.findViewById(R.id.tab_layout);
        tab_layout.addTab(mContext.getString(R.string.msgs_about_dlg_func_btn));
        tab_layout.addTab(mContext.getString(R.string.msgs_about_dlg_privacy_btn));
        tab_layout.addTab(mContext.getString(R.string.msgs_about_dlg_change_btn));

        tab_layout.adjustTabWidth();

        LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        int zf=100;

        LinearLayout ll_func = (LinearLayout) vi.inflate(R.layout.about_dialog_func, null);
        final WebView func_view = (WebView) ll_func.findViewById(R.id.about_dialog_function_view);
        func_view.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        func_view.setScrollbarFadingEnabled(false);
        setWebViewListener(func_view, zf);

        LinearLayout ll_privacy = (LinearLayout) vi.inflate(R.layout.about_dialog_privacy, null);
        final WebView privacy_view = (WebView) ll_privacy.findViewById(R.id.about_dialog_privacy_view);
        privacy_view.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        privacy_view.setScrollbarFadingEnabled(false);
        setWebViewListener(privacy_view, zf);

        LinearLayout ll_change = (LinearLayout) vi.inflate(R.layout.about_dialog_change, null);
        final WebView change_view = (WebView) ll_change.findViewById(R.id.about_dialog_change_view);
        change_view.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        change_view.setScrollbarFadingEnabled(false);
        setWebViewListener(change_view, zf);

        loadHelpFile(func_view, getString(R.string.msgs_dlg_title_about_func_desc));
        loadHelpFile(privacy_view, getString(R.string.msgs_dlg_title_about_privacy_desc));
        loadHelpFile(change_view, getString(R.string.msgs_dlg_title_about_change_desc));

        final CustomViewPagerAdapter adapter = new CustomViewPagerAdapter(mActivity,
                new WebView[]{func_view, privacy_view, change_view});
        final CustomViewPager viewPager = (CustomViewPager) dialog.findViewById(R.id.about_view_pager);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(3);
        viewPager.setSwipeEnabled(false);
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener(){
            @Override
            public void onPageSelected(int position) {
//                mUtil.addDebugMsg(2,"I","onPageSelected entered, pos="+position);
                tab_layout.getTabAt(position).select();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
//                mUtil.addDebugMsg(2,"I","onPageScrollStateChanged entered, state="+state);
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//		    	util.addDebugMsg(2,"I","onPageScrolled entered, pos="+position);
            }
        });

        tab_layout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
//                mUtil.addDebugMsg(2,"I","onTabSelected entered, state="+tab);
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
//                mUtil.addDebugMsg(2,"I","onTabUnselected entered, state="+tab);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
//                mUtil.addDebugMsg(2,"I","onTabReselected entered, state="+tab);
            }

        });

        final Button btnOk = (Button) dialog.findViewById(R.id.about_dialog_btn_ok);

        CommonDialog.setDlgBoxSizeLimit(dialog, true);

        // OKボタンの指定
        btnOk.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        // Cancelリスナーの指定
        dialog.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                btnOk.performClick();
            }
        });

        dialog.show();
	};

	public String getApplVersionName() {
		try {
		    String packegeName = getPackageName();
		    PackageInfo packageInfo = getPackageManager().getPackageInfo(packegeName, PackageManager.GET_META_DATA);
		    return packageInfo.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			return "Unknown";
		}
	};

	public void invokeSettings() {
		mUtil.addDebugMsg(1, "I", "invokeSettings Entered");
		Intent intent = new Intent(mContext,SettingsActivity.class);
		startActivityForResult(intent,0);
	}

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		mUtil.addDebugMsg(1, "I", "Return from settings");
		if (requestCode==0) applySettingParms();
    }

	private void applySettingParms() {
		int lb= mGp.settingLineBreak;
		int tab_stop= mGp.settingTabStop;
		String fn= mGp.settingFontFamily;
		String fs= mGp.settingFontStyle;
		String sz= mGp.settingFontSize;
		boolean t_sl= mGp.settingShowLineno;
		String prev_bgc=mGp.settingTextAreaBackgroundColor;

		int prev_theme= mGp.screenTheme;

		boolean prev_divider_line= mGp.settingShowDivederLine;

		boolean debug_enabled= mGp.debugEnabled;

		mGp.loadSettingParms(mContext);
		
		if (!mGp.settingFontFamily.equals(fn) ||
				!mGp.settingFontStyle.equals(fs) ||
				!mGp.settingFontSize.equals(sz) ||
				mGp.settingLineBreak!=lb ||
				mGp.settingShowLineno!=t_sl ||
				mGp.settingShowDivederLine!=prev_divider_line ||
				tab_stop!= mGp.settingTabStop) {
			ViewedFileListItem vfli=null;
			for (int i = 0; i< mGp.viewedFileList.size(); i++) {
				vfli= mGp.viewedFileList.get(i);
    			if (mGp.settingLineBreak!=lb) vfli.lineBreak= mGp.settingLineBreak;
    			if (mGp.settingShowLineno!=t_sl) vfli.showLineNo= mGp.settingShowLineno;
			}
			ViewedFileListItem vf=getViewedFileListItem(mGp.currentViewedFile);
			FileViewerFragment fvf=(FileViewerFragment)vf.file_view_fragment;
//			fvf.setFileViewerParameter(vf.viewd_file);
			fvf.rebuildTextListAdapter(false);
		}

		if (!prev_bgc.equals(mGp.settingTextAreaBackgroundColor)) {
		    mGp.applyTextAreaBackGroundColor();
		    for(ViewedFileListItem vfli:mGp.viewedFileList) {
		        if (vfli!=null && vfli.file_view_fragment!=null) vfli.file_view_fragment.reloadScreen();
            }
        }

		if (prev_theme!= mGp.screenTheme) {
			mGp.screenTheme=prev_theme;
			mCommonDlg.showCommonDialog(false, "W",
					mContext.getString(R.string.msgs_text_browser_theme_changed_msg),
					null, null);
            if (mGp.screenTheme==R.style.MainLight) mGp.setTextAreaBackGroundColor(mContext, TEXT_AREA_BACKGROUND_COLOR_DARK);
            else mGp.setTextAreaBackGroundColor(mContext, TEXT_AREA_BACKGROUND_COLOR_LIGHT);
		}
		
	};
	
	private void setTextAreaBackgroundColor() {
        if (mGp.settingTextAreaBackgroundColor.startsWith("#")) {
            if (mGp.settingTextAreaBackgroundColor.length()==7) {
                try {
                    mGp.themeColorList.text_background_color=(int)Long.parseLong("FF"+mGp.settingTextAreaBackgroundColor.substring(1), 16);
//                    log.info("bgc="+String.format("%08h",mGp.themeColorList.text_background_color));
                } catch(Exception e) {

                }
            }
        }
    }
	private CommonFileSelector2 mFileSelectorDialogFragment=null;

    private void showFileSelectDialog() {
        boolean enableCreate=false;
        String title=mContext.getString(R.string.msgs_text_browser_file_select_file);
        String filename="";
        String lurl=LocalMountPoint.getExternalStorageDir();
        String ldir="";

        NotifyEvent ntfy=new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener(){
            @Override
            public void positiveResponse(Context c, Object[] o) {
                Uri uri=((Uri)o[0]);
                SafFile3 in_file=new SafFile3(mContext, uri);
                if (in_file.exists()) {
                    String fid = null;
                    if (uri.getPath().lastIndexOf(".") > 0) {
                        fid = uri.getPath().substring(uri.getPath().lastIndexOf(".") + 1, uri.getPath().length()).toLowerCase();
                    }

                    String mt= MimeTypeMap.getSingleton().getMimeTypeFromExtension(fid);
                    String mt_from_system=mt;
                    mUtil.addDebugMsg(1, "I", "mime_type from system="+mt+", fid="+fid);
                    if (mt==null) {
                        if (fid!=null) {
                            if (fid.equals("log")) mt="text/plain";
                            else if ((fid.equals("m3u") || fid.equals("m3u8"))) mt="application/vnd.apple.mpegurl";
                        }
                    }

                    if (!isFileAlreadyViewed(in_file)) {
                        addFileToViewedFileList(true, null, in_file);
                    } else {
                        showFileByViewedFileList(in_file);
                    }
                    refreshOptionMenu();
                    mFileSelectorDialogFragment=null;
                } else {
                    NotifyEvent ntfy_file_not_found=new NotifyEvent(mContext);
                    ntfy_file_not_found.setListener(new NotifyEventListener() {
                        @Override
                        public void positiveResponse(Context context, Object[] objects) {
                            if (getViewedFileListCount()==0) {
                                mTerminateApplication=true;
                                finish();
                            }
                            mFileSelectorDialogFragment=null;
                        }

                        @Override
                        public void negativeResponse(Context context, Object[] objects) {

                        }
                    });
                    mCommonDlg.showCommonDialog(false, "W",
                            mContext.getString(R.string.msgs_text_browser_file_select_error_title),
                            String.format(mContext.getString(R.string.msgs_text_browser_file_select_error_msg),uri.getPath()),
                            ntfy_file_not_found);
                }
            }
            @Override
            public void negativeResponse(Context c, Object[] o) {
                if (getViewedFileListCount()==0) {
                    mTerminateApplication=true;
                    finish();
                }
                mFileSelectorDialogFragment=null;
            }
        });
        if (mFileSelectorDialogFragment==null) {
            boolean scoped_storage_mode=mGp.safMgr.isScopedStorageMode();
            mFileSelectorDialogFragment=
                    CommonFileSelector2.newInstance(scoped_storage_mode,
                            enableCreate, false, DIALOG_SELECT_CATEGORY_FILE, true, true, lurl, ldir, filename, title);
            mFileSelectorDialogFragment.showDialog(mGp.debugEnabled, getSupportFragmentManager(), mFileSelectorDialogFragment, ntfy);
        }
    };

    private boolean mEnableFileSelection=false;
	final public void setUiEnabled() {
		mEnableFileSelection=true;
		mViewedFileListSpinner.setEnabled(true);
	};

	final public void setUiDisabled() {
		mEnableFileSelection=false;
		mViewedFileListSpinner.setEnabled(false);
	};

	private void setViewedFilleSelectorListener() {
        mViewedFileListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int position, long arg3) {
				SafFile3 prev= mGp.currentViewedFile;
				if (mEnableFileSelection) {
					mGp.currentViewedFile= mGp.viewedFileList.get(position).viewd_file;
			        showFileByViewedFileList(mGp.currentViewedFile);
//			    	NotificationUtil.showOngoingNotificationMsg(mContext, mGp.commonNotification, mGp.currentViewedFile.uri.getPath());

					mUtil.addDebugMsg(1, "I", "ViewedFile was seleced, pos="+position+ ", prev="+prev+", new="+ mGp.currentViewedFile);
				} else {
					mUtil.addDebugMsg(1, "I", "ViewedFile selection ignored, pos="+position+ ", prev="+prev+", new="+ mGp.currentViewedFile);
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
        });

	};

    private void cleanupCacheFile() {
        File[] fl=mContext.getExternalCacheDirs();
        if (fl!=null && fl.length>0) {
            for(File cf:fl) {
                if (cf!=null) {
                    File[] child_list=cf.listFiles();
                    if (child_list!=null) {
                        for(File ch_item:child_list) {
                            if (ch_item!=null) {
                                if (!deleteCacheFile(ch_item)) break;
                            }
                        }
                    }
                }
            }
        } else {
            fl=mContext.getExternalCacheDirs();
        }
    }

    private boolean deleteCacheFile(File del_item) {
        boolean result=true;
        if (del_item.isDirectory()) {
            File[] child_list=del_item.listFiles();
            for(File child_item:child_list) {
                if (child_item!=null) {
                    if (!deleteCacheFile(child_item)) {
                        result=false;
                        break;
                    }
                }
            }
            if (result) result=del_item.delete();
        } else {
            result=del_item.delete();
        }
        return result;
    }

    private void showPrivacyPolicy() {
        final Dialog dialog = new Dialog(mActivity, mGp.screenTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.privacy_policy_dlg);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.privacy_polycy_dlg_title_view);
        final TextView title = (TextView) dialog.findViewById(R.id.privacy_polycy_dlg_title);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        title.setTextColor(mGp.themeColorList.title_text_color);
        title.setText(getString(R.string.msgs_about_dlg_privacy_btn));

        final WebView privacy_view = (WebView) dialog.findViewById(R.id.privacy_polycy_dlg_webview);
        privacy_view.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        loadHelpFile(privacy_view, getString(R.string.msgs_dlg_title_about_privacy_desc));
        setWebViewListener(privacy_view, 100);

        final Button btnOk = (Button) dialog.findViewById(R.id.privacy_polycy_dlg_close);

        CommonDialog.setDlgBoxSizeLimit(dialog, true);

        // OKボタンの指定
        btnOk.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        // Cancelリスナーの指定
        dialog.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                btnOk.performClick();
            }
        });

        dialog.show();
    }

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

    private void loadHelpFile(final WebView web_view, String fn) {
        final Handler hndl=new Handler();
        Thread th1=new Thread(){
            @Override
            public void run() {
                String html=convertMakdownToHtml(mContext, fn);
                final String b64= Base64.encodeToString(html.getBytes(), Base64.DEFAULT);
                hndl.post(new Runnable(){
                    @Override
                    public void run() {
//                        web_view.loadData(html_func, "text/html; charset=UTF-8", null);
                        web_view.loadData(b64, null, "base64");
                    }
                });
            }
        };
        th1.start();
    }

    private void setWebViewListener(WebView wv, int zf) {
        wv.getSettings().setTextZoom(zf);
//        wv.getSettings().setBuiltInZoomControls(true);
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading (WebView view, String url) {
                return false;
            }
        });
        wv.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event){
                if(event.getAction() == KeyEvent.ACTION_DOWN){
                    WebView webView = (WebView) v;
                    switch(keyCode){
                        case KeyEvent.KEYCODE_BACK:
                            if(webView.canGoBack()){
                                webView.goBack();
                                return true;
                            }
                            break;
                    }
                }
                return false;
            }
        });
    }

}
