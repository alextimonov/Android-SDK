/*
 * ********************************************************************************************************************
 *  <p/>
 *  BACKENDLESS.COM CONFIDENTIAL
 *  <p/>
 *  ********************************************************************************************************************
 *  <p/>
 *  Copyright 2012 BACKENDLESS.COM. All Rights Reserved.
 *  <p/>
 *  NOTICE: All information contained herein is, and remains the property of Backendless.com and its suppliers,
 *  if any. The intellectual and technical concepts contained herein are proprietary to Backendless.com and its
 *  suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by trade secret
 *  or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden
 *  unless prior written permission is obtained from Backendless.com.
 *  <p/>
 *  ********************************************************************************************************************
 */

package com.backendless.push;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import com.backendless.messaging.PublishOptions;
import com.backendless.push.registration.IReceiver;

class PushReceiver implements IReceiver
{
  private static int customLayout;
  private static int customLayoutTitle;
  private static int customLayoutDescription;
  private static int customLayoutImageContainer;
  private static int customLayoutImage;

  private static int notificationId = 1;

  @Override
  public void handleMessage( Context context, Intent intent, boolean showNotification )
  {
    if( showNotification )
    {
      CharSequence tickerText = intent.getStringExtra( PublishOptions.ANDROID_TICKER_TEXT_TAG );
      CharSequence contentTitle = intent.getStringExtra( PublishOptions.ANDROID_CONTENT_TITLE_TAG );
      CharSequence contentText = intent.getStringExtra( PublishOptions.ANDROID_CONTENT_TEXT_TAG );

      if( tickerText != null && tickerText.length() > 0 )
      {
        int appIcon = context.getApplicationInfo().icon;
        if( appIcon == 0 )
          appIcon = android.R.drawable.sym_def_app_icon;

        Intent notificationIntent = context.getPackageManager().getLaunchIntentForPackage( context.getApplicationInfo().packageName );
        PendingIntent contentIntent = PendingIntent.getActivity( context, 0, notificationIntent, 0 );
        Notification notification = new Notification( appIcon, tickerText, System.currentTimeMillis() );
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.setLatestEventInfo( context, contentTitle, contentText, contentIntent );

        registerResources( context );
        if( customLayout > 0 && customLayoutTitle > 0 && customLayoutDescription > 0 && customLayoutImageContainer > 0 )
        {
          NotificationLookAndFeel lookAndFeel = new NotificationLookAndFeel();
          lookAndFeel.extractColors( context );
          RemoteViews contentView = new RemoteViews( context.getPackageName(), customLayout );
          contentView.setTextViewText( customLayoutTitle, contentTitle );
          contentView.setTextViewText( customLayoutDescription, contentText );
          contentView.setTextColor( customLayoutTitle, lookAndFeel.getTextColor() );
          contentView.setFloat( customLayoutTitle, "setTextSize", lookAndFeel.getTextSize() );
          contentView.setTextColor( customLayoutDescription, lookAndFeel.getTextColor() );
          contentView.setFloat( customLayoutDescription, "setTextSize", lookAndFeel.getTextSize() );
          contentView.setImageViewResource( customLayoutImageContainer, customLayoutImage );
          notification.contentView = contentView;
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService( Context.NOTIFICATION_SERVICE );
        notificationManager.notify( notificationId++, notification );
      }
    }
  }

  public static void registerResources( Context context )
  {
    customLayout = context.getResources().getIdentifier( "notification", "layout", context.getPackageName() );
    customLayoutTitle = context.getResources().getIdentifier( "title", "id", context.getPackageName() );
    customLayoutDescription = context.getResources().getIdentifier( "text", "id", context.getPackageName() );
    customLayoutImageContainer = context.getResources().getIdentifier( "image", "id", context.getPackageName() );
    customLayoutImage = context.getResources().getIdentifier( "push_icon", "drawable", context.getPackageName() );
  }
}
