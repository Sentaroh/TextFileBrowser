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

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.TextView;

import com.sentaroh.android.Utilities3.NotifyEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncodeSelectorAdapter extends ArrayAdapter<EncodeListItem>{
    private static Logger log = LoggerFactory.getLogger(EncodeSelectorAdapter.class);
	private ArrayList<EncodeListItem> encode_list=null;
	private int mLayoutId=0;
	private Context mContext=null;
	
//	private ThemeColorList mThemeColorList;
	
	public EncodeSelectorAdapter(Context context, int id, 
			ArrayList<EncodeListItem> objects) {
		super(context, id, objects);
		mLayoutId=id;
		encode_list=objects;
		mContext=context;
//		mThemeColorList=ThemeUtil.getThemeColorList(mContext);
	};

	private NotifyEvent mNotifyClickListener=null;
	public void setClickListener(NotifyEvent ntfy){
	    mNotifyClickListener=ntfy;
    }

	@Override
	final public View getView(final int position, View convertView, final ViewGroup parent) {
		final ViewHolder holder;
//		Log.v("","count="+getCount()+", pos="+position);
		final EncodeListItem item = getItem(position);
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(mLayoutId, null);
            holder=new ViewHolder();
            
            holder.rb_checked=(RadioButton)v.findViewById(R.id.encode_name_selection_list_item_checked);
            
            v.setTag(holder);
        } else {
        	holder= (ViewHolder)v.getTag();
        }
        if (item != null) {
//        	Log.v("","name="+item.encode_name);
        	holder.rb_checked.setText(item.encode_name);
        	holder.rb_checked.setOnCheckedChangeListener(new OnCheckedChangeListener(){
				@Override
				public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
					if (isChecked) {
						for (int i=0;i<encode_list.size();i++) {
							encode_list.get(i).isChecked=false;
						}
						item.isChecked=isChecked;
						if (mNotifyClickListener!=null) mNotifyClickListener.notifyToListener(true, new Object[]{item.encode_name});
						notifyDataSetChanged();
					}
				}
        	});
        	holder.rb_checked.setChecked(item.isChecked);
       	}
        return v;
	};
	
	class ViewHolder {
		TextView tv_encode_name;
		RadioButton rb_checked;
	};

}
