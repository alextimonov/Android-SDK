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

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import com.backendless.push.registration.IReceiver;
import com.backendless.push.registration.Registrar;
import com.backendless.push.registration.RegistrationState;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class BackendlessBroadcastReceiver extends BroadcastReceiver
{
  private static final String TAG = "BackendlessBroadcastReceiver";
  private static final Random random = new Random();

  private static final int MAX_BACKOFF_MS = (int) TimeUnit.SECONDS.toMillis( 3600 );
  private static final int DEFAULT_BACKOFF_MS = 3000;

  private static final String TOKEN = Long.toBinaryString( random.nextLong() );
  private static final String EXTRA_TOKEN = "token";

  private static final String WAKELOCK_KEY = "GCM_LIB";
  private static PowerManager.WakeLock wakeLock;
  private static final Object LOCK = BackendlessBroadcastReceiver.class;

  //Fields are placed here because this class is most strongly referenced by android

  private IReceiver receiver = new Receiver();
  private int backOff = DEFAULT_BACKOFF_MS;

  @Override
  public final void onReceive( Context context, Intent intent )
  {
    synchronized( LOCK )
    {
      if( wakeLock == null )
      {
        PowerManager pm = (PowerManager) context.getSystemService( Context.POWER_SERVICE );
        wakeLock = pm.newWakeLock( PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_KEY );
      }
    }

    wakeLock.acquire();
    this.handleIntent( context, intent );
    setResult( Activity.RESULT_OK, null, null );
  }

  //API block
  public void onRegistered( Context context )
  {
  }

  public void onUnregistered( Context context )
  {
  }

  public boolean onMessage( Context context, Intent intent )
  {
    return true;
  }

  public void onError( Context context, String message )
  {
    if( message.contains( "device is not subscribed" ) )
      RegistrationState.setDeviceToken( context, null );

    throw new RuntimeException( message );
  }

  //Internal block
  private void handleIntent( Context context, Intent intent )
  {
    try
    {
      String action = intent.getAction();

      if( action.equals( GCMConstants.INTENT_FROM_GCM_REGISTRATION_CALLBACK ) )
        handleRegistration( context, intent );
      else if( action.equals( GCMConstants.INTENT_FROM_GCM_MESSAGE ) )
        handleMessage( context, intent );
      else if( action.equals( GCMConstants.INTENT_FROM_GCM_LIBRARY_RETRY ) )
      {
        String token = intent.getStringExtra( EXTRA_TOKEN );
        if( !TOKEN.equals( token ) )
          return;
        // retry last call
        handleError( context, intent );
      }
    }
    finally
    {
      synchronized( LOCK )
      {
        if( wakeLock != null )
          wakeLock.release();
      }
    }
  }

  private void handleError( Context context, Intent intent )
  {
    if( RegistrationState.getAction( context ).equals( GCMConstants.INTENT_TO_GCM_REGISTRATION ) )
      Registrar.getInstance().registrationFailed( intent.getStringExtra( GCMConstants.EXTRA_ERROR ), RegistrationState.getCallbackId( context ) );
    else
      Registrar.getInstance().unregistrationFailed( intent.getStringExtra( GCMConstants.EXTRA_ERROR ), RegistrationState.getCallbackId( context ) );
  }

  private void handleMessage( final Context context, Intent intent )
  {

    try
    {
      boolean showPushNotification = onMessage( context, intent );
      receiver.handleMessage( context, intent, showPushNotification );
    }
    catch( Throwable throwable )
    {
      Log.e( TAG, "Error processing push notification", throwable );
    }
  }

  private void handleRegistration( final Context context, Intent intent )
  {
    String error = intent.getStringExtra( GCMConstants.EXTRA_ERROR );
    String unregistered = intent.getStringExtra( GCMConstants.EXTRA_UNREGISTERED );

    // registration succeeded
    if( RegistrationState.getAction( context ).equals( GCMConstants.INTENT_TO_GCM_REGISTRATION ) )
    {
      backOff = DEFAULT_BACKOFF_MS;
      onRegistered( context );
      String deviceToken = intent.getStringExtra( GCMConstants.EXTRA_REGISTRATION_ID );
      RegistrationState.setDeviceToken( context, deviceToken );
      Registrar.getInstance().registrationCompleted( RegistrationState.getSenderId( context ), deviceToken, RegistrationState.getExpiration( context ), RegistrationState.getCallbackId( context ) );
      return;
    }

    // unregistration succeeded
    if( unregistered != null )
    {
      // Remember we are unregistered
      onUnregistered( context );
      backOff = DEFAULT_BACKOFF_MS;
      RegistrationState.setDeviceToken( context, null );
      Registrar.getInstance().unregistrationCompleted( RegistrationState.getCallbackId( context ) );

      return;
    }

    // Registration failed
    if( error.equals( GCMConstants.ERROR_SERVICE_NOT_AVAILABLE ) )
    {
      int backoffTimeMs = backOff;
      int nextAttempt = backoffTimeMs / 2 + random.nextInt( backoffTimeMs );
      Intent retryIntent = new Intent( GCMConstants.INTENT_FROM_GCM_LIBRARY_RETRY );
      retryIntent.putExtra( EXTRA_TOKEN, TOKEN );
      PendingIntent retryPendingIntent = PendingIntent.getBroadcast( context, 0, retryIntent, 0 );
      AlarmManager am = (AlarmManager) context.getSystemService( Context.ALARM_SERVICE );
      am.set( AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + nextAttempt, retryPendingIntent );
      // Next retry should wait longer.
      if( backoffTimeMs < MAX_BACKOFF_MS )
        backOff = backoffTimeMs * 2;
    }
    else
    {
      onError( context, error );
      handleError( context, intent );
    }
  }
}
