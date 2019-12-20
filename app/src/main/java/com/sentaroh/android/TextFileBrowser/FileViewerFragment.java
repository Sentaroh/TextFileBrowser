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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.sentaroh.android.Utilities3.Dialog.CommonDialog;
import com.sentaroh.android.Utilities3.Dialog.MessageDialogFragment;
import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.StringUtil;
import com.sentaroh.android.Utilities3.ThreadCtrl;
import com.sentaroh.android.Utilities3.ContextMenu.CustomContextMenu;
import com.sentaroh.android.Utilities3.ContextMenu.CustomContextMenuItem.CustomContextMenuOnClickListener;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.renderscript.ScriptGroup;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileViewerFragment extends Fragment {
    private static Logger log = LoggerFactory.getLogger(FileViewerFragment.class);
	private final static String APPLICATION_TAG="FileViewerFragment";
	private GlobalParameters mGp =null;
	
//	private boolean mTerminateApplication=false;
	private int mRestartStatus=0;

	private ViewedFileListItem mViewedFile=null;

	private IndexedFileReader mIdxReader=null;
	private ThreadCtrl mTcIndexReader=null;
	
	private FragmentManager mFragmentManager=null;
	private Resources mResources=null;
	private Context mContext;
	private MainActivity mMainActivity=null; 
	private String mLastMsgText="";

	private ListView mTextListView=null;
	private FileViewerAdapter mTextListAdapter=null;
//	private int TextFileBlockSize=1024*2048;
    private TextView mTextDisplayArea=null;
	
	private View mMainView=null;
	private Handler mUiHandler=null;
	private SafFile3 mMainUriFile =null;
	private TextView mMainViewMsgArea;
	private ProgressBar mMainViewProgressBar;

	private Button mMainViewScrollLeft1, mMainViewScrollLeft2,
			mMainViewScrollRight1, mMainViewScrollRight2;
	private ThreadCtrl mTcScroll;
	private Thread mThScroll=null;
	private boolean mScrollActive=false;


//	public void setOptions(boolean debug, int mln) {
//		mGp.debugEnabled=debug;
//	}
	
	public static FileViewerFragment newInstance(String path) {
//		if (mDebugEnable) 
//		log.debug("newInstance fp="+filepath);
		FileViewerFragment frag = new FileViewerFragment();
        Bundle bundle = new Bundle();
        bundle.putString("Path", path);
        frag.setArguments(bundle);
        return frag;
    };
    
	public FileViewerFragment() {
//		log.debug("Constructor(Default)");
	};

	public void setEncodeName(String enc_name) {
		mViewedFile.encodeName=enc_name;
	}
	public String getEncodeName() {
		return mIdxReader.getCurrentEncodeName();//mViewedFile.encodeName;
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
		log.debug("onConfigurationChanged entered");
		reloadScreen();
	};

	public void reloadScreen() {
        saveViewAttributes();
        if (!mViewedFile.ix_reader_view.isIndexCreationFinished()) {
            mMainViewProgressBar.setVisibility(ProgressBar.VISIBLE);
//			mIdxReader.refreshParentResources(mMainViewProgressBar,mMainViewMsgArea);
        } else {
            buildTextListAdapter();
            if (mTextListAdapter!=null) {
                restoreViewAttributes();
                mTextListView.setBackgroundColor(mGp.themeColorList.text_background_color);
                mMainView.setBackgroundColor(mGp.themeColorList.text_background_color);
            }
        }
    }

	@Override
	public void onSaveInstanceState(Bundle outState) {  
		super.onSaveInstanceState(outState);
		log.debug("onSaveInstanceState entered");
		saveViewAttributes();
//		saveTaskData();
	};  

	private void saveViewAttributes() {
		log.debug("saveViewAttributes entered");
		if (mMainActivity.isApplicationTerminating()) return;
		mLastMsgText=mMainViewMsgArea.getText().toString();
		mViewedFile.listViewPos[0]=mTextListView.getFirstVisiblePosition();
		if (mTextListView.getChildAt(0)!=null) mViewedFile.listViewPos[1]=mTextListView.getChildAt(0).getTop();
//		mViewedFile.lastMsgText=mMainViewMsgArea.getText().toString();
		if (mTextListAdapter!=null) {
			mViewedFile.copyFrom=mTextListAdapter.getCopyBegin();
			mViewedFile.copyTo=mTextListAdapter.getCopyEnd();
			mViewedFile.horizontalPos=mTextListAdapter.getHorizontalPosition();
			mViewedFile.adapterFindString=mTextListAdapter.getFindString();
			mViewedFile.adapterFindPosition=mTextListAdapter.getFindPostition();
		}
		final EditText et_find_string = (EditText)mMainView.findViewById(R.id.activity_browser_main_search_text);
		mViewedFile.searchString=et_find_string.getText().toString();
	};
	
	private void restoreViewAttributes() {
	    if (mTextListAdapter!=null) {
            mTextListView.setSelectionFromTop(mViewedFile.listViewPos[0],mViewedFile.listViewPos[1]);
            mTextListAdapter.setCopyBegin(mViewedFile.copyFrom);
            mTextListAdapter.setCopyEnd(mViewedFile.copyTo);
            mTextListAdapter.setHorizontalPosition(mViewedFile.horizontalPos);

            mTextListAdapter.setFindPosition(mViewedFile.adapterFindPosition);
            mTextListAdapter.setFindString(mViewedFile.adapterFindString);

            final EditText et_find_string = (EditText)mMainView.findViewById(R.id.activity_browser_main_search_text);
            et_find_string.setText(mViewedFile.searchString);

            mMainViewMsgArea.setText(mLastMsgText);
            if (mLastMsgText.equals("")) {
                mMainViewMsgArea.setVisibility(TextView.GONE);
            } else {
                mMainViewMsgArea.setVisibility(TextView.VISIBLE);
            }
        }
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext=getActivity().getApplicationContext();
        mGp =GlobalWorkArea.getGlobalParameters(getActivity());
        log.debug("onCreate entered");
//        setRetainInstance(true);
        mUiHandler=new Handler();
        mMainActivity=(MainActivity) getActivity();
        mResources=getResources();
        mFragmentManager=getFragmentManager();
        mRestartStatus=0;

        String path=getArguments().getString("Path");
        for (ViewedFileListItem item: mGp.viewedFileList) {
            if (item.viewd_file.getPath().equals(path)) {
                mMainUriFile=item.viewd_file;
                break;
            }
        }

//        try {
//            InputStream is=mMainUriFile.getInputStream();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }

        if (mMainUriFile!=null) initViewWidget();
	};

    // Default uncaught exception handler variable
    private void initViewWidget() {
        boolean found=false;
        if (mGp.viewedFileList!=null) {
            for (int i = 0; i< mGp.viewedFileList.size(); i++) {
            	if (mMainUriFile.getPath().equals(mGp.viewedFileList.get(i).viewd_file.getPath())) {
            		found=true;
            		mViewedFile= mGp.viewedFileList.get(i);
            		mViewedFile.file_view_fragment=this;
//            		Log.v("","frag update="+mViewedFile.file_view_fragment);
            		mIdxReader=mViewedFile.ix_reader_view;
            		mTcIndexReader=mViewedFile.tc_view;
            		if (mViewedFile.viewerParmsInitRequired) {
            			mViewedFile.viewerParmsInitRequired=false;
//            			mViewedFile.browseMode=mGp.settingBrowseMode;
            			mViewedFile.lineBreak= mGp.settingLineBreak;
            			mViewedFile.showLineNo= mGp.settingShowLineno;
            			mViewedFile.encodeName="";
            		} else {
            			if (mViewedFile.viewerParmsRestoreRequired) mRestartStatus=0;
            			else mRestartStatus=2;
            		}
            		if (mGp.debugEnabled) {
            			log.debug("Viewer option:"+
            					" File path="+mViewedFile.viewd_file.getPath()+
            					", Browse mode="+mViewedFile.browseMode+
            					", Line break="+mViewedFile.lineBreak+
            					", Show line no="+mViewedFile.showLineNo+
            					", Encode name="+mViewedFile.encodeName);
            		}
            		break;
            	}
            }
        }
        if (!found){
        	log.debug("ViewedFile item not found, fp="+ mMainUriFile.getPath());
        	mFragmentManager.beginTransaction().remove(this).commit();
        }
	};

	private Drawable mDefaultDviderLine=null;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
		log.debug("onCreateView entered");
//        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        
		View v = inflater.inflate(R.layout.text_browser_file_view, container, false);
		
////		Context context = new ContextThemeWrapper(getActivity(), mGp.screenTheme);
//		mContext = new ContextThemeWrapper(getActivity(), mGp.screenTheme);
//		// clone the inflater using the ContextThemeWrapper
//		LayoutInflater localInflater = inflater.cloneInContext(mContext);
//		// inflate using the cloned inflater, not the passed in default	
//		View v=localInflater.inflate(R.layout.text_browser_file_view, container, false);
		
		mMainView=v;
        
//		if (Build.VERSION.SDK_INT>=14) getActionBar().setHomeButtonEnabled(false);

        mMainViewMsgArea=(TextView)v.findViewById(R.id.activity_browser_main_msg);
		mMainViewScrollRight1=(Button)v.findViewById(R.id.activity_browser_main_scroll_right1);
		mMainViewScrollRight2=(Button)v.findViewById(R.id.activity_browser_main_scroll_right2);
		mMainViewScrollLeft1=(Button)v.findViewById(R.id.activity_browser_main_scroll_left1);
		mMainViewScrollLeft2=(Button)v.findViewById(R.id.activity_browser_main_scroll_left2);

		mTextListView=(ListView)v.findViewById(R.id.activity_browser_main_list_view);
        mTextListView.setLongClickable(true);
        mTextListView.setFastScrollEnabled(false);
        if (mDefaultDviderLine==null) mDefaultDviderLine=mTextListView.getDivider();
		if (mGp.settingShowDivederLine) mTextListView.setDivider(mDefaultDviderLine);
		else mTextListView.setDivider(null);

        mTextListView.setBackgroundColor(mGp.themeColorList.text_background_color);
        mMainView.setBackgroundColor(mGp.themeColorList.text_background_color);

        mTextDisplayArea=(TextView) mMainView.findViewById(R.id.activity_browser_main_text_view);
        mTextDisplayArea.setLongClickable(true);


        mMainViewProgressBar=(ProgressBar)mMainView.findViewById(R.id.activity_browser_main_progress_bar);

		mTextListAdapter=null;

		return v;
	}
    
	@Override
	public void onStart() {
		super.onStart();
		log.debug("onStart entered");
	};

