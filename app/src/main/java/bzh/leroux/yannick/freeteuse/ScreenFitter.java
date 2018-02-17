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

import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;

class ScreenFitter
{
  private Display mDisplay;

  // ---------------------------------------------------
  ScreenFitter (final View view,
                Display    display)
  {
    mDisplay = display;

    view.post (new Runnable ()
    {
      @Override
      public void run ()
      {
        onViewDisplayed (view);
      }
    });
  }

  // ---------------------------------------------------
  private void onViewDisplayed (View view)
  {
    float         metricsAR;
    float         viewAR;
    float         scale;
    DisplayMetrics metrics = new DisplayMetrics ();

    mDisplay.getMetrics (metrics);

    metricsAR = (float) metrics.widthPixels / (float) metrics.heightPixels;
    viewAR    = (float) view.getWidth () / (float) view.getHeight ();

    if (metricsAR > viewAR)
    {
      scale = (float) metrics.heightPixels * 0.8f / (float) view.getHeight ();
    }
    else
    {
      scale = metrics.widthPixels * 0.8f / view.getWidth ();
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
    {
      view.setScaleX (scale);
      view.setScaleY (scale);
    }
    else
    {
      Log.e (Freeteuse.TAG, "view.setScale not availlable on this device.");
    }
  }
}
