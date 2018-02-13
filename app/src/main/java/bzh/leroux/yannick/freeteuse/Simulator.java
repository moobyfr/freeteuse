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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

class Simulator extends FreeboxSniffer
{
  private BroadcastReceiver mReceiver;
  private Context           mContext;

  // ---------------------------------------------------
  Simulator (Context  context,
             Listener listener)
  {
    super (listener);

    mContext  = context;
    mReceiver = new BroadcastReceiver ()
    {
      @Override
      public void onReceive (Context context,
                             Intent  intent)
      {
        Freebox freebox = new Freebox (intent.getStringExtra ("ip"),
                                      35830);

        onFreeboxDetected (freebox);
      }
    };
  }

  // ---------------------------------------------------
  void start ()
  {
    IntentFilter filter = new IntentFilter ();

    filter.addAction ("bzh.leroux.yannick.freeteuse.FREEBOX");
    mContext.registerReceiver (mReceiver,
                               filter);
  }

  // ---------------------------------------------------
  void stop ()
  {
    mContext.unregisterReceiver (mReceiver);
  }
}