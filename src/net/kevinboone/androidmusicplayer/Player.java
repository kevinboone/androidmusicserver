/*
 *  Kevin's Music Server for Android
 *  Copyright (c)2015
 *  Distributed under the terms of the GNU Public Licence, version 2.0
 */

package net.kevinboone.androidmusicplayer;
import java.util.*;
import java.text.*;
import java.io.*;
import java.net.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.media.MediaPlayer;
import android.util.Log;
import android.media.*;
import android.media.audiofx.*;

public class Player implements
   MediaPlayer.OnCompletionListener,
   AudioManager.OnAudioFocusChangeListener
{
private MediaPlayer mediaPlayer = new MediaPlayer();
private String currentPlaybackUri = null; //File 
private TrackInfo currentPlaybackTrackInfo = null;
protected List<TrackInfo> playlist = new Vector<TrackInfo>();
protected int currentPlaylistIndex = -1;
private Equalizer eq = null;
private BassBoost bb = null; 
public static final int MAX_EQ_BANDS = 10;
protected AudioDatabase audioDatabase = null;
private Context context;

/** Constructor. */
  public Player (Context context)
    {
    this.context = context;
    mediaPlayer.setOnCompletionListener (this);
    audioDatabase = new AudioDatabase();
    RemoteControlReceiver.setPlayer (this);
    audioDatabase.scan (context);
    try
      {
      // I'm told that these constructors can fail. If they do,
      //  leave them as null. Other things will fail later, but
      //  at least the service will start up, and some things
      //  might still work
      eq = new Equalizer (0, mediaPlayer.getAudioSessionId());
      bb = new BassBoost (0, mediaPlayer.getAudioSessionId());
      }
    catch (Throwable e)
      {
      Log.w ("AMS", "Can't initialize effects: " + e.toString());
      }
    }

  public void stop()
    {
    mediaPlayer.reset();
    releaseAudioFocus();
    currentPlaybackUri = null;
    currentPlaybackTrackInfo = null; 
    }

  @Override
  public void onAudioFocusChange (int focusChange)
    {
    }


  public void getAudioFocus()
    {
    AudioManager am = (AudioManager) context.getSystemService
      (Context.AUDIO_SERVICE);
    am.requestAudioFocus(this, AudioManager.STREAM_MUSIC, 
      AudioManager.AUDIOFOCUS_GAIN);
    am.registerMediaButtonEventReceiver (new ComponentName 
      (context.getPackageName(), RemoteControlReceiver.class.getName()));
    }


  public void releaseAudioFocus()
    {
    AudioManager am = (AudioManager) context.getSystemService
      (Context.AUDIO_SERVICE);
    am.abandonAudioFocus (this);
    am.unregisterMediaButtonEventReceiver (new ComponentName 
      (context.getPackageName(), RemoteControlReceiver.class.getName()));
    }


  protected void playNextInPlaylist ()
      throws PlayerException
    {
    if (playlist.size() > 0 && currentPlaylistIndex < playlist.size() - 1)
      {
      movePlaylistIndexForward();
      playCurrentPlaylistItem();
      }
    }


  protected void playPrevInPlaylist ()
      throws PlayerException
    {
    if (playlist.size() > 0 && currentPlaylistIndex > 0) 
      {
      movePlaylistIndexBack();
      playCurrentPlaylistItem();
      }
    }


  /** Invoked by the Android player when playback of an item is complete.
      Try to advance to the next playlist item. */
  @Override
  public void onCompletion (MediaPlayer mp)
    {
    currentPlaybackUri = null; // set current URI so it' s clear we
                               // aren't just paused
    currentPlaybackTrackInfo = null; 
    releaseAudioFocus();
    Log.w ("AMP", "Playback completed: " + currentPlaybackUri);
    if (playlist.size() > 0 && currentPlaylistIndex < playlist.size() - 1)
      {
      try
        {
        playNextInPlaylist();
        }
      catch (Exception e)
        {
        Log.w ("AMS", e.toString());
        }
      }
    }


  public void setEqEnabled (boolean enabled)
    {
    eq.setEnabled (enabled); //TOD null check
    }


  public boolean getEqEnabled()
    {
    return eq.getEnabled(); //TOD null check
    }


  public void setEqBandLevel (int band, int level)
    {
    eq.setBandLevel ((short)band, (short)level);
    }


  public void setBBEnabled (boolean enabled)
    {
    bb.setEnabled (enabled);
    }


  public boolean getBBEnabled ()
    {
    return bb.getEnabled ();
    }


  public void setBBStrength (int strength)
    {
    bb.setStrength ((short)strength);
    }

  /** Bass boost strength, 0-1000. */
  public int getBBStrength ()
    {
    int bbStrength = bb.getRoundedStrength();
    return bbStrength;
    }

 
  public String getEqBandFreqRange (int band)
    {
    int[] fRange = eq.getBandFreqRange ((short)band);
    String sFreqRange = AndroidEqUtil.formatBandLabel (fRange); 
    return sFreqRange;
    }


  public int getEqBandLevel (int band)
    {
    int level = (int)eq.getBandLevel ((short)band); 
    return level;
    }


  public int getEqMinLevel()
    {
    short r[] = eq.getBandLevelRange();
    int minLevel = r[0];
    return minLevel;
    }


  public int getEqMaxLevel()
    {
    short r[] = eq.getBandLevelRange();
    int maxLevel = r[1];
    return maxLevel;
    }


  public int getEqNumberOfBands()
    {
    int numBands = eq.getNumberOfBands ();
    return numBands;
    }


  public void pause ()
    {
    mediaPlayer.pause();
    }


  public void clearPlaylist ()
    {
    mediaPlayer.reset();
    releaseAudioFocus();
    playlist = new Vector<TrackInfo>();
    currentPlaybackUri = null;
    currentPlaybackTrackInfo = null;
    }


  public void shufflePlaylist()
    {
    Collections.shuffle (playlist); 
    }


  public void movePlaylistIndexBack()
      throws PlayerException
    {
    if (playlist.size() == 0)
      throw new PlaylistEmptyException();

    if (currentPlaylistIndex <= 0) 
      throw new AlreadyAtStartOfPlaylistException();
     
    currentPlaylistIndex--;
    }


  public void movePlaylistIndexForward()
      throws PlayerException
    {
    if (playlist.size() == 0)
      throw new PlaylistEmptyException();

    if (currentPlaylistIndex >= playlist.size() - 1) 
      throw new AlreadyAtEndOfPlaylistException();
     
    currentPlaylistIndex++;
    }


  public void playCurrentPlaylistItem ()
      throws PlayerException
    {
    if (playlist.size() == 0)
      throw new PlaylistEmptyException();
    if (currentPlaylistIndex < 0)
      currentPlaylistIndex = 0;
    playInPlaylist (currentPlaylistIndex);
    }


  public void playInPlaylist (int index)
      throws PlayerException
    {
    if (playlist.size() == 0)
      throw new PlaylistEmptyException();

    if (index < 0 || index >= playlist.size())
      throw new PlaylistIndexOutOfRangeException();

    String uri = playlist.get (index).uri;
    currentPlaylistIndex = index;
    playFileNow (uri);
    }


  public int playAlbumNow (String album)
      throws PlayerException
    {
    List<String> albumURIs = audioDatabase.getAlbumURIs (context, album);
    int count = 0;
    clearPlaylist();
    for (String uri : albumURIs)
      {
      TrackInfo ti = audioDatabase.getTrackInfo (context, uri);
      playlist.add (ti);
      count++;
      }
 
    playFromStartOfPlaylist();
    return count;
    }


  /* TODO: handle non-existent album */
  public int addAlbumToPlaylist (String album)
      throws PlayerException
    {
    List<String> albumURIs = audioDatabase.getAlbumURIs (context, album);
    int count = 0;
    for (String uri : albumURIs)
      {
      TrackInfo ti = audioDatabase.getTrackInfo (context, uri);
      playlist.add (ti);
      count++;
      }
    return count;
    } 


  public void play ()
      throws PlayerException
    {
    if (currentPlaybackUri != null)
      {
      // We are paused (or even plaing), not stopped
      getAudioFocus();
      mediaPlayer.start();
      }
    else
      playFromStartOfPlaylist ();
    }

  
  public int getCurrentPlaybackPositionMsec()
    {
    int position = mediaPlayer.getCurrentPosition();
    return position;
    }


  public int getCurrentPlaybackDurationMsec()
    {
    int duration = mediaPlayer.getDuration();
    return duration;
    }


  /** May be a file or a content: URI. Will be null if nothing is playing. */
  public String getCurrentPlaybackUri ()
    {
    return currentPlaybackUri;
    }

  /** Note that "paused" is not playing. */
  public boolean isPlaying ()
    {
    return mediaPlayer.isPlaying();
    }


  /** May return null if nothing is playing, or a value with meaningless
      contents -- check playback status as well. */
  public TrackInfo getCurrentPlaybackTrackInfo ()
    {
    return currentPlaybackTrackInfo;
    }

  public List<TrackInfo> getPlaylist()
    {
    return playlist;
    }

  /**
  Adds the specified filesystem item, which might be a directory, 
  to the playlist, and return the number of items added.
  */
  public int addFileOrDirectoryToPlaylist (String path)
      throws PlayerException 
    {
    File f = new File (path);
    if (f.isDirectory ())
      {
      String[] list = f.list();      
      int count = 0;
      for (String name : list)
        {
        String cand = path + "/" + name;
        File f2 = new File (cand);
        if (isPlayableFile (f2.toString()))
          {
          TrackInfo ti = audioDatabase.getTrackInfo (context, cand);
          playlist.add (ti);
          count++;
          }
        }
      return count;
      }
    else
      {
      TrackInfo ti = audioDatabase.getTrackInfo (context, path);
      playlist.add (ti);
      return 1; 
      }
    }


  public void playFromStartOfPlaylist ()
      throws PlayerException
    {
    playInPlaylist (0);
    }

  /**
  Start playback of the file, and set the currentUri.
  */
  public void playFileNow (String uri)
      throws PlayerException 
    {
    mediaPlayer.reset();
    try
      {
      if (uri.startsWith ("content:"))
        {
        android.net.Uri contentUri = android.net.Uri.parse (uri);
        mediaPlayer.setDataSource (context, contentUri);
        }
      else
        {
        String filename = uri;
        mediaPlayer.setDataSource (filename);
        }
      mediaPlayer.prepare();
      getAudioFocus();
      currentPlaybackUri = uri;
      currentPlaybackTrackInfo = audioDatabase.getTrackInfo (context, uri);
      mediaPlayer.start();
      }
    catch (IOException e)
      {
      mediaPlayer.reset();
      currentPlaybackUri = null;
      throw new PlayerIOException (e.toString());
      } 
    }


  public void scanAudioDatabase ()
    {
    audioDatabase.scan (context);
    }

  public Set<String> getAlbums()
    {
    return audioDatabase.getAlbums();
    }


  public Set<String> getArtists()
    {
    return audioDatabase.getArtists();
    }


  public Set<String> getGenres()
    {
    return audioDatabase.getGenres();
    }


  public Set<String> getComposers()
    {
    return audioDatabase.getComposers();
    }


   public byte[] getEmbeddedPictureForTrackUri (String uri)
    {
    return audioDatabase.getEmbeddedPicture (context, uri);
    }


   public String getFilePathFromContentUri (android.net.Uri uri)
    {
    return audioDatabase.getFilePathFromContentUri (context, uri);
    }


   public List<String> getAlbumTrackUris (String album)
    {
    return audioDatabase.getAlbumURIs (context, album);
    }

   
  public Set<String> getAlbumsByArtist (String artist)
    {
    return audioDatabase.getAlbumsByArtist (context, artist);
    }


  public Set<String> getAlbumsByComposer (String artist)
    {
    return audioDatabase.getAlbumsByComposer (context, artist);
    }


  public Set<String> getAlbumsByGenre (String genre)
    {
    return audioDatabase.getAlbumsByGenre (context, genre);
    }

 
  public TrackInfo getTrackInfo (String uri)
    {
    return audioDatabase.getTrackInfo (context, uri);
    }


  /** Returns true if the filename suggests mp3, aac, etc. */
  static public boolean isPlayableFile (String name)
    {
    int p = name.lastIndexOf ('.');
    if (p <= 0) return false;
    String ext = name.substring (p);
    if (".mp3".equalsIgnoreCase (ext)) return true;
    if (".m4a".equalsIgnoreCase (ext)) return true;
    if (".aac".equalsIgnoreCase (ext)) return true;
    if (".ogg".equalsIgnoreCase (ext)) return true;
    if (".wma".equalsIgnoreCase (ext)) return true;
    if (".flac".equalsIgnoreCase (ext)) return true;
    return false;
    }

  public List<String> findTracks (String search, int start, int num)
    {
    return audioDatabase.findTracks (context, search, start, num);
    }

  
  public int getApproxNumTracks ()
    {
    return audioDatabase.getApproxNumTracks();
    }

}