//	private void putThreadInfo() {
//		Log.v("","id="+Thread.currentThread().getId()+", name="+Thread.currentThread().getName());
//	}
	
	@Override
	public void onResume() {
		super.onResume();
		log.debug("onResume entered, restartStatus="+mRestartStatus);
		if (mViewedFile==null) return;
		
		if (mRestartStatus==0) {
			NotifyEvent ntfy=new NotifyEvent(mContext);
			ntfy.setListener(new NotifyEventListener(){
				@Override
				public void positiveResponse(final Context c, final Object[] o) {
					if (mMainActivity.isApplicationTerminating()) return;
					buildTextListAdapter();
					if (mViewedFile.viewerParmsRestoreRequired) {
						restoreViewAttributes();
					} else {
						if (mIdxReader.getCharModeLineCount()==0) {
							mMainViewMsgArea.setVisibility(TextView.VISIBLE);
							mMainViewMsgArea.setText(c.getString(R.string.msgs_text_browser_ir_file_empty));
						} else {
							mMainViewMsgArea.setText("");
							mMainViewMsgArea.setVisibility(TextView.GONE);
						}
					}
					mViewedFile.viewerParmsRestoreRequired=false;
					mMainActivity.refreshOptionMenu();
//					setInitViewPosition();
                }
				@Override
				public void negativeResponse(Context c, final Object[] o) {
					if (mMainActivity.isApplicationTerminating()) return;
					if (mIdxReader!=null) {
						buildTextListAdapter();
						mMainViewMsgArea.setVisibility(TextView.VISIBLE);
						mMainViewMsgArea.setText((String)o[0]);
					} else {
						mMainViewMsgArea.setVisibility(TextView.VISIBLE);
						mMainViewMsgArea.setText("FileViewer not found error, restart browser.");
					}
					mMainActivity.refreshOptionMenu();
//					setInitViewPosition();
				}
			});
			if (mIdxReader!=null) createFileIndexList(mMainUriFile, ntfy);
			else ntfy.notifyToListener(false, null);
		} else if (mRestartStatus==1) {
            if (!mMainUriFile.exists()) {
                mMainViewMsgArea.post(new Runnable() {
                    @Override
                    public void run() {
                        mMainViewMsgArea.setVisibility(TextView.VISIBLE);
                        mMainViewMsgArea.setText(mContext.getString(R.string.msgs_text_browser_file_does_not_exist));
                    }
                });
                mTextListView.setAdapter(null);
                mTextListAdapter=null;
                mMainActivity.refreshOptionMenu();
            }
		} else if (mRestartStatus==2) {
//			Log.v("","search="+mViewedFile.searchEnabled+", aa="+mViewedFile.browseMode);
			buildTextListAdapter();
			restoreViewAttributes();
            mMainActivity.refreshOptionMenu();
//			setInitViewPosition();
		}
		mRestartStatus=1;

        mTextListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                log.debug("mTextListView click detected, pos="+pos);
                if (mTextListAdapter.getCopyBegin()!=-1) {
                    if (mTextListAdapter.isCopyActive()) {
                        int s_pos=0,e_pos=0;
                        if (mTextListAdapter.getCopyBegin()>mTextListAdapter.getCopyEnd()) {
                            s_pos=mTextListAdapter.getCopyEnd();
                            e_pos=mTextListAdapter.getCopyBegin();
                        } else if (mTextListAdapter.getCopyBegin()==mTextListAdapter.getCopyEnd()) {
                            s_pos=e_pos=mTextListAdapter.getCopyEnd();
                        } else if (mTextListAdapter.getCopyBegin()<mTextListAdapter.getCopyEnd()) {
                            e_pos=mTextListAdapter.getCopyEnd();
                            s_pos=mTextListAdapter.getCopyBegin();
                        }
                        if (pos>=s_pos && pos<=e_pos) {
                            final TextView main_view_msg=(TextView)mMainView.findViewById(R.id.activity_browser_main_msg);
                            String msg=copyToClipboard(s_pos, e_pos, mTextListAdapter);
                            CommonDialog.showToastShort(mMainActivity, msg);
                            main_view_msg.setVisibility(TextView.GONE);
                            main_view_msg.setText("");
                            mTextListAdapter.resetCopy();
                            mTextListAdapter.notifyDataSetChanged();
                        } else {
                            mTextListAdapter.setCopyEnd(pos);
                            mTextListAdapter.notifyDataSetChanged();
                        }
                    } else {
                        if (mTextListAdapter.getCopyBegin()==pos) {
                            final TextView main_view_msg=(TextView)mMainView.findViewById(R.id.activity_browser_main_msg);
                            String msg=copyToClipboard(pos, pos, mTextListAdapter);
                            CommonDialog.showToastShort(mMainActivity, msg);
                            main_view_msg.setVisibility(TextView.GONE);
                            main_view_msg.setText("");
                            mTextListAdapter.resetCopy();
                            mTextListAdapter.notifyDataSetChanged();
                        } else {
                            mTextListAdapter.setCopyEnd(pos);
                            mTextListAdapter.notifyDataSetChanged();
                        }
                    }
                } else {
//                    String msg=copyToClipboard(pos, pos, mTextListAdapter);
//                    Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
                }
            }
        });

        mTextListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
			    log.debug("mTextListView long click detected, pos="+pos);
				createCcmenu(mTextListView,mTextListAdapter, pos);
				return true;
			}
		});

        mTextDisplayArea.setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public boolean onLongClick(View v) {
                log.debug("mTextDisplayArea long click detected");
                createCcmenu(mTextListView,mTextListAdapter, 0);
                return false;
            }
        });
		setFindStringListener();
		
		setScrollButtonListener();
	};

