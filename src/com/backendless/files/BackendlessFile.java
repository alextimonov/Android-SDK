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

package com.backendless.files;

import com.backendless.Backendless;
import com.backendless.ThreadPoolService;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessException;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.exceptions.ExceptionMessage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BackendlessFile
{
  private static final int BUFFER_SIZE = 64 * 1024;
  private static final String FILE_DOWNLOAD_ERROR_MESSAGE = "Error during file download. Message: ";
  private static final String SERVER_RETURNED_HTTP = "Server returned HTTP ";
  private static final int TIMEOUT = 5000;
  private static final int MAX_PROGRESS = 10000;
  private static final String BACKENDLESS_API_URL = "https://api.backendless.com";
  private static final String FILE_IS_TOO_BIG = "File is too big to get it in byte array";
  private static final String USER_CANCELS_DOWNLOAD = "User cancels download";

  private String fileURL;
  private boolean userCancelsDownload = false;
  private boolean isAutoDownloadComplete = false;
  private byte[] autoDownloadedData;
  private InputStream inputStreamForDownloadedData;
  private Thread threadForAutoDownload;

  public BackendlessFile( String fileURL )
  {
    this.fileURL = fileURL;
    autoDownloadFileUrlToMemory();
  }

  public void setFileURL( String fileURL )
  {
    this.fileURL = fileURL;
  }

  public String getFileURL()
  {
    return fileURL;
  }

  public void remove() throws BackendlessException
  {
    Backendless.Files.remove( fileURL );
  }

  public void remove( AsyncCallback<Void> responder )
  {
    Backendless.Files.remove( fileURL, responder );
  }

  public void autoDownload( boolean enable )
  {

    if( enable )
      autoDownloadFileUrlToMemory();
    else
      interruptAutoDownload();

  }

  public void download(final OutputStream stream, final AsyncCallback<Void> asyncCallback )
  {
    final android.app.ProgressDialog progressDialog = null;
    download( stream, progressDialog, asyncCallback );
  }

  public void download( final OutputStream stream, final android.app.ProgressDialog progressDialog,
                        final AsyncCallback<Void> asyncCallback )
  {
    checkOutputStream( stream, asyncCallback );
    checkInternetConnection( asyncCallback );
    asyncDownloadToStream( stream, asyncCallback, progressDialog );
  }

  public void download( final String localFilePathName, final AsyncCallback<File> asyncCallback )
  {
    final android.app.ProgressDialog progressDialog = null;
    download( localFilePathName, progressDialog, asyncCallback );
  }

  public void download( String localFilePathName, android.app.ProgressDialog progressDialog,
                        AsyncCallback<File> asyncCallback )
  {
    checkFilePathName( localFilePathName, asyncCallback );
    checkInternetConnection( asyncCallback );
    asyncDownloadToFile( localFilePathName, asyncCallback, progressDialog );
  }

  public void download( final AsyncCallback<byte[]> asyncCallback )
  {
    final android.app.ProgressDialog progressDialog = null;
    download( progressDialog, asyncCallback );
  }

  public void download( android.app.ProgressDialog progressDialog, AsyncCallback<byte[]> asyncCallback )
  {
    checkInternetConnection( asyncCallback );
    asyncDownloadToByteArray( asyncCallback, progressDialog );
  }

  private void asyncDownloadToStream( final OutputStream outputStream, final AsyncCallback<Void> asyncCallback,
                                      final android.app.ProgressDialog progressDialog )
  {
    setListenerForProgressDialog( asyncCallback, progressDialog );
    ThreadPoolService.getPoolExecutor().execute(new Runnable()
    {
      @Override
      public void run()
      {

        if( isAutoDownloadComplete )
          downloadToStreamFromMemory( outputStream, asyncCallback );
        else
          downloadToStream( outputStream, asyncCallback, progressDialog );
      }
    } );
  }

  private void asyncDownloadToFile( final String localFilePathName, final AsyncCallback<File> asyncCallback,
                                    final android.app.ProgressDialog progressDialog )
  {
    setListenerForProgressDialog( asyncCallback, progressDialog );
    ThreadPoolService.getPoolExecutor().execute( new Runnable()
    {
      @Override public void run()
      {
        File outputFile = new File( localFilePathName );

        if( isAutoDownloadComplete )
          downloadToFileFromMemory( outputFile, asyncCallback );
        else
          downloadToFile( outputFile, asyncCallback, progressDialog );

        asyncCallback.handleResponse( outputFile );
      }
    } );
  }

  private void asyncDownloadToByteArray( final AsyncCallback<byte[]> asyncCallback,
                                         final android.app.ProgressDialog progressDialog )
  {
    setListenerForProgressDialog( asyncCallback, progressDialog );
    ThreadPoolService.getPoolExecutor().execute( new Runnable()
    {
      @Override
      public void run()
      {

        if( isAutoDownloadComplete )
          asyncCallback.handleResponse(autoDownloadedData);
        else
        {
          byte[] bytes = downloadToByteArray( asyncCallback, progressDialog );
          asyncCallback.handleResponse( bytes );
        }

      }
    } );
  }

  private <T> void setListenerForProgressDialog( final AsyncCallback<T> asyncCallback,
                                                 final android.app.ProgressDialog progressDialog )
  {
    progressDialog.setOnDismissListener( new android.content.DialogInterface.OnDismissListener()
    {
      @Override
      public void onDismiss( android.content.DialogInterface dialogInterface )
      {
        userCancelsDownload = true;
        asyncCallbackFaultOrThrowException( asyncCallback, USER_CANCELS_DOWNLOAD );
      }
    } );
  }

  private void downloadToStream(final OutputStream outputStream, final AsyncCallback<Void> asyncCallback,
                                final android.app.ProgressDialog progressDialog )
  {
    HttpURLConnection urlConnection = null;

    try
    {
      urlConnection = getHttpURLConnection( asyncCallback );
      long fileSize = urlConnection.getContentLengthLong();

      try (InputStream inputStream = new BufferedInputStream( urlConnection.getInputStream(), BUFFER_SIZE ))
      {
        readAndWrite( inputStream, outputStream, progressDialog, fileSize );
        outputStream.flush();
      }

    }
    catch( IOException e )
    {
      asyncCallbackFaultOrThrowException( asyncCallback, FILE_DOWNLOAD_ERROR_MESSAGE + e.getMessage() );
    }
    finally
    {

      if( urlConnection != null )
        urlConnection.disconnect();

    }
  }

  private void downloadToFile( final File outputFile, final AsyncCallback<File> asyncCallback,
                               final android.app.ProgressDialog progressDialog )
  {
    HttpURLConnection urlConnection = null;

    try
    {
      urlConnection = getHttpURLConnection( asyncCallback );
      long fileSize = urlConnection.getContentLengthLong();

      try (InputStream inputStream = new BufferedInputStream( urlConnection.getInputStream(), BUFFER_SIZE );
           OutputStream outputStream = new BufferedOutputStream( new FileOutputStream( outputFile ) ))
      {
        readAndWrite( inputStream, outputStream, progressDialog, fileSize );
      }

    }
    catch( IOException e )
    {
      asyncCallbackFaultOrThrowException( asyncCallback, FILE_DOWNLOAD_ERROR_MESSAGE + e.getMessage() );
    }
    finally
    {

      if( urlConnection != null )
        urlConnection.disconnect();

    }
  }

  private byte[] downloadToByteArray( final AsyncCallback<byte[]> asyncCallback,
                                      final android.app.ProgressDialog progressDialog )
  {
    HttpURLConnection urlConnection = null;
    ByteArrayOutputStream outputStream;

    try
    {
      urlConnection = getHttpURLConnection( asyncCallback );
      long fileSize = urlConnection.getContentLengthLong();

      if( fileSize > Integer.MAX_VALUE )
      {
        throw new IOException( FILE_IS_TOO_BIG );
      }

      try (InputStream inputStream = new BufferedInputStream( urlConnection.getInputStream(), BUFFER_SIZE ))
      {
        outputStream = new ByteArrayOutputStream();
        readAndWrite( inputStream, outputStream, progressDialog, fileSize );
      }

      return outputStream.toByteArray();
    }
    catch( IOException e )
    {
      asyncCallbackFaultOrThrowException( asyncCallback, FILE_DOWNLOAD_ERROR_MESSAGE + e.getMessage() );
      return new byte[ 0 ];
    }
    finally
    {

      if( urlConnection != null )
        urlConnection.disconnect();

    }
  }

  private void readAndWrite( InputStream inputStream, OutputStream outputStream,
                             final android.app.ProgressDialog progressDialog, long fileSize ) throws IOException
  {

    if( progressDialog == null )
      readAndWrite( inputStream, outputStream );
    else
    {
      long totalRead = 0;
      int bytesRead;
      Long progressValue;
      byte[] chunk = new byte[ BUFFER_SIZE ];
      userCancelsDownload = false;

      while( ( bytesRead = inputStream.read( chunk ) ) > 0 && !userCancelsDownload )
      {
        outputStream.write( chunk, 0, bytesRead );
        totalRead += bytesRead;
        progressValue = MAX_PROGRESS * totalRead / fileSize;
        progressDialog.setProgress( progressValue.intValue() );
      }

    }

  }

  private void readAndWrite( InputStream inputStream, OutputStream outputStream ) throws IOException
  {
    int bytesRead;
    byte[] chunk = new byte[ BUFFER_SIZE ];

    while( ( bytesRead = inputStream.read( chunk ) ) > 0 )
      outputStream.write( chunk, 0, bytesRead );

  }

  private <T> HttpURLConnection getHttpURLConnection( AsyncCallback<T> asyncCallback ) throws IOException
  {
    URL url = new URL( fileURL );
    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
    urlConnection.setConnectTimeout( TIMEOUT );
    urlConnection.setReadTimeout( TIMEOUT );
    urlConnection.connect();

    if( urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK )
      asyncCallbackFaultOrThrowException( asyncCallback, SERVER_RETURNED_HTTP + urlConnection.getResponseCode() + " " +
              urlConnection.getResponseMessage() );

    return urlConnection;
  }

  private <T> void checkOutputStream( OutputStream stream, AsyncCallback<T> asyncCallback )
  {

    if( stream == null )
      asyncCallbackFaultOrThrowException( asyncCallback, ExceptionMessage.NULL_STREAM);

  }

  private <T> void checkFilePathName( String localFilePathName, AsyncCallback<T> asyncCallback )
  {

    if( localFilePathName == null || localFilePathName.isEmpty() )
      asyncCallbackFaultOrThrowException( asyncCallback, ExceptionMessage.NULL_NAME );

    checkWriteToFilePathNameIsPossible( localFilePathName, asyncCallback );
  }

  private <T> void checkWriteToFilePathNameIsPossible( String localFilePathName, AsyncCallback<T> asyncCallback )
  {
    File parentFile = new File( localFilePathName ).getParentFile();

    if( !parentFile.exists() || !parentFile.isDirectory() || !parentFile.canWrite() || !checkWriteExternalStorage() )
      asyncCallbackFaultOrThrowException( asyncCallback, ExceptionMessage.UNABLE_DOWNLOAD_TO_DIRECTORY );

  }

  private <T> void asyncCallbackFaultOrThrowException( AsyncCallback<T> asyncCallback, String exceptionMessage )
  {

    if( asyncCallback != null )
      asyncCallback.handleFault( new BackendlessFault( exceptionMessage ) );
    else
      throw new BackendlessException( exceptionMessage );

  }

  //TODO remove getting permissions
  private boolean checkWriteExternalStorage()
  {
    return true;
  }

  private <T> void checkInternetConnection( AsyncCallback<T> asyncCallback )
  {
    checkIfInternetWorks( asyncCallback );
  }

  private boolean checkAccessNetworkState()
  {
    return true;
  }

  private boolean checkInternetState()
  {
    return true;
  }

  private <T> void checkIfInternetWorks( final AsyncCallback<T> asyncCallback )
  {
    ThreadPoolService.getPoolExecutor().execute(new Runnable()
    {
      @Override
      public void run()
      {

        try
        {
          URL url = new URL( BACKENDLESS_API_URL );
          HttpURLConnection connection = ( HttpURLConnection ) url.openConnection();
          connection.setConnectTimeout( TIMEOUT );
          connection.connect();
          if( connection.getResponseCode() != HttpURLConnection.HTTP_OK )
            throw new IOException();
        }
        catch( IOException e)
        {
          asyncCallbackFaultOrThrowException( asyncCallback, ExceptionMessage.INTERNET_CONNECTION_IS_NOT_AVAILABLE );
        }

      }
    } );
  }

  private void autoDownloadFileUrlToMemory()
  {
    checkInternetConnection( null );
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    asyncAutoDownloadToStream( outputStream );
  }

  private void asyncAutoDownloadToStream( final ByteArrayOutputStream outputStream )
  {
    threadForAutoDownload = new Thread( new Runnable() {
      @Override
      public void run()
      {
        autoDownloadToStream( outputStream );
      }
    });
    threadForAutoDownload.start();
  }

  private void autoDownloadToStream( ByteArrayOutputStream outputStream )
  {
    HttpURLConnection urlConnection = null;

    try
    {
      urlConnection = getHttpURLConnection( null );
      long fileSize = checkAvailableMemory( urlConnection );
      autoDownloadedData = new byte[ (int) fileSize ];

      try (InputStream inputStream = new BufferedInputStream( urlConnection.getInputStream(), BUFFER_SIZE ) )
      {
        readAndWrite( inputStream, outputStream );
      }

      autoDownloadedData = outputStream.toByteArray();
      inputStreamForDownloadedData = new ByteArrayInputStream( autoDownloadedData);
      isAutoDownloadComplete = true;

    }
    catch( IOException e )
    {
      throw new BackendlessException( FILE_DOWNLOAD_ERROR_MESSAGE + e.getMessage() );
    }
    finally
    {

      if( urlConnection != null )
        urlConnection.disconnect();

    }
  }

  private long checkAvailableMemory( HttpURLConnection urlConnection )
  {
    long fileSize = urlConnection.getContentLengthLong();
    long freeMemory = Runtime.getRuntime().freeMemory();

    if( freeMemory < fileSize )
      throw new BackendlessException( ExceptionMessage.NOT_ENOUGH_MEMORY );

    return fileSize;
  }

  private void downloadToFileFromMemory( File outputFile, AsyncCallback<File> asyncCallback )
  {

    try
    {
      OutputStream outputStream = new BufferedOutputStream( new FileOutputStream( outputFile ), BUFFER_SIZE );
      readAndWrite(inputStreamForDownloadedData, outputStream );
      outputStream.flush();
      outputStream.close();
    }
    catch( IOException e )
    {
      asyncCallbackFaultOrThrowException( asyncCallback, FILE_DOWNLOAD_ERROR_MESSAGE + e.getMessage() );
    }

  }

  private void downloadToStreamFromMemory( OutputStream outputStream, AsyncCallback<Void> asyncCallback )
  {

    try
    {
      readAndWrite(inputStreamForDownloadedData, outputStream );
    }
    catch( IOException e )
    {
      asyncCallbackFaultOrThrowException( asyncCallback, FILE_DOWNLOAD_ERROR_MESSAGE + e.getMessage() );
    }

  }

  private void interruptAutoDownload()
  {
    threadForAutoDownload.interrupt();
    isAutoDownloadComplete = false;
  }

  /*
  public void download()
  {
    URL file = new URL( fileURL );
    ReadableByteChannel rbc = Channels.newChannel(file.openStream());
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    baos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
  }
  */
}