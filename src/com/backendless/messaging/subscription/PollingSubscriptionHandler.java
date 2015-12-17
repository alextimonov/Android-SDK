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

import com.backendless.Backendless;
import com.backendless.Invoker;
import com.backendless.Messaging;
import com.backendless.Subscription;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessException;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.exceptions.ExceptionMessage;
import com.backendless.messaging.Message;
import com.backendless.messaging.MessagingHelper;
import com.backendless.messaging.SubscriptionOptions;

import java.util.List;

public class PollingSubscriptionHandler implements ISubscriptionHandler
{
  @Override
  public void subscribe( final String channelName, final AsyncCallback<List<Message>> subscriptionResponder,
                         SubscriptionOptions subscriptionOptions, final int pollingInterval,
                         final AsyncCallback<Subscription> responder )
  {
    try
    {
      MessagingHelper.checkChannelName( channelName );

      if( pollingInterval < 0 )
        throw new IllegalArgumentException( ExceptionMessage.WRONG_POLLING_INTERVAL );

      subscribeForPollingAccess( channelName, subscriptionOptions, new AsyncCallback<String>()
      {
        @Override
        public void handleResponse( String subscriptionId )
        {
          PubSubSubscription subscription = new PubSubSubscription();
          subscription.setChannelName( channelName );
          subscription.setSubscriptionId( subscriptionId );

          if( pollingInterval != 0 )
            subscription.setPollingInterval( pollingInterval );

          subscription.onSubscribe( subscriptionResponder );

          if( responder != null )
            responder.handleResponse( subscription );
        }

        @Override
        public void handleFault( BackendlessFault fault )
        {
          if( responder != null )
            responder.handleFault( fault );
        }
      } );
    }
    catch( Throwable e )
    {
      if( responder != null )
        responder.handleFault( new BackendlessFault( e ) );
    }
  }

  @Override
  public Subscription subscribe( String channelName, AsyncCallback<List<Message>> subscriptionResponder,
                                 SubscriptionOptions subscriptionOptions, int pollingInterval )
  {
    MessagingHelper.checkChannelName( channelName );

    if( pollingInterval < 0 )
      throw new IllegalArgumentException( ExceptionMessage.WRONG_POLLING_INTERVAL );

    String subscriptionId = subscribeForPollingAccess( channelName, subscriptionOptions );

    PubSubSubscription subscription = new PubSubSubscription();
    subscription.setChannelName( channelName );
    subscription.setSubscriptionId( subscriptionId );

    if( pollingInterval != 0 )
      subscription.setPollingInterval( pollingInterval );

    subscription.onSubscribe( subscriptionResponder );

    return subscription;
  }

  private void subscribeForPollingAccess( String channelName, SubscriptionOptions subscriptionOptions,
                                          AsyncCallback<String> responder )
  {
    try
    {
      if( channelName == null )
        throw new IllegalArgumentException( ExceptionMessage.NULL_CHANNEL_NAME );

      if( subscriptionOptions == null )
        subscriptionOptions = new SubscriptionOptions();

      Invoker.invokeAsync( Messaging.MESSAGING_MANAGER_SERVER_ALIAS, "subscribeForPollingAccess", new Object[] { Backendless.getApplicationId(), Backendless.getVersion(), channelName, subscriptionOptions }, responder );
    }
    catch( Throwable e )
    {
      if( responder != null )
        responder.handleFault( new BackendlessFault( e ) );
    }
  }

  private String subscribeForPollingAccess( String channelName,
                                            SubscriptionOptions subscriptionOptions ) throws BackendlessException
  {
    if( channelName == null )
      throw new IllegalArgumentException( ExceptionMessage.NULL_CHANNEL_NAME );

    if( subscriptionOptions == null )
      subscriptionOptions = new SubscriptionOptions();

    return Invoker.invokeSync( Messaging.MESSAGING_MANAGER_SERVER_ALIAS, "subscribeForPollingAccess", new Object[] { Backendless.getApplicationId(), Backendless.getVersion(), channelName, subscriptionOptions } );
  }
}
