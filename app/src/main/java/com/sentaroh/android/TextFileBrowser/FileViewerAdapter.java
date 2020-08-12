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

import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.ThemeColorList;
import com.sentaroh.android.Utilities3.ThemeUtil;
import com.sentaroh.android.Utilities3.Widget.NonWordwrapTextView;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class FileViewerAdapter extends BaseAdapter{
    private final static String APPLICATION_TAG="TextFileBrowser";
    private static Logger log = LoggerFactory.getLogger(FileViewerAdapter.class);

	private Context mContext;
	private int mResourceId;

	private GlobalParameters mGp=null;

	private IndexedFileReader fcReader;
	private Resources resources;
	
	private int copy_from_line=-1, copy_to_line=-1;
	private boolean copy_active=false;
	
	private Typeface view_type_face;
	private int view_font_size;
	private int view_line_break=1;
	private int view_measured_width=0;
	private boolean view_show_lineno=true;
	
	private int view_horizontal_pos=0;
	private int view_max_string_length=0;
	private int view_show_number_of_char=0;
	
	private int browse_mode=0;
	public static final int TEXT_BROWSER_BROWSE_MODE_CHAR=0;
	public static final int TEXT_BROWSER_BROWSE_MODE_HEX=1;
	
	private int row_count=0;
	private int line_no_digit=0;
	private int line_no_width=0;
	
	private int search_found_line_no=-1;

	private Paint text_paint=null;
	
	private ThemeColorList mThemeColorList;

	private NotifyEvent mNotifyError=null;

	private Handler mUiHandler=null;
    private int mHighlightBackgrounColor =0, mHighlightForegrounColor =0;

    private boolean mUseLightTheme=false;

    public FileViewerAdapter(Activity context,
                             int textViewResourceId, boolean dbg, IndexedFileReader fc,
                             int bm, boolean sl, int line_break, String font,
                             String style, String size, int ts, NotifyEvent ntfy_error) {
		mContext = context;
        mUiHandler=new Handler();
		mResourceId = textViewResourceId;
		browse_mode=bm;
		resources=context.getResources();
		fcReader=fc;
        mNotifyError=ntfy_error;
        mGp=GlobalWorkArea.getGlobalParameters(context);
		mThemeColorList=mGp.themeColorList;

        if (ThemeUtil.isLightThemeUsed(mContext)) {
            mUseLightTheme=true;
            mHighlightBackgrounColor =0xc080ff80;
            mHighlightForegrounColor =Color.BLACK;
        } else {
            mHighlightBackgrounColor =Color.YELLOW;
            mHighlightForegrounColor =Color.BLACK;
        }

        resetCopy();
		fcReader.setTabStopValue(ts);
		fcReader.setFontFamily(font);
		fcReader.setFontStyle(style);
		if (browse_mode==TEXT_BROWSER_BROWSE_MODE_CHAR)
			setRowCount(fcReader.getCharModeLineCount());
		else setRowCount(fcReader.getHexModeLineCount());
		
		view_max_string_length=fc.getCharModeMaxLineLength();
		line_no_digit=String.format("%d",row_count).length();
		setAppearance(line_break,sl,font, style, size);
        log.debug("Initialized, " +
                " mode="+bm+
                ", line break="+line_break+
                ", show lineno="+view_show_lineno+
                ", font name="+font+
                ", font style="+style+
                ", font size="+size+
                ", row count="+row_count+
                ", tab stop="+ts+
                ", width="+resources.getDisplayMetrics().widthPixels+
                ", height="+resources.getDisplayMetrics().heightPixels+
                ", dens="+dipToPx(resources,10)+
                ", char index line count="+fcReader.getCharModeLineCount()+
                ", hex index line count="+fcReader.getHexModeLineCount()
        );
	}

	@Override
	final public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	final public int getCount() {
		return row_count;
	}

	@Override
	final public long getItemId(int arg0) {
		return arg0;
	};

	final public void setRowCount(int count) {row_count=count;}
	
//	private static long b_time=0;
	@Override
	final public String[] getItem(int i) {
//		b_time=System.currentTimeMillis();
		String[] line=fcReader.fetchLine(browse_mode,i);
		return line;
	}

	final public void setAppearance(int lb, boolean sl, String family, 
			String style, String size) {
		view_font_size=Integer.valueOf(size);
		if (lb==CustomTextView.LINE_BREAK_NOTHING) {
			view_type_face=Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);
		} else {
			if (family.equals("NORMAL")) {
				if (style.equals("NORMAL")) view_type_face=Typeface.create(family, Typeface.NORMAL); 
				else if (style.equals("BOLD")) view_type_face=Typeface.create(family, Typeface.BOLD);
				else if (style.equals("BOLD_ITALIC")) view_type_face=Typeface.create(family, Typeface.BOLD_ITALIC);
				else if (style.equals("ITALIC")) view_type_face=Typeface.create(family, Typeface.ITALIC);
				else view_type_face=Typeface.create(family, Typeface.NORMAL);
			} else {
				if (family.equals("MONOSPACE")) view_type_face=Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);
				else view_type_face=Typeface.create(family, Typeface.NORMAL);
			}
		}
		
		view_line_break=lb;
		
		text_paint=new Paint();
		text_paint.setTextSize((int)dipToPx(resources,view_font_size));
		text_paint.setTypeface(view_type_face);
		view_show_lineno=sl;
		line_no_width=(int)dipToPx(resources,(line_no_digit*view_font_size+view_font_size)/2);		
	};
	
	final private int calNumberOfChar() {
        String mc="";
        for (int i=0;i<200;i++) mc+="W";
        int nc=text_paint.breakText(mc, true, view_measured_width, null);
        return nc;
	}
	
	final public void incrementHorizontalPosition(int incr) {
    	if (browse_mode==TEXT_BROWSER_BROWSE_MODE_CHAR && view_line_break==CustomTextView.LINE_BREAK_NOTHING) {
//            log.trace("before h_pos="+view_horizontal_pos+", nc="+view_show_number_of_char+", max="+view_max_string_length+", incr="+incr);
            view_show_number_of_char=calNumberOfChar();
    		if ((view_horizontal_pos+view_show_number_of_char) <= view_max_string_length) {
    			if ((view_horizontal_pos+incr+view_show_number_of_char) <=view_max_string_length) {
        			view_horizontal_pos+=incr;
    			} else {
                    view_horizontal_pos=(view_max_string_length-view_show_number_of_char)+1;
                    if (view_show_lineno) view_horizontal_pos++;
    			}
    		}
            log.trace("incrementHorizontalPosition h_pos="+view_horizontal_pos+", nc="+view_show_number_of_char+", max="+view_max_string_length+", incr="+incr);
    	}
	}

    public boolean canScrollRight() {
        boolean result=false;
        int nc=calNumberOfChar();
        if ((view_horizontal_pos+nc) <= view_max_string_length) result=true;
        else result=false;
        log.trace("canScrollRight result="+result+", h_pos="+view_horizontal_pos+", nc="+nc+", max="+view_max_string_length);
        return result;
    }

    final public void decrementHorizontalPosition(int incr) {
    	if (browse_mode==TEXT_BROWSER_BROWSE_MODE_CHAR &&
    			view_line_break==CustomTextView.LINE_BREAK_NOTHING) {
    		view_horizontal_pos-=incr;
    		if (view_horizontal_pos<0) view_horizontal_pos=0;
    	}
	}

	public boolean canScrollLeft() {
	    boolean result=false;
	    if (view_horizontal_pos>0) result=true;
	    else result=false;
        log.trace("canScrollLeft result="+result+", h_pos="+view_horizontal_pos+", nc="+view_show_number_of_char+", max="+view_max_string_length);
        return result;
    }

	final public int getHorizontalPosition() {return view_horizontal_pos;}
	final public void setHorizontalPosition(int p) {view_horizontal_pos=p;}
	
	final public boolean isCopyActive() {return copy_active;}
	final public void resetCopy() {
		copy_from_line=copy_to_line=-1;
		copy_active=false;
	}
	final public void setCopyBegin(int ln) {
		resetCopy();
		copy_from_line=ln;
		log.debug("setCopyBegin pos="+ln);
	}
	final public boolean setCopyEnd(int ln) {
		boolean result=false;
		if (getCopyBegin()!=-1 && ln!=-1) {
			copy_to_line=ln;
			copy_active=true;
			result=true;
            log.debug("setCopyEnd pos="+ln);
		}
		return result;
	}
	
	final public int getCopyBegin() {return copy_from_line;}
	final public int getCopyEnd() {
		if (isCopyActive()) return copy_to_line;
		else return -1;
	};

	private String mFindString="";
	private boolean mFindCaseSensitive=false;
    private boolean mFindWord=false;
	final public int findString(int mode, int start_no, String target, boolean case_sensitive, boolean find_word) {
        mFindString=target;
        mFindCaseSensitive=case_sensitive;
        mFindWord=find_word;
		search_found_line_no=fcReader.findString(FileViewerAdapter.TEXT_BROWSER_BROWSE_MODE_CHAR, start_no, target, case_sensitive, find_word);
		mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
		return search_found_line_no;
	}

	final public int getFindPostition() {
	    return search_found_line_no;
    }

    final public void setFindPosition(int position) {
        search_found_line_no=position;
    }

	final public String getFindString() {
	    return mFindString;
    }

    final public void setFindString(String find_string) {
        mFindString=find_string;
    }

    final public boolean isFindCaseSensitive() {
	    return mFindCaseSensitive;
    }

    final public void setFindCaseSensitive(boolean sensitive) {
        mFindCaseSensitive=sensitive;
    }

    final public void resetFindStringResult() {
        setFindPosition(-1);
        setFindString("");
        notifyDataSetChanged();
    }
	
	private Drawable default_bkgrnd=null;
	private boolean scale_cal_required=true;
	private float resize_text_size=0f;
	private ColorStateList mPrimaryTextColor=null;
	@Override
	final public View getView(final int position, View convertView, final ViewGroup parent) {
		final ViewHolder holder;
        String[] line = getItem(position);
        if (line!=null && line[0]!=null) {
            line[0]=line[0].replaceAll("\r", "").replaceAll("\\p{C}", "?");//Remove unprintable character
        }
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(mResourceId, null);
            holder=new ViewHolder();
            holder.ll_char1=(LinearLayout)v.findViewById(R.id.text_browser_list_item_charview);
            holder.ll_char1.setBackgroundColor(mThemeColorList.text_background_color);
            holder.ll_hex1=(LinearLayout)v.findViewById(R.id.text_browser_list_item_hexview);
            holder.ll_hex1.setBackgroundColor(mThemeColorList.text_background_color);
        	if (browse_mode==TEXT_BROWSER_BROWSE_MODE_CHAR) {
	            holder.tv_line= (TextView) v.findViewById(R.id.text_browser_list_item_char_line1);
	            holder.tv_line.setTextSize(TypedValue.COMPLEX_UNIT_DIP,view_font_size);
	            holder.tv_line.setWidth(line_no_width);
	            holder.cv_text= (CustomTextView) v.findViewById(R.id.text_browser_list_item_char_text1);
	            holder.cv_text.setTypeface(view_type_face);
	            holder.cv_text.setTextSize(view_font_size);
	            holder.cv_text.setLineBreak(view_line_break);

                holder.tv_nowrap=(NonWordwrapTextView) v.findViewById(R.id.text_browser_list_item_char_no_word_wrap);
//                holder.tv_nowrap.setDebugEnable(true);
                holder.tv_nowrap.setTypeface(view_type_face);
                holder.tv_nowrap.setTextSize(TypedValue.COMPLEX_UNIT_DIP,view_font_size);//view_font_size);
                if (view_line_break==CustomTextView.LINE_BREAK_NO_WORD_WRAP) holder.tv_nowrap.setWordWrapEnabled(false);
                else if (view_line_break==CustomTextView.LINE_BREAK_WORD_WRAP) holder.tv_nowrap.setWordWrapEnabled(true);

	            if (default_bkgrnd==null) default_bkgrnd=holder.cv_text.getBackground();
                if (mPrimaryTextColor==null) mPrimaryTextColor=holder.tv_line.getTextColors();
        		holder.ll_hex1.setVisibility(View.GONE);
        		holder.ll_char1.setVisibility(View.VISIBLE);
        	} else {
                holder.cv_text= (CustomTextView) v.findViewById(R.id.text_browser_list_item_char_text1);
                holder.cv_text.setTypeface(view_type_face);
                holder.cv_text.setTextSize(view_font_size);
                holder.cv_text.setLineBreak(view_line_break);

                holder.tv_nowrap=(NonWordwrapTextView) v.findViewById(R.id.text_browser_list_item_char_no_word_wrap);
//                holder.tv_nowrap.setDebugEnable(true);
                holder.tv_nowrap.setTypeface(view_type_face);
                holder.tv_nowrap.setTextSize(TypedValue.COMPLEX_UNIT_DIP,view_font_size);//view_font_size);
                if (view_line_break==CustomTextView.LINE_BREAK_NO_WORD_WRAP) holder.tv_nowrap.setWordWrapEnabled(false);
                else if (view_line_break==CustomTextView.LINE_BREAK_WORD_WRAP) holder.tv_nowrap.setWordWrapEnabled(true);

                holder.tv_addr1=(TextView)v.findViewById(R.id.text_browser_list_item_hex_addr1);
                holder.tv_hex1=(TextView)v.findViewById(R.id.text_browser_list_item_hex_hex1);
                holder.tv_char1=(TextView)v.findViewById(R.id.text_browser_list_item_hex_char1);
                holder.tv_hex2=(TextView)v.findViewById(R.id.text_browser_list_item_hex_hex2);
                holder.tv_char2=(TextView)v.findViewById(R.id.text_browser_list_item_hex_char2);
        		holder.ll_hex1.setVisibility(View.VISIBLE);
        		holder.ll_char1.setVisibility(View.GONE);
                if (mPrimaryTextColor==null) mPrimaryTextColor=holder.tv_addr1.getTextColors();
        		if (scale_cal_required) {
            		holder.tv_addr1.setTypeface(Typeface.MONOSPACE);
        			TextView tv_id=(TextView)v.findViewById(R.id.text_browser_list_item_identifier);
        			String[] sa=tv_id.getText().toString().split(",");
        			int no_char=Integer.parseInt(sa[2]);
        			scale_cal_required=false;
            		Paint paint=new Paint();
            		float cts=holder.tv_addr1.getTextSize();
            		paint.setTextSize(cts);
            		paint.setTypeface(holder.tv_addr1.getTypeface());
            		String mt="";
            		for (int i=0;i<no_char;i++) mt+="W";
            		float rp=paint.measureText(mt);
            		int width=resources.getDisplayMetrics().widthPixels;
            		float scale=((float)width)/rp;
            		resize_text_size=(cts*scale);
           			log.debug("Text resize parameter, " +
            					"id="+sa[0]+", Required pixecel="+rp+
            					", Screen width="+width+
            					", Text length="+mt.length()+
            					", Scale factor="+scale+
            					", New text size="+resize_text_size);
        		}
        		holder.tv_hex1.setTextSize(TypedValue.COMPLEX_UNIT_PX,resize_text_size);
        		holder.tv_hex1.setTypeface(Typeface.MONOSPACE);
        		holder.tv_hex2.setTextSize(TypedValue.COMPLEX_UNIT_PX,resize_text_size);
        		holder.tv_hex2.setTypeface(Typeface.MONOSPACE);
        		holder.tv_char1.setTextSize(TypedValue.COMPLEX_UNIT_PX,resize_text_size);
        		holder.tv_char1.setTypeface(Typeface.MONOSPACE);
        		holder.tv_char2.setTextSize(TypedValue.COMPLEX_UNIT_PX,resize_text_size);
        		holder.tv_char2.setTypeface(Typeface.MONOSPACE);
        		holder.tv_addr1.setTextSize(TypedValue.COMPLEX_UNIT_PX,resize_text_size);
        		holder.tv_addr1.setTypeface(Typeface.MONOSPACE);
        	}
            v.setTag(holder);
        } else {
        	holder= (ViewHolder)v.getTag();
        }
        if (line != null) {
            int default_bg_color=mThemeColorList.text_background_color;
        	if (browse_mode==TEXT_BROWSER_BROWSE_MODE_CHAR) {
        		if (view_measured_width==0) {
        			view_measured_width=holder.cv_text.getCVMeasuredWidth();
        		}
        		if (view_show_lineno) {
            		holder.tv_line.setText(String.format("%s", position+1));
            		holder.tv_line.setVisibility(TextView.VISIBLE);
        		} else {
            		holder.tv_line.setVisibility(TextView.GONE);
        		}
        		if (mGp.settingUseNoWordWrapTextView) {
                    if (mFindString.length()>0) holder.tv_nowrap.setText(markFindString(line[0]==null?"":line[0]), TextView.BufferType.SPANNABLE);
                    else holder.tv_nowrap.setText(line[0]==null?"":line[0], TextView.BufferType.NORMAL);
                    holder.tv_nowrap.invalidate();
                    holder.tv_nowrap.requestLayout();
                    if (view_line_break==CustomTextView.LINE_BREAK_NOTHING) {
                        holder.cv_text.setText(line[0]==null?"":line[0]);
                        holder.cv_text.setVisibility(View.VISIBLE);
                        holder.tv_nowrap.setVisibility(TextView.GONE);
                    } else {
                        holder.cv_text.setVisibility(View.GONE);
                        holder.tv_nowrap.setVisibility(TextView.VISIBLE);
                    }
                } else {
                    holder.cv_text.setText(line[0]==null?"":line[0]);
                    holder.ll_char1.setSelected(false);
                }

                holder.cv_text.setCVHorizontalPosition(view_horizontal_pos);
           		
           		if (search_found_line_no!=-1) {
           		    if ((position)==search_found_line_no) {
                        if (mUseLightTheme) {
                            holder.ll_char1.setBackgroundColor(Color.GRAY);
                            holder.cv_text.setBackgroundColor(Color.GRAY);
                            holder.cv_text.setTextColor(Color.WHITE);
                            holder.tv_line.setTextColor(Color.WHITE);
                            holder.tv_nowrap.setTextColor(Color.WHITE);
                        } else {
                            holder.ll_char1.setBackgroundColor(Color.GRAY);
                            holder.cv_text.setBackgroundColor(Color.GRAY);
                            holder.cv_text.setTextColor(Color.BLACK);
                            holder.tv_line.setTextColor(Color.BLACK);
                            holder.tv_nowrap.setTextColor(Color.BLACK);
                        }
           			} else {
                        holder.ll_char1.setBackgroundDrawable(default_bkgrnd);
                        holder.cv_text.setBackgroundDrawable(default_bkgrnd);
                        holder.cv_text.setTextColor(mPrimaryTextColor.getDefaultColor());
                        holder.tv_line.setTextColor(mPrimaryTextColor.getDefaultColor());
                        holder.tv_nowrap.setTextColor(mPrimaryTextColor.getDefaultColor());
           			}
           		} else {
                    holder.ll_char1.setBackgroundDrawable(default_bkgrnd);
       				holder.cv_text.setTextColor(mPrimaryTextColor.getDefaultColor());
                    holder.tv_line.setTextColor(mPrimaryTextColor.getDefaultColor());
                    holder.tv_nowrap.setTextColor(mPrimaryTextColor.getDefaultColor());
           		}
        	} else {
        		holder.ll_hex1.setSelected(false);
        		holder.tv_hex1.setText(line[0].substring(0,23));
        		holder.tv_hex2.setText(line[0].substring(24,47));
        		holder.tv_char1.setText(line[1].substring(0,8));
        		holder.tv_char2.setText(line[1].substring(8));
        		holder.tv_addr1.setText(String.format("%08x", position*16));
        	}
       		if (isCopyActive()) {
       		    log.trace("copyBegin="+getCopyBegin()+", copyEnd="+getCopyEnd()+", pos="+position);
       		    int bg_color=Color.GRAY;
       			if (getCopyBegin()>getCopyEnd()) {
           			if (position>=getCopyEnd() && position<=getCopyBegin()){
           	        	if (browse_mode==TEXT_BROWSER_BROWSE_MODE_CHAR) {
           	        	    if (mUseLightTheme) bg_color=Color.LTGRAY;
           	        	    else bg_color=Color.GRAY;
                        }
                        holder.cv_text.setBackgroundColor(bg_color);
                        holder.tv_nowrap.setBackgroundColor(bg_color);
                        holder.ll_hex1.setBackgroundColor(bg_color);
	       	        	if (browse_mode==TEXT_BROWSER_BROWSE_MODE_CHAR) holder.ll_char1.setSelected(true);
	       	        	else holder.ll_hex1.setSelected(true);
           			} else {
                        holder.cv_text.setBackgroundColor(mThemeColorList.text_background_color);
                        holder.tv_nowrap.setBackgroundColor(mThemeColorList.text_background_color);
                        holder.ll_hex1.setBackgroundColor(mThemeColorList.text_background_color);
                    }
       			} else {
           			if (position>=getCopyBegin() && position<=getCopyEnd()) {
                        if (browse_mode==TEXT_BROWSER_BROWSE_MODE_CHAR) {
                            if (mUseLightTheme) bg_color=Color.LTGRAY;
                            else bg_color=Color.GRAY;
                        }
                        holder.cv_text.setBackgroundColor(bg_color);
                        holder.tv_nowrap.setBackgroundColor(bg_color);
                        holder.ll_hex1.setBackgroundColor(bg_color);
	       	        	if (browse_mode==TEXT_BROWSER_BROWSE_MODE_CHAR) holder.ll_char1.setSelected(true);
	       	        	else holder.ll_hex1.setSelected(true);
                    } else {
                        holder.cv_text.setBackgroundDrawable(default_bkgrnd);
                        holder.tv_nowrap.setBackgroundDrawable(default_bkgrnd);
                        holder.ll_hex1.setBackgroundColor(mThemeColorList.text_background_color);
           			}
       			}
       		} else {
       			if (getCopyBegin()==position) {
       			    int bg_color=Color.GRAY;
                    if (browse_mode==TEXT_BROWSER_BROWSE_MODE_CHAR) {
                        if (mUseLightTheme) bg_color=Color.LTGRAY;
                        else bg_color=Color.GRAY;
                    }
                    holder.cv_text.setBackgroundColor(bg_color);
                    holder.tv_nowrap.setBackgroundColor(bg_color);
                    holder.ll_hex1.setBackgroundColor(bg_color);
       				if (browse_mode==TEXT_BROWSER_BROWSE_MODE_CHAR) holder.ll_char1.setSelected(true);
   	        		else holder.ll_hex1.setSelected(true);
       			} else {
                    holder.cv_text.setBackgroundDrawable(default_bkgrnd);
                    holder.tv_nowrap.setBackgroundDrawable(default_bkgrnd);
                    holder.ll_hex1.setBackgroundColor(mThemeColorList.text_background_color);
       			}
       		}
       	} else {
            if (!mErrorNotified) {
                mNotifyError.notifyToListener(true, new Object[]{fcReader.getLastErrorMessage()});
                mErrorNotified=true;
            }
        }
        return v;
	};

	private SpannableStringBuilder markFindString(String line) {
	    SpannableStringBuilder sb=new SpannableStringBuilder(line);
        if (mFindString.length()>0) {
            Pattern pattern=null;
            String find_str="";
            if (mFindWord) find_str="\\b"+mFindString+"\\b";
            else find_str=mFindString;
            if (!mFindCaseSensitive) pattern=Pattern.compile(find_str, Pattern.CASE_INSENSITIVE);
            else pattern=Pattern.compile(find_str);
            Matcher mt = pattern.matcher(line);
            while(mt.find()) {
//                log.info("group="+mt.group()+", s="+mt.start()+", e="+mt.end());
                BackgroundColorSpan bg_span = new BackgroundColorSpan(mHighlightBackgrounColor);
                ForegroundColorSpan fg_span = new ForegroundColorSpan(mHighlightForegrounColor);
                sb.setSpan(bg_span, mt.start(), mt.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(fg_span, mt.start(), mt.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
	    return sb;
    }

	private boolean mErrorNotified=false;
	
	final static private float dipToPx(Resources res, int dip) {
		float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, res.getDisplayMetrics());
		return px;
	};
	
	class ViewHolder {
		TextView tv_line;
		CustomTextView cv_text;
        NonWordwrapTextView tv_nowrap;
		LinearLayout ll_char1, ll_hex1;
		TextView tv_addr1,tv_hex1, tv_char1,tv_hex2, tv_char2;
	};

}
