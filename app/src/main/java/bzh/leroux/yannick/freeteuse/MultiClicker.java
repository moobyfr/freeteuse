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

import android.os.Handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

class MultiClicker implements Runnable
{
  public interface Listener
  {
    void onMultiClickStep (String key_name);
  }

  private Listener         mListener;
  private Handler          mHandler;
  private Iterator<String> mIterator;

  // ---------------------------------------------------
  MultiClicker (Listener listener)
  {
    mHandler  = new Handler ();
    mListener = listener;
  }

  // ---------------------------------------------------
  void start (String sequence)
  {
    String[]     table = sequence.split ("\\+");
    List<String> list  = new ArrayList<> (Arrays.asList (table));

    mIterator = list.iterator ();

    run ();
  }

  // ---------------------------------------------------
  @Override
  public void run ()
  {
    if (mIterator.hasNext ())
    {
      String current = mIterator.next ();

      try
      {
        long delay = Long.parseLong (current, 10);

        mHandler.postDelayed (this, delay);
      }
      catch (NumberFormatException ignore)
      {
        mListener.onMultiClickStep (current);
        mHandler.post (this);
      }
    }
    else
    {
      stop ();
    }
  }

  // ---------------------------------------------------
  void stop ()
  {
    mHandler.removeCallbacks(this);
    mIterator = null;

    mListener.onMultiClickStep (null);
  }

  // ---------------------------------------------------
  boolean stopped ()
  {
    return mIterator == null;
  }
}
