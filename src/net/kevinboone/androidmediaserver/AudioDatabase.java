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
import android.media.*;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.provider.MediaStore;

/** This class integrates the music server with the Android built-in
    media scanner. */
public class AudioDatabase
{
  private final String GENRE_ID = MediaStore.Audio.Genres._ID;
  private final String GENRE_NAME = MediaStore.Audio.Genres.NAME;
  private final String AUDIO_ID = MediaStore.Audio.Media._ID;

  protected TreeSet<String> albums = new TreeSet<String>();
  protected TreeSet<String> artists = new TreeSet<String>();
  protected TreeSet<String> composers = new TreeSet<String>();
  protected TreeSet<String> genres = new TreeSet<String>();
  MediaMetadataRetriever mmr = new MediaMetadataRetriever();

  /**
   Scan for albums, etc., in the Android media database. Note that this
   method does not cause Android to rescan its filesystem.
  */
  public void scan(Context context)
    {
    Log.w ("AMS", "Starting media database scan");
    albums = new TreeSet<String>(); // Clear any old entries
    artists = new TreeSet<String>(); // Clear any old entries
    composers = new TreeSet<String>(); // Clear any old entries
    Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    Cursor cur = context.getContentResolver().query(uri, null,
      MediaStore.Audio.Media.IS_MUSIC + " = 1", null, null);
    if (cur.moveToFirst()) 
      {
      int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
      int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
      int composerColumn = cur.getColumnIndex(MediaStore.Audio.Media.COMPOSER);
      int idColumn = cur.getColumnIndex(MediaStore.Audio.Media._ID);

      do 
        {
        long id = cur.getLong (idColumn);
        Uri extUri = ContentUris.withAppendedId 
          (android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        String album = cur.getString (albumColumn);
        if (album != null && album.length() > 0)
         albums.add (album);
        String composer = cur.getString (composerColumn);
        if (composer != null && composer.length() > 0)
         composers.add (composer);
        String artist = cur.getString (artistColumn);
        if (artist != null && artist.length() > 0)
         artists.add (artist);
        } while (cur.moveToNext());
      cur.close();


      cur = context.getContentResolver().query (
         MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
         new String[] { MediaStore.Audio.Genres._ID, 
           MediaStore.Audio.Genres.NAME}, null, null, null);
      for (cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext()) 
        {
        String genreID = cur.getString(0);
        String genreName = cur.getString(1);
        if (genreHasTracks(context, genreID))
          genres.add (genreName);
        }
      cur.close();
      }
    else
      Log.w ("AMS", "Media database scan produced no results");
    Log.w ("AMS", "Done media database scan");
    }


  /** 
    Try to determine whether the specified genre ID is associated with 
    any tracks. This is to prevent including empty genres in the list. 
    Notethat this method takes a genre ID, not a genre name, and so is
    probably not much use except as a helper to the scan() method 
  */
  public boolean genreHasTracks (Context context, String genreID)
    {
    Uri uri = MediaStore.Audio.Genres.Members.getContentUri
      ("external", Long.valueOf(genreID)); 

    String[] projection = new String[]{MediaStore.Audio.Media.TITLE, 
      MediaStore.Audio.Media._ID};

    Cursor cur = context.getContentResolver().query(uri, projection, 
      null, null, null);

    boolean ret;

    if (cur.moveToFirst())
      ret = true;
    else
      ret = false;    

    cur.close();

    return ret;
    }
  

  public Set<String> getAlbumsByArtist (Context context, String artist)
    {
    Set<String> results = new TreeSet<String>();

    Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    Cursor cur = context.getContentResolver().query(uri, null,
      MediaStore.Audio.Media.IS_MUSIC + " = 1", null, null);
    if (cur.moveToFirst()) 
      {
      int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
      int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);

      do 
        {
        String candArtist = cur.getString (artistColumn);
        if (candArtist != null && candArtist.length() > 0 
            && candArtist.equalsIgnoreCase (artist))
          {
          String album = cur.getString (albumColumn);
          if (album != null && album.length() > 0)
            results.add (album);
          }
        } while (cur.moveToNext());
      }
    cur.close();

    return results;
    }

  public Set<String> getAlbumsByGenre (Context context, String genre)
    {
    Set<String> results = new TreeSet<String>();
    for (String album : albums)
      {
      List<String> trackUris = getAlbumURIs (context, album);
      for (String trackUri : trackUris)
        {
        TrackInfo ti = getTrackInfo (context, trackUri, true); 
        if (genre.equals (ti.genre))
          {
          results.add (album);
          break;
          }
        // Because this is so slow, only check the first track of each
        //  album for genre
        break;
        }
      }

    return results;
    }

