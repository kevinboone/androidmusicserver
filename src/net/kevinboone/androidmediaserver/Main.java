/*
 *  Kevin's Music Server for Android
 *  Copyright (c)2015
 *  Distributed under the terms of the GNU Public Licence, version 2.0
 */
package net.kevinboone.androidmediaserver;

import android.app.Activity;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.media.*;
import android.content.*;
import java.io.*;

/** This class contains the Android user interface, such as it is. */
public class Main extends Activity
{
    @Override
    public void onCreate (Bundle savedInstanceState)
      {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);
      setVolumeControlStream(AudioManager.STREAM_MUSIC);
      TextView messageView = (TextView) findViewById (R.id.message);
      String ip = AndroidNetworkUtil.getWifiIP(this);
      if (ip != null)
        {
        try 
          {
          Intent intent = new Intent (this, WebPlayerService.class);
          startService (intent);
          messageView.setText ("http://" + ip + ":30000/");
          } 
        catch (Exception e) 
          {
          messageView.setText ("Error: " + e.getMessage());
          }
        }
      else
        {
        messageView.setText ("No WIFI connection?");
        }
      }

    @Override
    public void onDestroy()
    {
    super.onDestroy();
    Intent intent = new Intent (this, WebPlayerService.class);
    stopService (intent);
    }

   public void buttonStopClicked(View dummy)
   {
   finish();
   }

}
