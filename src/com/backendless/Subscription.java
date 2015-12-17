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

package com.backendless;

import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.ExceptionMessage;
import com.backendless.messaging.AndroidHandler;
import com.backendless.messaging.GenericMessagingHandler;
import com.backendless.messaging.IMessageHandler;
import com.backendless.messaging.Message;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@SuppressWarnings( "unchecked" )
public abstract class Subscription
{
  private String subscriptionId;
  private String channelName;

  public String getSubscriptionId()
  {
    return subscriptionId;
  }

  public synchronized void setSubscriptionId( String subscriptionId )
  {
    this.subscriptionId = subscriptionId;
  }

  public String getChannelName()
  {
    return channelName;
  }

  public synchronized void setChannelName( String channelName )
  {
    this.channelName = channelName;
  }

  public abstract boolean cancelSubscription();

  public abstract void onSubscribe( final AsyncCallback<List<Message>> subscriptionResponder );

  public abstract void pauseSubscription();

  public abstract void resumeSubscription();

  @Override
  public boolean equals( Object o )
  {
    if( this == o )
      return true;
    if( o == null || getClass() != o.getClass() )
      return false;

    Subscription that = (Subscription) o;

    if( subscriptionId != null ? !subscriptionId.equals( that.subscriptionId ) : that.subscriptionId != null )
      return false;
    return !(channelName != null ? !channelName.equals( that.channelName ) : that.channelName != null);
  }

  @Override
  public int hashCode()
  {
    int result = subscriptionId != null ? subscriptionId.hashCode() : 0;
    result = 31 * result + (channelName != null ? channelName.hashCode() : 0);
    return result;
  }
}