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

import java.util.*;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.*;
import hughai.mapping.HeightMap.HeightMapPos;
import hughai.ui.MainUI;
import hughai.unitdata.*;
import hughai.utils.*;
import hughai.basictypes.*;

// stores the position of all our buildings, on a 2d map
// so we can find our own nearest build position, without relying
// on the spring one, which tends not to be quite as configurable to
// our needs as our own one
public class BuildMap
{
   public static class BuildMapPos extends Int2 {
      public BuildMapPos() {
         
      }
      public BuildMapPos( Int2 int2 ) {
         x = int2.x;
         y = int2.y;
      }
      public BuildMapPos( int x, int y ) {
         super( x, y );
      }
      public TerrainPos toTerrainPos() {
         return new TerrainPos( x * 8 * granularity, 0, y * 8 * granularity );
      }
      HeightMapPos toHeightMapPos() {
         return new HeightMapPos( x, y );
      }
      public static BuildMapPos fromHeightMapPos( HeightMapPos heightMapPos ) {
         return new BuildMapPos( heightMapPos.x, heightMapPos.y );
      }
      public static BuildMapPos fromTerrainPos( TerrainPos terrainPos ) {
         return new BuildMapPos( (int)terrainPos.x / 8 / granularity,
               (int)terrainPos.z / 8 / granularity );
      }
      public boolean validate() {
         return x >= 0 && y >= 0
            && x < buildmapwidth && y < buildmapheight;
      }
   }

   public final static int granularity = 1;
   
   CSAI csai;
   OOAICallback aicallback;
   LogFile logfile;
   UnitDefHelp unitdefhelp;
   UnitController unitcontroller;
   DrawingUtils drawingUtils;
   //FriendlyUnitPositionObserver friendlyUnitPositionObserver;

   static class BuildingInfo
   {
      BuildMapPos mapPos;
      public int mapsizex;
      public int mapsizey;
      public BuildingInfo( BuildMapPos mapPos, int mapsizex, int mapsizey )
      {
         this.mapPos = mapPos;
         this.mapsizex = mapsizex;
         this.mapsizey = mapsizey;
      }
   }

   HashMap<Unit, BuildingInfo> buildinginfobyunit = new HashMap<Unit, BuildingInfo>(); // BuildingInfo by deployedid

   private boolean[][] SquareAvailable; // mapwidth by mapheight
   static int mapwidth;
   static int mapheight;
   
   static int buildmapwidth;
   static int buildmapheight;

   public BuildMap(PlayerObjects playerObjects)
   {
      csai = playerObjects.getCSAI();
      aicallback = csai.aicallback;
      logfile = playerObjects.getLogFile();  
      unitdefhelp = playerObjects.getUnitDefHelp();
      unitcontroller = playerObjects.getUnitController();
      drawingUtils = playerObjects.getDrawingUtils();
      //friendlyUnitPositionObserver = playerObjects.getFriendlyUnitPositionObserver();

      csai.registerGameListener( new GameListener() );

      if (csai.DebugOn)
      {
//         csai.RegisterVoiceCommand("showbuildmap", new DumpBuildMapHandler());
      }
      playerObjects.getMainUI().registerButton( "Show build map", new ShowBuildMapButton() );

      Init();
   }

   class ShowBuildMapButton implements MainUI.ButtonHandler {
      @Override
      public void go() {
         drawingUtils.DrawMap(SquareAvailable);
      }
   }

   public class DumpBuildMapHandler implements VoiceCommandHandler {
      @Override
      public void commandReceived(String cmd, String[] split, int player)
      {
         drawingUtils.DrawMap(SquareAvailable);
      }
   }

   public void Init()
   {
      // plan: mark every square available, then remove squares as we build
      // also, remove planned buildings, ie: metal extractors
      // note: get metal do do this for us
      // so we need UnitController to alert us about buildings in progress etc

      logfile.WriteLine( "BuildMap.Init()" );
      mapwidth = aicallback.getMap().getWidth();
      mapheight = aicallback.getMap().getHeight();
      buildmapwidth = mapwidth; //same...
      buildmapheight = mapheight;

      SquareAvailable = new boolean[ mapwidth][ mapheight ];
      for( int x = 0; x < mapwidth; x++ )
      {
         for( int y = 0; y < mapheight; y++ )
         {
            SquareAvailable[ x][ y ] = true;
         }
      }
      logfile.WriteLine( "BuildMap.Init finished()" );
   }
   
   public boolean isSquareAvailable( BuildMapPos buildMapPos ) {
      return SquareAvailable[buildMapPos.x][buildMapPos.y];
   }

