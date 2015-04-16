/*
 *  Kevin's Music Server for Android
 *  Copyright (c)2015
 *  Distributed under the terms of the GNU Public Licence, version 2.0
 */

package net.kevinboone.androidmusicplayer;
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
  protected static Player player = null;

  public static void setPlayer (Player _player)
    {
    player = _player;
    }

  @Override
  public void onReceive(Context context, Intent intent) 
    {
    try
      {
      if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) 
        {
        KeyEvent event = (KeyEvent)intent.getParcelableExtra
             (Intent.EXTRA_KEY_EVENT);
        if (event.getAction() == event.ACTION_DOWN)
          {
          if (KeyEvent.KEYCODE_MEDIA_PLAY == event.getKeyCode()) 
            {
            player.play();
            }
          else if (KeyEvent.KEYCODE_MEDIA_NEXT == event.getKeyCode()) 
            {
            player.playNextInPlaylist();
            }
          else if (KeyEvent.KEYCODE_MEDIA_PAUSE == event.getKeyCode()) 
            {
            player.pause();
            }
          else if (KeyEvent.KEYCODE_MEDIA_STOP == event.getKeyCode()) 
            {
            player.stop();
            }
          else if (KeyEvent.KEYCODE_MEDIA_PREVIOUS == event.getKeyCode()) 
            {
            player.playPrevInPlaylist();
            }
          }
        }
      } 
    catch (PlayerException e)
      {
      Log.w ("AMS", "Caught exception in remote control handler: " + e);
      }
    }
}

