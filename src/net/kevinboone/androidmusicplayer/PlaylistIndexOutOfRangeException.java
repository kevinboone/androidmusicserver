/*
 *  Kevin's Music Server for Android
 *  Copyright (c)2015
 *  Distributed under the terms of the GNU Public Licence, version 2.0
 */

package net.kevinboone.androidmusicplayer;

public class PlaylistIndexOutOfRangeException extends PlayerException 
{
  public PlaylistIndexOutOfRangeException ()
    {
    super (Errors.ERR_PL_RANGE);
    }

}



