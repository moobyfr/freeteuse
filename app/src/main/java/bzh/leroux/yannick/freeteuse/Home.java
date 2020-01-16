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
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import bzh.leroux.yannick.freeteuse.sniffers.DnsServiceSniffer;
import bzh.leroux.yannick.freeteuse.sniffers.FreeboxSniffer;
import bzh.leroux.yannick.freeteuse.sniffers.BonjourSniffer;
import bzh.leroux.yannick.freeteuse.sniffers.Simulator;

class Home implements FreeboxSniffer.Listener
{
  public interface Listener
  {
    void onFreeboxSelected (Freebox freebox);
    void onFreeboxDetected (Freebox freebox);
  }

  private DnsServiceSniffer mDnsServiceSniffer;
  private BonjourSniffer    mBonjourSniffer;
  private Simulator         mSimulator;
  private Context           mContext;
  private SharedPreferences mPreferences;
  private List<Freebox>     mBoxes;
  private Listener          mListener;
  private Painter           mPainter;
  private Logger            mLogger;

  // ---------------------------------------------------
  Home (Context           context,
        Listener          listener,
        SharedPreferences preferences)
  {
    mContext     = context;
    mListener    = listener;
    mPreferences = preferences;
    mPainter     = new Painter (context);
    mBoxes       = new ArrayList<> ();
  }

  // ---------------------------------------------------
  private void recoverSavedBoxes ()
  {
    String freeboxPool = mPreferences.getString ("freebox_pool", null);

    if (freeboxPool != null)
    {
      Freebox focus = null;

      Log.d (Freeteuse.TAG, freeboxPool.replace ("},", "},\n"));

      try
      {
        JSONArray array = new JSONArray (freeboxPool);

        for (int i = 0; i < array.length (); i++)
        {
          Freebox freebox = new Freebox (mContext,
                                         array.getJSONObject (i));

          if (freebox.isConsistent ())
          {
            mPainter.useColor (freebox.getColor ());
            mBoxes.add (freebox);

            if (freebox.hasFocus () || (focus == null))
            {
              focus = freebox;
            }
          }
        }
      }
      catch (org.json.JSONException e)
      {
        e.printStackTrace ();
      }

      if (focus != null)
      {
        mListener.onFreeboxSelected (focus);
      }
    }

    //noinspection ConstantConditions
    if (BuildConfig.BUILD_TYPE.equals ("debug"))
    {
      CharSequence text = String.valueOf (mBoxes.size ());

      Toast toast = Toast.makeText (mContext,
                                    text + " Freebox",
                                    Toast.LENGTH_SHORT);
      toast.show ();
    }
  }

  // ---------------------------------------------------
  void discloseBoxes ()
  {
    mLogger = new Logger (mContext,
                          "0x59",
                          "Home");

    mBoxes.clear ();

    recoverSavedBoxes ();

    mDnsServiceSniffer = new DnsServiceSniffer (mContext, this);
    mDnsServiceSniffer.start ();

    mBonjourSniffer = new BonjourSniffer (mContext, this);
    mBonjourSniffer.start ("_hid._udp");
    //mBonjourSniffer.start ("_services._dns-sd._udp");

    mSimulator = new Simulator (mContext, this);
    mSimulator.start ();
  }

  // ---------------------------------------------------
  void concealBoxes ()
  {
    mDnsServiceSniffer.stop ();
    mBonjourSniffer.stop    ();
    mSimulator.stop         ();

    save ();

    mBoxes.clear ();
  }

  // ---------------------------------------------------
  private void save ()
  {
    SharedPreferences.Editor editor = mPreferences.edit ();
    JSONArray                array  = new JSONArray ();

    for (Freebox box : mBoxes)
    {
      JSONObject json = box.getJson ();

      if (json != null)
      {
        Log.e (Freeteuse.TAG, String.valueOf (json));
        array.put (json);
      }
    }

    editor.putString ("freebox_pool", String.valueOf (array));
    editor.commit ();
  }

  // ---------------------------------------------------
  @Override
  public void onFreeboxDetected (Freebox        freebox,
                                 FreeboxSniffer sniffer)
  {
    for (Freebox box : mBoxes)
    {
      if (freebox.equals (box))
      {
        mLogger.Log ("- " + sniffer + "/" + freebox);

        box.detected ();
        mListener.onFreeboxDetected (box);
        return;
      }
    }

    if (freebox.getColor () == null)
    {
      freebox.setColor (mPainter.getColor ());
    }
    mBoxes.add (freebox);
    mListener.onFreeboxDetected (freebox);
  }

  // ---------------------------------------------------
  void paintBoxButtons (Freebox     middle,
                        ImageButton leftSelector,
                        ImageButton middleSelector,
                        ImageButton rightSelector)
  {
    Freebox rightBox = getNextReachable     (middle);
    Freebox leftBox  = getPreviousReachable (middle);

    rightSelector.setVisibility (View.INVISIBLE);
    if (rightBox != null)
    {
      rightSelector.setVisibility    (View.VISIBLE);
      rightSelector.setImageResource (mPainter.getResourceId ("next",
                                                              rightBox.getColor ()));
    }

    middleSelector.setImageResource (mPainter.getResourceId ("free",
                                                             middle.getColor ()));

    leftSelector.setVisibility  (View.INVISIBLE);
    if (leftBox != null)
    {
      leftSelector.setVisibility    (View.VISIBLE);
      leftSelector.setImageResource (mPainter.getResourceId ("previous",
                                                             leftBox.getColor ()));
    }
  }

  // ---------------------------------------------------
  Freebox getNextReachable (Freebox of)
  {
    boolean found = false;

    for (Freebox box : mBoxes)
    {
      if (found && box.isReachable ())
      {
        return box;
      }

      if (box.equals (of))
      {
        found = true;
      }
    }

    return null;
  }

  // ---------------------------------------------------
  Freebox getPreviousReachable (Freebox of)
  {
    Freebox previous = null;

    for (Freebox box : mBoxes)
    {
      if (box.equals (of))
      {
        break;
      }

      if (box.isReachable ())
      {
        previous = box;
      }
    }

    return previous;
  }
}
