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

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sentaroh.android.Utilities3.ThemeUtil;
import com.sentaroh.android.Utilities3.Widget.NonWordwrapCheckedTextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViewedFileListAdapter extends ArrayAdapter<ViewedFileListItem>{
    private static Logger log = LoggerFactory.getLogger(ViewedFileListAdapter.class);
	private ArrayList<ViewedFileListItem> mFileViewList=null;
	private Context mContext=null;
	@SuppressWarnings("unused")
	private Activity mActivity=null;
	private int mResourceId=-1;
//	private int mTextViewResourceId=-1;
	
	public ViewedFileListAdapter(Activity a, int resourceId, 
			ArrayList<ViewedFileListItem>objects) {
		super(a, resourceId,objects);
		mActivity=a;
		mContext=a.getApplicationContext();
		mResourceId=resourceId;
		mFileViewList=objects;
	}

	@Override
	public ViewedFileListItem getItem(int pos) {
		return mFileViewList.get(pos);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        TextView view;
        if (convertView == null) {
        	LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = (TextView)vi.inflate(mResourceId, null);
            
        } else {
            view = (TextView)convertView;
        }
//        view=(TextView)super.getView(position,convertView,parent);
        mSelectedPosition=position;
        view.setText(getItem(position).viewd_file.getName());
        view.setCompoundDrawablePadding(10);
        view.setCompoundDrawablesWithIntrinsicBounds(
        		mContext.getResources().getDrawable(android.R.drawable.arrow_down_float),
        		null, null, null);
        if (ThemeUtil.isLightThemeUsed(mActivity)) view.setTextColor(Color.BLACK);
        else view.setTextColor(Color.WHITE);
//        view.setTextColor(Color.BLACK);
//        if (text_size!=0) view.setTextSize(text_size);

        return view;
	}

	private int mSelectedPosition=0;
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mActivity);
            convertView = inflater.inflate(R.layout.spinner_dropdown_single_choice, null);
        }
        String text = getItem(position).viewd_file.getPath();
        final NonWordwrapCheckedTextView text_view=(NonWordwrapCheckedTextView)convertView.findViewById(R.id.text1);
//        text_view.setWordWrapByFilter(false);
        text_view.setText(text);
        if (position==mSelectedPosition) text_view.setChecked(true);
        else text_view.setChecked(false);

        text_view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                text_view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                setMultilineEllipsizeOld(text_view, 3, TextUtils.TruncateAt.START);
            }
        });
        return convertView;
    }

    public static void setMultilineEllipsizeOld(NonWordwrapCheckedTextView view, int maxLines, TextUtils.TruncateAt where) {
        if (maxLines >= view.getLineCount()) {
            // ellipsizeする必要無し
            return;
        }
        float avail = 0.0f;
        for (int i = 0; i < maxLines; i++) {
            avail += view.getLayout().getLineMax(i);
        }
        CharSequence ellipsizedText = TextUtils.ellipsize(view.getOriginalText(), view.getPaint(), avail, where);
        view.setText(ellipsizedText);
    }


}

