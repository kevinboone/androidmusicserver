/*
 *  Kevin's Music Server for Android
 *  Copyright (c)2015
 *  Distributed under the terms of the GNU Public Licence, version 2.0
 */
package net.kevinboone.androidmediaserver;

import android.net.wifi.*;
import android.content.Context;
import android.text.format.Formatter;
import java.io.*;

public class AndroidNetworkUtil  
{
/** Try to get the WIFI IP. Returns null if there is not one. I don' t know
    how relaible this is, to be honest. */
public static String getWifiIP (Context c)
  {
  WifiManager wifiMgr = (WifiManager) 
     c.getApplicationContext().getSystemService (Context.WIFI_SERVICE);
  WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
  if (wifiInfo != null)
    {
    int ip = wifiInfo.getIpAddress();
    if (ip == 0) return null;
    return Formatter.formatIpAddress(ip);
    }
  else
    return null;
  }
}