   // ReserveSpace; ideally the reserver would pass himself in as IBuildMapReserver or something , but object for now (not yet used)
   // note that destroying a unit over this space wont undo reservation, since not included in buildinginfobyid list
   // (maybe buildinginfobyowner???)
   public void ReserveSpace( Object owner, BuildMapPos buildMapPos, int sizex, int sizey )
   {
      if (csai.DebugOn)
      {
//         drawingUtils.DrawRectangle(
//               new AIFloat3((mapx - sizex / 2) * 8,
//                     0,
//                     ( mapy - sizey / 2 ) * 8),
//                     sizex * 8,
//                     sizey * 8 );
         // logfile.WriteLine( "marking " + thisx + " " + thisy + " as used by " + owner.toString() );
         //   aicallback.DrawUnit( "ARMMINE1", new AIFloat3( thisx * 8, aicallback.GetElevation( thisx * 8, thisy * 8 ), thisy * 8 ), 0.0f, 400, aicallback.GetMyAllyTeam(), true, true);
      }
      for (int deltax = 0; deltax < sizex; deltax++)
      {
         for( int deletay = 0; deletay < sizey; deletay++ )
         {
            int thisx = buildMapPos.x + deltax - sizex / 2;
            int thisy = buildMapPos.y + deletay - sizey / 2;
            if( thisx >= 0 && thisy >= 0 && thisx < mapwidth && thisy < mapheight )
            {
               SquareAvailable[ thisx][thisy ] = false;
            }
         }
      }
   }

   //class UnitListener extends UnitController.UnitAdapter {
   class GameListener extends GameAdapter {
      // mark squares as used
      // and keep record of where this unit was, and how big, in case it is destroyed
      @Override
      public void UnitCreated( Unit unit, Unit builder )
      {
         //logfile.WriteLine( "BuildMap.unitcreated " + unit.getUnitId() + " " + unit.getDef().getHumanName() );
         UnitDef unitdef = unit.getDef();
         if( !buildinginfobyunit.containsKey( unit ) && !unitdefhelp.IsMobile( unit.getDef() ) )
         {
            //AIFloat3 pos = friendlyUnitPositionObserver aicallback.GetUnitPos( id );
            TerrainPos pos = unitcontroller.getPos( unit );
            BuildMapPos buildMapPos = BuildMapPos.fromTerrainPos( pos );
            //int unitsizex = (int)Math.Ceiling( unitdef.xsize / 8.0 );
            //int unitsizey = (int)Math.Ceiling( unitdef.ysize / 8.0 );
            int unitsizex = unitdef.getXSize();
            int unitsizey = unitdef.getZSize();
            logfile.WriteLine( "Buildmap static unit created " + unitdef.getName() + " mappos " + buildMapPos + " unitsize " + unitsizex + " " + unitsizey );
            buildinginfobyunit.put( unit, new BuildingInfo( buildMapPos, unitsizex, unitsizey ) );
            if (csai.DebugOn)
            {
               //drawingUtils.DrawRectangle(
                 //    new AIFloat3((mapposx - unitdef.getXSize() / 2) * 8,
                   //        0,
                     //      (mapposy - unitdef.getZSize() / 2) * 8),
                       //    unitdef.getXSize() * 8,
                         //  unitdef.getZSize() * 8 );
            }
            for( int deltax = 0; deltax < unitsizex; deltax++ )
            {
               for( int deltay = 0; deltay < unitsizey; deltay++ )
               {
                  int thisx = buildMapPos.x + deltax - unitdef.getXSize() / 2;
                  int thisy = buildMapPos.y + deltay - unitdef.getZSize() / 2;
                  SquareAvailable[ thisx][thisy ] = false;
                  //if( csai.DebugOn )
                  //{
                  // logfile.WriteLine( "marking " + thisx + " " + thisy + " as used by " + unitdef.getName() );
                  //  aicallback.DrawUnit( "ARMMINE1", new AIFloat3( thisx * 8, aicallback.GetElevation( thisx * 8, thisy * 8 ), thisy * 8 ), 0.0f, 400, aicallback.GetMyAllyTeam(), true, true);
                  //}
               }
            }
         }
      }

      @Override
      public void UnitDestroyed( Unit unit, Unit enemy )
      {
         logfile.WriteLine( "BuildMap.unitdestroyed " + unit.getUnitId() + " " + unit.getDef().getHumanName() );
         if( buildinginfobyunit.containsKey( unit ) )
         {
            BuildingInfo buildinginfo = buildinginfobyunit.get(unit);
            for( int deltax = 0; deltax < buildinginfo.mapsizex / 8; deltax++ )
            {
               for( int deltay = 0; deltay < buildinginfo.mapsizey / 8; deltay++ )
               {
                  SquareAvailable[ buildinginfo.mapPos.x + deltax][ buildinginfo.mapPos.y + deltay ] = true;
               }
            }                
            buildinginfobyunit.remove( unit );
         }
      }
   }
}
