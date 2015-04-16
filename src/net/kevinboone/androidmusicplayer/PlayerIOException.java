/*
 *  Kevin's Music Server for Android
 *  Copyright (c)2015
 *  Distributed under the terms of the GNU Public Licence, version 2.0
 */

package net.kevinboone.androidmusicplayer;

public class PlayerIOException extends PlayerException 
{
  public PlayerIOException (String message)
    {
    super (Errors.ERR_IO, message);
    }

}


