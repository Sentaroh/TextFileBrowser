package com.sentaroh.android.TextFileBrowser;

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

import com.sentaroh.android.Utilities2.NotifyEvent;

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
