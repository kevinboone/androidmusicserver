/*
 *  Kevin's Music Server for Android
 *  Copyright (c)2015
 *  Distributed under the terms of the GNU Public Licence, version 2.0
 */
package net.kevinboone.androidmediaserver;

import android.app.Service;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.media.*;
import android.content.*;
import java.io.*;

public class WebPlayerService extends Service 
{
  private WebServer server = null;

  @Override
  public void onCreate() 
    {
    try
      {
      server = new WebServer (this);
      server.start();
      }
    catch (IOException e)
      {
      throw new RuntimeException (e);
      }
    }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) 
    {
    return START_STICKY;
    }

  @Override
  public IBinder onBind (Intent intent) 
    {
    return null;
    }

  @Override
  public void onDestroy() 
    {
    if (server != null)
      server.stop();
    }

}

