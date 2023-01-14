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
import java.net.*;
import android.preference.*;
import net.kevinboone.androidmediaserver.client.*;

/** This class contains the Android user interface, such as it is. */
public class Main extends Activity
{
    private Handler handler = new Handler();
    // Ugly, but we need the port number to be accessible to the background
    //  service, and there is no easy way to pass arguments to it
    protected static int port = 30000;
    protected int uiUpdateInterval = 5000; // msec
    protected int webUpdateInterval = 5000; // msec
    protected static int tracksPerPage = 30;
    protected static int maxSearchResults = 20;


    /*An arbitrary value to distinguish completion of the Settings activity
    from any other activity (there are none, at present) */
    private static final int RESULT_SETTINGS = 0;

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
       Update the UI from the server monitoring thread. 
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
      catch (ConnectException e)
        {
        /* This is probably the user's only indication that the web server
        did not initialize. */
        Log.e ("AMS", e.toString());
        messageView.setText 
          ("Can't connect to service: check server port number and restart");
        }
      catch (Exception e)
        {
        Log.e ("AMS", e.toString());
        messageView.setText (e.toString());
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

      applySettings();

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
          startBackgroundService();
          urlView.setText ("http://" + ip + ":" + port + "/");
          
          // If we get here, with luck the server is running

          handler.removeCallbacks (updateUITask); 
          handler.postDelayed (updateUITask, uiUpdateInterval);
          } 
        catch (Throwable e) 
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
      stopBackgroundService();
      super.onDestroy();
      }


   public void stopBackgroundService ()
     {
     Intent intent = new Intent (this, WebPlayerService.class);
     stopService (intent);
     }


   public void startBackgroundService ()
     {
     Intent intent = new Intent (this, WebPlayerService.class);
     startService (intent);
     }


   public void buttonSettingsClicked(View dummy)
     {
     Intent i = new Intent (this, SettingsActivity.class);
     startActivityForResult(i, RESULT_SETTINGS);
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

  @Override
  protected void onActivityResult (int requestCode,
      int resultCode, Intent data)
    {
    super.onActivityResult (requestCode, resultCode, data);
    switch (requestCode)
      {
      case RESULT_SETTINGS:
        applySettings (); 
      break;
      }
    }


  private void applySettings ()
    {
    uiUpdateInterval = getIntPreference ("uiupdateinterval", 5) * 1000;
    maxSearchResults = getIntPreference ("maxsearchresults", 20);
    tracksPerPage = getIntPreference ("tracksperpage", 30);
    webUpdateInterval = getIntPreference ("webupdateinterval", 5) * 1000;
    int newPort = getIntPreference ("port", 30000);
    if (newPort != port)
      {
      port = newPort;
      Log.w ("AMS", "Changing port number to " + port);
      stopBackgroundService();
      startBackgroundService();
      }
    }

  /** Wrapper around Android's brain-dead (non-)handling of integer-valued
 *       user preferences :/. */
  int getIntPreference (String name, int deflt)
    {
    SharedPreferences sharedPrefs =
      PreferenceManager.getDefaultSharedPreferences (this);

    int value = deflt;
    try
      {
      value =
        Integer.parseInt (sharedPrefs.getString (name, "" + deflt));
      }
    catch (Exception e)
      {
      value = deflt;
      }
    return value;
    }



}
