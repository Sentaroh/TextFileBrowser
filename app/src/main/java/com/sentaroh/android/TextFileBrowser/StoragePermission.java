package com.sentaroh.android.TextFileBrowser;

import android.app.Dialog;
import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.sentaroh.android.Utilities2.Dialog.CommonDialog;
import com.sentaroh.android.Utilities2.NotifyEvent;
import com.sentaroh.android.Utilities2.SafManager3;
import com.sentaroh.android.Utilities2.ThemeColorList;
import com.sentaroh.android.Utilities2.ThemeUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class StoragePermission {

    private Context mContext=null;
    private MainActivity mActivity=null;
    private CommonDialog commonDlg=null;
    
    private Dialog mDialog=null;

    private SafManager3 mSafMgr =null;

    private static Logger log= LoggerFactory.getLogger(StoragePermission.class);

    private NotifyEvent mNtfyGrantRequest=null;

    public StoragePermission(MainActivity a, SafManager3 saf_mgr, CommonDialog cd, NotifyEvent ntfy_request) {
        mContext = a.getApplicationContext();
        mActivity = a;
        mSafMgr =saf_mgr;
        commonDlg = cd;
        mNtfyGrantRequest=ntfy_request;
    }

    public boolean isStoragePermissionGranted() {
        ArrayList<String> rows=buildStoragePermissionRequiredList();
        if (rows.size()>0) return true;
        else return false;
    }

    private ArrayList<String> buildStoragePermissionRequiredList() {
        final ArrayList<SafManager3.StorageVolumeInfo> svi_list= SafManager3.getStorageVolumeInfo(mContext);
        final ArrayList<String> rows=new ArrayList<String>();

        for(SafManager3.StorageVolumeInfo ssi:svi_list) {
            if (!mSafMgr.isUuidRegistered(ssi.uuid)) {
                if (ssi.uuid.equals(SafManager3.SAF_FILE_PRIMARY_UUID)) {
                    if (mSafMgr.isScopedStorageMode()) rows.add(ssi.description);
                } else {
                    rows.add(ssi.description);
                }
            }
        }
        return rows;
    }

    public void showDialog() {
        initDialog();
    }

    private void initDialog() {
        // カスタムダイアログの生成
        mDialog = new Dialog(mActivity);
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setContentView(R.layout.storage_permission_dlg);

        ThemeColorList tcl= ThemeUtil.getThemeColorList(mActivity);

        LinearLayout ll_dlg_view = (LinearLayout) mDialog.findViewById(R.id.storage_permission_dlg_view);
//        ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

        LinearLayout title_view = (LinearLayout) mDialog.findViewById(R.id.storage_permission_dlg_title_view);
        title_view.setBackgroundColor(tcl.title_background_color);
        TextView dlg_title = (TextView) mDialog.findViewById(R.id.storage_permission_dlg_title);
        dlg_title.setTextColor(tcl.title_text_color);

        CommonDialog.setDlgBoxSizeLimit(mDialog, true);

        final Button btn_ok = (Button) mDialog.findViewById(R.id.storage_permission_dlg_ok);
//        btn_ok.setBackgroundColor(Color.DKGRAY);
        final Button btn_cancel = (Button) mDialog.findViewById(R.id.storage_permission_dlg_cancel);
//        btn_cancel.setBackgroundColor(Color.TRANSPARENT);//.DKGRAY);
        final TextView tv_msg = (TextView) mDialog.findViewById(R.id.storage_permission_dlg_msg);

        final ListView lv = (ListView) mDialog.findViewById(R.id.storage_permission_dlg_storage_list);
        final ArrayList<SafManager3.StorageVolumeInfo> svi_list= SafManager3.getStorageVolumeInfo(mContext);

        final ArrayList<String> rows=buildStoragePermissionRequiredList();
        if (rows.size()==0) {
            commonDlg.showCommonDialog(false, "W", "There was no storage requiring permissions.","",null);
            return;
        }

        lv.setAdapter(new ArrayAdapter<String>(mActivity, android.R.layout.simple_list_item_multiple_choice, rows));
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        lv.setScrollingCacheEnabled(false);
        lv.setScrollbarFadingEnabled(false);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
                log.debug("clicked pos="+pos);
                SparseBooleanArray checked = lv.getCheckedItemPositions();
                boolean selected=false;
                for (int i = 0; i <= rows.size(); i++) {
                    if (checked.get(i) == true) {
                        selected=true;
                        break;
                    }
                }
                if (selected) CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                else CommonDialog.setViewEnabled(mActivity, btn_ok, false);
            }
        });

        CommonDialog.setViewEnabled(mActivity, btn_ok, false);
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SparseBooleanArray checked = lv.getCheckedItemPositions();
                boolean selected=false;
                ArrayList<String> uuid_list=new ArrayList<String>();
                for (int i = 0; i <= rows.size(); i++) {
                    if (checked.get(i) == true) {
                        String r_desc=rows.get(i);
                        for(SafManager3.StorageVolumeInfo ssi:svi_list) {
                            if (ssi.description.equals(r_desc)) {
                                uuid_list.add(ssi.uuid);
                            }
                        }
                    }
                }
                if (uuid_list.size()>0) mNtfyGrantRequest.notifyToListener(true, new Object[]{uuid_list});
                mDialog.dismiss();
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNtfyGrantRequest.notifyToListener(false, null);
                mDialog.dismiss();
            }
        });

        mDialog.show();
    }
}