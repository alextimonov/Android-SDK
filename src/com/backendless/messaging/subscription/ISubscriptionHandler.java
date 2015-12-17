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
import com.backendless.messaging.Message;
import com.backendless.messaging.SubscriptionOptions;

import java.util.List;

public interface ISubscriptionHandler
{
  void subscribe( final String channelName, final AsyncCallback<List<Message>> subscriptionResponder,
                         SubscriptionOptions subscriptionOptions, final int pollingInterval,
                         final AsyncCallback<Subscription> responder );

  Subscription subscribe( String channelName, AsyncCallback<List<Message>> subscriptionResponder,
                          SubscriptionOptions subscriptionOptions,
                          int pollingInterval );
}
