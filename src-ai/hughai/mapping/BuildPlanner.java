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
// ======================================================================================
//

package hughai.mapping;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.*;
import hughai.mapping.MovementMaps.MovementMapPos;
import hughai.unitdata.*;
import hughai.utils.*;
import hughai.basictypes.*;


// arguably reservations should go through this class, since this
// is essentially the controller and BuildMap is the model
public class BuildPlanner
{
   public final int BuildMargin = 8;

   CSAI csai;
   OOAICallback aicallback;
   LogFile logfile;
   UnitDefHelp unitdefhelp;
   Maps maps;
   //	BuildMap buildMap;
   //	MovementMaps movementMaps;

   int mapwidth;
   int mapheight;

   public BuildPlanner(PlayerObjects playerObjects)
   {
      csai = playerObjects.getCSAI();
      aicallback = csai.aicallback;
      logfile = playerObjects.getLogFile();  
      unitdefhelp = playerObjects.getUnitDefHelp();
      //		buildMap = playerObjects.getBuildMap();
      //		movementMaps = playerObjects.getMovementMaps();
      maps = playerObjects.getMaps();

      mapwidth = aicallback.getMap().getWidth();
      mapheight = aicallback.getMap().getHeight();
   }

   public TerrainPos ClosestBuildSite( UnitDef unitdef, TerrainPos approximatepos, int maxposradius, int mindistancemapunits )
   {
      // ok, so plan is we work our way out in a kindof spiral
      // we start in centre, and increase radius, and work around in a square at each radius, until we find something
      int centrex = (int)( approximatepos.x / 8 );
      int centrey = (int)( approximatepos.z / 8 );
      //int radius = 0;
      int radius = mindistancemapunits;
      int unitsizexwithmargin = unitdef.getXSize() + 2 * BuildMargin;
      int unitsizeywithmargin = unitdef.getZSize() + 2 * BuildMargin;
      if( unitdefhelp.IsFactory( unitdef ) )
      {
         unitsizeywithmargin += 8;
         centrey += 4;
      }
      // while( radius < mapwidth || radius < mapheight ) // hopefully we never get quite this far...
      while( radius < ( maxposradius / 8 ) )
      {
         //  logfile.WriteLine( "ClosestBuildSite radius " + radius );
         for( int deltax = -radius; deltax <= radius; deltax++ )
         {
            for( int deltay = -radius; deltay <= radius; deltay++ )
            {
               if( deltax == radius || deltax == -radius || deltay == radius || deltay == -radius ) // ignore the hollow centre of square
               {
                  // logfile.WriteLine( "delta " + deltax + " " + deltay );
                  boolean positionok = true;
                  // go through each square in proposed site, check not build on
                  for( int buildmapy = centrey - unitsizeywithmargin / 2; positionok && buildmapy < centrey + unitsizeywithmargin / 2; buildmapy++ )
                  {
                     //String logline = "";
                     for( int buildmapx = centrex - unitsizexwithmargin / 2; positionok && buildmapx < centrex + unitsizexwithmargin / 2; buildmapx++ )
                     {
                        //								int thisx = buildmapx + deltax;
                        //								int thisy = buildmapy + deltay;
                        BuildMap.BuildMapPos buildMapPos = new BuildMap.BuildMapPos( buildmapx + deltax, buildmapy + deltay );
                        HeightMap.HeightMapPos heightMapPos = buildMapPos.toHeightMapPos();
                        MovementMapPos movementMapPos = MovementMapPos.fromHeightMapPos( heightMapPos );
                        if( !buildMapPos.validate()
                              || !maps.getBuildMap().isSquareAvailable( buildMapPos )
                              || !maps.getMovementMaps().vehicleCanMoveOk( movementMapPos ) )
                           //								      thisx < 0 || thisy < 0 || thisx >= mapwidth || thisy >= mapwidth ||
                           //										!maps.getBuildMap().isSquareAvailable[ thisx][thisy ] ||
                           //										!maps.getMovementMaps().vehiclemap[ thisx / 2][ thisy / 2 ] )
                        {
                           //logfile.WriteLine( "check square " + buildmapx + " " + buildmapy + " NOK" );
                           positionok = false;
                           //   logline += "*";
                        }
                        else
                        {
                           //   logline += "-";
                        }
                        //logfile.WriteLine( "check square " + buildmapx + " " + buildmapy + "Ok" );
                     }
                     //    logfile.WriteLine( logline );
                  }
                  // logfile.WriteLine("");
                  if( positionok )
                  {
                     return new TerrainPos( ( centrex + deltax ) * 8, 0, ( centrey + deltay ) * 8 );
                  }
               }
            }
         }
         radius++;
      }
      return null;
   }
}

