/*
 *  Kevin's Music Server for Android
 *  Copyright (c)2015
 *  Distributed under the terms of the GNU Public Licence, version 2.0
 */

package net.kevinboone.textutils;

public class EscapeUtils
  {
  /** For data sent back to the browser, which will have strings enclosed
      in single quotes, convert ' to \'. */ 
  public static String escapeJSON (String s)
    {
    StringBuffer sb = new StringBuffer();
    int l = s.length();
    for (int i = 0; i < l; i++)
      {
      char c = s.charAt(i);
      if (c == '\'')
        sb.append ("\\'");
      else
        sb.append (c);
      }
    return new String (sb);
    }

  /** Escape single-quotes in SQL data values. */
  public static String escapeSQL (String s)
    {
    StringBuffer sb = new StringBuffer();
    int l = s.length();
    for (int i = 0; i < l; i++)
      {
      char c = s.charAt(i);
      if (c == '\'')
        sb.append ("''");
      else
        sb.append (c);
      }
    return new String (sb);
    }

  } 


