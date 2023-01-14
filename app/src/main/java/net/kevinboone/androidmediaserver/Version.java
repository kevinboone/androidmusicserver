/*
 *  Kevin's Music Server for Android
 *  Copyright (c)2015
 *  Distributed under the terms of the GNU Public Licence, version 2.0
 */

package net.kevinboone.androidmediaserver;
import java.text.*;
import java.util.*;
import java.util.zip.*;
import android.content.*;
import android.content.pm.*;

public class Version
  {
  private static final String versionString = "0.0.5";

  public static String getVersionString () { return versionString; }

  public static String getBuildDateString (Context context)
    {
    long date = getBuildDate (context);
    return new SimpleDateFormat ("yyyy/MM/dd hh:mm", Locale.getDefault())
                    .format(date);
    } 

  public static long getBuildDate (Context context)
    {
    try 
      {
      ApplicationInfo ai = context.getPackageManager()
                        .getApplicationInfo(context.getPackageName(), 0);
      ZipFile zf = new ZipFile(ai.sourceDir);
      ZipEntry ze = zf.getEntry("classes.dex");
      long time = ze.getTime();
      zf.close();
      return time;
      } 
    catch (Exception e) 
      {
      return 0;
      }
    } 
  }

