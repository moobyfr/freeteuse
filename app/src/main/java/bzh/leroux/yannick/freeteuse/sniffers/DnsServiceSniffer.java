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

package bzh.leroux.yannick.freeteuse.sniffers;

import android.content.Context;
import android.net.wifi.WifiManager;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import bzh.leroux.yannick.freeteuse.Freebox;

public class DnsServiceSniffer extends    FreeboxSniffer
                               implements ServiceListener
{
  private Thread                    mThread;
  private WifiManager.MulticastLock mMulticastLock;

  // ---------------------------------------------------
  public DnsServiceSniffer (Context  context,
                            Listener listener)
  {
    super ("DnsServiceSniffer",
            listener);

    {
      Context     appContext = context.getApplicationContext ();
      WifiManager wifiMgr    = (WifiManager) appContext.getSystemService (Context.WIFI_SERVICE);

      if (wifiMgr != null)
      {
        mMulticastLock = wifiMgr.createMulticastLock (context.getPackageName ());

        mThread = new Thread (new Runnable ()
        {
          @Override
          public void run ()
          {
            List<JmDNS> sniffers;

            if (mMulticastLock != null)
            {
              mMulticastLock.setReferenceCounted (true);
              mMulticastLock.acquire ();
            }

            sniffers = getSnifferList ();

            try
            {
              Thread.sleep (Long.MAX_VALUE);
            }
            catch (InterruptedException ignore)
            {
            }

            if (sniffers != null)
            {
              try
              {
                for (JmDNS sniffer : sniffers) {
                  sniffer.close ();
                }
              }
              catch (IOException ignore)
              {
              }
            }

            if (mMulticastLock != null)
            {
              mMulticastLock.release ();
            }
          }
        }, "DnsServiceSniffer");
      }
    }
  }

  // ---------------------------------------------------
  public void start ()
  {
    mThread.start ();
  }

  // ---------------------------------------------------
  public void stop ()
  {
    mThread.interrupt ();
    super.stop ();
  }

  // ---------------------------------------------------
  private int getIpVersion (InetAddress address)
  {
    if (address instanceof Inet6Address)
    {
      return 6;
    }

    return  4;
  }

  // ---------------------------------------------------
  private List<JmDNS> getSnifferList ()
  {
    try
    {
      ArrayList<JmDNS>              jmdnsList  = new ArrayList<> ();
      InetAddress                   ipAddress  = InetAddress.getLocalHost ();
      int                           ipVersion  = getIpVersion (ipAddress);
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces ();

      for (NetworkInterface iface : Collections.list (interfaces))
      {
        if (!iface.isLoopback () && iface.isUp () && iface.supportsMulticast ())
        {
          for (InetAddress address : Collections.list (iface.getInetAddresses ()))
          {
            if (getIpVersion (address) == ipVersion)
            {
              JmDNS jmdns;

              jmdns = JmDNS.create (address,
                                    "FreeTeuse:"
                                            + iface.getDisplayName ()
                                            + address);

              jmdns.addServiceListener ("_hid._udp.local.",
                                        DnsServiceSniffer.this);
              jmdnsList.add (jmdns);
            }
          }
        }
      }

      return jmdnsList;
    }
    catch (IOException e)
    {
      e.printStackTrace ();
    }

    return null;
  }

  // ---------------------------------------------------
  @Override
  public void serviceAdded (ServiceEvent event)
  {
  }

  // ---------------------------------------------------
  @Override
  public void serviceRemoved (ServiceEvent event)
  {
  }

  // ---------------------------------------------------
  @Override
  public void serviceResolved (ServiceEvent event)
  {
    ServiceInfo serviceInfo = event.getInfo ();
    String      address     = serviceInfo.getHostAddress ();
    int         port        = serviceInfo.getPort ();
    Freebox     freebox;

    address = address.replaceAll ("[\\[\\]]", "");

    freebox  = new Freebox (address,
                            port,
                            null);

    onFreeboxDetected (freebox);

  }
}
