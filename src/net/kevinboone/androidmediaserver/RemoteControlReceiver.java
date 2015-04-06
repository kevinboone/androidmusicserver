/*
 *  Kevin's Music Server for Android
 *  Copyright (c)2015
 *  Distributed under the terms of the GNU Public Licence, version 2.0
 */

package net.kevinboone.androidmediaserver;
import java.util.*;
import java.io.*;
import java.net.*;
import android.content.*;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.*;
import android.media.*;

public class RemoteControlReceiver extends BroadcastReceiver 
{
  protected static WebServer webServer = null;

  public static void setWebServer (WebServer w)
    {
    webServer = w;
    }

  @Override
  public void onReceive(Context context, Intent intent) 
    {
    if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) 
      {
      KeyEvent event = (KeyEvent)intent.getParcelableExtra
           (Intent.EXTRA_KEY_EVENT);
      if (KeyEvent.KEYCODE_MEDIA_PLAY == event.getKeyCode()) 
        {
        webServer.play();
        }
      else if (KeyEvent.KEYCODE_MEDIA_NEXT == event.getKeyCode()) 
        {
        webServer.playNextInPlaylist();
        }
      else if (KeyEvent.KEYCODE_MEDIA_PAUSE == event.getKeyCode()) 
        {
        webServer.pause();
        }
      else if (KeyEvent.KEYCODE_MEDIA_STOP == event.getKeyCode()) 
        {
        webServer.stopPlayback();
        }
      else if (KeyEvent.KEYCODE_MEDIA_PREVIOUS == event.getKeyCode()) 
        {
        webServer.playPrevInPlaylist();
        }
      }
    } 
}

