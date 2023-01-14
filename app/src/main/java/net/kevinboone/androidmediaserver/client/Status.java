package net.kevinboone.androidmediaserver.client;

import java.io.*;
import java.net.*;
import org.json.*;

public class Status 
{
  protected TransportStatus transportStatus = TransportStatus.UNKNOWN;
  protected int position;
  protected int duration;
  protected String title;
  protected String artist;
  protected String album;
  protected String uri;

  public enum TransportStatus {UNKNOWN, STOPPED, PAUSED, PLAYING}; 

  public void setTransportStatus (TransportStatus ts)
    {
    this.transportStatus = ts;
    }

  public TransportStatus getTransportStatus () 
    {
    return transportStatus;
    }

  public void setTitle (String s) { title = s; }
  public void setUri (String s) { uri = s; }
  public void setArtist (String s) { artist = s; }
  public void setAlbum (String s) { album = s; }
  public void setDuration (int d) { duration = d; }
  public void setPosition (int d) { position = d; }

  public String getTitle () { return title; }
  public String getUri () { return uri; }
  public String getArtist () { return artist; }
  public String getAlbum () { return album; }
  public int getPosition () { return position; }
  public int getDuration () { return duration; }
  

  public static String transportStatusToString (TransportStatus ts)
    {
    switch (ts)
      {
      case STOPPED: return "stopped";
      case PAUSED: return "paused";
      case PLAYING: return "playing";
      }
    return "unknown";
    }

  public String toString()
    {
    StringBuffer sb = new StringBuffer();
    sb.append ("Transport status: ");
    sb.append (transportStatusToString (transportStatus));
    sb.append ("\n");
    sb.append ("Title: ");
    sb.append (title);
    sb.append ("\n");
    sb.append ("Album: ");
    sb.append (album);
    sb.append ("\n");
    sb.append ("Artist: ");
    sb.append (artist);
    sb.append ("\n");
    sb.append ("Position: ");
    sb.append ("" + position);
    sb.append ("\n");
    sb.append ("Duration: ");
    sb.append ("" + duration);
    sb.append ("\n");
    return new String (sb);
    }


}

