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

import com.backendless.ContextHandler;
import com.backendless.Subscription;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessException;
import com.backendless.exceptions.ExceptionMessage;
import com.backendless.messaging.Message;
import com.backendless.messaging.MessagingHelper;
import com.backendless.messaging.SubscriptionOptions;
import com.backendless.push.registration.Registrar;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PubSubSubscriptionHandler implements ISubscriptionHandler
{
  private static Map<String, HashMap.SimpleEntry<Subscription, AsyncCallback<List<Message>>>> responders = new ConcurrentHashMap<String, AbstractMap.SimpleEntry<Subscription, AsyncCallback<List<Message>>>>();
  private Object lock = new Object();

  @Override
  public void subscribe( final String channelName, final AsyncCallback<List<Message>> subscriptionResponder,
                         final SubscriptionOptions subscriptionOptions, int pollingInterval,
                         final AsyncCallback<Subscription> responder )
  {

    final String GCMSenderID = MessagingHelper.getGcmSenderId();

    if( GCMSenderID == null )
      throw new BackendlessException( ExceptionMessage.GCM_SENDER_ID_NOT_DECLARED );

//    final String deviceToken

    //Backendless.Messaging.registerDevice( GCMSenderID, channelName, new AsyncCallback<String>()

//    new AsyncTask<Void, Void, Void>()
//    {
//      @Override
//      protected Void doInBackground( Void... params )
//      {
//        try
//        {
//          Messaging.registerDeviceGCMSync( ContextHandler.getAppContext(), GCMSenderID, Arrays.asList( new String[] { channelName } ), null );
//          DeviceRegistration registration = new DeviceRegistration();
//          registration.setDeviceId( Messaging.DEVICE_ID );
//          registration.setDeviceToken( deviceToken );
//          registration.setOs( Messaging.OS );
//          registration.setOsVersion( Messaging.OS_VERSION );
//        }
//        catch( RuntimeException t )
//        {
//          if( responder != null )
//            responder.handleFault( new BackendlessFault( t.getMessage() ) );
//        }
//
//        return null;
//      }
//    }.execute();
  }

  @Override
  public Subscription subscribe( String channelName, AsyncCallback<List<Message>> subscriptionResponder,
                                 SubscriptionOptions subscriptionOptions, int pollingInterval )
  {
    final String GCMSenderID = MessagingHelper.getGcmSenderId();
    if( GCMSenderID == null )
      throw new BackendlessException( ExceptionMessage.GCM_SENDER_ID_NOT_DECLARED );

    Registrar.getInstance().register( ContextHandler.getAppContext(), GCMSenderID, Arrays.asList( new String[] { channelName } ) );
    return null;
  }

  public static AsyncCallback<List<Message>> getResponder( String subscriptionIdentity )
  {
    AbstractMap.SimpleEntry<Subscription, AsyncCallback<List<Message>>> entry = responders.get( subscriptionIdentity );
    return entry == null ? null : entry.getValue();
  }

  public static Subscription getSubscription( String subscriptionIdentity )
  {
    AbstractMap.SimpleEntry<Subscription, AsyncCallback<List<Message>>> entry = responders.get( subscriptionIdentity );
    return entry == null ? null : entry.getKey();
  }

  private static String getSubscriptionIdentity( String channelName, SubscriptionOptions subscriptionOptions )
  {
    String subscriptionIdentity = channelName;

    if( subscriptionOptions == null )
      return subscriptionIdentity;

    if( subscriptionOptions.getSubtopic() != null )
      subscriptionIdentity += subscriptionOptions.getSubtopic();

    if( subscriptionOptions.getSelector() != null )
      subscriptionIdentity += subscriptionOptions.getSelector();

    return subscriptionIdentity;
  }
}
