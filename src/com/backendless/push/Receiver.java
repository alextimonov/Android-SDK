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
import com.backendless.push.registration.IReceiver;

class Receiver implements IReceiver
{
  static final String SUBSCRIBER_IDENTITY_KEY = "BL_SUBSCRIPTION_IDENTITY";

  private static final IReceiver pushReceiver = new PushReceiver();
  private static final IReceiver pubSubReceiver = new PubSubReceiver();

  @Override
  public void handleMessage( Context context, Intent intent, BackendlessBroadcastReceiver receiver )
  {
    String subscriberIdentity = intent.getStringExtra( SUBSCRIBER_IDENTITY_KEY );

    if( subscriberIdentity == null || subscriberIdentity.isEmpty())
       pushReceiver.handleMessage( context, intent, receiver );
    else
      pubSubReceiver.handleMessage( context, intent, receiver );

  }
}
