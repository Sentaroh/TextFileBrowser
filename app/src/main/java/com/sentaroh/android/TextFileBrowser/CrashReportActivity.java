package com.sentaroh.android.TextFileBrowser;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.sentaroh.android.Utilities2.CrashReport;
import com.sentaroh.android.Utilities2.Dialog.CommonDialog;
import com.sentaroh.android.Utilities2.Dialog.MessageDialogAppFragment;
import com.sentaroh.android.Utilities2.Dialog.MessageDialogFragment;
import com.sentaroh.android.Utilities2.NotifyEvent;
import com.sentaroh.android.Utilities2.ThemeColorList;
import com.sentaroh.android.Utilities2.ThemeUtil;

public class CrashReportActivity extends CrashReport {
    private Context mContext=null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
