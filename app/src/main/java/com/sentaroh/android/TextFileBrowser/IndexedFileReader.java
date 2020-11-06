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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.universalchardet.UniversalDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sentaroh.android.Utilities3.EncryptUtilV3;
import com.sentaroh.android.Utilities3.MiscUtil;
import com.sentaroh.android.Utilities3.LocalMountPoint;
import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.ThreadCtrl;
import com.sentaroh.android.Utilities3.Dialog.CommonDialog;

public class IndexedFileReader {

	private final static String APPLICATION_TAG="TextFileBrowser";
	private final static int INDEX_CREATE_BUFFER_SIZE=1024*256;
	private final static int CACHE_FILE_BUFFER_SIZE= 4092*256;

    private static Logger log = LoggerFactory.getLogger(IndexedFileReader.class);

	class CommonParms {
		public int charModeBlockSize=1024*8;
		public int hexModeBlockSize=1024*8; //必ず16の倍数にすること
		public int cachePoolBufferCount=64;
		public int cachePoolSize=64;
		public String encodeName="";
        public String defaultEncodeName="";
//		public boolean autoDetectEncodeAllFile=false;
		
		public int tab_stop_value=4;

		public String mime_type=null;

		public SafFile3 input_file =null;
		
		public int cache_text_list_index=0;
//		public ArrayList<CachePool> cachePool=null;
		public ArrayList<CachePool> cachePool=null;
		public int cache_pool_mode=0;
		public float cache_hit=0,cache_miss=0;

		public CachePool search_cache=null;

		public Context context;
		
		public ThreadCtrl tcIndexReader=null;
		
		public ArrayList<FileIndexListItem> charModeFileIndex=null;
		public ArrayList<FileIndexListItem> hexModeFileIndex=null;

		public String defaultSettingIndexCache;
		
		public String lastErrorMessage="";
		
		public long index_create_progress=-1;
		
		public Handler parentUiHandler=null;
		public ProgressBar parentProgressBar=null;
		public TextView parentMsgView=null;
		
		public StringBuilder sbCharModeText=null;
		
		public MainActivity mainActivity=null;

		public boolean encodingAutoDetectFailed=false;
        public Handler uiHandler=null;

    }

	private CommonParms cparms=new CommonParms();

    public boolean isEncodingAutoDetectFailed() {return cparms.encodingAutoDetectFailed;}

    public String getLastErrorMessage() {
	    return cparms.lastErrorMessage;
    }

	final public String getCurrentEncodeName() {return cparms.encodeName;}
	
	final public ArrayList<FileIndexListItem> getCharModeFileIndexList() {
		return cparms.charModeFileIndex;
	};
	final public void setCharModeFileIndexList(ArrayList<FileIndexListItem> p) {
		cparms.charModeFileIndex=p;
	};
	final public ArrayList<FileIndexListItem> getHexModeFileIndexList() {
		return cparms.hexModeFileIndex;
	};
	final public void setHexModeFileIndexList(ArrayList<FileIndexListItem> p) {
		cparms.hexModeFileIndex=p;
	};
	final public void setTabStopValue(int p) {
		cparms.tab_stop_value=p;
		cparms.cachePool=null;
	};

	private String mFontFamily=null;
	final public void setFontFamily(String font_family) {
        mFontFamily=font_family;
    }
    final public String getFontFamily() {
        return mFontFamily;
    }

    final public boolean isMonospaceFontUsed() {
	    return getFontFamily().equals("MONOSPACE");
    }

    private String mFontStyle=null;
    final public void setFontStyle(String font_style) {
        mFontStyle=font_style;
    }
    final public String getFontStyle() {
        return mFontStyle;
    }

    public IndexedFileReader(Context c, CommonDialog cd,
                             ThreadCtrl tc,
                             String default_encoding, String cache_option,
			int ib_char_size, int ib_hex_size, int cp_size, MainActivity ma) {
        cparms.uiHandler=new Handler();
		cparms.mainActivity=ma;
		cparms.context=c;
		cparms.tcIndexReader=tc;
		cparms.cachePool=null;
		cparms.cache_text_list_index=0;
		cparms.charModeFileIndex=new ArrayList<FileIndexListItem>(); 
		cparms.hexModeFileIndex=new ArrayList<FileIndexListItem>();
		cparms.defaultSettingIndexCache=cache_option;
		
//		cparms.encodeName=encoding;
		cparms.defaultEncodeName=default_encoding;
//		cparms.autoDetectEncodeAllFile=adcd;
		
		cparms.charModeBlockSize=ib_char_size*1024;
		cparms.hexModeBlockSize=ib_hex_size*1024;
		cparms.cachePoolSize=cp_size*1024;

		cparms.sbCharModeText=new StringBuilder(cparms.charModeBlockSize);
		
		if (log.isDebugEnabled()) 
			debugMsg("IndexedFileReader init", "EncodingName="+cparms.encodeName
//					+", Alway determin char code="+cparms.autoDetectEncodeAllFile
					+", Index file cache size option="+cparms.defaultSettingIndexCache
					+", Char buffer size="+cparms.charModeBlockSize
					+", Hex buffer size="+cparms.hexModeBlockSize
					+", buffer pool size="+cparms.cachePoolSize
					);

	};

	final public int findString(int mode, int start_no, String target, boolean case_sensitive, boolean find_word) {
		return findString(cparms, mode, start_no, target, case_sensitive, find_word, isMonospaceFontUsed());
	};

	final static private int findString(CommonParms cparms, int mode, int start_no, String target, boolean case_sensitive, boolean find_word, boolean isMonospaceFontUsed) {
		if (start_no<0) return -1;
		int result=-1;
		if (mode!=FileViewerAdapter.TEXT_BROWSER_BROWSE_MODE_CHAR)
			return result;
		int no_of_lines=0;
		for (int i=0;i<cparms.charModeFileIndex.size();i++) {
			no_of_lines+=cparms.charModeFileIndex.get(i).no_of_lines;
		}
		String pat_str="";
        if (find_word) pat_str="\\b"+target+"\\b";
        else pat_str=target;

        Pattern patrn=null;
		if (case_sensitive) patrn=Pattern.compile(pat_str);
		else patrn=Pattern.compile(pat_str, Pattern.CASE_INSENSITIVE);
		String[] line=new String[2];
		Matcher matcher=null;
		for (int i=start_no;i<no_of_lines;i++) {
			int[] local_cache=searchLineNoFromCachePool(cparms.cachePool,i);
			if (local_cache==null) {//cache miss by local
				if (cparms.search_cache==null) {//cache search empty
					cparms.search_cache=loadCacheBlock(cparms,mode, cparms.charModeFileIndex,cparms.hexModeFileIndex, i, isMonospaceFontUsed) ;
					line=cparms.search_cache.cache[i-cparms.search_cache.start_line_number];
					matcher=patrn.matcher(line[0]);
					if (matcher!=null && matcher.find()) {
						result=i;
						break;
					}
				} else {//check search_cache
					if (cparms.search_cache.start_line_number<=i && cparms.search_cache.end_line_number>=i) {
						line=cparms.search_cache.cache[i-cparms.search_cache.start_line_number];
						if (line!=null) {
							if (line[0]!=null) {
								matcher=patrn.matcher(line[0]);
								if (matcher!=null && matcher.find()) {
									result=i;
									break;
								}
							}
						}
					} else {//cache miss by search
						cparms.search_cache=loadCacheBlock(cparms,mode, cparms.charModeFileIndex,cparms.hexModeFileIndex, i, isMonospaceFontUsed) ;
						line=cparms.search_cache.cache[i-cparms.search_cache.start_line_number];
						if (line!=null) {
							matcher=patrn.matcher(line[0]);
							if (matcher!=null && matcher.find()) {
								result=i;
								break;
							}
						}
					}
				}
			} else {//cache hit from local
				int cp_index=local_cache[0];
				CachePool cp_item=cparms.cachePool.get(cp_index);
				line=cp_item.cache[i-cparms.cachePool.get(cp_index).start_line_number];
				if (line!=null && line[0]!=null) {
					matcher=patrn.matcher(line[0]);
					if (matcher!=null && matcher.find()) {
						result=i;
						break;
					}
				}
			}
		}
		return result;
	};

