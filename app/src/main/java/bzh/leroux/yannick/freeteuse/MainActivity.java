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
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import java.util.Hashtable;

public class MainActivity extends    Activity
                          implements Home.Listener,
                                     MultiClicker.Listener,
                                     View.OnClickListener,
                                     View.OnTouchListener, Freebox.Listener
{
  private Freebox                  mActiveFreebox;
  private Hashtable<Integer, View> mKeys;
  private MultiClicker             mMultiClicker;
  private Wifi                     mWifi;
  private View                     mProgressBar;
  private ScreenFitter             mScreenFitter;
  private Home                     mHome;
  private View                     mStatusView;

  // ---------------------------------------------------
  @Override
  protected void onCreate (Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);
    setContentView (R.layout.activity_main);

    mStatusView = findViewById (R.id.status);

    mScreenFitter = new ScreenFitter (findViewById (R.id.key_grid),
                                      getWindowManager().getDefaultDisplay());

    mProgressBar = findViewById (R.id.progressBar);

    {
      mKeys = new Hashtable<> ();

      listenToTouchEvent((ViewGroup) findViewById(R.id.key_grid));
    }

    mHome = new Home (this,
                      this,
                      getPreferences (Context.MODE_PRIVATE));
  }

  // ---------------------------------------------------
  @Override
  protected void onResume ()
  {
    super.onResume ();

    mHome.discloseBoxes ();

    mMultiClicker = new MultiClicker (this);
    mWifi         = new Wifi (this);

    mProgressBar.setVisibility (View.INVISIBLE);
  }

  // ---------------------------------------------------
  @Override
  protected void onPause ()
  {
    mHome.concealBoxes ();
    mMultiClicker.stop ();
    mWifi.stop ();

    disconnectFreebox ();

    super.onPause ();
  }

  // ---------------------------------------------------
  public void onClick (View view)
  {
    if (mMultiClicker.stopped () && (mActiveFreebox != null))
    {
      String[] tags = ((String) view.getTag ()).split (":");

      if (tags[0].equals("onClick"))
      {
        mActiveFreebox.pressRcuKey (tags[1],
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
  public void onPreviousFreebox (View view)
  {
    if (mMultiClicker.stopped ())
    {
      onFreeboxSelected (mHome.getPreviousReachable (mActiveFreebox));
    }
  }


  // ---------------------------------------------------
  public void onNextFreebox (View view)
  {
    if (mMultiClicker.stopped ())
    {
      onFreeboxSelected (mHome.getNextReachable (mActiveFreebox));
    }
  }

  // ---------------------------------------------------
  @SuppressLint("ClickableViewAccessibility")
  @Override
  public boolean onTouch (View        view,
                          MotionEvent motionEvent)
  {
    if (mMultiClicker.stopped () && (mActiveFreebox != null))
    {
      String[] tags = ((String) view.getTag()).split(":");

      if (motionEvent.getAction () == MotionEvent.ACTION_DOWN)
      {
        mActiveFreebox.pressRcuKey (tags[1],
                                    tags[2],
                                   false);
      }
      else if (motionEvent.getAction () == MotionEvent.ACTION_UP)
      {
        mActiveFreebox.releaseRcuKey (tags[1],
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
        if (mActiveFreebox != null)
        {
          if (event.getAction () == KeyEvent.ACTION_DOWN)
          {
            if (event.getRepeatCount () == 0)
            {
              mActiveFreebox.onVolumePressed (event.getKeyCode ());
            }
          }
          else if (event.getAction () == KeyEvent.ACTION_UP)
          {
            mActiveFreebox.onVolumeReleased (event.getKeyCode ());
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

      if ((view != null) && (mActiveFreebox != null))
      {
        String[] tags = ((String) view.getTag ()).split (":");

        if (tags[0].equals ("onClick") || tags[0].equals ("onTouch"))
        {
          mActiveFreebox.pressRcuKey (tags[1],
                                      tags[2],
                                     true);
        }
      }
      mProgressBar.setVisibility (View.VISIBLE);
    }
  }

  // ---------------------------------------------------
  @Override
  public void onFreeboxSelected (Freebox freebox)
  {
    connectFreebox (freebox);

    mHome.paintBoxButtons (mActiveFreebox,
                           (ImageButton) findViewById (R.id.Kprevious_box),
                           (ImageButton) findViewById (R.id.Kfree),
                           (ImageButton) findViewById (R.id.Knext_box));
  }

  // ---------------------------------------------------
  @Override
  public void onFreeboxDetected (Freebox freebox)
  {
    if (mActiveFreebox == null)
    {
      connectFreebox (freebox);
    }

    mHome.paintBoxButtons (mActiveFreebox,
                           (ImageButton) findViewById (R.id.Kprevious_box),
                           (ImageButton) findViewById (R.id.Kfree),
                           (ImageButton) findViewById (R.id.Knext_box));
  }

  // ---------------------------------------------------
  @Override
  public void onFreeboxStatus (String status)
  {
    View view = findViewById (R.id.status);

    Log.e (Freeteuse.TAG, ">>> " + status + " <<<");

    if (status.equals ("connected"))
    {
      view.setVisibility (View.INVISIBLE);
    }
    else
    {
      view.setVisibility (View.VISIBLE);
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
        String tag = (String) child.getTag ();

        if (tag != null)
        {
          String[] tags = tag.split (":");

          switch (tags[0])
          {
            case "onClick":
            case "onMultiClick":
              child.setOnClickListener (this);
              break;

            case "onTouch":
              child.setOnTouchListener (this);
              break;
          }
        }

        mKeys.put (child.getId (), child);
      }
    }
  }

  // ---------------------------------------------------
  private void connectFreebox (Freebox freebox)
  {
    if (freebox != null)
    {
      if (mActiveFreebox != null)
      {
        mActiveFreebox.releaseFocus ();
        disconnectFreebox ();
      }

      mActiveFreebox = freebox;
      mActiveFreebox.grabFocus ();
      mActiveFreebox.connect (this);
    }
  }

  // ---------------------------------------------------
  private void disconnectFreebox ()
  {
    if (mActiveFreebox != null)
    {
      mActiveFreebox.disconnect ();
      mActiveFreebox = null;

      mStatusView.setVisibility (View.VISIBLE);
    }
  }

  // ---------------------------------------------------
  static
  {
    System.loadLibrary ("freebxsdk");
    System.loadLibrary ("freeteuse");
  }
}
