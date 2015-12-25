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

import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import com.backendless.Backendless;
import com.backendless.Subscription;
import com.backendless.ThreadPoolService;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessException;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.messaging.Message;
import com.backendless.messaging.PublishOptions;
import com.backendless.messaging.subscription.PushSubscriptionHandler;
import com.backendless.persistence.BackendlessSerializer;
import com.backendless.push.registration.IReceiver;

import java.util.Arrays;
import java.util.List;

class PubSubReceiver implements IReceiver
{
  @Override
  public void handleMessage( Context context, Intent intent, boolean showNotification )
  {
    String message = intent.getStringExtra( PublishOptions.MESSAGE_TAG );
    String subscriptionIdentity = intent.getStringExtra( Receiver.SUBSCRIBER_IDENTITY_KEY );
    List<Message> messageList = null;

    final AsyncCallback<List<Message>> responder = PushSubscriptionHandler.getResponder( subscriptionIdentity );
    if( responder == null )
    {
      return; //maybe we should show warning?
    }

    if(message== null || message.isEmpty())
    {
      final Subscription subscription = PushSubscriptionHandler.getSubscription( subscriptionIdentity );

      ThreadPoolService.getPoolExecutor().execute( new Runnable()
      {
        @Override
        public void run()
        {
          try
          {
            List<Message> messageList = Backendless.Messaging.pollMessages( subscription.getChannelName(), subscription.getSubscriptionId() );
            responder.handleResponse( messageList );
          }
          catch( BackendlessException e )
          {
            responder.handleFault( new BackendlessFault( e.getMessage() ) );
          }
        }
      } );

      return;
    }

    byte[] byteMessage = Base64.decode( message, Base64.DEFAULT );
    Message deserializedMessage = BackendlessSerializer.deserializeAMF( byteMessage );
    messageList = Arrays.asList( new Message[] { deserializedMessage } );

    responder.handleResponse( messageList );
  }
}