	final public String[] fetchLine(int mode,int line_no) {
		return fetchLine(cparms, mode,line_no, isMonospaceFontUsed());
	};
	final static private String[] fetchLine(CommonParms cparms, int mode,int line_no, boolean isMonospaceFontUsed) {
		String[] line=new String[2];
		if (cparms.charModeFileIndex.size()==0) {
			line[0]=cparms.context.getString(R.string.msgs_text_browser_ir_file_empty);
		} else {
			if (cparms.cachePool==null || cparms.cache_pool_mode!=mode) {
				if (cparms.cache_pool_mode==FileViewerAdapter.TEXT_BROWSER_BROWSE_MODE_CHAR) {
					cparms.cachePoolBufferCount=cparms.cachePoolSize/cparms.charModeBlockSize;
				} else {
					cparms.cachePoolBufferCount=cparms.cachePoolSize/cparms.hexModeBlockSize;
				}
				cparms.cache_pool_mode=mode;
				cparms.search_cache=null;
				if (log.isDebugEnabled()) debugMsg("fetchLine","cashPool was initialized"+
							" pool size="+cparms.cachePoolBufferCount);
				cparms.cachePool=new ArrayList<CachePool>();
				CachePool cp_item=loadCacheBlock(cparms,mode, cparms.charModeFileIndex, cparms.hexModeFileIndex, line_no, isMonospaceFontUsed) ;
				if (cp_item.cache!=null) {
                    cachePoolAdd(cparms,cp_item);
                    line=cp_item.cache[line_no-cp_item.start_line_number];
                } else {
				    line=null;
                }
			} else {
				int[] result=searchLineNoFromCachePool(cparms.cachePool,line_no);
				if (result==null) {
//					cache_miss++;
					if (log.isDebugEnabled()) debugMsg("fetchLine","cache missied line no="+line_no);
					CachePool cp_item=loadCacheBlock(cparms,mode, cparms.charModeFileIndex, cparms.hexModeFileIndex, line_no, isMonospaceFontUsed) ;
					if (log.isDebugEnabled()) debugMsg("fetchLine","cache loaded, line no="+line_no+
								", cp size="+cparms.cachePool.size()+
								", cp block="+cp_item.block+", cp start="+cp_item.start_line_number);
                    if (cp_item.cache!=null) {
                        cachePoolAdd(cparms,cp_item);
                        line=cp_item.cache[line_no-cp_item.start_line_number];
                    } else {
                        line=null;
                    }
				} else {
//					cache_hit++;
					int cp_index=result[0];
					CachePool cp_item=cparms.cachePool.get(cp_index);
					cachePoolMoveToTop(cparms,cp_item);
					line=cp_item.cache[line_no-cp_item.start_line_number];
				}
			}
		}
		return line;
	};
	
	final static private CachePool loadCacheBlock(CommonParms cparms, int mode, 
			ArrayList<FileIndexListItem> tbidx_char,
			ArrayList<FileIndexListItem> tbidx_hex,int line_no, boolean isMonospaceFontUsed) {
		CachePool cp_item=new CachePool();
		cparms.cache_text_list_index=0;
		if (mode==FileViewerAdapter.TEXT_BROWSER_BROWSE_MODE_CHAR) {
			int[] idxno=searchLineNoFromFileIndex(cparms,tbidx_char,line_no) ;
			cp_item.block=tbidx_char.get(idxno[0]).block_number;
			cp_item.start_line_number=idxno[1];//(int)start_no;
			cp_item.end_line_number=cp_item.start_line_number+ tbidx_char.get(idxno[0]).no_of_lines-1;
			cp_item.cache= readFileBlock(cparms,mode, tbidx_char, tbidx_hex, idxno[0], isMonospaceFontUsed);
			if (log.isDebugEnabled()) debugMsg("loadCacheBlock(Char)","block no="+idxno[0]+
						", start no="+cp_item.start_line_number+
						", end no="+cp_item.end_line_number);
		} else {
			int[] idxno=searchLineNoFromFileIndex(cparms,tbidx_hex,line_no) ;
			cp_item.block=tbidx_hex.get(idxno[0]).block_number;
			cp_item.start_line_number=tbidx_hex.get(idxno[0]).start_line_number;//idxno[1];//(int)start_no;
			cp_item.end_line_number=cp_item.start_line_number+tbidx_hex.get(idxno[0]).no_of_lines-1;
			cp_item.cache=readFileBlock(cparms,mode, tbidx_char, tbidx_hex, idxno[0], isMonospaceFontUsed);
			if (log.isDebugEnabled()) debugMsg("loadCacheBlock(Hex)","block no="+idxno[0]+
						", start no="+cp_item.start_line_number+
						", end no="+cp_item.end_line_number);
		}
		return cp_item;
	}

	final private static void debugMsg(String id,String text) {
//			Log.v(APPLICATION_TAG,(id+"                           ").substring(0,25)+text);
			log.debug(id+" "+text);
	};
	
	final static private void cachePoolAdd(CommonParms cparms, CachePool cp_item) {
//		if (log.isDebugEnabled()) debugMsg("cachePoolAdd","entered"+"CachePool size="+cp.size()+
//					", block="+cp_item.block+
//					", start number="+cp_item.start_line_number);
		cparms.cachePool.add(cp_item);
		if (cparms.cachePool.size()>cparms.cachePoolBufferCount) cachePoolCleanup(cparms);
	};

	final static private void cachePoolCleanup(CommonParms cparms) {
		while((cparms.cachePool.size()+5)>cparms.cachePoolBufferCount) {
//			if (log.isDebugEnabled()) 
//				debugMsg("cachePoolCleanup","Remove cache="+cp.get(0).block);
			cparms.cachePool.remove(0);
		}
	};
	
	final static private void cachePoolMoveToTop(CommonParms cparms,CachePool cp_item) {
		if (cparms.cachePool.size()>1) {
			if (cparms.cachePool.get(cparms.cachePool.size()-1).block!=cp_item.block) {
				for (int i=0;i<cparms.cachePool.size()-1;i++) {
					if (cparms.cachePool.get(i).block==cp_item.block) {
//						debugMsg("cachePoolMoveToTop",
//								"new block="+cp_item.block+", prev block="+cp.get(cp.size()-1).block);
						cparms.cachePool.remove(i);
						cparms.cachePool.add(cp_item);
//						for (int j=0;j<cachePool.size();j++) 
//							Log.v("","blk="+cachePool.get(j).block);
					}
				}
			}
		}
	};

//	private static long b_time;
	final static private int[] searchLineNoFromCachePool(ArrayList<CachePool> cp,int line_no) {
//		b_time=System.currentTimeMillis();
		int[] result=null;
		for (int i=cp.size()-1;i>=0;i--) {
			CachePool cp_item=cp.get(i);
			if (line_no>=cp_item.start_line_number && //Cache hit 
					line_no<=cp_item.end_line_number) {
//				debugMsg("searchLineNoFromCachePool","founded, line no="+line_no+
//							", cp block="+cp_item.block+", cp start="+cp_item.start_line_number);
				result=new int[]{i,cp_item.block,cp_item.start_line_number};
			}
		}
//		Log.v("","search time="+(System.currentTimeMillis()-b_time));
		return result;
	};
	