//	private void setInitViewPosition() {
//		if (Build.VERSION.SDK_INT==19) {
//			mUiHandler.postDelayed(new Runnable(){
//				@Override
//				public void run() {
//					mTextListView.setSelection(0);
//					mTextListView.setSelectionFromTop(mViewedFile.listViewPos[0],mViewedFile.listViewPos[1]);
//				}
//			}, 500);
//		}
//	};
	
	@Override
	public void onPause() {
		super.onPause();
		log.debug("onPause entered");
		if (mViewedFile==null) return;
        // Application process is follow
	};

	@Override
	public void onStop() {
		super.onStop();
		log.debug("onStop entered");
		if (mViewedFile==null) return;
        // Application process is follow
		saveViewAttributes();
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		log.debug("onDestroy entered");
        // Application process is follow
		if (mViewedFile!=null) mViewedFile.file_view_fragment=null;
//		if (mTerminateApplication) {
//			mTextListAdapter=null;
//		} else {
//			
//		}
	};
	
	public void switchDisplayMode() {
		switchDisplayMode(mViewedFile.browseMode);
	};

	private void switchDisplayMode(int c_mode) {
		if (c_mode==FileViewerAdapter.TEXT_BROWSER_BROWSE_MODE_CHAR) {
			mViewedFile.browseMode=FileViewerAdapter.TEXT_BROWSER_BROWSE_MODE_HEX;
			if (mViewedFile.searchEnabled) switchFindWidget();
		} else {
			mViewedFile.browseMode=FileViewerAdapter.TEXT_BROWSER_BROWSE_MODE_CHAR;
		}
		rebuildTextListAdapter(true);
		if (Build.VERSION.SDK_INT==19) {
			mUiHandler.postDelayed(new Runnable(){
				@Override
				public void run() {
					mTextListView.setSelection(0);
				}
			}, 100);
		}
	};

	public void reloadFile() {
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				int pos=0,posTop=0;
				pos=mTextListView.getFirstVisiblePosition();
				if (mTextListView.getChildAt(0)!=null) posTop=mTextListView.getChildAt(0).getTop();
				buildTextListAdapter();
				if (mTextListAdapter!=null) {
                    restoreViewAttributes();
                    mTextListView.setSelectionFromTop(pos,posTop);
                    if (mIdxReader.getCharModeLineCount()==0) {
                        mMainViewMsgArea.setVisibility(TextView.VISIBLE);
                        mMainViewMsgArea.setText(c.getString(R.string.msgs_text_browser_ir_file_empty));
                    } else {
                        mMainViewMsgArea.setText("");
                        mMainViewMsgArea.setVisibility(TextView.GONE);
                    }
                }
				mMainActivity.refreshOptionMenu();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
				buildTextListAdapter();
				mMainActivity.refreshOptionMenu();
			}
		});
		mTextListView.setAdapter(null);
		boolean rc=createFileIndexList(mMainUriFile, ntfy);
        if (!rc) {
            mMainViewMsgArea.setVisibility(TextView.VISIBLE);
            mMainViewMsgArea.setText(mContext.getString(R.string.msgs_text_browser_file_does_not_exist));
        }
	};
	
	private boolean createFileIndexList(SafFile3 in_file, final NotifyEvent p_ntfy) {
	    boolean result=false;
        try {
            InputStream is=in_file.getInputStream();
            if (is.available()<(1024*1024*1024*2)-1024) {
                ProgressBar pb=(ProgressBar)mMainView.findViewById(R.id.activity_browser_main_progress_bar);
                mIdxReader.houseKeepIndexCacheFolder(mGp.settingIndexCache);
                mIdxReader.startFileIndexCreation(in_file, mTcIndexReader,p_ntfy, pb,
                        mMainViewMsgArea, mViewedFile.encodeName, mUiHandler);
            } else {
                p_ntfy.notifyToListener(false,new Object[]{
                        getString(R.string.msgs_text_browser_file_file_size_too_big)});
            }
            result=true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    };
	
	public void rebuildTextListAdapter(boolean reset_copy) {
        if (mGp.settingShowDivederLine) mTextListView.setDivider(mDefaultDviderLine);
        else mTextListView.setDivider(null);
        String f_str=mTextListAdapter.getFindString();
        int sel_pos=mTextListAdapter.getFindPostition();

        if (reset_copy) {
			int pos=0,posTop=0;
			pos=mTextListView.getFirstVisiblePosition();
			if (mTextListView.getChildAt(0)!=null) posTop=mTextListView.getChildAt(0).getTop();
			buildTextListAdapter();
			if (mTextListAdapter!=null) {
			    mTextListAdapter.setFindString(f_str);
			    mTextListAdapter.setFindPosition(sel_pos);
                mMainViewMsgArea.setText("");
                mMainViewMsgArea.setVisibility(TextView.GONE);
                mTextListAdapter.resetCopy();
                mTextListView.setSelectionFromTop(pos,posTop);
            } else {
                mTextListView.setAdapter(null);
            }
		} else {
			int pos=0,posTop=0;
			pos=mTextListView.getFirstVisiblePosition();
			if (mTextListView.getChildAt(0)!=null) posTop=mTextListView.getChildAt(0).getTop();
			int cp_begin=mTextListAdapter.getCopyBegin();
			int cp_end=mTextListAdapter.getCopyEnd();
			int h_pos=mTextListAdapter.getHorizontalPosition();
			buildTextListAdapter();
            if (mTextListAdapter!=null) {
                mTextListAdapter.setFindString(f_str);
                mTextListAdapter.setFindPosition(sel_pos);
                mTextListView.setSelectionFromTop(pos,posTop);
                mTextListAdapter.setHorizontalPosition(h_pos);
                if (cp_begin!=-1) {
                    mTextListAdapter.setCopyBegin(cp_begin);
                    mTextListAdapter.setCopyEnd(cp_end);
                }
            } else {
                mTextListView.setAdapter(null);
            }
		}
	};

//	static public boolean isFileExists(Context c, UriFileInfo file_info) {
//        boolean file_exists=false;
//        if (file_info.uri_type.equals(UriFileInfo.URI_TYPE_CONTENT_SCHEME)) {
//            try {
//                InputStream is = c.getContentResolver().openInputStream(file_info.uri);
//                file_exists = true;
//            } catch (Exception e) {
////            e.printStackTrace();
//            }
//        } else if (file_info.uri_type.equals(UriFileInfo.URI_TYPE_SAF_FILE_SCHEME)) {
//                try {
//                    InputStream is=c.getContentResolver().openInputStream(file_info.uri);
//                    file_exists=true;
//                } catch (Exception e) {
////            e.printStackTrace();
//                }
//        } else {
//            try {
//                if (file_info.file_path!=null) {
//                    File lf=new File(file_info.file_path);
//                    file_exists=lf.exists();
//                }
//            } catch (Exception e) {
////            e.printStackTrace();
//            }
//        }
//        return file_exists;
//    }

	@SuppressLint("NewApi")
	private void buildTextListAdapter() {
		LinearLayout ll_scroll=
				(LinearLayout)mMainView.findViewById(R.id.activity_browser_main_scroll_view);
		if (mViewedFile.lineBreak==CustomTextView.LINE_BREAK_NOTHING &&
			mViewedFile.browseMode==FileViewerAdapter.TEXT_BROWSER_BROWSE_MODE_CHAR)
			ll_scroll.setVisibility(LinearLayout.VISIBLE);
		else ll_scroll.setVisibility(LinearLayout.GONE);

//		mTextListView=(ListView)mMainView.findViewById(R.id.activity_browser_main_view);

        NotifyEvent ntfy_error=new NotifyEvent(mContext);
        ntfy_error.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                String emsg=(String)objects[0];
//                mTextListView.setAdapter(null);
                mMainViewMsgArea.setVisibility(TextView.VISIBLE);
                mTextListView.setVisibility(ListView.GONE);
                mMainViewMsgArea.setText("File read error. Error="+emsg);
            }

            @Override
            public void negativeResponse(Context context, Object[] objects) {}
        });

        boolean file_exists=mMainUriFile.exists();
        if (file_exists) {
            int ts=1;
//            if (mGp.settingFontFamily.equals("MONOSPACE") || mViewedFile.lineBreak==CustomTextView.LINE_BREAK_NOTHING) ts=mGp.settingTabStop;
            mTextListAdapter=new FileViewerAdapter(getActivity(),
                    R.layout.text_browser_list_item, mGp.debugEnabled, mIdxReader,
                    mViewedFile.browseMode, mViewedFile.showLineNo,
                    mViewedFile.lineBreak, mGp.settingFontFamily,
                    mGp.settingFontStyle, mGp.settingFontSize,
                    mGp.settingTabStop,
                    ntfy_error);
            final int sel_pos=mTextListView.getFirstVisiblePosition();
            mTextListView.setAdapter(null);
            mTextListView.setAdapter(mTextListAdapter);
            if (mTextListAdapter.getCount()>=140) mTextListView.setFastScrollEnabled(true);
            else mTextListView.setFastScrollEnabled(false);
            mTextListView.setSelection(sel_pos);
            mTextListView.setScrollingCacheEnabled(true);

            mTextListView.setSelected(true);

            final LinearLayout find_view = (LinearLayout) mMainView.findViewById(R.id.activity_browser_main_search_find_view);
            if (mViewedFile.searchEnabled && mViewedFile.browseMode==FileViewerAdapter.TEXT_BROWSER_BROWSE_MODE_CHAR) {
                find_view.setVisibility(LinearLayout.VISIBLE);
            }

            mUiHandler.post(new Runnable(){
                @Override
                public void run() {
                    setScrollButtonEnabled();
//                mTextListView.setFastScrollEnabled(true);
                }
            });
        } else {
            mMainViewMsgArea.setVisibility(TextView.VISIBLE);
            mMainViewMsgArea.setText(mContext.getString(R.string.msgs_text_browser_file_does_not_exist));
            mTextListAdapter=null;
        }
    };

	@SuppressWarnings("unused")
	private void setUiMsg(final TextView tv_msg, Handler handler, final String msg) {
		handler.post(new Runnable(){
			@Override
			public void run() {
				tv_msg.setVisibility(TextView.VISIBLE);
				tv_msg.setText(msg);
			}
		});
	};
	
	private void createCcmenu(final ListView lv, final FileViewerAdapter adapter, final int pos) {
	    if (adapter==null) return;
		final TextView main_view_msg=(TextView)mMainView.findViewById(R.id.activity_browser_main_msg);
		CustomContextMenu mCcMenu = new CustomContextMenu(mResources,mFragmentManager);

		mCcMenu.addMenuItem(getString(R.string.msgs_text_browser_move_top),R.drawable.text_browser_menu_top)
	  	    .setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				mViewedFile.findPosIsValid=false;
				lv.setSelection(0);
			}
	  	});
		mCcMenu.addMenuItem(getString(R.string.msgs_text_browser_move_bottom),R.drawable.text_browser_menu_bottom)
	  	    .setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				mViewedFile.findPosIsValid=false;
				lv.setSelection(adapter.getCount()-1);
			}
	  	});
        if (adapter.isCopyActive()) {//Copyエリアが選択されている
//            int s_pos=adapter.getCopyBegin(), e_pos=adapter.getCopyEnd();
//            if (adapter.getCopyBegin()>adapter.getCopyEnd()) {
//                s_pos=adapter.getCopyBegin();
//                e_pos=adapter.getCopyEnd();
//            } else if (adapter.getCopyBegin()<adapter.getCopyEnd()) {
//                s_pos=adapter.getCopyBegin();
//                e_pos=adapter.getCopyEnd();
//            } else if (adapter.getCopyBegin()==adapter.getCopyEnd()) {
//                s_pos=adapter.getCopyBegin();
//            }
//            if (pos>=s_pos && pos<=e_pos) {
//                setCotextMenuPerformCopy(mCcMenu, main_view_msg, lv, adapter, pos);
//                setCotextMenuResetCopy(mCcMenu, main_view_msg, lv, adapter, pos);
//                setCotextMenuSelectAll(mCcMenu, main_view_msg, lv, adapter, pos);
//            } else {
//                setCotextMenuSetCopyFrom(mCcMenu, main_view_msg, lv, adapter, pos);
//                setCotextMenuSetCopyEnd(mCcMenu, main_view_msg, lv, adapter, pos);
//                setCotextMenuResetCopy(mCcMenu, main_view_msg, lv, adapter, pos);
//                setCotextMenuSelectAll(mCcMenu, main_view_msg, lv, adapter, pos);
//            }
            setCotextMenuSetCopyFrom(mCcMenu, main_view_msg, lv, adapter, pos);
            setCotextMenuSetCopyEnd(mCcMenu, main_view_msg, lv, adapter, pos);
            setCotextMenuPerformCopy(mCcMenu, main_view_msg, lv, adapter, pos);
            setCotextMenuResetCopy(mCcMenu, main_view_msg, lv, adapter, pos);
            setCotextMenuSelectAll(mCcMenu, main_view_msg, lv, adapter, pos);
        } else if (adapter.getCopyBegin()!=-1) {//Copy開始が選択されている
//            if (pos==adapter.getCopyBegin()) {
//                setCotextMenuSetCopyEnd(mCcMenu, main_view_msg, lv, adapter, pos);
//                setCotextMenuPerformCopy(mCcMenu, main_view_msg, lv, adapter, pos);
//                setCotextMenuResetCopy(mCcMenu, main_view_msg, lv, adapter, pos);
//                setCotextMenuSelectAll(mCcMenu, main_view_msg, lv, adapter, pos);
//            } else {
//                setCotextMenuSetCopyFrom(mCcMenu, main_view_msg, lv, adapter, pos);
//                setCotextMenuSetCopyEnd(mCcMenu, main_view_msg, lv, adapter, pos);
//                setCotextMenuResetCopy(mCcMenu, main_view_msg, lv, adapter, pos);
//                setCotextMenuSelectAll(mCcMenu, main_view_msg, lv, adapter, pos);
//            }
            setCotextMenuSetCopyFrom(mCcMenu, main_view_msg, lv, adapter, pos);
            setCotextMenuSetCopyEnd(mCcMenu, main_view_msg, lv, adapter, pos);
            setCotextMenuPerformCopy(mCcMenu, main_view_msg, lv, adapter, pos);
            setCotextMenuResetCopy(mCcMenu, main_view_msg, lv, adapter, pos);
            setCotextMenuSelectAll(mCcMenu, main_view_msg, lv, adapter, pos);
        } else {//無選択
            setCotextMenuSetCopyFrom(mCcMenu, main_view_msg, lv, adapter, pos);
//            setCotextMenuSetCopyEnd(mCcMenu, main_view_msg, lv, adapter, pos);
            setCotextMenuPerformCopy(mCcMenu, main_view_msg, lv, adapter, pos);
//            setCotextMenuResetCopy(mCcMenu, main_view_msg, lv, adapter, pos);
            setCotextMenuSelectAll(mCcMenu, main_view_msg, lv, adapter, pos);
        }
		if (mViewedFile.browseMode==FileViewerAdapter.TEXT_BROWSER_BROWSE_MODE_CHAR) {
			mCcMenu.addMenuItem(getString(R.string.msgs_text_browser_cc_select_encode)+" ("+mIdxReader.getCurrentEncodeName()+")")
		  	.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					EncodeSelectorFragment esfm=EncodeSelectorFragment.newInstance();
					esfm.showDialog(mFragmentManager, esfm);
				}
		  	});
		}
		if (mViewedFile.browseMode==FileViewerAdapter.TEXT_BROWSER_BROWSE_MODE_CHAR) {
			if (mViewedFile.showLineNo) {
				mCcMenu.addMenuItem(getString(R.string.msgs_text_browser_cc_hide_line_no))
			  	.setOnClickListener(new CustomContextMenuOnClickListener() {
					@Override
					public void onClick(CharSequence menuTitle) {
						mViewedFile.showLineNo=false;
						mMainActivity.refreshOptionMenu();
						rebuildTextListAdapter(false);
					}
			  	});
			} else {
				mCcMenu.addMenuItem(getString(R.string.msgs_text_browser_cc_show_line_no))
			  	.setOnClickListener(new CustomContextMenuOnClickListener() {
					@Override
					public void onClick(CharSequence menuTitle) {
						mViewedFile.showLineNo=true;
						mMainActivity.refreshOptionMenu();
						rebuildTextListAdapter(false);
					}
			  	});
			};

            if (mTextListView.getDivider()!=null) {
                mCcMenu.addMenuItem(getString(R.string.msgs_text_browser_cc_hide_line_divider))
                        .setOnClickListener(new CustomContextMenuOnClickListener() {
                            @Override
                            public void onClick(CharSequence menuTitle) {
                                mTextListView.setDivider(null);
                                mTextListAdapter.notifyDataSetChanged();
                            }
                        });
            } else {
                mCcMenu.addMenuItem(getString(R.string.msgs_text_browser_cc_show_line_divider))
                        .setOnClickListener(new CustomContextMenuOnClickListener() {
                            @Override
                            public void onClick(CharSequence menuTitle) {
                                mTextListView.setDivider(mDefaultDviderLine);
                                mTextListAdapter.notifyDataSetChanged();
                            }
                        });
            };

            if (mViewedFile.lineBreak==CustomTextView.LINE_BREAK_NOTHING) {
				//current=no line break
				mCcMenu.addMenuItem(getString(R.string.msgs_text_browser_cc_line_break_no_word))
			  	.setOnClickListener(new CustomContextMenuOnClickListener() {
					@Override
					public void onClick(CharSequence menuTitle) {
						mViewedFile.lineBreak=CustomTextView.LINE_BREAK_NO_WORD_WRAP;
						rebuildTextListAdapter(false);
						mMainActivity.refreshOptionMenu();
//						applySettingParms();
					}
			  	});
				mCcMenu.addMenuItem(getString(R.string.msgs_text_browser_cc_line_break_word))
			  	.setOnClickListener(new CustomContextMenuOnClickListener() {
					@Override
					public void onClick(CharSequence menuTitle) {
						mViewedFile.lineBreak=CustomTextView.LINE_BREAK_WORD_WRAP;
						rebuildTextListAdapter(false);
						mMainActivity.refreshOptionMenu();
					}
			  	});
			} else if (mViewedFile.lineBreak==CustomTextView.LINE_BREAK_NO_WORD_WRAP) {
				//current=no word line break
				mCcMenu.addMenuItem(getString(R.string.msgs_text_browser_cc_line_break_nothing))
			  	.setOnClickListener(new CustomContextMenuOnClickListener() {
					@Override
					public void onClick(CharSequence menuTitle) {
						mViewedFile.lineBreak=CustomTextView.LINE_BREAK_NOTHING;
						rebuildTextListAdapter(false);
						mMainActivity.refreshOptionMenu();
					}
			  	});
				mCcMenu.addMenuItem(getString(R.string.msgs_text_browser_cc_line_break_word))
			  	.setOnClickListener(new CustomContextMenuOnClickListener() {
					@Override
					public void onClick(CharSequence menuTitle) {
						mViewedFile.lineBreak=CustomTextView.LINE_BREAK_WORD_WRAP;
						rebuildTextListAdapter(false);
						mMainActivity.refreshOptionMenu();
					}
			  	});
			} else if (mViewedFile.lineBreak==CustomTextView.LINE_BREAK_WORD_WRAP) {
				//current=word line break
				mCcMenu.addMenuItem(getString(R.string.msgs_text_browser_cc_line_break_nothing))
			  	.setOnClickListener(new CustomContextMenuOnClickListener() {
					@Override
					public void onClick(CharSequence menuTitle) {
						mViewedFile.lineBreak=CustomTextView.LINE_BREAK_NOTHING;
						rebuildTextListAdapter(false);
						mMainActivity.refreshOptionMenu();
					}
			  	});
				mCcMenu.addMenuItem(getString(R.string.msgs_text_browser_cc_line_break_no_word))
			  	.setOnClickListener(new CustomContextMenuOnClickListener() {
					@Override
					public void onClick(CharSequence menuTitle) {
						mViewedFile.lineBreak=CustomTextView.LINE_BREAK_NO_WORD_WRAP;
						rebuildTextListAdapter(false);
						mMainActivity.refreshOptionMenu();
					}
			  	});
			}
		}
        mCcMenu.addMenuItem(getString(R.string.msgs_text_browser_cc_properties)).setOnClickListener(new CustomContextMenuOnClickListener() {
            @Override
            public void onClick(CharSequence menuTitle) {
                String fp="";
                fp=mMainUriFile.getPath();
                MessageDialogFragment cdf =MessageDialogFragment.newInstance(false, "I", "File Properties",
                                "Path="+fp+"\n"+
                                "Name="+mMainUriFile.getName()+"\n"+
                                "Size="+mMainUriFile.length()+"\n"+
                                "Last modified="+ StringUtil.convDateTimeTo_YearMonthDayHourMinSec(mMainUriFile.lastModified())+"\n"+
                                "Mime type="+mMainUriFile.getMimeType()+"\n"+
                                "Encoding="+mIdxReader.getCurrentEncodeName()+"\n"
                        );
                cdf.showDialog(getFragmentManager(),cdf,null);

            }
        });
		mCcMenu.createMenu();
	};

	private void setCotextMenuSetCopyFrom(final CustomContextMenu mCcMenu, final TextView main_view_msg, final ListView lv, final FileViewerAdapter adapter, final int pos) {
        mCcMenu.addMenuItem(getString(R.string.msgs_text_browser_copy_from),
                R.drawable.ic_24_select_begin)
                .setOnClickListener(new CustomContextMenuOnClickListener() {
                    @Override
                    public void onClick(CharSequence menuTitle) {
                        adapter.setCopyBegin(pos);
                        adapter.notifyDataSetChanged();
                        main_view_msg.setVisibility(TextView.VISIBLE);
                        main_view_msg.setText(getString(R.string.msgs_text_browser_copymsg_specify_end));
                    }
                });
    }

    private void setCotextMenuPerformCopy(final CustomContextMenu mCcMenu, final TextView main_view_msg, final ListView lv, final FileViewerAdapter adapter, final int pos) {
	    String text="";
//        if (mViewedFile.lineBreak!=CustomTextView.LINE_BREAK_NOTHING) {
//            if (adapter.getItem(pos)[0].length()>20) {
//                text=" ("+adapter.getItem(pos)[0].substring(0,20)+"..."+")";
//            } else {
//                text=" ("+adapter.getItem(pos)[0]+")";
//            }
//        }
        mCcMenu.addMenuItem(getString(R.string.msgs_text_browser_do_copy)+text)
                .setOnClickListener(new CustomContextMenuOnClickListener() {
                    @Override
                    public void onClick(CharSequence menuTitle) {
                        String msg="";
                        if (adapter.isCopyActive()) {
                            if (adapter.getCopyBegin()>adapter.getCopyEnd())
                                msg=copyToClipboard(adapter.getCopyEnd(),adapter.getCopyBegin(),adapter);
                            else msg=copyToClipboard(adapter.getCopyBegin(),adapter.getCopyEnd(),adapter);
                            CommonDialog.showToastShort(mMainActivity, msg);
                            adapter.resetCopy();
                            adapter.notifyDataSetChanged();
                        } else {
                            msg=copyToClipboard(pos,pos,adapter);
                            CommonDialog.showToastShort(mMainActivity, msg);
                            adapter.resetCopy();
                            adapter.notifyDataSetChanged();
                        }
                        main_view_msg.setVisibility(TextView.GONE);
                        main_view_msg.setText("");
                    }
                });
    }

    private void setCotextMenuSetCopyEnd(final CustomContextMenu mCcMenu, final TextView main_view_msg, final ListView lv, final FileViewerAdapter adapter, final int pos) {
        mCcMenu.addMenuItem(getString(R.string.msgs_text_browser_copy_end),
                R.drawable.ic_24_select_end)
                .setOnClickListener(new CustomContextMenuOnClickListener() {
                    @Override
                    public void onClick(CharSequence menuTitle) {
                        adapter.setCopyEnd(pos);
                        adapter.notifyDataSetChanged();
                        main_view_msg.setVisibility(TextView.GONE);
                        main_view_msg.setText("");
                    }
                });
    }

    private void setCotextMenuResetCopy(final CustomContextMenu mCcMenu, final TextView main_view_msg, final ListView lv, final FileViewerAdapter adapter, final int pos) {
        mCcMenu.addMenuItem(getString(R.string.msgs_text_browser_reset_copy))
                .setOnClickListener(new CustomContextMenuOnClickListener() {
                    @Override
                    public void onClick(CharSequence menuTitle) {
                        adapter.resetCopy();
                        adapter.notifyDataSetChanged();
                        main_view_msg.setVisibility(TextView.GONE);
                        main_view_msg.setText("");
                    }
                });
    }

    private void setCotextMenuSelectAll(final CustomContextMenu mCcMenu, final TextView main_view_msg, final ListView lv, final FileViewerAdapter adapter, final int pos) {
        mCcMenu.addMenuItem(getString(R.string.msgs_text_browser_select_all_for_copy),
                R.drawable.ic_24_select_all)
                .setOnClickListener(new CustomContextMenuOnClickListener() {
                    @Override
                    public void onClick(CharSequence menuTitle) {
                        adapter.resetCopy();
                        adapter.setCopyBegin(0);
                        adapter.setCopyEnd(adapter.getCount()-1);
                        adapter.notifyDataSetChanged();
                        main_view_msg.setVisibility(TextView.GONE);
                        main_view_msg.setText("");
                    }
                });
    }

    private String copyToClipboard(int from_line, int to_line, FileViewerAdapter adapter) {
		 ClipboardManager cm = 
		      (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
		 String sep="";
		 int c_line=0;
		 StringBuilder out= new StringBuilder("");
		 for (int i=from_line;i<=to_line;i++) {
			 c_line++;
			 out.append(sep);
			 out.append(adapter.getItem(i)[0]);
			 sep="\n";
		 }
		 cm.setText(out);
		 return String.format(getString(R.string.msgs_text_browser_copymsg_copied),c_line);
	};

	public void switchFindWidget() {
        final LinearLayout find_view = (LinearLayout) mMainView.findViewById(R.id.activity_browser_main_search_find_view);
		final Button btnFind = (Button)mMainView.findViewById(R.id.activity_browser_main_search_btn);
		final EditText et_find_string = (EditText)mMainView.findViewById(R.id.activity_browser_main_search_text);
		final CheckBox cb_case = (CheckBox)mMainView.findViewById(R.id.activity_browser_main_search_case_sensitive);
		final TextView main_msg = (TextView)mMainView.findViewById(R.id.activity_browser_main_msg);
		if (mViewedFile.searchEnabled) {
		    find_view.setVisibility(LinearLayout.GONE);
			mViewedFile.searchEnabled=false;
			mTextListAdapter.resetFindStringResult();
            main_msg.setText("");
            main_msg.setVisibility(TextView.GONE);
        } else {
            find_view.setVisibility(LinearLayout.VISIBLE);
			mViewedFile.findResultPos=-1;
			mViewedFile.searchEnabled=true;
			mTextListAdapter.setFindString(et_find_string.getText().toString());
		}
	}

    private void setFindStringListener() {
        final LinearLayout find_view = (LinearLayout) mMainView.findViewById(R.id.activity_browser_main_search_find_view);
		final Button btnFind = (Button)mMainView.findViewById(R.id.activity_browser_main_search_btn);
		final EditText et_find_string = (EditText)mMainView.findViewById(R.id.activity_browser_main_search_text);
		final CheckBox cb_case = (CheckBox)mMainView.findViewById(R.id.activity_browser_main_search_case_sensitive);
        final CheckBox cb_word = (CheckBox)mMainView.findViewById(R.id.activity_browser_main_search_find_word);
		final TextView main_msg = (TextView)mMainView.findViewById(R.id.activity_browser_main_msg);
		
		cb_case.setChecked(mViewedFile.searchCaseSensitive);
		cb_case.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				mViewedFile.searchCaseSensitive=arg1;
			}
		});

        cb_word.setChecked(mViewedFile.searchCaseSensitive);
        cb_word.setOnCheckedChangeListener(new OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
                mViewedFile.searchByWord=arg1;
            }
        });

        et_find_string.setText(mViewedFile.searchString);
		if (mViewedFile.searchString.equals("")) CommonDialog.setButtonEnabled(mMainActivity, btnFind, false);
		et_find_string.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable arg0) {
				if (arg0.toString().equals("")) CommonDialog.setButtonEnabled(mMainActivity, btnFind, false);
				else CommonDialog.setButtonEnabled(mMainActivity, btnFind, true);
			}
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,int arg3) {
				main_msg.setText("");
				main_msg.setVisibility(TextView.GONE);
			}
		});
		
		mTextListView.setOnScrollListener(new OnScrollListener(){
			@Override
			public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {
			}
			@Override
			public void onScrollStateChanged(AbsListView arg0, int arg1) {
//				Log.v("","onScrollStateChanged, arg1="+arg1);
				if (arg1== OnScrollListener.SCROLL_STATE_TOUCH_SCROLL||
						arg1== OnScrollListener.SCROLL_STATE_FLING)
					mViewedFile.findPosIsValid=false;

			}
		});

		btnFind.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				mViewedFile.searchString=et_find_string.getText().toString();
                CommonDialog.setViewEnabled(mMainActivity, btnFind, false);
				int start_pos_temp=0;
                log.debug("find before start_pos="+start_pos_temp+", result_pos="+mViewedFile.findResultPos);
				if (mViewedFile.findResultPos==-1 || !mViewedFile.findPosIsValid) {
					//表示されている先頭から検索開始
					start_pos_temp=mTextListView.getFirstVisiblePosition();
				} else {//検索結果の次の行から検索開始
					//前回の検索結果が有効（ユーザーがスクロールしていない）
					start_pos_temp=mViewedFile.findResultPos+1;
				}
				final int start_pos=start_pos_temp;
				mViewedFile.findPosIsValid=true;
				final Handler hndl=new Handler();
				Thread th=new Thread(){
				    @Override
                    public void run() {
                        mViewedFile.findResultPos=mTextListAdapter.findString(
                                FileViewerAdapter.TEXT_BROWSER_BROWSE_MODE_CHAR,
                                start_pos, mViewedFile.searchString, cb_case.isChecked(), cb_word.isChecked());
                        hndl.post(new Runnable() {
                            @Override
                            public void run() {
                                CommonDialog.setViewEnabled(mMainActivity, btnFind, true);
                                if (mViewedFile.findResultPos!=-1) {
                                    mUiHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            mTextListView.setSelection(mViewedFile.findResultPos);
                                        }
                                    });
                                    main_msg.setText("");
                                    main_msg.setVisibility(TextView.GONE);
                                } else {
                                    mViewedFile.findResultPos=mTextListView.getFirstVisiblePosition();//start_pos;//検索開始位置を戻す
                                    mTextListView.setSelected(false);
                                    main_msg.setText(mContext.getString(R.string.msgs_text_browser_search_string_not_found));
                                    main_msg.setVisibility(TextView.VISIBLE);
                                }
                                log.debug("find after start_pos="+start_pos+", result_pos="+mViewedFile.findResultPos);
                            }
                        });
                    }
                };
				th.start();
