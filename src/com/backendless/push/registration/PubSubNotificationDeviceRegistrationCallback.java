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

import com.backendless.Backendless;
import com.backendless.DeviceRegistration;
import com.backendless.Invoker;
import com.backendless.Messaging;
import com.backendless.messaging.SubscriptionOptions;
import weborb.v3types.GUID;

public class PubSubNotificationDeviceRegistrationCallback implements IDeviceRegistrationCallback
{
  private String id = new GUID().toString();
  private String subscriptionId;
  private String channelName;
  private SubscriptionOptions subscriptionOptions;

  public PubSubNotificationDeviceRegistrationCallback( String channelName, SubscriptionOptions subscriptionOptions )
  {
    this.channelName = channelName;
    this.subscriptionOptions = subscriptionOptions;
  }

  @Override
  public void registered( String senderId, String deviceToken, Long registrationExpiration )
  {
    DeviceRegistration registration = new DeviceRegistration();
    registration.setDeviceId( Messaging.DEVICE_ID );
    registration.setDeviceToken( deviceToken );
    registration.setOs( Messaging.OS );
    registration.setOsVersion( Messaging.OS_VERSION );

    subscriptionId = Invoker.invokeSync( Messaging.MESSAGING_MANAGER_SERVER_ALIAS, "subscribeForPollingAccess", new Object[]
            { Backendless.getApplicationId(), Backendless.getVersion(), channelName, subscriptionOptions, registration });

  }

  @Override
  public void unregister()
  {
    boolean success = Backendless.Messaging.unregisterDeviceOnServer();
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

  public String getSubscriptionId()
  {
    return subscriptionId;
  }
}
