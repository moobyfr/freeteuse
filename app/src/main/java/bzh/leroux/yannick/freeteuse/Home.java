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


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jmdns.ServiceInfo;

class Home implements DnsServiceSniffer.Listener

{
  public interface Listener
  {
    void onNewFreebox (Freebox freebox);
  }

  private DnsServiceSniffer mDnsServiceSniffer;
  private Context           mContext;
  private SharedPreferences mPreferences;
  private List<Freebox>     mBoxes;
  private Listener          mListener;

  // ---------------------------------------------------
  Home (Context           context,
        Listener          listener,
        SharedPreferences preferences)
  {
    mContext     = context;
    mListener    = listener;
    mPreferences = preferences;
    mBoxes       = new ArrayList<> ();
  }

  // ---------------------------------------------------
  void startDiscovering ()
  {
    mDnsServiceSniffer = new DnsServiceSniffer (mContext, this);

    mDnsServiceSniffer.execute("_hid._udp");
  }

  // ---------------------------------------------------
  void stopDiscovering ()
  {
    mDnsServiceSniffer.cancel (true);
  }

  // ---------------------------------------------------
  @Override
  public void onDnsService (ServiceInfo serviceInfo)
  {
    if (serviceInfo != null)
    {
      Log.d ("FreeTeuse", Arrays.toString (serviceInfo.getHostAddresses ())
                              + ":" + serviceInfo.getPort ());

      Freebox detected_box = new Freebox (mPreferences, serviceInfo);

      for (Freebox box : mBoxes)
      {
        if (detected_box.Is (box))
        {
          return;
        }
      }

      mBoxes.add (detected_box);
      detected_box.saveAddress ();

      mListener.onNewFreebox (detected_box);
    }
  }
}