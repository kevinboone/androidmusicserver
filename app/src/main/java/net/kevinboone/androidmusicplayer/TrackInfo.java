/*
 *  Kevin's Music Server for Android
 *  Copyright (c)2015
 *  Distributed under the terms of the GNU Public Licence, version 2.0
 */

package net.kevinboone.androidmusicplayer;

/** A simple data structure for audio track information. */
public class TrackInfo
  {
  public String uri;
  public String title;
  public String artist;
  public String album;
  public String composer;
  public String trackNumber;
  public String genre;

  /** Constructor takes a uri argument to ensure that at least the uri is
      set, even if nothing else is. */
  public TrackInfo (String uri)
    {
    this.uri = uri;
    }

  private TrackInfo ()
    {
    }

  /** If we don't have a proper title for the item, strip the path and
      extension from the URI and use that instead. */
  public static String makeTitleFromUri (String uri)
    {
    int p = uri.lastIndexOf ('/');
    if (p >= 0)
      uri = uri.substring (p + 1);
    p = uri.lastIndexOf ('.');
    if (p > 0)
      uri = uri.substring (0, p);
    return uri;
    }

  }