//				Log.v("TextFileBrowser","start_pos="+start_pos+", result_pos="+mViewedFile.findResultPos);
			}
		});
	}

	private static final int SCROLL_SMALL_AMOUNT=2;
    private static final int SCROLL_LARGE_AMOUNT=20;
	@SuppressLint("ClickableViewAccessibility")
	private void setScrollButtonListener() {
		mTcScroll=new ThreadCtrl();
		mTcScroll.setEnabled();
		mMainViewScrollRight1.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
                scrollTouchListener("Left1",arg0, event, SCROLL_LARGE_AMOUNT);
				return false;
			}
		});
		mMainViewScrollRight2.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
                scrollTouchListener("Left1",arg0, event, SCROLL_SMALL_AMOUNT);
				return false;
			}
		});
		mMainViewScrollLeft1.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
                scrollTouchListener("Left1",arg0, event, SCROLL_LARGE_AMOUNT*-1);
				return false;
			}
		});
		mMainViewScrollLeft2.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
                scrollTouchListener("Left2",arg0, event, SCROLL_SMALL_AMOUNT*-1);
				return false;
			}
		});

	};

	private void scrollTouchListener(String id, View arg0, MotionEvent event, int scroll_amount) {
        if (event.getAction()==MotionEvent.ACTION_DOWN) {
            if (log.isTraceEnabled()) log.trace(id+" Action=ACTION_DOWN, amount="+scroll_amount+", tc="+mTcScroll.isEnabled());
            if (mScrollActive) {
                mTcScroll.setDisabled();
//                log.trace(id+" mThScroll isAlive="+mThScroll.isAlive()+", isDeamon="+mThScroll.isDaemon()+", isInterrupted="+mThScroll.isInterrupted());
                waitThreadTerminate();
                mTcScroll.setEnabled();
            }
            startScroll(mTcScroll, scroll_amount);//Scroll left
        } else if (event.getAction()==MotionEvent.ACTION_UP) {
            if (log.isTraceEnabled()) log.trace(id+" Action=ACTION_UP, amount="+scroll_amount+", tc="+mTcScroll.isEnabled());
            mTcScroll.setDisabled();
            waitThreadTerminate();
            mTcScroll.setEnabled();
        } else if (event.getAction()==MotionEvent.ACTION_CANCEL) {
            if (log.isTraceEnabled()) log.trace(id+" Action=ACTION_CANCEL, amount="+scroll_amount+", tc="+mTcScroll.isEnabled());
            mTcScroll.setDisabled();
            waitThreadTerminate();
            mTcScroll.setEnabled();
        }

    }

	private void waitThreadTerminate() {
		try {
			mThScroll.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	};
	private void startScroll(final ThreadCtrl tc, final int move) {
        if (log.isTraceEnabled()) log.trace("startScroll entered, tc="+tc.isEnabled()+", active="+mScrollActive);
		final Handler hndl=new Handler();
		mScrollActive=true;
		mThScroll=new Thread(){
			@Override
			public void run() {
			   scroller(tc, hndl, move);
			}
		};
		mThScroll.start();
        log.trace("startScroll exited");
	};

	private void scroller(final ThreadCtrl tc, final Handler hndl, final int move) {
        if (log.isTraceEnabled()) log.trace("scroller started, amount="+move+", tc="+tc.isEnabled());
        while(tc.isEnabled()) {
            hndl.post(new Runnable(){
                @Override
                public void run() {
                    if (move>0) {
                        if (mTextListAdapter.canScrollRight()){
                            mTextListAdapter.incrementHorizontalPosition(move);
                            mTextListAdapter.notifyDataSetChanged();
                            Handler sh=new Handler();
                            sh.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (log.isTraceEnabled()) log.trace("Scroll right ended, TC enabled="+mTcScroll.isEnabled());
                                    setScrollButtonEnabled();
                                }
                            });
                        } else {
                            if (log.isTraceEnabled()) log.trace("Scroll right not processed, TC enabled="+mTcScroll.isEnabled());
                            mTcScroll.setDisabled();
                            waitThreadTerminate();
                        }
                    } else {
                        if (mTextListAdapter.canScrollLeft()){
                            mTextListAdapter.decrementHorizontalPosition(move*-1);
                            mTextListAdapter.notifyDataSetChanged();
                            Handler sh=new Handler();
                            sh.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (log.isTraceEnabled()) log.trace("Scroll left ended, TC enabled="+mTcScroll.isEnabled());
                                    setScrollButtonEnabled();
                                }
                            });
                        } else {
                            if (log.isTraceEnabled()) log.trace("Scroll left not processed, TC enabled="+mTcScroll.isEnabled());
                            mTcScroll.setDisabled();
                            waitThreadTerminate();
                        }
                    }
                }
            });
            waitSpecifiedTime(200);
        }
        mScrollActive=false;
        mTcScroll.setEnabled();
        if (log.isTraceEnabled()) log.trace("scroller ended");
    }

    private void setScrollButtonEnabled() {
        boolean can_scroll_left=mTextListAdapter.canScrollLeft();
        boolean can_scroll_right=mTextListAdapter.canScrollRight();
        if (mGp.settingUseLightTheme) {
            if (can_scroll_left) {
                mMainViewScrollLeft1.setAlpha(1.0f);
                mMainViewScrollLeft2.setAlpha(1.0f);
            } else {
                mMainViewScrollLeft1.setAlpha(.3f);
                mMainViewScrollLeft2.setAlpha(.3f);
            }
            if (can_scroll_right) {
                mMainViewScrollRight1.setAlpha(1.0f);
                mMainViewScrollRight2.setAlpha(1.0f);
            } else {
                mMainViewScrollRight1.setAlpha(.3f);
                mMainViewScrollRight2.setAlpha(.3f);
            }
        }
        if (can_scroll_left) {
            log.trace("Scroll left enabled");
            mMainViewScrollLeft1.setEnabled(true);
            mMainViewScrollLeft2.setEnabled(true);
        } else {
            log.trace("Scroll left disabled");
            mMainViewScrollLeft1.setEnabled(false);
            mMainViewScrollLeft2.setEnabled(false);
        }
        if (can_scroll_right) {
            log.trace("Scroll right enabled");
            mMainViewScrollRight1.setEnabled(true);
            mMainViewScrollRight2.setEnabled(true);
        } else {
            log.trace("Scroll right disabled");
            mMainViewScrollRight1.setEnabled(false);
            mMainViewScrollRight2.setEnabled(false);
        }
    }

    private void waitSpecifiedTime(long wt) {
		try {
			Thread.sleep(wt);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	};

}
