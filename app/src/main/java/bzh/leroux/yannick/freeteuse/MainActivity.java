// Copyright (C) Yannick Le Roux.
// This file is part of Freeteuse.
//
//   Freeteuse is free software: you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation, either version 3 of the License, or
//   (at your option) any later version.
//
//   Freeteuse is distributed in the hope that it will be useful,
//   but WITHOUT ANY WARRANTY; without even the implied warranty of
//   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//   GNU General Public License for more details.
//
//   You should have received a copy of the GNU General Public License
//   along with Freeteuse.  If not, see <http://www.gnu.org/licenses/>.

package bzh.leroux.yannick.freeteuse;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import java.util.Arrays;
import java.util.Hashtable;

import javax.jmdns.ServiceInfo;

public class MainActivity extends    Activity
                          implements DnsServiceSniffer.Listener,
                                     MultiClicker.Listener,
                                     View.OnClickListener,
                                     View.OnTouchListener
{
  private DnsServiceSniffer mDnsServiceSniffer;
  private Freebox           mFreebox;
  Hashtable<Integer, View>  mKeys;
  MultiClicker              mMultiClicker;
  Wifi                      mWifi;
  View                      mProgressBar;
  ScreenFitter              mScreenFitter;

  // ---------------------------------------------------
  @Override
  protected void onCreate (Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);
    setContentView (R.layout.activity_main);

    if (Build.VERSION.SDK_INT >= 11)
    {
      ActionBar actionBar = getActionBar ();

      if (actionBar != null)
      {
        actionBar.hide ();
      }
    }

    mScreenFitter = new ScreenFitter (findViewById (R.id.key_grid),
                                      getWindowManager().getDefaultDisplay());

    mProgressBar = findViewById (R.id.progressBar);

    {
      mKeys = new Hashtable<> ();

      listenToTouchEvent((ViewGroup) findViewById(R.id.key_grid));
    }
  }

  // ---------------------------------------------------
  @Override
  protected void onResume ()
  {
    super.onResume ();

    connectFreebox (new Freebox (getPreferences(Context.MODE_PRIVATE)));

    {
      mDnsServiceSniffer = new DnsServiceSniffer (this, this);

      mDnsServiceSniffer.execute ("_hid._udp");
    }

    mMultiClicker = new MultiClicker (this);
    mWifi         = new Wifi (this);

    mProgressBar.setVisibility (View.INVISIBLE);
  }

  // ---------------------------------------------------
  @Override
  protected void onPause ()
  {
    mDnsServiceSniffer.cancel (true);
    mMultiClicker.stop ();
    mWifi.stop ();
    disconnectFreebox ();

    super.onPause ();
  }

  // ---------------------------------------------------
  @Override
  public void onClick (View view)
  {
    if (mMultiClicker.stopped ())
    {
      String[] tags = ((String) view.getTag ()).split (":");

      if (tags[0].equals("onClick"))
      {
        mFreebox.pressRcuKey (tags[1],
                              tags[2],
                              true);
      }
      else if (tags[0].equals("onMultiClick"))
      {
        mMultiClicker.start (tags[1]);
      }
    }
  }

  // ---------------------------------------------------
  @SuppressLint("ClickableViewAccessibility")
  @Override
  public boolean onTouch (View        view,
                          MotionEvent motionEvent)
  {
    if (mMultiClicker.stopped ())
    {
      String[] tags = ((String) view.getTag()).split(":");

      if (motionEvent.getAction () == MotionEvent.ACTION_DOWN)
      {
        mFreebox.pressRcuKey (tags[1],
                              tags[2],
                              false);
      }
      else if (motionEvent.getAction () == MotionEvent.ACTION_UP)
      {
        mFreebox.releaseRcuKey (tags[1],
                                tags[2]);
      }
    }

    return false;
  }

  // --------------------------------------------------------
  @Override
  public boolean dispatchKeyEvent (KeyEvent event)
  {
    switch (event.getKeyCode ())
    {
      case KeyEvent.KEYCODE_VOLUME_UP:
      case KeyEvent.KEYCODE_VOLUME_DOWN:
        if (mFreebox != null)
        {
          if (event.getAction () == KeyEvent.ACTION_DOWN)
          {
            if (event.getRepeatCount () == 0)
            {
              mFreebox.onVolumePressed (event.getKeyCode ());
            }
          }
          else if (event.getAction () == KeyEvent.ACTION_UP)
          {
            mFreebox.onVolumeReleased (event.getKeyCode ());
          }
        }
        return true;
    }

    return super.dispatchKeyEvent (event);
  }

  // ---------------------------------------------------
  @Override
  public void onMultiClickStep (String key_name)
  {
    if (key_name == null)
    {
      mProgressBar.setVisibility (View.INVISIBLE);
    }
    else
    {
      Resources resources    = getResources ();
      String    package_name = getPackageName ();
      int       id           = resources.getIdentifier (key_name, "id", package_name);
      View      view         = findViewById (id);

      if (view != null)
      {
        String[] tags = ((String) view.getTag ()).split (":");

        if (tags[0].equals ("onClick") || tags[0].equals ("onTouch"))
        {
          mFreebox.pressRcuKey (tags[1],
                                tags[2],
                                true);
        }
      }
      mProgressBar.setVisibility (View.VISIBLE);
    }
  }

  // ---------------------------------------------------
  @Override
  public void onDnsService (ServiceInfo serviceInfo)
  {
    if (serviceInfo != null)
    {
      Log.d ("FreeTeuse", Arrays.toString (serviceInfo.getHostAddresses ())
                               + ":" + serviceInfo.getPort ());

      Freebox detected_box = new Freebox (getPreferences (Context.MODE_PRIVATE),
                                          serviceInfo);

      if (!detected_box.Is (mFreebox))
      {
        connectFreebox(detected_box);
        detected_box.saveAddress();
      }
    }
  }

  // ---------------------------------------------------
  private void listenToTouchEvent (ViewGroup parent)
  {
    for (int i = 0; i < parent.getChildCount (); i++)
    {
      final View child = parent.getChildAt (i);

      if (child instanceof ViewGroup)
      {
        listenToTouchEvent ((ViewGroup) child);
      }
      else if (child instanceof ImageButton)
      {
        String[] tags = ((String) child.getTag()).split(":");

        switch (tags[0])
        {
          case "onClick":
          case "onMultiClick":
            child.setOnClickListener(this);
            break;

          case "onTouch":
            child.setOnTouchListener(this);
            break;
        }

        mKeys.put (child.getId (), child);
      }
    }
  }

  // ---------------------------------------------------
  private void connectFreebox (Freebox freebox)
  {
    disconnectFreebox ();

    mFreebox = freebox;
    mFreebox.connect();
  }

  // ---------------------------------------------------
  private void disconnectFreebox ()
  {
    if (mFreebox != null)
    {
      mFreebox.disconnect ();
      mFreebox = null;
    }
  }

  // ---------------------------------------------------
  static
  {
    System.loadLibrary ("freebxsdk");
    System.loadLibrary ("freeteuse");
  }
}
