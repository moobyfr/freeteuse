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

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

class DnsServiceSniffer extends    AsyncTask<String, Void, Void>
                        implements ServiceListener
{
  public interface Listener
  {
    void onDnsService (ServiceInfo serviceInfo);
  }

  private ServiceInfo mServiceInfo;
  private Listener    mListener;

  // ---------------------------------------------------
  DnsServiceSniffer (Listener listener)
  {
    mListener = listener;
  }

  // ---------------------------------------------------
  @Override
  protected Void doInBackground (String... service)
  {
    mServiceInfo = null;

    try
    {
      JmDNS jmdns = JmDNS.create ();

      jmdns.addServiceListener (service[0] + ".local.", this);
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

    return null;
  }

  // ---------------------------------------------------
  @Override
  public void serviceAdded (ServiceEvent event)
  {
    ServiceInfo info = event.getInfo ();

    Log.d ("FreeTeuse", "DnsServiceSniffer::serviceAdded: " + info.getName ());
  }

  // ---------------------------------------------------
  @Override
  public void serviceRemoved (ServiceEvent event)
  {
    Log.d ("FreeTeuse", "DnsServiceSniffer::serviceRemoved: " + event.getInfo ());
  }

  // ---------------------------------------------------
  @Override
  public void serviceResolved (ServiceEvent event)
  {
    mServiceInfo = event.getInfo ();
    cancel (true);
  }

  // ---------------------------------------------------
  @Override
  protected void onPostExecute (Void result)
  {
    mListener.onDnsService (mServiceInfo);
  }

  // ---------------------------------------------------
  @Override
  protected void onCancelled ()
  {
    mListener.onDnsService (mServiceInfo);
  }
}
