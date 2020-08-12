package com.sentaroh.android.TextFileBrowser;

/*
The MIT License (MIT)
Copyright (c) 2011-2019 Sentaroh

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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.storage.StorageManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.sentaroh.android.TextFileBrowser.BuildConfig;

import com.sentaroh.android.Utilities3.AppUncaughtExceptionHandler;
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
import com.sentaroh.android.Utilities3.ThemeUtil;
import com.sentaroh.android.Utilities3.ThreadCtrl;
import com.sentaroh.android.Utilities3.Zip.ZipUtil;

import static com.sentaroh.android.TextFileBrowser.Constants.*;
import static com.sentaroh.android.TextFileBrowser.LogWriter.LOG_FILE_NAME_ARCHIVE_PREFIX;
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

	@Override  
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		log.debug("onSaveInstanceState entered");
        saveViewContents(outState);
	};

	@Override
	protected void onRestoreInstanceState(Bundle savedState) {  
		super.onRestoreInstanceState(savedState);
		log.debug("onRestoreInstanceState entered");
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
		log.debug("onNewIntent entered, restartStatus="+mRestartStatus);
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

        log.debug("onCreate entered, SDK="+ Build.VERSION.SDK_INT+", Appl="+getApplVersionName());

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
//		mViewedFileListAdapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
//        mViewedFileListAdapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
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
            mGp.logFlush();
            mGp.createCrashReport("UncaughtException detected\n"+strace);
        }
    };

    @Override
	public void onStart() {
		super.onStart();
		log.debug("onStart entered");
	};

	@Override
	public void onRestart() {
		super.onStart();
		log.debug("onRestart entered");
        mGp.commonNotification.notificationManager.cancelAll();
	};

	@Override
	public void onResume() {
		super.onResume();
		log.debug("onResume entered, restartStatus="+mRestartStatus);
		if (mRestartStatus==0) {
	    	Intent in=getIntent();
            if (in==null || in.getData()==null) {
		    	NotificationUtil.initNotification(mContext, mGp.commonNotification);
		    	NotifyEvent ntfy=new NotifyEvent(mContext);
		    	ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        showFileSelectDialog();
                    }
                    @Override
                    public void negativeResponse(Context context, Object[] objects) {}
                });
                if (checkLegacyStoragePermissions(ntfy)) ntfy.notifyToListener(true, null);
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
//				mViewedFileListAdapter.notifyDataSetChanged();
                if (i==mSavedViewedFileListSpinnerPosition)
                    mGp.currentViewedFile=vfli.viewd_file;
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
        log.debug("received flag="+String.format("0x%8x", flag)+", intent="+in.getData().toString());
        try {
            final SafFile3 in_file=new SafFile3(mContext, in.getData());
            InputStream is=in_file.getInputStreamByUri();
            NotifyEvent ntfy=new NotifyEvent(mContext);
            ntfy.setListener(new NotifyEventListener() {
                @Override
                public void positiveResponse(Context context, Object[] objects) {
                    if (!isFileAlreadyViewed(in_file)) {
                        addFileToViewedFileList(true, in_file);
                        showFileByViewedFileList(in_file);
                    } else {
                        showFileByViewedFileList(in_file);
                    }
                }
                @Override
                public void negativeResponse(Context context, Object[] objects) {}
            });
            if (checkLegacyStoragePermissions(ntfy)) ntfy.notifyToListener(true, null);
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
		log.debug("isFileAlreadyViewed, Uri="+in_file.getPath()+", result="+result);
		return result;
	};

	private int getViewedFileListCount() {
		int result=0;
		if (mGp.viewedFileList!=null) result= mGp.viewedFileList.size();
		log.debug("getViewedFileListCount, result="+result);
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
        log.debug("isTextMimeType result="+result+", MimeType="+mime_type);
	    return result;
    }
	private void addFileToViewedFileList(boolean set_selection, SafFile3 in_file) {
		log.debug("addFileToViewedFileList, fp="+in_file.getPath());
		ViewedFileListItem vfli=new ViewedFileListItem();

		if (mGp.settingShowAllFileAsText || (isTextMimeTypex(in_file.getMimeType()))) vfli.browseMode=FileViewerAdapter.TEXT_BROWSER_BROWSE_MODE_CHAR;
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
		log.debug("removeFileFromViewedFileList, fp="+fp+", result="+rem_pos);
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
		if (in_file!=null) log.debug("getViewedFileListItem, fp="+in_file.getPath()+", result="+pos);
		else log.debug("getViewedFileListItem, fp=null");
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
				break;
			}
		}
		log.debug("showFileByViewedFileList, fp="+in_file.getPath()+", result="+pos);
	};

	@Override
	public void onPause() {
		super.onPause();
		log.debug("onPause entered");
		mGp.logFlush();
        // Application process is follow
	};

	@Override
	public void onStop() {
		super.onStop();
		log.debug("onStop entered");
        // Application process is follow
        if (mGp.currentViewedFile!=null)
            NotificationUtil.showOngoingNotificationMsg(mContext, mGp.commonNotification, mGp.currentViewedFile.getPath());
        mGp.logFlush();
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		log.debug("onDestroy entered");
        // Application process is follow
		if (mTerminateApplication) {
	    	NotificationUtil.clearNotification(mContext, mGp.commonNotification);
//			deleteTaskData();
            mGp.logClose();
            cleanupCacheFile();
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
                if (vfli.searchEnabled && vfli.file_view_fragment!=null) {
                    vfli.file_view_fragment.switchFindWidget();
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
		log.debug("onCreateOptionsMenu Entered");
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_tb_setting, menu);
		return true;
	};
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		log.debug("onPrepareOptionsMenu Entered");
        super.onPrepareOptionsMenu(menu);
		ViewedFileListItem vf=getViewedFileListItem(mGp.currentViewedFile);
        CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_open), true);
        CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_clear_cache), true);

        if (SafManager3.buildStoragePermissionRequiredList(mContext).size()>0) menu.findItem(R.id.menu_tb_storage_permission).setVisible(true);
        else menu.findItem(R.id.menu_tb_storage_permission).setVisible(false);

        if (vf!=null) {
	        if (vf.browseMode==FileViewerAdapter.TEXT_BROWSER_BROWSE_MODE_CHAR) {
	        	menu.findItem(R.id.menu_tb_mode_swicth)
	        	.setTitle(getString(R.string.msgs_tb_menu_mode_switch_hex))
	        	.setIcon(R.drawable.ic_32_hex);
				menu.findItem(R.id.menu_tb_settings).setVisible(true);
				menu.findItem(R.id.menu_tb_mode_swicth).setVisible(true);
				menu.findItem(R.id.menu_tb_reload).setVisible(true);
				menu.findItem(R.id.menu_tb_open).setVisible(true);
				menu.findItem(R.id.menu_tb_find).setVisible(true);
				menu.findItem(R.id.menu_tb_about).setVisible(true);
	        } else {
	        	menu.findItem(R.id.menu_tb_mode_swicth)
	        	.setTitle(getString(R.string.msgs_tb_menu_mode_switch_char))
	        	.setIcon(R.drawable.ic_32_text);
				menu.findItem(R.id.menu_tb_settings).setVisible(true);
				menu.findItem(R.id.menu_tb_mode_swicth).setVisible(true);
				menu.findItem(R.id.menu_tb_reload).setVisible(true);
				menu.findItem(R.id.menu_tb_open).setVisible(true);
				menu.findItem(R.id.menu_tb_find).setVisible(false);
				menu.findItem(R.id.menu_tb_about).setVisible(true);
	        }
			if (vf.tc_view.isThreadActive()) {
				CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_settings), false);
				CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_mode_swicth), false);
				CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_reload), false);
				CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_open), false);
//				CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_open).setEnabled(false);
				CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_find), false);
				CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_about), false);
			} else {
				CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_settings), true);
				CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_mode_swicth), true);
				CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_reload), true);
				menu.findItem(R.id.menu_tb_open).setVisible(true);
//				menu.findItem(R.id.menu_tb_open).setEnabled(true);
				CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_find), true);
				CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_about), true);
			}
			if (!vf.viewd_file.exists()) {
                CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_mode_swicth), false);
                CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_find), false);
            }
		}

		if (mGp.isLogFileExists()) CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_send_log), true);
		else CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_send_log), false);

        if (mGp.isLogRemovableFileExists()) CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_remove_log_file), true);
        else CommonDialog.setMenuItemEnabled(mActivity, menu, menu.findItem(R.id.menu_tb_remove_log_file), false);

        return true;
	};

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		log.debug("onOptionsItemSelected Entered");
		ViewedFileListItem vf=getViewedFileListItem(mGp.currentViewedFile);
		FileViewerFragment fvf=(FileViewerFragment)vf.file_view_fragment;
		if (item.getItemId()== R.id.menu_tb_settings) {
			invokeSettings();
			refreshOptionMenu();
		} else if (item.getItemId()== R.id.menu_tb_open) {
            showFileSelectDialog();
		} else if (item.getItemId()== R.id.menu_tb_reload) {
			fvf.reloadFile();
		} else if (item.getItemId()== R.id.menu_tb_mode_swicth) {
			fvf.switchDisplayMode();
			refreshOptionMenu();
		} else if (item.getItemId()== R.id.menu_tb_find) {
			fvf.switchFindWidget();
			refreshOptionMenu();
        } else if (item.getItemId()== R.id.menu_tb_storage_permission) {
            requestStoragePermissions(REQUEST_CODE_EXTERNAL_STORAGE_ACCESS_PERMISSION);
		} else if (item.getItemId()== R.id.menu_tb_about) {
			aboutTextFileBrowser();
		} else if (item.getItemId()== R.id.menu_tb_exit) {
			confirmExit();
        } else if (item.getItemId()== R.id.menu_tb_send_log) {
            sendLogFile();
        } else if (item.getItemId()== R.id.menu_tb_clear_cache) {
            clearCache();
        } else if (item.getItemId()== R.id.menu_tb_remove_log_file) {
            removeLogFile();
		}
		return false;
	};

	private void clearCache() {
        IndexedFileReader.removeIndexCache(mContext);
	    mCommonDlg.showCommonDialog(false, "W", "File index cache file was removed, restart the app.", "",null);
    }

    private void removeLogFile() {
	    NotifyEvent ntfy=new NotifyEvent(mContext);
	    ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                mGp.logRotate();
                mGp.logRemoveFile();
            }

            @Override
            public void negativeResponse(Context context, Object[] objects) {

            }
        });
        mCommonDlg.showCommonDialog(true, "W", "Do you wants to delete existing log file.", "", ntfy);
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

    private final static String MAIL_TO="gm.developer.fhoshino@gmail.com";
    private void sendLogFile() {
        final String zip_file_name=getExternalCacheDir().getPath()+"/log.zip";
        mGp.logRotate();

        File log_dir=new File(mGp.logGetLogDirectory());
        File[] log_fl=log_dir.listFiles();
        ArrayList<String>log_list=new ArrayList<String>();
        File cr_file=new File(log_dir+"/crash_report.txt");
        if (cr_file.exists()) log_list.add(cr_file.getPath());
        for(File item:log_fl) {
            if (item.getName().startsWith(LOG_FILE_NAME_ARCHIVE_PREFIX))
                log_list.add(item.getPath());
        }
        if (log_list.size()==0) {
            MessageDialogFragment mdf =MessageDialogFragment.newInstance(false, "W",
                    "No log files founds.",
                    "");
            mdf.showDialog(getSupportFragmentManager(), mdf, null);
            return;
        }

        final String[] file_name=log_list.toArray(new String[log_list.size()]);
        final ThreadCtrl tc=new ThreadCtrl();
        NotifyEvent ntfy=new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener(){
            @Override
            public void positiveResponse(Context c, Object[] o) {
            }
            @Override
            public void negativeResponse(Context c, Object[] o) {
                tc.setDisabled();
            }
        });

        final ProgressBarDialogFragment pbdf=ProgressBarDialogFragment.newInstance(
                mContext.getString(R.string.msgs_log_file_list_dlg_send_zip_file_creating),
                "",
                mContext.getString(R.string.msgs_common_dialog_cancel),
                mContext.getString(R.string.msgs_common_dialog_cancel));
        final FragmentManager fm=getSupportFragmentManager();
        pbdf.showDialog(fm, pbdf, ntfy,true);
        final Handler hndl=new Handler();
        Thread th=new Thread() {
            @Override
            public void run() {
                File lf=new File(zip_file_name);
                lf.delete();
//                String[] file_name=new String[]{mGp.logFile.getPath()};
                String[] lmp= LocalMountPoint.convertFilePathToMountpointFormat(mContext, file_name[0]);
                ZipUtil.createZipFile(mContext, tc,pbdf,zip_file_name,lmp[0],file_name);
                if (tc.isEnabled()) {
                    Intent intent=new Intent();
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setAction(Intent.ACTION_SEND);
//				    intent.setType("message/rfc822");
//				    intent.setType("text/plain");
                    intent.setType("application/zip");
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{MAIL_TO});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "TextFileBrowser log file");
                    Uri uri= FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".provider",lf);
                    intent.putExtra(Intent.EXTRA_STREAM, uri);///Uri.fromFile(lf));
                    mActivity.startActivity(intent);
                } else {
                    lf.delete();
                    MessageDialogFragment mdf =MessageDialogFragment.newInstance(false, "W",
                            mContext.getString(R.string.msgs_log_file_list_dlg_send_zip_file_cancelled), "");
                    mdf.showDialog(fm, mdf, null);
                }
                pbdf.dismiss();
            };
        };
        th.start();
    };

    private void aboutTextFileBrowser() {
		mCommonDlg.showCommonDialog(false, "I", 
				getString(R.string.msgs_tb_menu_about), String.format(
				getString(R.string.msgs_text_browser_about_tb),getApplVersionName()), 
				null);
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
		log.debug("invokeSettings Entered");
		Intent intent = new Intent(mContext,SettingsActivity.class);
		startActivityForResult(intent,0);
	}

	private SafManager3.StorageVolumeInfo mPrimaryStorageVolume=null;
	private void requestInternalStoragePermission() {
        NotifyEvent ntfy_term=new NotifyEvent(mContext);
        ntfy_term.setListener(new NotifyEventListener(){
            @Override
            public void positiveResponse(Context c, Object[] o) {
                Intent intent=mPrimaryStorageVolume.volume.createOpenDocumentTreeIntent();
                startActivityForResult(intent, REQUEST_CODE_PRIMARY_STORAGE_ACCESS_REQUEST);
            }
            @Override
            public void negativeResponse(Context c, Object[] o) {
                showInternalStoragePermissionDenyMessage();
            }
        });
        mCommonDlg.showCommonDialog(true, "W",
                mContext.getString(R.string.msgs_main_permission_internal_storage_title),
                mContext.getString(R.string.msgs_main_permission_internal_storage_request_msg), ntfy_term);

    }

    private final int REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE=1;
    @SuppressLint("NewApi")
    private boolean checkLegacyStoragePermissions(final NotifyEvent p_ntfy) {
        if (Build.VERSION.SDK_INT>=23 && Build.VERSION.SDK_INT<=30) {
            log.debug("Prermission WriteExternalStorage="+checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)+
                    ", WakeLock="+checkSelfPermission(Manifest.permission.WAKE_LOCK));
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                NotifyEvent ntfy=new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener(){
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        mNotifyStorageAccessPermitted=p_ntfy;
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
                    }
                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                        showInternalStoragePermissionDenyMessage();
                    }
                });
                mCommonDlg.showCommonDialog(false, "W",
                        mContext.getString(R.string.msgs_main_permission_internal_storage_title),
                        mContext.getString(R.string.msgs_main_permission_internal_storage_request_msg), ntfy);
            } else {
                return true;
            }
        } else {
            return true;
        }
        return false;
    };

    private NotifyEvent mNotifyStorageAccessPermitted=null;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE == requestCode) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mNotifyStorageAccessPermitted!=null) {
                    final NotifyEvent ntfy=mNotifyStorageAccessPermitted;
                    Handler hndl=new Handler();
                    hndl.postDelayed(new Runnable(){
                        @Override
                        public void run() {
                            ntfy.notifyToListener(true, null);
                        }
                    },100);
                }
                mNotifyStorageAccessPermitted=null;
            } else {
                showInternalStoragePermissionDenyMessage();
            }
        }
    }

    private final static int REQUEST_CODE_PRIMARY_STORAGE_ACCESS_REQUEST =40;
    private final static int REQUEST_CODE_EXTERNAL_STORAGE_ACCESS_PERMISSION =41;
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		log.debug("Return from settings");
		if (requestCode==0) applySettingParms();
        else if (requestCode == REQUEST_CODE_PRIMARY_STORAGE_ACCESS_REQUEST || requestCode == REQUEST_CODE_EXTERNAL_STORAGE_ACCESS_PERMISSION) {
            if (resultCode == Activity.RESULT_OK) {
                log.debug("Storage picker action="+data.getAction()+", path="+data.getData().getPath());
                if (mGp.safMgr.isRootTreeUri(data.getData())) {
                    mGp.safMgr.addUuid(data.getData());
                    mGp.safMgr.refreshSafList();
                    if (requestCode == REQUEST_CODE_PRIMARY_STORAGE_ACCESS_REQUEST) showFileSelectDialog();
                } else {
                    NotifyEvent ntfy=new NotifyEvent(mContext);
                    ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                        @Override
                        public void positiveResponse(Context context, Object[] objects) {
                            if (requestCode == REQUEST_CODE_PRIMARY_STORAGE_ACCESS_REQUEST) {
                                requestInternalStoragePermission();
                            } else {
                                requestStoragePermissions(requestCode);
                            }
                        }
                        @Override
                        public void negativeResponse(Context context, Object[] objects) {
                            showInternalStoragePermissionDenyMessage();
                        }
                    });
                    if (requestCode == REQUEST_CODE_PRIMARY_STORAGE_ACCESS_REQUEST) {
                        mCommonDlg.showCommonDialog(true, "W", mContext.getString(R.string.msgs_main_permission_internal_storage_reselect_msg),
                                data.getData().getPath(), ntfy);
                    } else {
                        mCommonDlg.showCommonDialog(true, "W", mContext.getString(R.string.msgs_main_permission_external_storage_reselect_msg),
                                data.getData().getPath(), ntfy);
                    }
                }
            } else {
                if (requestCode == REQUEST_CODE_PRIMARY_STORAGE_ACCESS_REQUEST) {
                    showInternalStoragePermissionDenyMessage();
                }
            }
        }
    }

    private void showInternalStoragePermissionDenyMessage() {
        NotifyEvent ntfy_deny_internal=new NotifyEvent(mContext);
        ntfy_deny_internal.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                finish();
            }
            @Override
            public void negativeResponse(Context context, Object[] objects) {}
        });
        mCommonDlg.showCommonDialog(false, "W",
                mContext.getString(R.string.msgs_main_permission_internal_storage_title),
                mContext.getString(R.string.msgs_main_permission_internal_storage_denied_msg), ntfy_deny_internal);
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

		if (debug_enabled!= mGp.debugEnabled) {
		    if (mGp.debugEnabled) {
		        mGp.logInit(mContext);
            } else {
		        if (debug_enabled) {
                    mGp.logClose();
                    mGp.logInit(mContext);
                }
            }
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
                    log.debug("mime_type from system="+mt+", fid="+fid);
                    if (mt==null) {
                        if (fid!=null) {
                            if (fid.equals("log")) mt="text/plain";
                            else if ((fid.equals("m3u") || fid.equals("m3u8"))) mt="application/vnd.apple.mpegurl";
                        }
                    }

                    if (!isFileAlreadyViewed(in_file)) {
                        addFileToViewedFileList(true, in_file);
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

    private void requestInternalStoragePermissions(int req_code) {

    }
    private void requestStoragePermissions(int req_code) {
        NotifyEvent ntfy_request=new NotifyEvent(mContext);
        ntfy_request.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                ArrayList<String>uuid_list=(ArrayList<String>)objects[0];
                Intent intent = null;
                StorageManager sm = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
                ArrayList<SafManager3.StorageVolumeInfo>vol_list=SafManager3.getStorageVolumeInfo(mContext);
                for(String uuid:uuid_list) {
                    for(SafManager3.StorageVolumeInfo svi:vol_list) {
                        if (svi.uuid.equals(uuid)) {
                            if (Build.VERSION.SDK_INT>=24) {
                                if (mGp.safMgr.isScopedStorageMode()) {
                                    intent=svi.volume.createOpenDocumentTreeIntent();
                                    startActivityForResult(intent, req_code);
                                    break;
                                } else {
                                    if (!svi.uuid.equals(SafManager3.SAF_FILE_PRIMARY_UUID)) {
                                        if (Build.VERSION.SDK_INT>=29) intent=svi.volume.createOpenDocumentTreeIntent();
                                        else intent=svi.volume.createAccessIntent(null);
                                        startActivityForResult(intent, req_code);
                                        break;
                                    }
                                }
                            } else {
                                if (!svi.uuid.equals(SafManager3.SAF_FILE_PRIMARY_UUID)) {
                                    intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                                    startActivityForResult(intent, req_code);
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void negativeResponse(Context context, Object[] objects) {
            }
        });
        StoragePermission sp=new StoragePermission(mActivity, mGp.safMgr, mCommonDlg, ntfy_request);
        sp.showDialog();
    }

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

					log.debug("ViewedFile was seleced, pos="+position+ ", prev="+prev+", new="+ mGp.currentViewedFile);
				} else {
					log.debug("ViewedFile selection ignored, pos="+position+ ", prev="+prev+", new="+ mGp.currentViewedFile);
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

}
