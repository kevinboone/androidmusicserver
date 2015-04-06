/*
 *  Kevin's Music Server for Android
 *  Copyright (c)2015
 *  Distributed under the terms of the GNU Public Licence, version 2.0
 */
package net.kevinboone.androidmediaserver;

/** This class contains error codes and methods to format then as text. */
public class Errors 
{
  public static final int ERR_OK = 0;
  public static final int ERR_PL_EMPTY = 1;
  public static final int ERR_PL_RANGE = 2;

  /** Gets the string corresponding to an error code. */
  public static String perror (int errorCode)
    {
    switch (errorCode)
      {
      case ERR_OK: return "OK";
      case ERR_PL_EMPTY: return "Playlist empty";
      case ERR_PL_RANGE: return "Playlist index out of range";
      }
    return "Unknown error";
    }



}


