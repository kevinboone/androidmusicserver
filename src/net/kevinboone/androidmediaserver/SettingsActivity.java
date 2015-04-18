package net.kevinboone.androidmediaserver;

import android.app.*;
import android.util.Log;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.content.*;
import android.preference.*;

/** This class defines the Android settings page, such as it is. */
public class SettingsActivity extends PreferenceActivity
{
  @Override
  public void onCreate(Bundle savedInstanceState)
    {
    super.onCreate (savedInstanceState);
    addPreferencesFromResource (R.xml.preferences);
    }

}


