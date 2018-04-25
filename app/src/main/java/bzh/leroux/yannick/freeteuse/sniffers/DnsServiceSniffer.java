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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import bzh.leroux.yannick.freeteuse.CheatCode;
import bzh.leroux.yannick.freeteuse.Freebox;
import bzh.leroux.yannick.freeteuse.Freeteuse;

public class DnsServiceSniffer extends    FreeboxSniffer
                               implements ServiceListener
{
  private Thread                    mThread;
  private WifiManager.MulticastLock mMulticastLock;
  private Handler                   mListenerHandler;
  private Context                   mContext;
  private AlertDialog               mAlert;
  private List<String>              mLogs;
  private CheatCode                 mCheatCode;

  // ---------------------------------------------------
  public DnsServiceSniffer (Context context,
                            Listener listener)
  {
    super (listener);

    mContext   = context;
    mLogs      = new ArrayList<> ();
    mCheatCode = new CheatCode ();

    {
      Context     appContext = context.getApplicationContext ();
      WifiManager wifiMgr    = (WifiManager) appContext.getSystemService (Context.WIFI_SERVICE);

      if (wifiMgr != null)
      {
        mMulticastLock = wifiMgr.createMulticastLock (context.getPackageName ());

        mListenerHandler = new Handler ();

        mThread = new Thread (new Runnable ()
        {
          @Override
          public void run ()
          {
            JmDNS jmdns;

            if (mMulticastLock != null)
            {
              mMulticastLock.setReferenceCounted (true);
              mMulticastLock.acquire ();
            }

            jmdns = getJmDns ();
            if (jmdns != null)
            {
              jmdns.addServiceListener ("_hid._udp.local.",
                                        DnsServiceSniffer.this);
            }

            try
            {
              Thread.sleep (Long.MAX_VALUE);
            }
            catch (InterruptedException ignore)
            {
            }

            if (jmdns != null)
            {
              try
              {
                jmdns.close ();
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
    hideAlert ();
    mThread.interrupt ();
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
  private JmDNS getJmDns ()
  {
    try
    {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
      {
        InetAddress                   ipAddress  = InetAddress.getLocalHost ();
        int                           ipVersion  = getIpVersion (ipAddress);
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces ();
        JmDNS                         jmdns      = null;

        for (NetworkInterface iface : Collections.list (interfaces))
        {
          Log (String.format (Locale.FRENCH, "(1) %s <%d/%d/%d>",
                              iface.toString (),
                              !iface.isLoopback ()?1:0,
                              iface.isUp ()?1:0,
                              iface.supportsMulticast ()?1:0));

          if (jmdns == null)
          {
            if (!iface.isLoopback () && iface.isUp () && iface.supportsMulticast ())
            {
              for (InetAddress address : Collections.list (iface.getInetAddresses ()))
              {
                if (getIpVersion (address) == ipVersion)
                {
                  jmdns = JmDNS.create (address,
                                        "FreeTeuse:"
                                                + iface.getDisplayName ()
                                                + address);
                  Log ("(2) " + address);
                }
              }
            }
          }
        }
        return jmdns;
      }
      else
      {
        return JmDNS.create ("FreeTeuse:");
      }
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
    ServiceInfo info = event.getInfo ();

    Log ("(3) " + info.getName ());
  }

  // ---------------------------------------------------
  @Override
  public void serviceRemoved (ServiceEvent event)
  {
    Log ("(4) " + event.getInfo ());
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
        final Freebox freebox = new Freebox (serviceInfo);

        onFreeboxDetected (freebox);
      }
    });

    Log ("(5) " + Arrays.toString (serviceInfo.getHostAddresses ())
            + ":" + serviceInfo.getPort ());
  }

  // ---------------------------------------------------
  private void Log (String log)
  {
    Log.d (Freeteuse.TAG, log);
    mLogs.add (log);
  }

  // ---------------------------------------------------
  public void onClick (String tag)
  {
    hideAlert ();

    if (mCheatCode.discovered (tag))
    {
      StringBuilder       description = new StringBuilder ();
      AlertDialog.Builder builder     = new AlertDialog.Builder (mContext);

      builder.setTitle ("LOG");

      for (String log: mLogs)
      {
        description.append (log);
        description.append ("\n");
      }

      builder.setMessage (description);

      builder.setPositiveButton ("Fermer", new DialogInterface.OnClickListener()
      {
        public void onClick (DialogInterface dialog,
                             int              which)
        {
          hideAlert ();
        }
      });

      mAlert = builder.create ();
      mAlert.show ();
    }
  }

  // ---------------------------------------------------
  private void hideAlert ()
  {
    if (mAlert != null)
    {
      mAlert.dismiss ();
      mAlert = null;
    }
  }
}
