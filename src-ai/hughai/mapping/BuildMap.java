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
   CSAI csai;
   OOAICallback aicallback;
   LogFile logfile;
   UnitDefHelp unitdefhelp;
   UnitController unitcontroller;
   DrawingUtils drawingUtils;
   //FriendlyUnitPositionObserver friendlyUnitPositionObserver;

   static class BuildingInfo
   {
      public int mapx;
      public int mapy;
      public int mapsizex;
      public int mapsizey;
      public BuildingInfo( int mapx, int mapy, int mapsizex, int mapsizey )
      {
         this.mapx = mapx;
         this.mapy = mapy;
         this.mapsizex = mapsizex;
         this.mapsizey = mapsizey;
      }
   }

   HashMap<Unit, BuildingInfo> buildinginfobyunit = new HashMap<Unit, BuildingInfo>(); // BuildingInfo by deployedid

   public boolean[][] SquareAvailable; // mapwidth by mapheight
   int mapwidth;
   int mapheight;

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

   // ReserveSpace; ideally the reserver would pass himself in as IBuildMapReserver or something , but object for now (not yet used)
   // note that destroying a unit over this space wont undo reservation, since not included in buildinginfobyid list
   // (maybe buildinginfobyowner???)
   public void ReserveSpace( Object owner, int mapx, int mapy, int sizex, int sizey )
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
      for (int x = 0; x < sizex; x++)
      {
         for( int y = 0; y < sizey; y++ )
         {
            int thisx = mapx + x - sizex / 2;
            int thisy = mapy + y - sizey / 2;
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
            Float3 pos = unitcontroller.getPos( unit );
            int mapposx = (int)( pos.x / 8 );
            int mapposy = (int)( pos.z / 8 );
            //int unitsizex = (int)Math.Ceiling( unitdef.xsize / 8.0 );
            //int unitsizey = (int)Math.Ceiling( unitdef.ysize / 8.0 );
            int unitsizex = unitdef.getXSize();
            int unitsizey = unitdef.getZSize();
            logfile.WriteLine( "Buildmap static unit created " + unitdef.getName() + " mappos " + mapposx + " " + mapposy + " unitsize " + unitsizex + " " + unitsizey );
            buildinginfobyunit.put( unit, new BuildingInfo( mapposx, mapposy, unitsizex, unitsizey ) );
            if (csai.DebugOn)
            {
               //drawingUtils.DrawRectangle(
                 //    new AIFloat3((mapposx - unitdef.getXSize() / 2) * 8,
                   //        0,
                     //      (mapposy - unitdef.getZSize() / 2) * 8),
                       //    unitdef.getXSize() * 8,
                         //  unitdef.getZSize() * 8 );
            }
            for( int x = 0; x < unitsizex; x++ )
            {
               for( int y = 0; y < unitsizey; y++ )
               {
                  int thisx = mapposx + x - unitdef.getXSize() / 2;
                  int thisy = mapposy + y - unitdef.getZSize() / 2;
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
            for( int x = 0; x < buildinginfo.mapsizex / 8; x++ )
            {
               for( int y = 0; y < buildinginfo.mapsizey / 8; y++ )
               {
                  SquareAvailable[ buildinginfo.mapx + x][ buildinginfo.mapy + y ] = true;
               }
            }                
            buildinginfobyunit.remove( unit );
         }
      }
   }
}
