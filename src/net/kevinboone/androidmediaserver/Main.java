/*
 *  Kevin's Music Server for Android
 *  Copyright (c)2015
 *  Distributed under the terms of the GNU Public Licence, version 2.0
 */
package net.kevinboone.androidmediaserver;

import android.app.Activity;
import android.util.Log;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.media.*;
import android.content.*;
import java.io.*;
import net.kevinboone.androidmediaserver.client.*;

/** This class contains the Android user interface, such as it is. */
public class Main extends Activity
{
    private Handler handler = new Handler();
    private int port = 30000;
    private int uiUpdateInterval = 5000; // msec

    TextView titleView;
    TextView albumView;
    TextView artistView;
    TextView transportStatusView;

    TextView messageView;

    /* Define a runnable for use with the Handler, that will 
       update the UI on the main thread every 5 seconds */
    private Runnable updateUITask = new Runnable()
      {
      public void run()
        {
        messageView.setText ("");
        updateUI();
        handler.postDelayed (this, uiUpdateInterval);
        }
      };

    /**
       Update the UI from the server thread. 
    */
    public void updateUI()
      {
      try
        {
        Client client = new Client ("localhost", port);
        Status status = client.getStatus();
        titleView.setText (status.getTitle());
        albumView.setText (status.getAlbum());
        artistView.setText (status.getArtist());
        transportStatusView.setText 
          (status.transportStatusToString (status.getTransportStatus()));
        }
      catch (Exception e)
        {
        Log.e ("AMS", e.toString());
        }
      }

    @Override
    public void onCreate (Bundle savedInstanceState)
      {
      super.onCreate(savedInstanceState);

      /*These lines allow network operations on the main thread. In nearly
      all cases this is a bad idea, but here the network operation is
      to a different thread in this same application, and should never
      block. If it does block, the app's hosed anyway, so this won't
      make it worse. */
      StrictMode.ThreadPolicy policy = 
        new StrictMode.ThreadPolicy.Builder().permitAll().build();
      StrictMode.setThreadPolicy(policy); 

      setContentView(R.layout.main);
      setVolumeControlStream(AudioManager.STREAM_MUSIC);
      TextView urlView = (TextView) findViewById (R.id.url);
      titleView = (TextView) findViewById (R.id.title);
      messageView = (TextView) findViewById (R.id.message);
      artistView = (TextView) findViewById (R.id.artist);
      albumView = (TextView) findViewById (R.id.album);
      transportStatusView = (TextView) findViewById (R.id.transport_status);
      String ip = AndroidNetworkUtil.getWifiIP(this);
      if (ip != null)
        {
        try 
          {
          Intent intent = new Intent (this, WebPlayerService.class);
          startService (intent);
          urlView.setText ("http://" + ip + ":" + port + "/");
          
          // If we get here, with luck the server is running

          handler.removeCallbacks (updateUITask); 
          handler.postDelayed (updateUITask, uiUpdateInterval);
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

   public void buttonShutdownClicked(View dummy)
   {
   finish();
   }


   public void buttonPlayClicked(View dummy)
   {
   play();
   updateUI();
   }


   public void buttonPauseClicked(View dummy)
   {
   pause();
   updateUI();
   }


   public void buttonNextClicked(View dummy)
   {
   next();
   updateUI();
   }


   public void buttonPrevClicked(View dummy)
   {
   prev();
   updateUI();
   }


   public void buttonStopClicked(View dummy)
   {
   stop();
   updateUI();
   }


  public void play()
    {
    Client client = new Client ("localhost", port);
    try
      {
      client.play();
      }
    catch (Exception e)
      {
      messageView.setText (e.getMessage());
      }
    }


  public void pause()
    {
    Client client = new Client ("localhost", port);
    try
      {
      client.pause();
      }
    catch (Exception e)
      {
      messageView.setText (e.getMessage());
      }
    }


  public void next()
    {
    Client client = new Client ("localhost", port);
    try
      {
      client.next();
      }
    catch (Exception e)
      {
      messageView.setText (e.getMessage());
      }
    }


  public void prev()
    {
    Client client = new Client ("localhost", port);
    try
      {
      client.prev();
      }
    catch (Exception e)
      {
      messageView.setText (e.getMessage());
      }
    }


  public void stop()
    {
    Client client = new Client ("localhost", port);
    try
      {
      client.stop();
      }
    catch (Exception e)
      {
      messageView.setText (e.getMessage());
      }
    }


}
