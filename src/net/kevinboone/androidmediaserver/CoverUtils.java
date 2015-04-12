/*
 *  Kevin's Music Server for Android
 *  Copyright (c)2015
 *  Distributed under the terms of the GNU Public Licence, version 2.0
 */

package net.kevinboone.androidmediaserver;
import java.util.*;
import java.text.*;
import java.io.*;
import java.net.*;

public class CoverUtils
  {
  public static String getCoverFileForTrackFile (String filePath)
    {
    if (filePath.startsWith ("file://"))
      filePath = filePath.substring (7);

    int p = filePath.lastIndexOf ('/');
    if (p <= 0) return null;

    filePath = filePath.substring (0, p);

    String cand = makeFile (filePath, "folder.jpg");
    if (doesFileExist (cand)) return cand;
    cand = makeFile (filePath, "folder.png");
    if (doesFileExist (cand)) return cand;
    cand = makeFile (filePath, "cover.jpg");
    if (doesFileExist (cand)) return cand;
    cand = makeFile (filePath, "cover.png");
    if (doesFileExist (cand)) return cand;
    cand = makeFile (filePath, ".folder.png");
    if (doesFileExist (cand)) return cand;
    cand = makeFile (filePath, ".folder.jpg");
    if (doesFileExist (cand)) return cand;

    return null;
    }

  private static String makeFile (String dir, String name)
    {
    return dir + "/" + name;
    }

  private static boolean doesFileExist (String filePath)
    {
    File f = new File (filePath);
    if (f.isFile()) return true;
    return false;
    }

  }


