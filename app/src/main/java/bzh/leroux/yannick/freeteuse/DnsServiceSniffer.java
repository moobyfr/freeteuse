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
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.Arrays;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

class DnsServiceSniffer extends    FreeboxSniffer
                        implements ServiceListener
{
  private Thread                    mThread;
  private WifiManager.MulticastLock mMulticastLock;
  private Handler                   mListenerHandler;
  private Context                   mContext;


  // ---------------------------------------------------
  DnsServiceSniffer (Context  context,
                     Listener listener)
  {
    super (listener);

    mContext = context;

    {
      Context     appContext = context.getApplicationContext ();
      WifiManager wifiMgr    = (WifiManager) appContext.getSystemService (Context.WIFI_SERVICE);

      if (wifiMgr != null)
      {
        mMulticastLock = wifiMgr.createMulticastLock (context.getPackageName ());
      }
    }

    mListenerHandler = new Handler ();

    mThread = new Thread (new Runnable ()
    {
      @Override
      public void run ()
      {
        if (mMulticastLock != null)
        {
          mMulticastLock.setReferenceCounted (true);
          mMulticastLock.acquire ();
        }

        try
        {
          JmDNS jmdns = JmDNS.create ();

          jmdns.addServiceListener ("_hid._udp.local.",
                                    DnsServiceSniffer.this);
        }
        catch (IOException e)
        {
          e.printStackTrace ();
        }

        try
        {
          Thread.sleep (Long.MAX_VALUE);
        }
        catch (InterruptedException ignore)
        {
        }

        if (mMulticastLock != null)
        {
          mMulticastLock.release ();
        }
      }
    });
  }

  // ---------------------------------------------------
  void start ()
  {
    mThread.start ();
  }

  // ---------------------------------------------------
  void stop ()
  {
    mThread.interrupt ();
  }

  // ---------------------------------------------------
  @Override
  public void serviceAdded (ServiceEvent event)
  {
    ServiceInfo info = event.getInfo ();

    Log.d (Freeteuse.TAG, "DnsServiceSniffer::serviceAdded: " + info.getName ());
  }

  // ---------------------------------------------------
  @Override
  public void serviceRemoved (ServiceEvent event)
  {
    Log.d (Freeteuse.TAG, "DnsServiceSniffer::serviceRemoved: " + event.getInfo ());
  }

  // ---------------------------------------------------
  @Override
  public void serviceResolved (ServiceEvent event)
  {
    final ServiceInfo serviceInfo = event.getInfo ();

    mListenerHandler.post (new Runnable ()
    {
      @Override
      public void run ()
      {
        final Freebox freebox = new Freebox (mContext,
                                             serviceInfo);

        onFreeboxDetected (freebox);
      }
    });

    Log.d (Freeteuse.TAG, Arrays.toString (serviceInfo.getHostAddresses ())
            + ":" + serviceInfo.getPort ());
  }
}