	final static private int[] searchLineNoFromFileIndex(CommonParms cparms,ArrayList<FileIndexListItem> tbidx, int line_no){
		int[] result=null;
		for (FileIndexListItem idx_item : tbidx) {
			if (line_no>=idx_item.start_line_number &&
					line_no<(idx_item.start_line_number+
							idx_item.no_of_lines)) {
				result=new int[3];
				result[0]=idx_item.block_number;//
				result[1]=idx_item.start_line_number;
				result[2]=idx_item.no_of_lines;
				break;
			}
		}
		if (result!=null) {
			if (log.isDebugEnabled()) debugMsg("searchCharModeLineNoFromFileIndex",
					"line no="+line_no+", block="+result[0]+
					", start line number="+result[1]);
		} else {
			if (log.isDebugEnabled()) debugMsg("searchCharModeLineNoFromFileIndex",
					" line no="+line_no+", can not be found.");
		}

		return result;
	};

	final static private String[][] readFileBlock(CommonParms cparms, int mode,  
			ArrayList<FileIndexListItem> tbidx_char,
			ArrayList<FileIndexListItem> tbidx_hex,int block_no, boolean isMonospaceFontUsed) {
		if (log.isDebugEnabled()) debugMsg("readFileBlock block","number="+block_no);

		FileIndexListItem tfli=new FileIndexListItem();
		String[][] tal=null;
		ThreadCtrl tc=new ThreadCtrl();
		tc.setEnabled();
		InputStream fis=null;
		try {
			if (mode==FileViewerAdapter.TEXT_BROWSER_BROWSE_MODE_CHAR) {
				tfli=tbidx_char.get(block_no);
				fis=cparms.input_file.getInputStreamByUri();
				byte[] buff= new byte[cparms.charModeBlockSize];
				fis.skip(tfli.start_pos);
				fis.read(buff);
				tal=new String[tfli.no_of_lines][2];
				if (log.isDebugEnabled()) 
					debugMsg("readFileBlock block","Char mode number="+block_no
							+", start_pos="+tfli.start_pos
							+", end_pos="+tfli.end_pos
							+", no of lines="+tfli.no_of_lines
							);
				createCharModeTextList(cparms,tc,tal,tfli,buff, (int)(tfli.end_pos-tfli.start_pos+1), isMonospaceFontUsed);
			} else {
				tfli=tbidx_hex.get(block_no);
                fis=cparms.input_file.getInputStreamByUri();
				byte[] buff= new byte[cparms.hexModeBlockSize];
                fis.skip(tfli.start_pos);
                fis.read(buff);
				tal=new String[tfli.no_of_lines][2];
				if (log.isDebugEnabled()) 
					debugMsg("readFileBlock block","Hex mode number="+block_no
							+", start_pos="+tfli.start_pos
							+", end_pos="+tfli.end_pos
							+", no of lines="+tfli.no_of_lines
							);
//				createHexModeTextList(cparms,tc,tal,tfli,buff, nr, tfli.no_of_lines);
				createHexModeTextList(cparms,tc,tal,tfli,buff, 
						(int)(tfli.end_pos-tfli.start_pos+1), tfli.no_of_lines);
			}
			fis.close();
		} catch (Exception e) {
			e.printStackTrace();
			tal=null;
			cparms.lastErrorMessage=e.getMessage();
            log.debug("readFileBlock error="+e.getMessage()+"\n"+MiscUtil.getStackTraceString(e));
		} finally {
			try {
				if (fis!=null) fis.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return tal;
	};

	private boolean mIndexCreationFinished=true;
	private NotifyEvent mParentNotifyInstance=null;
	final public boolean isIndexCreationFinished() {
		return mIndexCreationFinished;
	}
	final public void refreshParentResources(ProgressBar pb, TextView tv) {
		cparms.parentProgressBar=pb;
		cparms.parentMsgView=tv;
	}
	final public void startFileIndexCreation(final SafFile3 in_file, ThreadCtrl tc,
			NotifyEvent ntfy, ProgressBar pb, TextView tv, String enc_name,
			Handler hndl) {
		cparms.input_file =in_file;
		cparms.tcIndexReader=tc;
		cparms.tcIndexReader.setEnabled();
		cparms.tcIndexReader.setThreadActive(true);
		cparms.encodeName=enc_name;
		mParentNotifyInstance=ntfy;
		cparms.parentProgressBar=pb;
		cparms.parentProgressBar.setVisibility(ProgressBar.VISIBLE);
		cparms.parentProgressBar.setMax(100);
		cparms.parentProgressBar.setProgress(0);
		cparms.parentMsgView=tv;
		cparms.parentUiHandler=hndl;
		mIndexCreationFinished=false;
		
		cparms.parentMsgView.setVisibility(TextView.VISIBLE);
		cparms.parentMsgView.setText(cparms.context.getString(R.string.msgs_text_browser_file_reading_file));

		if (log.isDebugEnabled()) debugMsg("startFileIndexCreation"," fp="+in_file.getPath()+", encode="+enc_name);

		cparms.charModeFileIndex=new ArrayList<FileIndexListItem>();
		cparms.hexModeFileIndex=new ArrayList<FileIndexListItem>();

		refreshOptionMenu();
		
		Thread th=new Thread(new Runnable(){
			@SuppressLint("DefaultLocale")
			@Override
			public void run() {
                cparms.mime_type=in_file.getMimeType();
				if (!loadSavedFileIndex(cparms,cparms.charModeFileIndex,cparms.hexModeFileIndex)) {
					boolean result_hex=createHexModeIndexList(cparms, cparms.tcIndexReader);
					boolean result_char=createCharModeIndexList(cparms, cparms.tcIndexReader);
					mIndexCreationFinished=true;
					if (result_hex && result_char) {
//						Log.v("","id0 id="+Thread.currentThread().getId()+", name="+Thread.currentThread().getName());
						saveFileIndex(cparms,cparms.charModeFileIndex,cparms.hexModeFileIndex);
						cparms.tcIndexReader.setThreadActive(false);
						cparms.parentUiHandler.post(new Runnable() {
							@Override
							public void run() {
//								Log.v("","id0 id="+Thread.currentThread().getId()+", name="+Thread.currentThread().getName());
								cparms.parentProgressBar.setVisibility(ProgressBar.GONE);
								cparms.parentMsgView.setVisibility(TextView.GONE);
								cparms.parentMsgView.setText("");
								if (cparms.tcIndexReader.isEnabled()) mParentNotifyInstance.notifyToListener(true, null);
								else mParentNotifyInstance.notifyToListener(false, 
									new String[]{cparms.context.getString(R.string.msgs_text_browser_file_reading_cancelled)});
							}
						});
					} else {//index creation error
						cparms.charModeFileIndex.clear();
						cparms.hexModeFileIndex.clear();
						cparms.tcIndexReader.setThreadActive(false);
						cparms.parentUiHandler.post(new Runnable() {
							@Override
							public void run() {
								cparms.parentMsgView.setVisibility(TextView.GONE);
								cparms.parentMsgView.setText("");
								cparms.parentProgressBar.setVisibility(ProgressBar.GONE);
								mParentNotifyInstance.notifyToListener(false,new Object[]{cparms.lastErrorMessage});
							}
						});
					}
					refreshOptionMenu();
				} else {
					mIndexCreationFinished=true;
					cparms.tcIndexReader.setThreadActive(false);
//					Log.v("","id1 id="+Thread.currentThread().getId()+", name="+Thread.currentThread().getName());
					cparms.parentUiHandler.post(new Runnable() {
						@Override
						public void run() {
//							String tmp="id1 id="+Thread.currentThread().getId()+", name="+Thread.currentThread().getName();
//							Log.v("",tmp);
//							Log.v("","id1 id="+Thread.currentThread().getId()+", name="+Thread.currentThread().getName());
							cparms.parentProgressBar.setVisibility(ProgressBar.GONE);
							cparms.parentMsgView.setVisibility(TextView.GONE);
							cparms.parentMsgView.setText("");
							if (cparms.tcIndexReader.isEnabled()) mParentNotifyInstance.notifyToListener(true, null);
							else mParentNotifyInstance.notifyToListener(false, null);
						}
					});
					refreshOptionMenu();
				}
			}
			
		}) ;
		
//		th.setPriority(Thread.MIN_PRIORITY);
		th.start();
	};
	
	final private void refreshOptionMenu() {
		cparms.parentUiHandler.post(new Runnable(){
			@SuppressLint("NewApi")
			@Override
			public void run() {
				if (cparms.tcIndexReader.isThreadActive()) {
					cparms.mainActivity.setUiDisabled();
				} else {
					cparms.mainActivity.setUiEnabled();
				}
				if (Build.VERSION.SDK_INT >= 11) cparms.mainActivity.invalidateOptionsMenu();				
			}
		});
	}

	final public int getCharModeLineCount() {
		int row_char=0;
		for (int i=0;i<cparms.charModeFileIndex.size();i++)
			row_char+=cparms.charModeFileIndex.get(i).no_of_lines;
		if (log.isDebugEnabled()) 
			debugMsg("getCharModeLineCount","row count="+row_char);
		return row_char;
	};
	final public int getHexModeLineCount() {
		int row_hex=0;
		for (int i=0;i<cparms.hexModeFileIndex.size();i++)
			row_hex+=cparms.hexModeFileIndex.get(i).no_of_lines;
		if (log.isDebugEnabled()) debugMsg("getHexModeLineCount","row count="+row_hex);
		return row_hex;
	};
	final public int getCharModeMaxLineLength() {
		int wm=0;
		for (int i=0;i<cparms.charModeFileIndex.size();i++) {
			if (wm<cparms.charModeFileIndex.get(i).max_string_length) {
				wm=cparms.charModeFileIndex.get(i).max_string_length;
			}
		}
		if (log.isDebugEnabled()) debugMsg("getCharModeMaxLengthString","max line length="+wm);
		return wm;
	};

    static final public void removeIndexCache(Context c) {
        String dir=getCacheDirectory(c);
        File lf=new File(dir);
        File[] fl=lf.listFiles();
        for (File fl_item : fl) {
            fl_item.delete();
            if (log.isDebugEnabled()) debugMsg("houseKeepIndexCacheFolder","file "+fl_item.getPath()+" was deleted");
        }
    };

    final public void houseKeepIndexCacheFolder(String option) {
		cparms.defaultSettingIndexCache=option;
		String dir=LocalMountPoint.getExternalStorageDir()+"/TextFileBrowser/cache/";
		File lf=new File(dir);
		File[] fl=lf.listFiles();
		if (fl!=null && fl.length!=0) {
			if (option.equals(Constants.INDEX_CACHE_NO_CACHE)) {
				for (File fl_item : fl) {
					fl_item.delete();
					if (log.isDebugEnabled()) debugMsg("houseKeepIndexCacheFolder","file "+fl_item.getPath()+" was deleted");
				}
			} else if (option.equals(Constants.INDEX_CACHE_UP_TO_10MB)) {
				long size=0;
				for (File fl_item : fl) {
					size+=fl_item.length();
				}
				if (size>(10*1024*1024)) removeOldIndexCache(cparms,fl, (long)(10*1024*1024));
			} else if (option.equals(Constants.INDEX_CACHE_UP_TO_50MB)) {
				long size=0;
				for (File fl_item : fl) {
					size+=fl_item.length();
				}
				if (size>(50*1024*1024)) removeOldIndexCache(cparms,fl, (long)(50*1024*1024));			}
		}
	};

	final static private void removeOldIndexCache(CommonParms cparms, File[] fl, Long limit) {
		ArrayList<String> sort_list=new ArrayList<String>();
		String sort_item="";
		long c_size=0;
		for (File fl_item : fl) {
			sort_item=fl_item.lastModified()+","+
					String.format("%16s",fl_item.length())+","+fl_item.getPath();
			sort_list.add(sort_item);
			c_size+=fl_item.length();
		}
		Collections.sort(sort_list);
		for (int i=0;i<sort_list.size();i++) {
			String[] itlist=sort_list.get(i).split(",");
			if (log.isDebugEnabled()) debugMsg("removeOldIndexCache","file "+itlist[2]+" was deleted");
			File lf=new File(itlist[2]);
			lf.delete();
			c_size-=Long.valueOf(itlist[1].trim());
			if (c_size<=limit) break;
		}
	};

	static private String getCacheDirectory(Context c) {
        String dir=c.getExternalFilesDir(null).getPath()+"/index_cache/";
        return dir;
    }

	final static private boolean loadSavedFileIndex(CommonParms cparms,
			ArrayList<FileIndexListItem> tbidx_char, ArrayList<FileIndexListItem> tbidx_hex) {
		boolean result=false;
		if (cparms.defaultSettingIndexCache.equals(Constants.INDEX_CACHE_NO_CACHE))
			return result;
//		String dir=LocalMountPoint.getExternalStorageDir()+"/TextFileBrowser/cache/";
        String dir=getCacheDirectory(cparms.context);
		String idx_path="";
		try {
			idx_path = dir+EncryptUtilV3.makeSHA1Hash(cparms.input_file.getPath());
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		}
		File lf = new File(idx_path);
		log.info("loadSavedFileIndex index fp="+idx_path);
		if (lf.exists()) {
		    try {
		    	FileInputStream fis = new FileInputStream(lf);
		    	BufferedInputStream buf=new BufferedInputStream(fis,CACHE_FILE_BUFFER_SIZE);
			    ObjectInputStream ois = new ObjectInputStream(buf);
			    FileIndexCacheHolder fich=new FileIndexCacheHolder();
			    fich.readExternal(ois);
			    ois.close();
			    InputStream is=cparms.input_file.getInputStreamByUri();

			    log.info("loadFileIndex fich.file_size="+fich.file_size+", is.available()="+is.available()+
                        ", fich.last_modified="+fich.last_modified+", file_last_modified="+cparms.input_file.lastModified()+
                        ", fich.char_index.size()="+fich.char_index.size()+
                        ", fich.char_blksize="+fich.char_blksize+", cparms.charModeBlockSize="+cparms.charModeBlockSize+
                        ", fich.hex_blksize="+fich.hex_blksize+", cparms.hexModeBlockSize="+cparms.hexModeBlockSize+
                        ", fich.tab_stop="+fich.tab_stop+", cparms.tab_stop_value="+cparms.tab_stop_value+
                        ", cparms.encodeName="+cparms.encodeName+", fich.encode_name="+fich.encode_name);
			    if (fich.file_size==is.available() &&
			    		fich.char_index.size()>0 &&
			    		fich.last_modified==cparms.input_file.lastModified() &&
			    		fich.char_blksize==cparms.charModeBlockSize &&
			    		fich.hex_blksize==cparms.hexModeBlockSize &&
			    		fich.tab_stop==cparms.tab_stop_value){
			    	String enc_name="";
			    	if (fich.encode_name!=null) enc_name=fich.encode_name;
//                  if (cparms.encodeName.equals("")||enc_name.equals(cparms.encodeName)) {
			    	if (enc_name.equals(cparms.encodeName) ||
                            (!enc_name.equals("") && cparms.encodeName.equals(""))) {
			    		cparms.encodeName=enc_name;
					    tbidx_char.addAll(fich.char_index);
					    tbidx_hex.addAll(fich.hex_index);
					    result=true;
					    if (log.isDebugEnabled()) 
					    	debugMsg("loadSavedFileIndex","saved index was loaded"+
							    ", encode="+enc_name+
							    ", char index size="+tbidx_char.size()+
							    ", hex index size="+tbidx_hex.size());
					    
					    lf=new File(idx_path);
					    lf.setLastModified(System.currentTimeMillis());
			    	} else {
			    		deleteSavedIndexFile(cparms,idx_path);
                        debugMsg("loadSavedFileIndex","Encode name was changed="+cparms.encodeName+", fich="+fich.encode_name);
			    	}
			    } else {
			    	deleteSavedIndexFile(cparms,idx_path);
                    debugMsg("loadSavedFileIndex","Other was changed="+cparms.encodeName+", fich="+fich.encode_name);
                    if (cparms.encodeName.equals("")) cparms.encodeName=fich.encode_name;
			    }
			} catch (StreamCorruptedException e) {
				e.printStackTrace();
				cparms.lastErrorMessage=e.getMessage();
                log.debug("loadSavedFileIndex error="+e.getMessage()+"\n"+MiscUtil.getStackTraceString(e));
			} catch (Exception e) {
				e.printStackTrace();
				cparms.lastErrorMessage=e.getMessage();
                log.debug("loadSavedFileIndex error="+e.getMessage()+"\n"+MiscUtil.getStackTraceString(e));
			}
		} else if (log.isDebugEnabled()) debugMsg("loadSavedFileIndex","saved index can not be found");

		return result;
	};

	final static private void deleteSavedIndexFile(CommonParms cparms, String idx_path) {
	    File lf=new File(idx_path);
    	lf.delete();
    	if (log.isDebugEnabled()) 
    		debugMsg("loadSavedFileIndex","saved index was invalid, index was deleted");
	};
	
	final static private void saveFileIndex(CommonParms cparms,
			ArrayList<FileIndexListItem> tbidx_char, ArrayList<FileIndexListItem> tbidx_hex) {

		if (cparms.defaultSettingIndexCache.equals(Constants.INDEX_CACHE_NO_CACHE))
			return ;
		
		if (tbidx_char.size()==0) return;

		String dir=getCacheDirectory(cparms.context);

		String idx_path="";
		try {
			idx_path = dir+EncryptUtilV3.makeSHA1Hash(cparms.input_file.getPath());
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		}
		
		File lf=new File(dir);
		if (!lf.exists()) lf.mkdirs();

		log.info("saveFileIndex Index fp="+idx_path);
		if (lf.exists()) {
		    try {
		    	FileOutputStream fos = new FileOutputStream(idx_path);
		    	BufferedOutputStream buf =new BufferedOutputStream(fos,CACHE_FILE_BUFFER_SIZE);
			    ObjectOutputStream oos = new ObjectOutputStream(buf);
			    FileIndexCacheHolder fich = new FileIndexCacheHolder();
			    fich.file_path=cparms.input_file.getPath();
			    fich.encode_name=cparms.encodeName;
			    InputStream is=cparms.input_file.getInputStreamByUri();
			    fich.file_size=is.available();
			    fich.last_modified=cparms.input_file.lastModified();
			    
			    fich.char_blksize=cparms.charModeBlockSize;
			    fich.hex_blksize=cparms.hexModeBlockSize;
			    
			    fich.char_index=tbidx_char;
			    fich.hex_index=tbidx_hex;
			    
			    fich.tab_stop=cparms.tab_stop_value;

			    fich.writeExternal(oos);
			    oos.close();
		    	if (log.isDebugEnabled()) debugMsg("saveFileIndex","index was saved"+
		    				", Encode="+cparms.encodeName);
                log.info("saveFileIndex fich.file_size="+fich.file_size+", is.available()="+is.available()+", fich.char_index.size()="+fich.char_index.size()+
                        ", fich.char_blksize="+fich.char_blksize+", cparms.charModeBlockSize="+cparms.charModeBlockSize+
                        ", fich.hex_blksize="+fich.hex_blksize+", cparms.hexModeBlockSize="+cparms.hexModeBlockSize+
                        ", fich.tab_stop="+fich.tab_stop+", cparms.tab_stop_value="+cparms.tab_stop_value+
                        ", cparms.encodeName="+cparms.encodeName+", fich.encode_name="+fich.encode_name);
			} catch (StreamCorruptedException e) {
				e.printStackTrace();
				cparms.lastErrorMessage=e.getMessage();
                log.debug("saveFileIndex error="+e.getMessage()+"\n"+MiscUtil.getStackTraceString(e));
			} catch (Exception e) {
				e.printStackTrace();
				cparms.lastErrorMessage=e.getMessage();
                log.debug("saveFileIndex error="+e.getMessage()+"\n"+MiscUtil.getStackTraceString(e));
			} 
		} 
	};

	final static private boolean createCharModeIndexList(final CommonParms cparms, ThreadCtrl tc) {
//		if (log.isDebugEnabled()) debugMsg("createCharModeIndexList","entered");
        if (log.isDebugEnabled()) debugMsg("createCharModeIndexList","entered encoding="+cparms.encodeName);
		boolean success=true;
		
		InputStream fis=null;
		BufferedInputStream bis=null;
		try {
			fis=cparms.input_file.getInputStreamByUri();
			bis=new BufferedInputStream(fis,INDEX_CREATE_BUFFER_SIZE);
		    long f_size=bis.available();
//		    Log.v("","fs="+lf.length()+", f_s="+f_size);
		    long f_idx=0, f_read_size=0;;
		    byte[] buff=new byte[cparms.charModeBlockSize];
//		    Log.v("","blksize="+cparms.charModeBlockSize);
		    int b_remnant=0, b_idx=0;
	    	int b_read_size=0;
	    	long tprog=0;
		    while(f_idx<f_size) {
		    	if (!tc.isEnabled()) break;
	    		tprog=(f_idx*100)/f_size;
//	    		log.trace("tprog="+tprog+", f_idx="+f_idx+", f_size="+f_size);
	    		if (cparms.index_create_progress==-1 || tprog!=cparms.index_create_progress) {
	    			cparms.index_create_progress=tprog;
	    			cparms.parentUiHandler.post(new Runnable(){
						@Override
						public void run() {
							cparms.parentProgressBar.setProgress((int)cparms.index_create_progress);
						}
	    			});
	    		}
		    	if (b_remnant==0) {
		    		f_read_size=bis.read(buff,0,cparms.charModeBlockSize);
		    		b_read_size=(int)f_read_size;
//		    		log.trace("rem=0 f_read_size="+f_read_size+", b_read_size="+b_read_size);
		    	} else {
//                    log.trace("rem>0 b_idx="+b_idx+", b_read_size="+b_read_size);
		    		for (int i=b_idx;i<=b_read_size-1;i++) 
		    			buff[i-b_idx]=buff[i];

		    		f_read_size=bis.read(buff,b_remnant,(cparms.charModeBlockSize-b_remnant));
		    		if (f_read_size==-1) b_read_size=(int)(b_remnant);
		    		else b_read_size=(int)(f_read_size+b_remnant);
//                    log.trace("rem>0 f_read_size="+f_read_size+", b_read_size="+b_read_size+", b_remnant="+b_remnant);
		    	}
		    	if (b_read_size>0) {
//                    log.trace("b_read_size="+b_read_size+", f_idx="+f_idx);
		    		b_idx=determinCharModeNoOfLine(cparms,cparms.charModeFileIndex,buff,b_read_size,f_idx);
		    		b_remnant=(b_read_size-b_idx);
		    		f_idx+=b_idx;
//                    log.trace("b_idx="+b_idx+", f_idx="+f_idx+", b_remnant="+b_remnant);
		    	} else break;
		    }
		} catch (Exception e) {
			e.printStackTrace();
			success=false;
			cparms.lastErrorMessage=e.getMessage();
            log.debug("createCharModeIndexList error="+e.getMessage()+"\n"+MiscUtil.getStackTraceString(e));
		} finally {
			try {
				if (bis!=null) bis.close();
			} catch (IOException e) {
				e.printStackTrace();
				success=false;
			}
		}
		if (tc.isEnabled()) {
			if (success) {
				for (int i=1;i<cparms.charModeFileIndex.size();i++) {
					cparms.charModeFileIndex.get(i).block_number=i;
					cparms.charModeFileIndex.get(i).start_line_number=
							cparms.charModeFileIndex.get(i-1).start_line_number+
							cparms.charModeFileIndex.get(i-1).no_of_lines;
				}
				int tot_lines=0;
				for (int i=0;i<cparms.charModeFileIndex.size();i++) {
//					if (log.isDebugEnabled()) 
//						debugMsg("createCharModeIndexList","TextFileCharIndex "+
//							" block="+cparms.charModeFileIndex.get(i).block_number+
//							", start pos="+cparms.charModeFileIndex.get(i).start_pos+
//							", end pos="+cparms.charModeFileIndex.get(i).end_pos+
//							", start line number="+cparms.charModeFileIndex.get(i).start_line_number+
//							", number of lines="+cparms.charModeFileIndex.get(i).no_of_lines);
					tot_lines+=cparms.charModeFileIndex.get(i).no_of_lines;
				}
				if (log.isDebugEnabled()) 
					debugMsg("createCharModeIndexList",
						" created list size="+cparms.charModeFileIndex.size()+", total lines="+tot_lines);
			} else {
				if (log.isDebugEnabled()) debugMsg("createCharModeIndexList","index creation error.");
			}
		}
		return success;
	};

	final static private boolean createHexModeIndexList(CommonParms cparms, ThreadCtrl tc) {
		if (log.isDebugEnabled()) debugMsg("createHexModeIndexList","entered");
		boolean success=true;
        try {
            InputStream is=cparms.input_file.getInputStreamByUri();
            long f_size=is.available();
            for (long f_idx=0;f_idx<f_size;f_idx+=cparms.hexModeBlockSize) {
                if (!tc.isEnabled()) {
                    break;
                }
                FileIndexListItem tfcli=new FileIndexListItem();
                tfcli.start_pos=f_idx;
                if ((f_idx+cparms.hexModeBlockSize)<=f_size) {
                    tfcli.end_pos=f_idx+cparms.hexModeBlockSize-1;
//				if (HexModeBlockSize%16>0) tfcli.no_of_lines=HexModeBlockSize/16+1;
//				else tfcli.no_of_lines=HexModeBlockSize/16;
                    tfcli.no_of_lines=cparms.hexModeBlockSize/16;
                } else {
                    tfcli.end_pos=f_size-1;
                    if (((f_size-f_idx)%16)==0) {
                        tfcli.no_of_lines=(int)((f_size-f_idx)/16);
                    } else {
                        tfcli.no_of_lines=(int)((f_size-f_idx)/16)+1;
                    }
//				Log.v("","f_size="+f_size+", f_idx="+f_idx+", end_pos="+tfcli.end_pos+", no of lines="+tfcli.no_of_lines);
                }
                cparms.hexModeFileIndex.add(tfcli);
//				Log.v("","Hex  start="+tfcli.start_pos+", end="+tfcli.end_pos+", lines="+tfcli.no_of_lines);
            }
            if (tc.isEnabled()) {
                for (int i=1;i<cparms.hexModeFileIndex.size();i++) {
                    cparms.hexModeFileIndex.get(i).block_number=i;
                    cparms.hexModeFileIndex.get(i).start_line_number=
                            cparms.hexModeFileIndex.get(i-1).start_line_number+
                                    cparms.hexModeFileIndex.get(i-1).no_of_lines;
                }
                int tot_lines=0;
                for (int i=0;i<cparms.hexModeFileIndex.size();i++) {
                    tot_lines+=cparms.hexModeFileIndex.get(i).no_of_lines;
//				if (log.isDebugEnabled())
//					debugMsg("createHexModeIndexList","TextFileHexIndex "+
//						" block="+cparms.hexModeFileIndex.get(i).block_number+
//						", start pos="+cparms.hexModeFileIndex.get(i).start_pos+
//						", end pos="+cparms.hexModeFileIndex.get(i).end_pos+
//						", start line number="+cparms.hexModeFileIndex.get(i).start_line_number+
//						", number of lines="+cparms.hexModeFileIndex.get(i).no_of_lines);
                }
                if (log.isDebugEnabled())
                    debugMsg("createHexModeIndexList","created list size="+cparms.hexModeFileIndex.size()+
                            ", total lines="+tot_lines);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.debug("createHexModeIndexList error="+e.getMessage()+"\n"+MiscUtil.getStackTraceString(e));
        }
        return success;
	};

	final static private int determinCharModeNoOfLine(CommonParms cparms,
			ArrayList<FileIndexListItem> tbidx,
			byte[] buff, long bufsz, long spos) {
//		hexString("determinCharModeNoOfLine", buff, 0, (int) bufsz);
		String t_encode_name="";
//		if (cparms.encodeName.equals("") ||
////                cparms.autoDetectEncodeAllFile ||
//				(cparms.mime_type!=null && cparms.mime_type.startsWith("text"))) {
        if (cparms.encodeName.equals("")) {
			t_encode_name=determinCharset(cparms, buff, (int)bufsz);
            if (log.isDebugEnabled()) debugMsg("determinCharModeNoOfLine","Detected Encoding="+t_encode_name);
			if (t_encode_name!=null && cparms.encodeName.equals("")) {
				cparms.encodeName=t_encode_name;
			} else {
                if (t_encode_name==null) {
                    cparms.encodeName=cparms.defaultEncodeName;
                    cparms.encodingAutoDetectFailed=true;
                    cparms.uiHandler.post(new Runnable(){
                        @Override
                        public void run() {
                            CommonDialog.showToastLong(cparms.mainActivity, "Auto detect failed, Encoding assumed="+cparms.encodeName);
                        }
                    });
                    if (log.isDebugEnabled()) debugMsg("determinCharModeNoOfLine","Auto detect failed, Encoding assumed="+cparms.encodeName);
                }
            }
		} else {
            if (log.isDebugEnabled()) debugMsg("determinCharModeNoOfLine","Specified Encoding name="+cparms.encodeName);
        }
		
		FileIndexListItem tfcli=new FileIndexListItem();
		tfcli.start_pos=spos;
		boolean breaked=false;
		int s_idx=0;
		int b_idx=0, b_incr=1;
		int prev_start=0;
		while (b_idx<bufsz) {
			if (!(buff[b_idx]==0x0d)) {//Ignore Line feed
				if (buff[b_idx]==0x0a) {//New line found
					if (tfcli.max_string_length<((int)(b_idx-s_idx-1))){
						tfcli.max_string_length=((int)(b_idx-s_idx-1));
					}
					if ((cparms.encodeName.equals("UTF-16LE")||
						 cparms.encodeName.equals("UTF-32LE"))) {
						s_idx=b_idx+2;
						tfcli.no_of_lines++;
						tfcli.end_pos=b_idx+1;
						b_idx++;
					} else {
						s_idx=b_idx+1;
						tfcli.no_of_lines++;
						tfcli.end_pos=b_idx;
					}
					breaked=true;
//                    log.trace("b_idx="+b_idx+", bufsz="+bufsz+", s_idx="+s_idx+", breaked="+breaked+", lines="+tfcli.no_of_lines+", endpos="+tfcli.end_pos);
//                    hexString("determinCharModeNoOfLine", buff, prev_start, ((int)tfcli.end_pos-prev_start));
//                    String txt=new String(buff,prev_start,((int)tfcli.end_pos-prev_start));
//                    Log.v(APPLICATION_TAG,txt);
                    prev_start=b_idx+1;
				}
			}
			b_idx+=b_incr;
		}
		if (!breaked) {//force new line
			tfcli.no_of_lines++;
			tfcli.end_pos=bufsz-1;
            tfcli.max_string_length=((int)(b_idx-s_idx-1));
		}		
		tfcli.end_pos+=spos;
		tbidx.add(tfcli);
		if (log.isDebugEnabled()) debugMsg("determinCharModeNoOfLine"," spos="+spos+
				", no of lines="+tfcli.no_of_lines+
				", start pos="+tfcli.start_pos+
				", end pos="+tfcli.end_pos+
				", processd byte count="+(tfcli.end_pos-tfcli.start_pos+1)+
				", max_line_length="+tfcli.max_string_length);
		return (int)(tfcli.end_pos-tfcli.start_pos+1);//processed byte count
	}
	
	
	private final static String[] hex_table=new String[] {
		"00 ","01 ","02 ","03 ","04 ","05 ","06 ","07 ","08 ","09 ","0a ","0b ","0c ","0d ","0e ","0f ",
		"10 ","11 ","12 ","13 ","14 ","15 ","16 ","17 ","18 ","19 ","1a ","1b ","1c ","1d ","1e ","1f ",
		"20 ","21 ","22 ","23 ","24 ","25 ","26 ","27 ","28 ","29 ","2a ","2b ","2c ","2d ","2e ","2f ",
		"30 ","31 ","32 ","33 ","34 ","35 ","36 ","37 ","38 ","39 ","3a ","3b ","3c ","3d ","3e ","3f ",
		"40 ","41 ","42 ","43 ","44 ","45 ","46 ","47 ","48 ","49 ","4a ","4b ","4c ","4d ","4e ","4f ",
		"50 ","51 ","52 ","53 ","54 ","55 ","56 ","57 ","58 ","59 ","5a ","5b ","5c ","5d ","5e ","5f ",
		"60 ","61 ","62 ","63 ","64 ","65 ","66 ","67 ","68 ","69 ","6a ","6b ","6c ","6d ","6e ","6f ",
		"70 ","71 ","72 ","73 ","74 ","75 ","76 ","77 ","78 ","79 ","7a ","7b ","7c ","7d ","7e ","7f ",
		"80 ","81 ","82 ","83 ","84 ","85 ","86 ","87 ","88 ","89 ","8a ","8b ","8c ","8d ","8e ","8f ",
		"90 ","91 ","92 ","93 ","94 ","95 ","96 ","97 ","98 ","99 ","9a ","9b ","9c ","9d ","9e ","9f ",
		"a0 ","a1 ","a2 ","a3 ","a4 ","a5 ","a6 ","a7 ","a8 ","a9 ","aa ","ab ","ac ","ad ","ae ","af ",
		"b0 ","b1 ","b2 ","b3 ","b4 ","b5 ","b6 ","b7 ","b8 ","b9 ","ba ","bb ","bc ","bd ","be ","bf ",
		"c0 ","c1 ","c2 ","c3 ","c4 ","c5 ","c6 ","c7 ","c8 ","c9 ","ca ","cb ","cc ","cd ","ce ","cf ",
		"d0 ","d1 ","d2 ","d3 ","d4 ","d5 ","d6 ","d7 ","d8 ","d9 ","da ","db ","dc ","dd ","de ","df ",
		"e0 ","e1 ","e2 ","e3 ","e4 ","e5 ","e6 ","e7 ","e8 ","e9 ","ea ","eb ","ec ","ed ","ee ","ef ",
		"f0 ","f1 ","f2 ","f3 ","f4 ","f5 ","f6 ","f7 ","f8 ","f9 ","fa ","fb ","fc ","fd ","fe ","ff "};

	final static private void createHexModeTextList(
			CommonParms cparms, ThreadCtrl tc,String[][]tal, 
			FileIndexListItem tbil,byte[] buff, int byte_count, int no_of_lines) {
		if (!tc.isEnabled()) return;
//		Log.v("","bc="+byte_count+", nl="+no_of_lines);
		StringBuffer hex_image=new StringBuffer(48);
		byte[] char_image=new byte[16];
		for (int i=0;i<byte_count;i+=16) {
//			if (!tc.isEnable()) {
//				break;
//			}
			for (int j=0;j<16;j++) {
				if ((i+j)<byte_count) {
					hex_image.append(hex_table[(0xff&buff[j+i])]);
//					hex_image.append(" ");
					if (buff[i+j]<0x20 || buff[i+j]>0x7e ) {
						char_image[j]=0x2e;
					} else char_image[j]=buff[i+j];
				} else {
					hex_image.append("   ");
					char_image[j]=0x20;
				}
			}
			tal[cparms.cache_text_list_index][0]=hex_image.toString();
			tal[cparms.cache_text_list_index][1]=new String(char_image);
			cparms.cache_text_list_index++;
//			if (log.isDebugEnabled()) debugMsg("createHexModeTextList","text="+hex_image.toString());
			hex_image.setLength(0);
		}
	};

	final static private void createCharModeTextList(
			CommonParms cparms, ThreadCtrl tc,String[][]tal, 
			FileIndexListItem tbil,byte[] fileIoArea, int byte_count, boolean isMonospaceFontUsed) {
		if (log.isDebugEnabled()) debugMsg("createCharModeTextList","entered byte count="+byte_count);
//		hexString("",fileIoArea,0,byte_count);
		if (!cparms.encodeName.equals("")) {
			String line=convertCharset(cparms,cparms.encodeName, tal,fileIoArea,0,byte_count);
//			String n_line=line.replaceAll("\t", "     ");
			int ll=line.length();
			String wl="";
			cparms.sbCharModeText.setLength(0);
			for (int i=0;i<ll;i++) {
				wl=line.substring(i,i+1);
				if (wl.equals("\t")) {
				    if (isMonospaceFontUsed) {
                        int ts=cparms.tab_stop_value-
                                (cparms.sbCharModeText.length()%cparms.tab_stop_value);
                        for (int j=0;j<ts;j++) {
                            cparms.sbCharModeText.append(" ");
                        }
                    } else {
                        cparms.sbCharModeText.append(" ");
                    }
				} else {
					cparms.sbCharModeText.append(wl);
				}
			}
			String[] n_array=cparms.sbCharModeText.toString().split("\n");
//            log.trace("tal_length="+tal.length);
//            for(String l_item:n_array) Log.v(APPLICATION_TAG,"Line="+l_item);
			for (int i=0;i<tal.length;i++) {
				if (i<n_array.length) {
					tal[cparms.cache_text_list_index][0]=n_array[i];
				} else {
//					hexString("Line",line.getBytes(),0,line.getBytes().length);
				}
				cparms.cache_text_list_index++;
			}
		} else {
			byte[] out_area=new byte[byte_count*2];
			int out_idx=-1;

			for (int i=0;i<byte_count;i++) {
				if ((i%500)==0 && !tc.isEnabled()) {
					break;
				}
				if (!(fileIoArea[i]==0x0d)) {//Ignore Line feed
					if (fileIoArea[i]==0x0a) {//New line found
						tal[cparms.cache_text_list_index][0]=
							convertCharset(cparms,null, tal,out_area,0,out_idx+1);
						cparms.cache_text_list_index++;
						out_idx=-1;
					} else {//Accumlate output area
						out_idx++;
			 			if (fileIoArea[i]==0x09) {//Tab
			 			    if (isMonospaceFontUsed) {
                                int ts=cparms.tab_stop_value-(out_idx%cparms.tab_stop_value);
                                for (int n_ts=0;n_ts<(ts+1);n_ts++) {
                                    out_area[out_idx+n_ts]=0x20;
                                }
                                out_idx+=ts;
                            } else {
                                out_area[out_idx]=0x20;
                                out_idx++;
                            }
						} else if (//Unprintable
								(fileIoArea[i]<0x20 || fileIoArea[i]>0x7e )) {
							out_area[out_idx]=0x2e;//replace period(2e) ?(3f)
						} else {
							out_area[out_idx] = fileIoArea[i];
						}
					}
				}
			}
			if (out_idx!=-1) {
				tal[cparms.cache_text_list_index][0]=
						convertCharset(cparms,null, tal,out_area,0,out_idx+1);
				cparms.cache_text_list_index++;
			}
		}
		if (log.isDebugEnabled()) debugMsg("createCharModeTextList","list size="+tal.length);

//		Log.v("","list size="+tal.size()+", block="+byte_count);
	};
	
	final public static void hexString(String id, byte[]in, int offset, int count) {
		String str = "";
		String sep=id;
		for(int i=offset; i<offset+count; i++) {
			if ((i%16)==0) {
				if (i!=0) {
					Log.v("HexString",str);
					str="";
				}
				str+=sep+" "+String.format("%08x ", i);
				str += String.format("%02x ", in[i]);	
			} else {
				str += String.format("%02x ", in[i]);
			}
		}
		if (str.length()!=0) Log.v("HexString",str);
	};

	final static private String determinCharset(CommonParms cparms, byte[] input, int bufsz) {
		String encoding="";
//		if (cparms.encodeName.equals("")) {
//		} else {
//			encoding=cparms.encodeName;
//		}
	    UniversalDetector detector = new UniversalDetector(null);
	    
	    detector.handleData(input, 0, bufsz);
	    detector.dataEnd();
	    encoding = detector.getDetectedCharset();
	    detector.reset();
//	    if (log.isDebugEnabled()) debugMsg("determinCharset","detected encoding name="+encoding);
	    if (encoding!=null) {
	    	try {
	    		if (!Charset.isSupported(encoding)) encoding=null;
	    	} catch (IllegalCharsetNameException e) {
	    		e.printStackTrace();
	    		encoding=null;
	    		cparms.lastErrorMessage=e.getMessage();
                log.debug("determinCharset error="+e.getMessage()+"\n"+MiscUtil.getStackTraceString(e));
	    	}
	    }
	    return encoding;
	};

	final static private String convertCharset(CommonParms cparms, String encoding,String[][]tal, 
			byte[] in, int pos, int length) {
		String text="";
//		hexString("convertCharset", in, pos, length);
		try {
//			debugMsg("convertCharset","encode=\""+encoding+"\"");
			if (encoding==null||encoding.equals("")) {
				text=new String(in,pos,length);
			} else {
				text=new String(in,pos,length,encoding);
			}
//			if (log.isDebugEnabled()) debugMsg("convertCharset","text="+text);

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			cparms.lastErrorMessage=e.getMessage();
            log.debug("convertCharset error="+e.getMessage()+"\n"+MiscUtil.getStackTraceString(e));
		}
		return text;
	};
	//エンド

    static class CachePool {
        public int block=0;
        public String[][] cache=null;
        public int start_line_number=0, end_line_number=0;
    }

    static class FileIndexCacheHolder implements Externalizable {
        private static final long serialVersionUID = 26L;
        public String file_path="";
        public String encode_name=null;
        public int tab_stop=4;
        public long last_modified=0, file_size=0;
        public int char_blksize=0, hex_blksize=0;
        public ArrayList<FileIndexListItem> char_index;
        public ArrayList<FileIndexListItem> hex_index;

        public FileIndexCacheHolder() {}

        @Override
        public void readExternal(ObjectInput input) throws IOException,
                ClassNotFoundException {
            long sid=input.readLong();
            if (serialVersionUID!=sid) {
                throw new IOException("serialVersionUID was not matched by saved UID");
            }
            file_path=input.readUTF();
            encode_name=input.readUTF();
            last_modified=input.readLong();
            file_size=input.readLong();
            char_blksize=input.readInt();
            hex_blksize=input.readInt();
            tab_stop=input.readInt();
            char_index=readArrayList(input);
            hex_index=readArrayList(input);
        }
        @Override
        public void writeExternal(ObjectOutput output) throws IOException {
            output.writeLong(serialVersionUID);
            output.writeUTF(file_path);
            output.writeUTF(encode_name);
            output.writeLong(last_modified);
            output.writeLong(file_size);
            output.writeInt(char_blksize);
            output.writeInt(hex_blksize);
            output.writeInt(tab_stop);
            writeArrayList(output,char_index);
            writeArrayList(output,hex_index);
        };

        private static void writeArrayList(ObjectOutput output, ArrayList<FileIndexListItem>list)
                throws IOException {
            int lsz=-1;
            if (list==null) output.writeInt(lsz);
            else {
                lsz=list.size();
                output.writeInt(lsz);
                for (int i=0;i<lsz;i++) list.get(i).writeExternal(output);
            }
        }
        private static ArrayList<FileIndexListItem> readArrayList(ObjectInput input)
                throws IOException, ClassNotFoundException {
            ArrayList<FileIndexListItem> result=null;
            int lsz=-2;
            lsz=input.readInt();
            if (lsz!=-1) {
                result=new ArrayList<FileIndexListItem>();
                for (int i=0;i<lsz;i++) {
                    FileIndexListItem fil=new FileIndexListItem();
                    fil.readExternal(input);
                    result.add(fil);
                }
            }
            return result;
        }
    }

    static class FileIndexListItem implements Externalizable {
        private static final long serialVersionUID = 21L;
        public int block_number=0;
    //		public String encode_name=null;
    //		public String uri_file_info="";

        public long start_pos=0;
        public long end_pos=0;
        public int  no_of_lines=0, start_line_number=0;
        public int max_string_length=0;

        public FileIndexListItem() {}

        @Override
        public void readExternal(ObjectInput input) throws IOException,
                ClassNotFoundException {
            long sid=input.readLong();
            if (serialVersionUID!=sid) {
                throw new IOException("serialVersionUID was not matched by saved UID");
            }

            block_number=input.readInt();
    //			encode_name=(String)input.readObject();
    //			uri_file_info=input.readUTF();
            start_pos=input.readLong();
            end_pos=input.readLong();
            no_of_lines=input.readInt();
            start_line_number=input.readInt();
            max_string_length=input.readInt();
        }
        @Override
        public void writeExternal(ObjectOutput output) throws IOException {
            output.writeLong(serialVersionUID);

            output.writeInt(block_number);
    //			output.writeObject(encode_name);
    //			output.writeUTF(uri_file_info);
            output.writeLong(start_pos);
            output.writeLong(end_pos);
            output.writeInt(no_of_lines);
            output.writeInt(start_line_number);
            output.writeInt(max_string_length);
        }
    }
}
