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

import android.util.Log;
import android.view.KeyEvent;

import org.json.JSONException;
import org.json.JSONObject;

import javax.jmdns.ServiceInfo;

class Freebox
{
  public interface Listener
  {
    void onFreeboxStatus (String status);
  }

  private long     mRcu;
  private String   mAddress;
  private int      mPort;
  private boolean  mHasFocus;
  private boolean  mReachable;
  private String   mColor;
  private Thread   mStatusLooper;
  private Listener mListener;


  // ---------------------------------------------------
  Freebox (JSONObject json)
  {
    try
    {
      mPort     = json.getInt     ("port");
      mAddress  = json.getString  ("address");
      mHasFocus = json.getBoolean ("focus");
      mColor    = json.getString  ("color");
    }
    catch (JSONException e)
    {
      e.printStackTrace ();
    }
  }

  // ---------------------------------------------------
  Freebox (ServiceInfo serviceInfo)
  {
    mReachable = true;

    mAddress = serviceInfo.getHostAddress ();
    mAddress = mAddress.replaceAll ("[\\[\\]]", "");
    mPort    = serviceInfo.getPort ();
    Log.d (Freeteuse.TAG, mAddress);
  }

  // ---------------------------------------------------
  Freebox (String address,
           int    port)
  {
    mReachable = true;

    mAddress = address;
    mPort    = port;
  }

  // ---------------------------------------------------
  boolean hasFocus ()
  {
    return mHasFocus;
  }

  // ---------------------------------------------------
  boolean isConsistent ()
  {
    return mAddress != null;
  }

  // ---------------------------------------------------
  boolean isReachable ()
  {
    return mReachable;
  }

  // ---------------------------------------------------
  void detected ()
  {
    mReachable = true;
  }

  // ---------------------------------------------------
  String getColor ()
  {
    return mColor;
  }

  // ---------------------------------------------------
  void setColor (String color)
  {
    mColor = color;
  }

  // ---------------------------------------------------
  boolean equals (Freebox freebox)
  {
    //noinspection SimplifiableIfStatement
    if (freebox == null)
    {
      return false;
    }

    return ((mPort == freebox.mPort) && mAddress.equals (freebox.mAddress));
  }

  // ---------------------------------------------------
  JSONObject getJson ()
  {
    JSONObject json = new JSONObject ();

    try
    {
      json.put ("port",    mPort);
      json.put ("address", mAddress);
      json.put ("focus",   mHasFocus);
      json.put ("color",   mColor);
    }
    catch (JSONException e)
    {
      e.printStackTrace ();
      return null;
    }

    return json;
  }

  // ---------------------------------------------------
  void connect (Listener listener)
  {
    mListener = listener;

    if ((mAddress != null) && (mPort != 0))
    {
      mRcu = jniCreateRcu ();

      jniConnectRcu (mRcu,
                     mAddress,
                     mPort);

      mStatusLooper = new Thread (new Runnable ()
      {
        @Override
        public void run ()
        {
          while (true)
          {
            String status = jniReadRcuStatus (mRcu);

            Log.d (Freeteuse.TAG, ">>> " + status + " <<<");
            if (status.equals ("EXIT"))
            {
              break;
            }
          }
        }
      });
      mStatusLooper.start ();
    }
  }

  // ---------------------------------------------------
  void disconnect ()
  {
    jniDisconnectRcu (mRcu);

    try
    {
      mStatusLooper.join ();
    }
    catch (InterruptedException e)
    {
      e.printStackTrace ();
    }

    mListener = null;
  }

  // ---------------------------------------------------
  void onVolumePressed (int keyCode)
  {
    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
    {
      pressRcuKey ("0x01",
                   "0xe9",
                   false);
    }
    else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
    {
      pressRcuKey ("0x01",
                   "0xea",
                   false);
    }
  }

  // ---------------------------------------------------
  void onVolumeReleased (int keyCode)
  {
    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
    {
      releaseRcuKey ("0x01",
                     "0xe9");
    }
    else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
    {
      releaseRcuKey ("0x01",
                     "0xea");
    }
  }

  // ---------------------------------------------------
  void pressRcuKey (String  report_id,
                    String  key_code,
                    boolean with_release)
  {
    try
    {
      jniPressRcuKey (mRcu,
                      Integer.decode(report_id),
                      Integer.decode(key_code),
                      with_release);
    }
    catch (NumberFormatException e)
    {
      Log.e (Freeteuse.TAG, String.valueOf(e));
    }
  }

  // ---------------------------------------------------
  void releaseRcuKey (String  report_id,
                      String  key_code)
  {
    jniReleaseRcuKey (mRcu,
                      Integer.decode(report_id),
                      Integer.decode(key_code));
  }

  // ---------------------------------------------------
  void grabFocus ()
  {
    mHasFocus = true;
  }

  // ---------------------------------------------------
  void releaseFocus ()
  {
    mHasFocus = false;
  }

  // ---------------------------------------------------
  private native long   jniCreateRcu     ();
  private native void   jniConnectRcu    (long rcu, String address, int port);
  private native void   jniDisconnectRcu (long rcu);
  private native void   jniPressRcuKey   (long rcu, int report_id, int key_code, boolean with_key_release);
  private native void   jniReleaseRcuKey (long rcu, int report_id, int key_code);
  private native String jniReadRcuStatus (long rcu);
}
