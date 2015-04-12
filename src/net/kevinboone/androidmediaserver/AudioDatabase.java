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
  protected TreeSet<String> albums = new TreeSet<String>();
  protected TreeSet<String> artists = new TreeSet<String>();
  protected TreeSet<String> composers = new TreeSet<String>();
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
      }
    else
      Log.w ("AMS", "Media database scan produced no results");
    Log.w ("AMS", "Done media database scan");
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
      URI and title (which is made up) set */
  TrackInfo getTrackInfo (Context context, String uri)
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



