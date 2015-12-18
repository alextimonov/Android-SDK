package com.backendless.exceptions;


public class IllegalDeliveryMethodTypeException extends BackendlessException
{
  public IllegalDeliveryMethodTypeException()
  {
    this("You can use DeliveryMethod.PUSH only if register Backendless");
  }

  public IllegalDeliveryMethodTypeException( String message )
  {
    super( message );
  }
}
