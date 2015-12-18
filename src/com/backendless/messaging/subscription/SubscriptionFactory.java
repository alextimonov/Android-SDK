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

package com.backendless.messaging.subscription;

import com.backendless.Subscription;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessException;
import com.backendless.exceptions.IllegalDeliveryMethodTypeException;
import com.backendless.messaging.DeliveryMethod;
import com.backendless.messaging.Message;
import com.backendless.messaging.MessagingHelper;
import com.backendless.messaging.SubscriptionOptions;

import java.util.List;

public class SubscriptionFactory implements ISubscriptionHandler
{
  private static final SubscriptionFactory instance = new SubscriptionFactory();
  private static final ISubscriptionHandler pollSubscriptionHandler = new PollingSubscriptionHandler();
  private static final ISubscriptionHandler pushSubscriptionHandler = new PushSubscriptionHandler();

  public static SubscriptionFactory get()
  {
    return instance;
  }

  private SubscriptionFactory()
  {
  }

  @Override
  public void subscribe( String channelName, AsyncCallback<List<Message>> subscriptionResponder, SubscriptionOptions subscriptionOptions, int pollingInterval, AsyncCallback<Subscription> responder )
  {

  }

  @Override
  public Subscription subscribe( String channelName, AsyncCallback<List<Message>> subscriptionResponder, SubscriptionOptions subscriptionOptions, int pollingInterval )
  {
    return null;
  }

  private ISubscriptionHandler getSubscriptionHandler( SubscriptionOptions subscriptionOptions )
  {
    if( MessagingHelper.getGcmSenderId() == null )
    {
      if( subscriptionOptions == null || subscriptionOptions.getDeliveryMethod() == DeliveryMethod.POLL )
      {
        return pollSubscriptionHandler;
      }

      throw new IllegalDeliveryMethodTypeException(  );
    }
  }
}
