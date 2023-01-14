/*
 *  Kevin's Music Server for Android
 *  Copyright (c)2015
 *  Distributed under the terms of the GNU Public Licence, version 2.0
 */

package net.kevinboone.androidmusicplayer;

public class AlreadyAtEndOfPlaylistException extends PlayerException 
{
  public AlreadyAtEndOfPlaylistException ()
    {
    super (Errors.ERR_ALREADY_AT_END_OF_PLAYLIST);
    }

}


