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

import android.util.Log;
import com.backendless.Backendless;
import com.backendless.DeviceRegistration;
import com.backendless.Invoker;
import com.backendless.Messaging;
import com.backendless.ThreadPoolService;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessException;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.messaging.SubscriptionOptions;
import weborb.v3types.GUID;

public class PubSubNotificationDeviceRegistrationCallback implements IDeviceRegistrationCallback
{
  public static final String TAG = "PubSubNotificationDeviceRegistrationCallback";
  private String id = new GUID().toString();
  private String channelName;
  private SubscriptionOptions subscriptionOptions;
  private AsyncCallback<String> subscriptionAsyncCallback;
  private String subscriptionId;

  public PubSubNotificationDeviceRegistrationCallback( String channelName, SubscriptionOptions subscriptionOptions,
                                                       AsyncCallback<String> subscriptionAsyncCallback )
  {
    this.channelName = channelName;
    this.subscriptionOptions = subscriptionOptions;
    this.subscriptionAsyncCallback = subscriptionAsyncCallback;
  }

  @Override
  public void registered( String senderId, String deviceToken, Long registrationExpiration )
  {
    DeviceRegistration registration = new DeviceRegistration();
    registration.setDeviceId( Messaging.DEVICE_ID );
    registration.setDeviceToken( deviceToken );
    registration.setOs( Messaging.OS );
    registration.setOsVersion( Messaging.OS_VERSION );

    if( subscriptionOptions == null )
      subscriptionOptions = new SubscriptionOptions();

    Invoker.invokeAsync( Messaging.MESSAGING_MANAGER_SERVER_ALIAS, "subscribeForPollingAccess", new Object[] { Backendless.getApplicationId(), Backendless.getVersion(), channelName, subscriptionOptions, registration }, new AsyncCallback<String>()
    {
      @Override
      public void handleResponse( String subscriptionId )
      {
        PubSubNotificationDeviceRegistrationCallback.this.subscriptionId = subscriptionId;
        subscriptionAsyncCallback.handleResponse( subscriptionId );
        Log.d( TAG, "registered: subscribeForPollingAccess on server subscription id is: " + subscriptionId );
      }

      @Override
      public void handleFault( BackendlessFault fault )
      {
        subscriptionAsyncCallback.handleFault( fault );
      }
    } );
  }

  @Override
  public void unregister()
  {
    ThreadPoolService.getPoolExecutor().execute( new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          Invoker.invokeSync( Messaging.MESSAGING_MANAGER_SERVER_ALIAS, "unsubscribe", new Object[] { subscriptionId } );
          Log.d( TAG, "unregister successful" );
        }
        catch( BackendlessException e )
        {
          Log.e( TAG, "unregister error: " + e.getMessage() );
        }
      }
    } );
  }

  @Override
  public void registrationFailed( String error )
  {

  }

  @Override
  public void unRegistrationFailed( String error )
  {

  }

  @Override
  public String getIdentity()
  {
    return id;
  }
}
