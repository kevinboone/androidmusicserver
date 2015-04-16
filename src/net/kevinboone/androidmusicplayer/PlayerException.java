/*
 *  Kevin's Music Server for Android
 *  Copyright (c)2015
 *  Distributed under the terms of the GNU Public Licence, version 2.0
 */

package net.kevinboone.androidmusicplayer;

public class PlayerException extends Exception 
{
  private int code = 0;

  public PlayerException (int code, String message)
    {
    super (message);
    this.code = code;
    }

  public PlayerException (int code)
    {
    super (Errors.perror (code));
    this.code = code;
    }

  public int getCode() { return code; }
}

