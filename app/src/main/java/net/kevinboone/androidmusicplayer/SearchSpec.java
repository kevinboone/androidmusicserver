/*
 *  Kevin's Music Server for Android
 *  Copyright (c)2015
 *  Distributed under the terms of the GNU Public Licence, version 2.0
 */

package net.kevinboone.androidmusicplayer;

public class SearchSpec 
{
  private String text;

  public SearchSpec (String text)
    {
    this.text = text;
    }

  public String getText() { return text; }

}

