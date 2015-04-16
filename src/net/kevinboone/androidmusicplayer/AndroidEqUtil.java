/*
 *  Kevin's Music Server for Android
 *  Copyright (c)2015
 *  Distributed under the terms of the GNU Public Licence, version 2.0
 */

package net.kevinboone.androidmusicplayer;


public class AndroidEqUtil
  {
  /** Formats the frequency range as bizarrely reported by the EQ
 *    API into something readable. */
  public static String formatBandLabel (int[] band)
    {
    return milliHzToString(band[0]) + "-" + milliHzToString(band[1]);
    }


 private static String milliHzToString (int milliHz)
    {
    if (milliHz < 1000) return "";
    if (milliHz < 1000000)
      return "" + (milliHz / 1000) + "Hz";
    else
      return "" + (milliHz / 1000000) + "kHz";
    }
  }


