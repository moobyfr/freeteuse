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
import android.content.res.Resources;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

class Painter
{
  private final List<String> mColors = Arrays.asList ("yellow", "green", "blue", "red");

  private Context                    mContext;
  private Hashtable<String, Integer> mColorTable;

  // ---------------------------------------------------
  Painter (Context context)
  {
    mContext    = context;
    mColorTable = new Hashtable<> ();

    for (String color : mColors)
    {
      mColorTable.put (color, 0);
    }
  }

  // ---------------------------------------------------
  void useColor (String color)
  {
    if (mColorTable.containsKey (color))
    {
      Integer count = mColorTable.get (color);

      count++;
      mColorTable.put (color, count);
    }
  }

  // ---------------------------------------------------
  String getColor ()
  {
    Integer lowestValue = 0;
    String  bestColor   = mColors.get (0);

    for (String currentColor : mColors)
    {
      Integer currentValue = mColorTable.get (currentColor);

      if (currentValue <= lowestValue)
      {
        lowestValue = currentValue;
        bestColor   = currentColor;
      }
    }

    useColor (bestColor);

    return bestColor;
  }

  // ---------------------------------------------------
  int getResourceId (String name,
                     String color)
  {
    int id = -1;

    try
    {
      Resources resources = mContext.getResources ();

      id = resources.getIdentifier ("x" + name + color,
                                    "drawable",
                                     mContext.getPackageName ());
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }

    return id;
  }
}
