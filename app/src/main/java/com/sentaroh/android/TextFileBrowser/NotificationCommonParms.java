package com.sentaroh.android.TextFileBrowser;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

class NotificationCommonParms {
	public NotificationManager notificationManager=null;
	public Notification notification=null;
	public NotificationCompat.Builder notificationBuilder=null;
	public NotificationCompat.BigTextStyle notificationBigTextStyle=null;
	public Intent notificationIntent=null;
	public PendingIntent notificationPendingIntent;
	public String notificationLastShowedMessage=null,
			notificationLastShowedTitle="TextFileBrowser";
	public String notificationAppName="TextFileBrowser";
	public boolean notiifcationEnabled=true;
}
