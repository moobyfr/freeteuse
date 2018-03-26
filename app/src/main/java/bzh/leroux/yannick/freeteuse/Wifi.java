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

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;

class Wifi extends BroadcastReceiver
{
  private Context     mContext;
  private AlertDialog mAlert;

  // ---------------------------------------------------
  Wifi (Context context)
  {
    IntentFilter filter = new IntentFilter (ConnectivityManager.CONNECTIVITY_ACTION);

    context.registerReceiver (this,
                              filter);

    mContext = context;
  }

  // ---------------------------------------------------
  @Override
  public void onReceive (Context context,
                         Intent  intent)
  {
    ConnectivityManager netMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

    if (netMgr != null)
    {
      NetworkInfo networkInfo = netMgr.getActiveNetworkInfo ();

      hideAlert ();

      if ((networkInfo != null) && networkInfo.isConnected ())
      {
        Context     appContext = context.getApplicationContext ();
        WifiManager wifiMgr    = (WifiManager) appContext.getSystemService (Context.WIFI_SERVICE);

        if (wifiMgr != null)
        {
          WifiInfo connectionInfo = wifiMgr.getConnectionInfo ();
          String   ssid           = connectionInfo.getSSID ();

          if (ssid != null)
          {
            if ((ssid.equals("FreeWifi") || ssid.equals("FreeWifi_secure")))
            {
              displayAlert("Le réseau " + ssid + " ne permet pas l'utilisation de la télécommande.",
                           "Changer de réseau");
            }
            return;
          }
        }
      }

      displayAlert ("La télécommande n'est pas utilisable sans connexion réseau",
                    "Se connecter au réseau");
    }
  }

  // ---------------------------------------------------
  void displayAlert (String description,
                     String action)
  {
    hideAlert ();

    {
      AlertDialog.Builder builder = new AlertDialog.Builder (mContext);

      builder.setTitle ("WIFI");
      builder.setMessage (description);

      builder.setPositiveButton (action, new DialogInterface.OnClickListener()
      {
        public void onClick (DialogInterface dialog,
                            int              which)
        {
          hideAlert ();
          mContext.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
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

  // ---------------------------------------------------
  void stop ()
  {
    mContext.unregisterReceiver (this);

    hideAlert ();
  }
}
