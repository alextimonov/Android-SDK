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
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.util.Log;
import com.backendless.Backendless;
import com.backendless.push.GCMConstants;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Registrar implements IRegistrar
{
  private static final Registrar instance = new Registrar();
  private static final IRegistrar service = Backendless.isFireOS() ? new AdmRegister() : new GcmRegistrar();
  private static final Lock lock = new ReentrantLock();
  private static final Object notify = new Object();
  private static final int DEFAULT_ON_SERVER_LIFESPAN_MS = /*2 days*/1000 * 60 * 60 * 24 * 2;
  private static final String TAG = "Registrar";

  private Registrar()
  {
  }

  public static Registrar getInstance()
  {
    return instance;
  }

  private static final List<String> defaultPermissions;

  private Map<String, IDeviceRegistrationCallback> callbackMap = new ConcurrentHashMap<String, IDeviceRegistrationCallback>();

  static
  {
    defaultPermissions = new ArrayList<String>();
    defaultPermissions.add( GCMConstants.PERMISSION_GCM_MESSAGE );
    defaultPermissions.add( GCMConstants.PERMISSION_ANDROID_ACCOUNTS );
    defaultPermissions.add( GCMConstants.PERMISSION_ANDROID_INTERNET );
  }

  @Override
  public void register( Context context, String senderId, Date expiration, IDeviceRegistrationCallback callback )
  {
    Log.d( TAG, "register: check device and manifest" );
    checkDevice( context );
    checkManifest( context );
    lock.lock();
    Log.d( TAG, "register: lock acquired" );
    try
    {
      callbackMap.put( callback.getIdentity(), callback );
      String deviceToken = RegistrationState.getDeviceToken( context );
      Log.d( TAG, "register: current device token: " + deviceToken );

      if( deviceToken != null && !deviceToken.isEmpty() )
      {
        registrationCompleted( senderId, deviceToken, getRegistrationExpiration( expiration ), callback.getIdentity() );
        return;
      }

      Log.d( TAG, "register: device token not found, will register on google service " );

      RegistrationState.setCallbackId( context, callback.getIdentity() );
      RegistrationState.setAction( context, GCMConstants.INTENT_TO_GCM_REGISTRATION );
      RegistrationState.setExpiration( context, getRegistrationExpiration( expiration ) );
      RegistrationState.setSenderId( context, senderId );

      service.register( context, senderId, expiration, callback );

      waitForActionDone();
      Log.d( TAG, "register: registered" );
    }
    finally
    {
      lock.unlock();
      Log.d( TAG, "register: lock released" );
    }
  }

  @Override
  public void unregister( Context context, IDeviceRegistrationCallback callback )
  {
    lock.lock();
    Log.d( TAG, "unregister: lock acquired" );
    try
    {
      callbackMap.put( callback.getIdentity(), callback );
      Log.d( TAG, "unregister: callback size: " + callbackMap.size() );

      if( !isRegistered( context ) || callbackMap.size() > 1 )
      {
        Log.d( TAG, "unregister: already unregistered or there more then one callbacks" );
        unregistrationCompleted( callback.getIdentity() );
        return;
      }

      RegistrationState.setCallbackId( context, callback.getIdentity() );
      RegistrationState.setAction( context, GCMConstants.INTENT_TO_GCM_UNREGISTRATION );
      Log.d( TAG, "unregister: start unregister on device" );
      service.unregister( context, callback );
      waitForActionDone();
      Log.d( TAG, "unregister: unregistered" );

    }
    finally
    {
      lock.unlock();
      Log.d( TAG, "unregister: lock released" );
    }
  }

  @Override
  public void registrationCompleted( String senderId, String deviceToken, Long registrationExpiration,
                                     String callbackId )
  {
    Log.d( TAG, "registrationCompleted: device token is " + deviceToken );
    IDeviceRegistrationCallback deviceRegistrationCallback = callbackMap.get( callbackId );

    if( deviceRegistrationCallback != null )
      deviceRegistrationCallback.registered( senderId, deviceToken, registrationExpiration );

    notifyForActionDone();
  }

  @Override
  public void registrationFailed( String error, String callbackId )
  {
    Log.d( TAG, "registrationFailed: " + error );
    IDeviceRegistrationCallback deviceRegistrationCallback = callbackMap.get( callbackId );

    if( deviceRegistrationCallback != null )
      deviceRegistrationCallback.registrationFailed( error );

    callbackMap.remove( callbackId );
    notifyForActionDone();
  }

  @Override
  public void unregistrationCompleted( String callbackId )
  {
    IDeviceRegistrationCallback deviceRegistrationCallback = callbackMap.get( callbackId );

    if( deviceRegistrationCallback != null )
      deviceRegistrationCallback.unregister();

    callbackMap.remove( callbackId );
    notifyForActionDone();
  }

  @Override
  public void unregistrationFailed( String error, String callbackId )
  {
    IDeviceRegistrationCallback deviceRegistrationCallback = callbackMap.get( callbackId );

    if( deviceRegistrationCallback != null )
      deviceRegistrationCallback.unRegistrationFailed( error );

    notifyForActionDone();
  }

  @Override
  public boolean isRegistered( Context context )
  {
    String deviceToken = RegistrationState.getDeviceToken( context );
    return !(deviceToken == null || deviceToken.isEmpty());
  }

  private static void checkManifest( Context context )
  {
    PackageManager packageManager = context.getPackageManager();
    String packageName = context.getPackageName();

    checkPermission( packageName + ".permission.C2D_MESSAGE", packageManager );
    for( String permission : defaultPermissions )
      checkPermission( permission, packageManager );

    // check receivers
    PackageInfo receiversInfo;
    try
    {
      receiversInfo = packageManager.getPackageInfo( packageName, PackageManager.GET_RECEIVERS );
    }
    catch( PackageManager.NameNotFoundException e )
    {
      throw new IllegalStateException( "Could not get receivers for package " + packageName );
    }

    ActivityInfo[] receivers = receiversInfo.receivers;
    if( receivers == null || receivers.length == 0 )
      throw new IllegalStateException( "No receiver for package " + packageName );

    Set<String> allowedReceivers = new HashSet<String>();
    for( ActivityInfo receiver : receivers )
      if( GCMConstants.PERMISSION_GCM_INTENTS.equals( receiver.permission ) )
        allowedReceivers.add( receiver.name );

    if( allowedReceivers.isEmpty() )
      throw new IllegalStateException( "No receiver allowed to receive " + GCMConstants.PERMISSION_GCM_INTENTS );

    checkReceiver( context, allowedReceivers, GCMConstants.INTENT_FROM_GCM_REGISTRATION_CALLBACK );
    checkReceiver( context, allowedReceivers, GCMConstants.INTENT_FROM_GCM_MESSAGE );
  }

  private static void checkReceiver( Context context, Set<String> allowedReceivers, String action )
  {
    PackageManager pm = context.getPackageManager();
    String packageName = context.getPackageName();
    Intent intent = new Intent( action );
    intent.setPackage( packageName );
    List<ResolveInfo> receivers = pm.queryBroadcastReceivers( intent, PackageManager.GET_INTENT_FILTERS );

    if( receivers.isEmpty() )
      throw new IllegalStateException( "No receivers for action " + action );

    // make sure receivers match
    for( ResolveInfo receiver : receivers )
    {
      String name = receiver.activityInfo.name;
      if( !allowedReceivers.contains( name ) )
        throw new IllegalStateException( "Receiver " + name +
                                                 " is not set with permission " +
                                                 GCMConstants.PERMISSION_GCM_INTENTS );
    }
  }

  private static void checkPermission( String packageName, PackageManager packageManager )
  {
    try
    {
      packageManager.getPermissionInfo( packageName, PackageManager.GET_PERMISSIONS );
    }
    catch( PackageManager.NameNotFoundException e )
    {
      throw new IllegalStateException( "Application does not define permission " + packageName );
    }
  }

  private static void checkDevice( Context context )
  {
    int version = Build.VERSION.SDK_INT;

    if( version < 8 )
      throw new UnsupportedOperationException( "Device must be at least " +
                                                       "API Level 8 (instead of " + version + ")" );

    PackageManager packageManager = context.getPackageManager();

    try
    {
      packageManager.getPackageInfo( GCMConstants.GSF_PACKAGE, 0 );
    }
    catch( PackageManager.NameNotFoundException e )
    {
      throw new UnsupportedOperationException( "Device does not have package " + GCMConstants.GSF_PACKAGE );
    }
  }

  private static long getRegistrationExpiration( Date time )
  {
    return time == null ? DEFAULT_ON_SERVER_LIFESPAN_MS + System.currentTimeMillis() : time.getTime();
  }

  private void waitForActionDone()
  {
    try
    {
      synchronized( notify )
      {
        notify.wait();
      }
    }
    catch( InterruptedException e )
    {
      throw new RuntimeException( e );
    }
  }

  private void notifyForActionDone()
  {

    synchronized( notify )
    {
      notify.notifyAll();
    }
  }
}