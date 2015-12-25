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

package com.backendless.messaging;

import com.backendless.exceptions.BackendlessException;
import com.backendless.exceptions.ExceptionMessage;
import com.backendless.exceptions.WrongGCMSenderFormatException;
import com.backendless.push.BackendlessBroadcastReceiver;

public class MessagingHelper
{
  private static String gcmSenderId;

  public static void checkChannelName( String channelName ) throws BackendlessException
  {
    if( channelName == null )
      throw new IllegalArgumentException( ExceptionMessage.NULL_CHANNEL_NAME );

    if( channelName.equals( "" ) )
      throw new IllegalArgumentException( ExceptionMessage.NULL_CHANNEL_NAME );
  }

  public synchronized static String getGcmSenderId()
  {
    if(gcmSenderId != null)
      return gcmSenderId;

    android.content.Context context = com.backendless.ContextHandler.getAppContext();
    android.content.pm.PackageManager packageManager = context.getPackageManager();
    String packageName = context.getPackageName();
    android.content.pm.ActivityInfo[] receivers = getReceivers( packageManager, packageName );

    if( receivers == null )
      return null;

    for( android.content.pm.ActivityInfo receiver : receivers )
    {
      if( receiverExtendsPushBroadcast( receiver ) )
      {
        gcmSenderId = retrieveSenderIdMetaPresent( context, receiver.name );
        return gcmSenderId;
      }
    }

    return null;
  }

  private static android.content.pm.ActivityInfo[] getReceivers( android.content.pm.PackageManager packageManager,
                                                                 String packageName )
  {
    // check receivers
    android.content.pm.PackageInfo receiversInfo;
    try
    {
      receiversInfo = packageManager.getPackageInfo( packageName, android.content.pm.PackageManager.GET_RECEIVERS );
    }
    catch( android.content.pm.PackageManager.NameNotFoundException e )
    {
      throw new IllegalStateException( "Could not get receivers for package " + packageName );
    }

    return receiversInfo.receivers;
  }

  private static String retrieveSenderIdMetaPresent( android.content.Context context, String receiverName )
  {
    android.content.pm.ActivityInfo appi = null;
    try
    {
      appi = context.getPackageManager().getReceiverInfo( new android.content.ComponentName( context, receiverName ), android.content.pm.PackageManager.GET_META_DATA );
    }
    catch( android.content.pm.PackageManager.NameNotFoundException e )
    {
      throw new BackendlessException( e );
    }

    if( appi == null )
      return null;

    android.os.Bundle bundle = appi.metaData;


    String gcmSenderId = bundle == null ? null : bundle.getString( "GCMSenderId" );
    if( gcmSenderId != null && !gcmSenderId.toLowerCase().endsWith( "l" ) )
      throw new WrongGCMSenderFormatException(  );
    return gcmSenderId == null ? null : gcmSenderId.substring( 0, gcmSenderId.length()-1 );
  }

  private static boolean receiverExtendsPushBroadcast( android.content.pm.ActivityInfo receiver )
  {
    try
    {
      Class clazz = Class.forName( receiver.name );
      return BackendlessBroadcastReceiver.class.isAssignableFrom( clazz );
    }
    catch( ClassNotFoundException e )
    {
      throw new RuntimeException( e );
    }
  }
}
