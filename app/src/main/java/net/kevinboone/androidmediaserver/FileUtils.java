/*
 *  Kevin's Music Server for Android
 *  Copyright (c)2015
 *  Distributed under the terms of the GNU Public Licence, version 2.0
 */

package net.kevinboone.androidmediaserver;
import java.util.Locale;

public class FileUtils 
  {
  public static String getMimeType (String filename)
    {
    String lc = filename.toLowerCase(Locale.getDefault());
    if (lc.endsWith (".jpg"))
      return "image/jpeg";
    if (lc.endsWith (".png"))
      return "image/png";
    if (lc.endsWith (".gif"))
      return "image/gif";
    return "application/octet-stream";
    }
  }


