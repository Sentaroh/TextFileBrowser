package com.sentaroh.android.TextFileBrowser;

/*
The MIT License (MIT)
Copyright (c) 2011-2013 Sentaroh

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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;
import android.view.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationUtil {
    private static Logger log = LoggerFactory.getLogger(NotificationUtil.class);
    final public static String NOTIFICATION_CHANNEL_DEFAULT="Main";

	@SuppressWarnings("deprecation")
	static final public void initNotification(Context c, NotificationCommonParms gwa) {
		gwa.notificationManager=(NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
		gwa.notification=new Notification(R.drawable.ic_32_browse_text,
    			c.getString(R.string.app_name),0);

		gwa.notificationAppName=c.getString(R.string.app_name);
		
		gwa.notificationIntent = new Intent(c,MainActivity.class);
		gwa.notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		gwa.notificationIntent.setAction(Intent.ACTION_MAIN);
		gwa.notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

		gwa.notificationPendingIntent =PendingIntent.getActivity(c, 0, gwa.notificationIntent,
    					PendingIntent.FLAG_UPDATE_CURRENT);

        buildNotification(c,gwa);

        if (Build.VERSION.SDK_INT>=26) {
            NotificationChannel def_ch = new NotificationChannel(
                    NOTIFICATION_CHANNEL_DEFAULT,
                    NOTIFICATION_CHANNEL_DEFAULT,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            def_ch.enableLights(false);
            def_ch.setShowBadge(false);
            def_ch.setSound(null,null);
            def_ch.enableVibration(false);
            def_ch.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            gwa.notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_DEFAULT);
            gwa.notificationManager.createNotificationChannel(def_ch);
        }
	};

	static final public void setNotificationEnabled(Context c, NotificationCommonParms gwa) {
		gwa.notiifcationEnabled=true;
	};
	static final public void setNotificationDisabled(Context c, NotificationCommonParms gwa) {
		gwa.notiifcationEnabled=false;
	};
	static final public boolean isNotificationEnabled(Context c, NotificationCommonParms gwa) {
		return gwa.notiifcationEnabled;
	};
	
	static final public void buildNotification(Context c, NotificationCommonParms gwa) {
		gwa.notificationBuilder=new Builder(c);
		gwa.notificationBuilder.setContentIntent(gwa.notificationPendingIntent)
//		   		.setTicker(gwa.notificationAppName)
			   	.setOngoing(true)
			   	.setAutoCancel(false)
			   	.setSmallIcon(R.drawable.ic_64_text_icon_bw)
			    .setContentTitle(gwa.notificationAppName)
			    .setContentText("")
                .setChannelId(NOTIFICATION_CHANNEL_DEFAULT)
//		    	.setSubText("subtext")
		    	.setLargeIcon(BitmapFactory.decodeResource(c.getResources(), R.drawable.main_icon))
			    .setWhen(System.currentTimeMillis())
//			    .addAction(action_icon, action_title, action_pi)
			    ;
        gwa.notification=gwa.notificationBuilder.build();
        gwa.notificationBigTextStyle =new NotificationCompat.BigTextStyle(gwa.notificationBuilder);
        gwa.notificationBigTextStyle
                .setBigContentTitle(gwa.notificationLastShowedTitle)
                .bigText(gwa.notificationLastShowedMessage);

    };
	
	final static public Notification getNotification(Context c, NotificationCommonParms gwa) {
		return gwa.notification;
	};
	
	final static public void setNotificationMessage(Context c, NotificationCommonParms gwa,  
			String msg) {
		gwa.notificationLastShowedMessage=msg;
	};
	
	final static public Notification reshowOngoingNotificationMsg(Context c, NotificationCommonParms gwa) {
		if (!isNotificationEnabled(c,gwa)) return gwa.notification;
		gwa.notificationBuilder
			.setContentTitle(gwa.notificationLastShowedTitle)
		    .setContentText(gwa.notificationLastShowedMessage)
		    ;
		if (Build.VERSION.SDK_INT<16) {//JB以外
			gwa.notification=gwa.notificationBuilder.build();
			gwa.notificationManager.notify(R.string.app_name,gwa.notification);
		} else {
			gwa.notificationBigTextStyle = 
   		   			new NotificationCompat.BigTextStyle(gwa.notificationBuilder);
			gwa.notificationBigTextStyle
				.setBigContentTitle(gwa.notificationLastShowedTitle)
				.bigText(gwa.notificationLastShowedMessage);

			gwa.notification=gwa.notificationBigTextStyle.build();
			gwa.notificationManager.notify(R.string.app_name,gwa.notification);
		}
    	return gwa.notification;
	};

	final static public Notification showOngoingNotificationMsg(Context c, NotificationCommonParms gwa, String msg ) {
		setNotificationMessage(c,gwa,msg);

		if (!isNotificationEnabled(c,gwa)) return gwa.notification;

//		gwa.notificationBuilder.setContentIntent(gwa.notificationPendingIntent)
//		    .setContentTitle(gwa.notificationLastShowedTitle)
//		    .setContentText(gwa.notificationLastShowedMessage)
//		    .setWhen(System.currentTimeMillis())
//		    ;
		gwa.notificationBuilder
			.setContentTitle(gwa.notificationLastShowedTitle)
		    .setContentText(gwa.notificationLastShowedMessage)
		    ;
        gwa.notificationBigTextStyle
                .setBigContentTitle(gwa.notificationLastShowedTitle)
                .bigText(gwa.notificationLastShowedMessage);
        gwa.notification=gwa.notificationBigTextStyle.build();
        gwa.notificationManager.notify(R.string.app_name,gwa.notification);

    	return gwa.notification;
	};

//	final static public void showNoticeNotificationMsg(Context context, NotificationCommonParms gwa, String msg ) {
//
//		clearNotification(gwa);
//		
//		Intent dummy_intent = new Intent(context,SMBSyncMain.class);
//		PendingIntent dummy_pi =PendingIntent.getActivity(context, 0, dummy_intent,
//    					PendingIntent.FLAG_UPDATE_CURRENT);
//		dummy_pi.cancel();
//
//		boolean valid_log_file_exists=false;
//		if (!gwa.currentLogFilePath.equals("") && !gwa.settingLogOption.equals("0")) {
//			File lf=new File(gwa.currentLogFilePath);
//			if (lf.exists()) valid_log_file_exists=true;
//		}
//			
//		Intent br_log_intent = new Intent(android.content.Intent.ACTION_VIEW);
//		br_log_intent.setDataAndType(Uri.parse("file://"+gwa.currentLogFilePath), "text/plain");
//		PendingIntent br_log_pi=PendingIntent.getActivity(context, 0, br_log_intent,
//				PendingIntent.FLAG_UPDATE_CURRENT);
//
//		NotificationCompat.Builder builder=new NotificationCompat.Builder(context);
//		
//		builder.setTicker(gwa.notificationAppName)
//			   	.setOngoing(false)
//			   	.setAutoCancel(true)
//			   	.setSmallIcon(R.drawable.ic_48_smbsync)
//			    .setContentTitle(context.getString(R.string.app_name))
//			    .setContentText(msg)
//			    .setWhen(System.currentTimeMillis())
////			    .addAction(action_icon, action_title, action_pi)
//			    ;
//		if (valid_log_file_exists) builder.setContentIntent(br_log_pi);
//		else builder.setContentIntent(dummy_pi);
//		
//		Notification notification=builder.build();
//		gwa.notificationManager.notify(R.string.app_name,notification);
//    		
////		if (Build.VERSION.SDK_INT<16) {//JB以外
////		} else {
////			BigTextStyle bigTextStyle = 
////   		   			new NotificationCompat.BigTextStyle(builder);
////			bigTextStyle
////				.setBigContentTitle(gwa.notificationLastShowedTitle)
////				.bigText(msg);
////			Notification notification=bigTextStyle.build();
////			gwa.notificationManager.notify(R.string.app_name,notification);
////		}
//    		
//	};
	
	final static public void clearNotification(Context c, NotificationCommonParms gwa) {
		gwa.notificationManager.cancelAll();
	};

}

