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
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import weborb.v3types.GUID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PushNotificationDeviceRegistrationCallback implements IDeviceRegistrationCallback
{
  private Object lock = new Object();
  private String guid = new GUID().toString();

  private static final PushNotificationDeviceRegistrationCallback instance = new PushNotificationDeviceRegistrationCallback();

  Map<List<String>, Collection<AsyncCallback<Void>>> unNotified = new HashMap<List<String>, Collection<AsyncCallback<Void>>>();

  public static IDeviceRegistrationCallback createCallback( List<String> channels, AsyncCallback<Void> callback )
  {
    if( callback == null )
      return instance;

    return instance.addCallback( channels, callback );
  }

  private PushNotificationDeviceRegistrationCallback()
  {
  }

  @Override
  public void registered( String senderId, String deviceToken, Long registrationExpiration )
  {
    synchronized ( lock )
    {
      Iterator<List<String>> it = unNotified.keySet().iterator();

      while ( it.hasNext() )
      {
        List<String> channels = it.next();

        Collection<AsyncCallback<Void>> callbacks = unNotified.get( channels );

        for ( final AsyncCallback<Void> callback : callbacks )
        {
          try
          {
            Backendless.Messaging.registerDeviceOnServer( deviceToken, channels, registrationExpiration );
            callback.handleResponse( null );
          }
          catch ( Exception e )
          {
            callback.handleFault( new BackendlessFault( e.getMessage() ) );
          }
        }

        unNotified.remove( channels );
      }
    }
  }

  @Override
  public void unregister()
  {

    synchronized ( lock )
    {
      Iterator<List<String>> it = unNotified.keySet().iterator();

      while ( it.hasNext() )
      {
        List<String> channels = it.next();

        Collection<AsyncCallback<Void>> callbacks = unNotified.get( channels );

        for ( final AsyncCallback<Void> callback : callbacks )
        {
          try
          {
            boolean success = Backendless.Messaging.unregisterDeviceOnServer();

            if( success )
              callback.handleResponse( null );
            else
              callback.handleFault( new BackendlessFault( "Unregistration on backendless server failed" ) );
          }
          catch ( Exception e )
          {
            callback.handleFault( new BackendlessFault( e.getMessage() ) );
          }
        }

        unNotified.remove( channels );
      }
    }
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
    return guid;
  }

  private PushNotificationDeviceRegistrationCallback addCallback( List<String> channels, AsyncCallback<Void> callback )
  {
    synchronized ( lock )
    {
      if( !unNotified.containsKey( channels ) )
      {
        unNotified.put( channels, new ArrayList<AsyncCallback<Void>>() );
      }

      unNotified.get( channels ).add( callback );
    }

    return this;
  }
}
