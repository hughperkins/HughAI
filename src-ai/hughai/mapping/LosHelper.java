// Copyright Hugh Perkins 2006, 2009
// hughperkins@gmail.com http://manageddreams.com
//
// This program is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the
// Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
// or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
//  more details.
//
// You should have received a copy of the GNU General Public License along
// with this program in the file licence.txt; if not, write to the
// Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-
// 1307 USA
// You can find the licence also on the web at:
// http://www.opensource.org/licenses/gpl-license.php
//

package hughai.mapping;

import java.util.*;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;
import com.springrts.ai.oo.Map;

import hughai.PlayerObjects;
import hughai.basictypes.*;
import hughai.*;
import hughai.mapping.LosMap.LosMapPos;
import hughai.utils.*;


public class LosHelper
{
   OOAICallback aicallback;
   //	MovementMaps movementMaps;
   //	LosMap losmap;
   //	Maps playerObjects.getMaps();
   PlayerObjects playerObjects;
   DrawingUtils drawingUtils;

   public LosHelper( PlayerObjects playerObjects ) {
      aicallback = playerObjects.getAicallback();
      //		movementMaps = playerObjects.getMovementMaps();
      //		losmap = playerObjects.getLosMap();
      //		playerObjects.getMaps() = playerObjects.getMaps();
      this.playerObjects = playerObjects;
      drawingUtils = playerObjects.getDrawingUtils();
   }

   // note: need to check compatible area
   public TerrainPos GetNearestUnseen(TerrainPos currentpos, UnitDef unitdef, int unseensmeansthismanyframes)
   {
      Map map = aicallback.getMap();
      int mapwidth = map.getWidth();
      int mapheight = map.getHeight();

      int currentunitarea = playerObjects.getMaps().getMovementMaps().GetArea(unitdef, currentpos);
      LosMap losMap = playerObjects.getMaps().getLosMap();
      int losmapwidth = LosMap.losmapwidth;
      int losmapheight = LosMap.losmapheight;
      int maxradius = (int)Math.sqrt(losmapheight * losmapheight + losmapwidth * losmapwidth);
      int unitlosradius = (int)unitdef.getLosRadius(); // this is in map / 2 units, so it's ok
      LosMapPos[] circlepoints = CreateCirclePoints(unitlosradius);
      int bestradius = 10000000;
      int bestarea = 0;
      TerrainPos bestpos = null;
      LosMapPos unitmappos = LosMapPos.fromTerrainPos( currentpos );
      //		int unitmapx = (int)(currentpos.x / 16);
      //		int unitmapy = (int)(currentpos.y / 16);
      int thisframecount = playerObjects.getFrameController().getFrame();
      // step around in unitlosradius steps
      for (int radiuslosunits = unitlosradius * 2; 
         radiuslosunits <= maxradius; 
         radiuslosunits += unitlosradius)
      {
         // calculate angle for a unitlosradius / 2 step at this radius.
         // DrawingUtils.DrawCircle(currentpos, radiuslosunits * 16);

         // as we move further out, we need to move through smaller angles
         double anglestepradians = 2 * Math.asin((double)unitlosradius / 2 / (double)radiuslosunits);
         //csai.DebugSay("anglestepradians: " + anglestepradians);
         //return null;
         for (double angleradians = 0; angleradians <= Math.PI * 2; angleradians += anglestepradians)
         {
            int unseenarea = 0;
            LosMapPos searchmappos = unitmappos.add( new LosMapPos(
                  (int)( radiuslosunits * Math.cos(angleradians) ),
                  (int)( radiuslosunits * Math.sin(angleradians) ) ) );
//            int searchmapx = unitmappos.x + (int)((double)radiuslosunits * Math.cos(angleradians));
//            int searchmapy = unitmappos.y + (int)((double)radiuslosunits * Math.sin(angleradians));
            if (searchmappos.validate() )
            {
               // if (csai.DebugOn)
               //  {
               //      int groupnumber = DrawingUtils.DrawCircle(new AIFloat3(searchmapx * 16, 50 + aicallback.GetElevation( searchmapx * 16, searchmapy * 16 ), searchmapy * 16), unitlosradius * 16);
               //     aicallback.SetFigureColor(groupnumber, 1, 1, 0, 0.5);
               // }

//               LosMapPos searchMapPos = new LosMapPos( searchmapx, searchmapy );
               TerrainPos searchTerrainPos = searchmappos.toTerrainPos();
               int thisareanumber = playerObjects.getMaps()
                   .getMovementMaps().GetArea(
                         unitdef, searchTerrainPos );
//               .getMovementMaps().GetArea(unitdef, new Float3(searchmapx * 16, 0, searchmapy * 16));
               if (thisareanumber == currentunitarea)
               {//
                  //if (csai.DebugOn)
                  // {
                  //     int groupnumber = DrawingUtils.DrawCircle(new AIFloat3(searchmapx * 16, 100, searchmapy * 16), unitlosradius * 16);
                  //     aicallback.SetFigureColor(groupnumber, 1, 1, 0, 0.5);
                  // }
                  for (LosMapPos point : circlepoints)
                  {
                     LosMapPos thismappos = searchmappos.add( point );
//                     int thismapx = searchmapx + point.x;
//                     int thismapy = searchmapy + point.y;
                     if (thismappos.validate())
                     {
//                        LosMapPos losMapPos = new LosMapPos( thismapx, thismapy );
                        int lastseenframecount = losMap.getLastSeenFrameCount( thismappos );
                        if (thisframecount - lastseenframecount > unseensmeansthismanyframes ) {
//                        if (thisframecount - losMap.LastSeenFrameCount[thismapx][ thismapy]                                                                                                               > unseensmeansthismanyframes)
//                        {
                           unseenarea++;
                        }
                     }
                  }
                  if (unseenarea >= (circlepoints.length) * 8 / 10)
                  {
                     //							drawingUtils.DrawCircle(
                     //									new AIFloat3(searchmapx * 16, 
                     //											100 * aicallback.getMap().getElevationAt(searchmapx * 16, searchmapy * 16),
                     //											searchmapy * 16),
                     //											unitlosradius * 16);
                     //							drawingUtils.setFigureColor( 0f, 1f, 0.5f);
                     return searchTerrainPos;
                  }
                  // return new AIFloat3(searchmapx * 16, 0, searchmapy * 16); // for debugging, remove later
               }
            }
         }
      }
      return null;
   }

   // returns points for a circle centered at origin of radius radius
   // the points are all integer points that lie in this circle
   LosMapPos[] CreateCirclePoints(int radius)
   {
      ArrayList<LosMapPos> pointsal = new ArrayList<LosMapPos>();
      for (int y = -radius; y <= radius; y++)
      {
         int xextent = (int)Math.sqrt(radius * radius - y * y);
         for (int x = -xextent; x <= xextent; x++)
         {
            pointsal.add(new LosMapPos(x, y));
         }
      }
      return (LosMapPos[])pointsal.toArray(new LosMapPos[0]);
   }

}

