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

package com.backendless.utils;

import com.backendless.async.callback.AsyncCallback;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ReflectionUtil
{
  /**
   * Retrieves the value of the field with given name from the given object.
   *
   * @param object    object containing the field
   * @param fieldName name of the field
   * @return Object, which is the value of the given field in the given object; null, if for some reason setAccessible(true) didn't work
   * @throws NoSuchFieldException if object doesn't have a field with such name
   */
  public static Object getFieldValue( Object object, String fieldName ) throws NoSuchFieldException
  {
    Field field = getField( object.getClass(), fieldName );
    field.setAccessible( true );

    try
    {
      return field.get( object );
    }
    catch( IllegalAccessException e )
    {
      // shouldn't ever be thrown, because setAccessible(true) was called before
      return null;
    }
  }

  /**
   * Retrieves a Field with a given name from the given class or its superclass.
   *
   * @param clazz     Class containing the field
   * @param fieldName name of the field
   * @return Field with given name from given class
   * @throws NoSuchFieldException if class doesn't have a field with such name
   */
  public static Field getField( Class clazz, String fieldName ) throws NoSuchFieldException
  {
    try
    {
      return clazz.getDeclaredField( fieldName );
    }
    catch( NoSuchFieldException noSuchFieldException )
    {
      if( clazz.getSuperclass() != null )
      {
        return getField( clazz.getSuperclass(), fieldName );
      }
      else
      {
        throw noSuchFieldException;
      }
    }
  }

  public static <T> Type getCallbackGenericType( AsyncCallback<T> callback )
  {
    Type[] genericInterfaces = callback.getClass().getGenericInterfaces();
    Type asyncCallbackInterface = null;

    for( Type genericInterface : genericInterfaces )
    {
      if( !(genericInterface instanceof ParameterizedType) )
        continue;

      Type rawType = ((ParameterizedType) genericInterface).getRawType();
      if( rawType instanceof Class && AsyncCallback.class.isAssignableFrom( (Class) rawType ) )
      {
        asyncCallbackInterface = genericInterface;
        break;
      }
    }

    return ((ParameterizedType) asyncCallbackInterface).getActualTypeArguments()[ 0 ];
  }
}
