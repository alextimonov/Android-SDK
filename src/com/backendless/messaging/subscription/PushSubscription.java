package com.backendless.messaging.subscription;

import android.content.Context;
import com.backendless.Subscription;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.messaging.Message;
import com.backendless.push.registration.PubSubNotificationDeviceRegistrationCallback;
import com.backendless.push.registration.Registrar;

import java.util.List;

public class PushSubscription extends Subscription
{
  private PubSubNotificationDeviceRegistrationCallback deviceRegistrationCallback;
  private AsyncCallback<List<Message>> messagesCallback;
  private Context context;

  @Override
  public boolean cancelSubscription()
  {
    Registrar.getInstance().unregister( context, deviceRegistrationCallback );
    return true;
  }

  @Override
  public void onSubscribe( AsyncCallback<List<Message>> subscriptionResponder )
  {
    messagesCallback = subscriptionResponder;
  }

  @Override
  public void pauseSubscription()
  {

  }

  @Override
  public void resumeSubscription()
  {

  }

  public PubSubNotificationDeviceRegistrationCallback getDeviceRegistrationCallback()
  {
    return deviceRegistrationCallback;
  }

  public void setDeviceRegistrationCallback( PubSubNotificationDeviceRegistrationCallback deviceRegistrationCallback )
  {
    this.deviceRegistrationCallback = deviceRegistrationCallback;
  }

  public AsyncCallback<List<Message>> getMessagesCallback()
  {
    return messagesCallback;
  }

  public Context getContext()
  {
    return context;
  }

  public void setContext( Context context )
  {
    this.context = context;
  }
}
