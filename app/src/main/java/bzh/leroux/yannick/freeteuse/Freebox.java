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

import android.content.SharedPreferences;
import android.util.Log;
import android.view.KeyEvent;

import javax.jmdns.ServiceInfo;

class Freebox
{
  private long              mRcu;
  private SharedPreferences mSharedPreferences;
  private String            mAddress;
  private int               mPort;

  // ---------------------------------------------------
  Freebox (SharedPreferences sharedPreferences)
  {
    mSharedPreferences = sharedPreferences;

    mPort    = mSharedPreferences.getInt    ("port", 0);
    mAddress = mSharedPreferences.getString ("address", null);
  }

  // ---------------------------------------------------
  Freebox (SharedPreferences sharedPreferences,
           ServiceInfo       serviceInfo)
  {
    mSharedPreferences = sharedPreferences;

    mAddress = serviceInfo.getHostAddress ();
    mAddress = mAddress.replaceAll ("[\\[\\]]", "");
    mPort    = serviceInfo.getPort ();
    Log.d ("FreeTeuse", mAddress);
  }

  // ---------------------------------------------------
  boolean Is (Freebox freebox)
  {
    //noinspection SimplifiableIfStatement
    if (freebox == null)
    {
      return false;
    }

    return ((mPort == freebox.mPort) && mAddress.equals (freebox.mAddress));
  }

  // ---------------------------------------------------
  void saveAddress ()
  {
    SharedPreferences.Editor editor = mSharedPreferences.edit ();

    editor.putInt    ("port",    mPort);
    editor.putString ("address", mAddress);
    editor.commit ();

    Log.i ("FreeTeuse", "<<Address saved>>");
  }

  // ---------------------------------------------------
  void connect ()
  {
    if ((mAddress != null) && (mPort != 0))
    {
      mRcu = jniCreateRcu ();

      jniConnectRcu (mRcu,
                     mAddress,
                     mPort);
    }
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
      Log.e ("FreeTeuse", String.valueOf(e));
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
  void disconnect ()
  {
    jniDisconnectRcu (mRcu);
  }

  // ---------------------------------------------------
  private native long jniCreateRcu     ();
  private native void jniConnectRcu    (long rcu, String address, int port);
  private native void jniDisconnectRcu (long rcu);
  private native void jniPressRcuKey   (long rcu, int report_id, int key_code, boolean with_key_release);
  private native void jniReleaseRcuKey (long rcu, int report_id, int key_code);
}