  public Set<String> getAlbums()
   {
   return albums;
   }

  public Set<String> getArtists()
   {
   return artists;
   }

  public Set<String> getComposers()
   {
   return composers;
   }

  public Set<String> getGenres()
   {
   return genres;
   }


  /** Get a list of content URIs (of the form content:...) for
      the specified album */
  public List<String> getAlbumURIs (Context context, String album)
    {
    Vector<String> list = new Vector<String>();

    String escAlbum = EscapeUtils.escapeSQL (album);
    Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    Cursor cur = context.getContentResolver().query(uri, null,
      MediaStore.Audio.Media.IS_MUSIC + " = 1 and " 
       + MediaStore.Audio.Media.ALBUM + "= '" + escAlbum + "'" , null, 
         MediaStore.Audio.Media.TRACK + "," + MediaStore.Audio.Media.TITLE);
    if (cur.moveToFirst()) 
      {
      int idColumn = cur.getColumnIndex(MediaStore.Audio.Media._ID);

      do 
        {
        Long id = cur.getLong (idColumn);
        Uri extUri = ContentUris.withAppendedId 
         (android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        list.add (extUri.toString());
        } while (cur.moveToNext());
      cur.close();
      }
    return list;
    }

  /** Try to get track info from the uri, which may be a simple filename,
      or a content: uri. If it fails, return a TrackInfo with only the
      URI and title (which is made up) set. THis method does _not_ 
      retrieve genre information, which is very slow. */
  TrackInfo getTrackInfo (Context context, String uri)
    {
    return getTrackInfo (context, uri, false);
    }

  /** Try to get track info from the uri, which may be a simple filename,
      or a content: uri. If it fails, return a TrackInfo with only the
      URI and title (which is made up) set */
  TrackInfo getTrackInfo (Context context, String uri, boolean includeGenre)
    {
    try
      {
      if (uri.startsWith ("content:"))
        {
        android.net.Uri contentUri = android.net.Uri.parse (uri);
        mmr.setDataSource (context, contentUri);
        }
      else
        {
        String filename = WebServer.DOCROOT + "/" + uri;
        mmr.setDataSource (filename);
        }

      TrackInfo ti = new TrackInfo(uri); 
      ti.title = mmr.extractMetadata (mmr.METADATA_KEY_TITLE);
      ti.artist = mmr.extractMetadata (mmr.METADATA_KEY_ARTIST);
      ti.composer  = mmr.extractMetadata (mmr.METADATA_KEY_COMPOSER);
      ti.album = mmr.extractMetadata (mmr.METADATA_KEY_ALBUM);
      ti.trackNumber = mmr.extractMetadata (mmr.METADATA_KEY_CD_TRACK_NUMBER);
      if (includeGenre)
        ti.genre = mmr.extractMetadata (mmr.METADATA_KEY_GENRE);
      else
        ti.genre = "";
      return ti;
      }
    catch (Throwable e)
      {
      Log.w ("AMS", "Error fetching media metadata: " + e.toString());
      TrackInfo ti = new TrackInfo(uri); 
      ti.title = TrackInfo.makeTitleFromUri (uri);
      return ti;
      }
    }

  /** 
     Try to get the embedded picture for an item, if there is one. 
     If not, or in the event of error, return null. For some reason,
     calls on the metadata extractor are not thread safe, so this method
     has to be synchronized :/
  */
  synchronized byte[] getEmbeddedPicture (Context context, String uri)
    {
    try
      {
      if (uri.startsWith ("content:"))
        {
        android.net.Uri contentUri = android.net.Uri.parse (uri);
        mmr.setDataSource (context, contentUri);
        }
      else
        {
        String filename = WebServer.DOCROOT + "/" + uri;
        mmr.setDataSource (filename);
        }

      byte[] ep = mmr.getEmbeddedPicture ();
      return ep;
      }
    catch (Throwable e)
      {
      Log.w ("AMS", "Error fetching embdedded picture: " + e.toString());
      return null;
      }
    }


public String getFilePathFromContentUri (Context context, Uri uri)
    {
    String filePath;
    String[] filePathColumn = {android.provider.MediaStore.MediaColumns.DATA};

    Cursor cursor = context.getContentResolver().query (uri, filePathColumn, 
      null, null, null);
    cursor.moveToFirst();

    int columnIndex = cursor.getColumnIndex (filePathColumn[0]);
    filePath = cursor.getString(columnIndex);
    cursor.close();
    return filePath;
    }

}



