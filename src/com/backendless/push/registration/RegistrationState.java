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

package com.backendless.push.registration;

import android.content.Context;
import android.content.SharedPreferences;

public class RegistrationState
{
  static String PROPERTY_GCM_DEVICE_TOKEN = "bl_gcmDeviceToken";
  static String PREFERENCES = "com.google.android.gcm";
  static String ACTION_KEY = "bl_action";
  static String CALLBACK_ID_KEY = "bl-callback-id";
  static String EXPIRATION_KEY = "bl-expiration";
  static String SENDER_KEY = "bl-sender-id";

  private static SharedPreferences getPreferences( Context context )
  {
    return context.getSharedPreferences( PREFERENCES, Context.MODE_PRIVATE );
  }

  public static String getDeviceToken( Context context )
  {
    return getPreferences( context ).getString( PROPERTY_GCM_DEVICE_TOKEN, "" );
  }

  public static void setDeviceToken( Context context, String deviceToken )
  {
    SharedPreferences.Editor editor = getPreferences( context ).edit();
    editor.putString( PROPERTY_GCM_DEVICE_TOKEN, deviceToken );
    editor.commit();
  }

  public static String getSenderId( Context context )
  {
    return getPreferences( context ).getString( SENDER_KEY, "" );
  }

  public static void setSenderId( Context context, String senderId )
  {
    SharedPreferences.Editor editor = getPreferences( context ).edit();
    editor.putString( SENDER_KEY, senderId );
    editor.commit();
  }

  public static String getAction( Context context )
  {
    return getPreferences( context ).getString( ACTION_KEY, "" );
  }

  public static void setAction( Context context, String action )
  {
    SharedPreferences.Editor editor = getPreferences( context ).edit();
    editor.putString( ACTION_KEY, action );
    editor.commit();
  }

  public static String getCallbackId( Context context )
  {
    return getPreferences( context ).getString( CALLBACK_ID_KEY, "" );
  }

  public static void setCallbackId( Context context, String callbackId )
  {
    SharedPreferences.Editor editor = getPreferences( context ).edit();
    editor.putString( CALLBACK_ID_KEY, callbackId );
    editor.commit();
  }

  public static long getExpiration( Context context )
  {
    return getPreferences( context ).getLong( EXPIRATION_KEY, 0 );
  }

  public static void setExpiration( Context context, long expiration )
  {
    SharedPreferences.Editor editor = getPreferences( context ).edit();
    editor.putLong( EXPIRATION_KEY, expiration );
    editor.commit();
  }
}
