package com.sentaroh.android.TextFileBrowser;

import android.app.Activity;
import android.content.Context;

public class GlobalWorkArea {
    static private GlobalParameters gp=null;
    static public GlobalParameters getGlobalParameters(Activity a) {
        if (gp ==null) {
            gp =new GlobalParameters();
            gp.init(a);
        }
        return gp;
    }

    static public GlobalParameters getGlobalParameters() {
        return gp;
    }

    static public void clearGp() {
        gp=null;
    }
}
