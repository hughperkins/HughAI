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

// in charge of determining when we last had LOS to each part of the map
// what we can do is to go through each of our units, get its radar radius, and check it off against map
// we obviously dont need to do this very often for static units
// slower movements (tanks) probably need this less often than faster ones (scouts, planes)    
//
// We cache the position of each unit at the time we wrote its losprint onto the losmap
// we only update the lospring from a unit when it has moved significantly, or a lot of time has passed
//
// possible optimization: dont do every unit, check when units are close to others, using a sector by sector index (caveat: need to check losradius is equivalent, or sort by losradius)
//
// why dont we just copy the spring-generated losmap each frame?  Well, because that would be slower(?)

package hughai.mapping;

import java.util.*;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;
import com.springrts.ai.oo.Map;

import hughai.*;
import hughai.basictypes.*;
import hughai.mapping.EnemyMap.EnemyMapPos;
import hughai.unitdata.*;
import hughai.utils.*;
import hughai.ui.*;

// holds the last seen frame-time of each part of the map, in a 2d array
public class LosMap
{
   public static final int granularity = 2;
   
   public static class LosMapPos extends Int2 {
      public LosMapPos() {

      }
      public LosMapPos( Int2 int2 ) {
         x = int2.x;
         y = int2.y;
      }
      public LosMapPos( int x, int y ) {
         super( x, y );
      }
      public TerrainPos toTerrainPos() {
         return new TerrainPos( x * 8 * granularity, 0, y * 8 * granularity );
      }
      public static LosMapPos fromTerrainPos( TerrainPos terrainPos ) {
         return new LosMapPos( (int)terrainPos.x / 8 / granularity,
               (int)terrainPos.z / 8 / granularity );
      }
      public LosMapPos add( LosMapPos second ) {
         return new LosMapPos( super.add(  second ) );
      }
      public boolean validate(){
         return x >= 0 && y >= 0 && x < losmapwidth && y < losmapheight;
      }
   }

   PlayerObjects playerObjects;
   CSAI csai;
   OOAICallback aicallback;
   LogFile logfile;
   DrawingUtils drawingUtils;
   UnitDefHelp unitDefHelp;
   UnitController unitcontroller;
   Config config;

   public HashMap<Unit,Integer> LastLosRefreshFrameCountByUnit = new HashMap<Unit,Integer>(); // all units here, just dont update static ones very often
   public HashMap<Unit,TerrainPos> PosAtLastRefreshByUnit = new HashMap<Unit,TerrainPos>();
   private int[][] LastSeenFrameCount; // map, mapwidth / 2 by mapheight / 2
   int lasttotalrefresh = -100000;

   static int losmapwidth;
   static int losmapheight;
   
   public int getLosMapWidth(){ return losmapwidth; }
   public int getLosMapHeight(){ return losmapheight; }
   
   int mapwidth;
   int mapheight;

   public LosMap( PlayerObjects playerObjects )
   {
      this.playerObjects = playerObjects;
      csai = playerObjects.getCSAI();
      aicallback = csai.aicallback;
      logfile = playerObjects.getLogFile();
      unitcontroller = playerObjects.getUnitController();
      drawingUtils = playerObjects.getDrawingUtils();
      unitDefHelp = playerObjects.getUnitDefHelp();
      config = playerObjects.getConfig();

      unitcontroller.registerListener(new UnitControllerHandler());

      csai.registerGameListener( new GameListenerHandler() );

      Map map = aicallback.getMap();
      mapwidth = map.getWidth();
      mapheight = map.getHeight();
      
      losmapwidth = mapwidth / granularity;
      losmapheight = mapheight / granularity;

      logfile.WriteLine( "LosMap, create losarray" );
      LastSeenFrameCount = new int[ mapwidth / granularity][ mapheight / granularity ];
      logfile.WriteLine( "losarray created, initializing..." );
      for( int y = 0; y < mapheight / granularity; y++ )
      {
         for( int x = 0; x < mapwidth / granularity; x++ )
         {
            LastSeenFrameCount[ x][ y ] = -1000000;
         }
      }
      logfile.WriteLine( "losarray initialized" );

      if (csai.DebugOn)
      {
//         csai.RegisterVoiceCommand("showlosmap", new DumpLosMapHandler());
      }
      playerObjects.getMainUI().registerButton( "Show los map", new ShowLosMapButton() );
   }
   
   public int getLastSeenFrameCount( LosMapPos losMapPos ) {
      return LastSeenFrameCount[losMapPos.x][losMapPos.y];
   }
   
   class ShowLosMapButton implements MainUI.ButtonHandler {
      @Override
      public void go() {
         showLosMap(200);
      }
   }

   class DumpLosMapHandler implements VoiceCommandHandler {
      @Override
      public void commandReceived(String cmd, String[] split, int player)
      {
         int maxageseconds = 200;
         if( split.length > 2 ) {
            maxageseconds = Integer.parseInt( split[2] );
         }
         showLosMap( maxageseconds);
      }
   }
   
   void showLosMap(int maxageseconds) {
      int mapdrawgranularity = config.getMapDrawGranularity() / granularity;
      boolean[][]losmap = new boolean[mapwidth / granularity / mapdrawgranularity ][ mapheight/ 2 / mapdrawgranularity ];
      int frame = playerObjects.getFrameController().getFrame();
      for( int y = 0; y < mapheight / granularity / mapdrawgranularity; y += 1 )
      {
         for( int x = 0; x < mapwidth / granularity / mapdrawgranularity; x  += 1 )
         {
            losmap[x][y] = ( frame - LastSeenFrameCount[ x * mapdrawgranularity][ y * mapdrawgranularity ] < maxageseconds * 30 );
         }
      }
      drawingUtils.DrawMap(losmap);
   }

   void DoIncrementalRefreshes()
   {
      //  logfile.WriteLine( "LosMap start incrementalrefreshes" );
      int numupdates = 0;
      int thresholddistance = config.getLosmapdistancethresholdforunitupdate();
      int thresholddistancesquared = thresholddistance * thresholddistance;
      //for( Unit mobileidobject : friendlyunitpositionobserver.MobileUnitIds )
      for( Unit unit : unitcontroller.units )
      {
         UnitDef unitdef = unitcontroller.getUnitDef( unit );
         if( unitDefHelp.IsMobile( unitdef ) ) {
            TerrainPos currentpos = unitcontroller.getPos(unit);
            TerrainPos lastpos = PosAtLastRefreshByUnit.get( unit );
            boolean doUpdate = false;
            if( lastpos == null ) {
               doUpdate = true;
            } else {
               float distancesquared = lastpos.GetSquaredDistance( 
                     currentpos );
               if( distancesquared >= thresholddistancesquared )
               {
                  doUpdate = true;
               }
            }
            if( doUpdate ) {
               UpdateLosForUnit( unit );
               numupdates++;
            }
         } else { // if static unit, we still  need to handle the los ;-)
            TerrainPos lastpos = PosAtLastRefreshByUnit.get( unit );
            if( lastpos == null ) {
               UpdateLosForUnit( unit );
               numupdates++;
            }
         }
      }
      //   logfile.WriteLine( "LosMap end incrementalrefreshes, " + numupdates + " units refreshed" );
   }

   // right, this used to just update every few frames,
   // but in JNA, that's quite slow, since getPos() takes about 0.2 milliseconds to
   // run, which is a lot, for every mobile unit every frame
   // so now, what we do is: just run this once a second or so,
   // and we interpolate between the last position and this one
   void UpdateLosForUnit( Unit unit )
   {
      //      logfile.WriteLine( "Updating los for unit " + unit.getUnitId() + " " + unit.getDef().getHumanName() );
      int frame = playerObjects.getFrameController().getFrame();
      TerrainPos pos = unitcontroller.getPos( unit );
      TerrainPos lastpos = PosAtLastRefreshByUnit.get( unit );
      UnitDef unitdef = unit.getDef();
//      int seenmapx = (int)( pos.x / 16 );
//      int seenmapy = (int)( pos.z / 16 );
      if( unitdef == null ) {
         PosAtLastRefreshByUnit.remove(unit);
         this.LastLosRefreshFrameCountByUnit.remove(unit);
         return;
      }
      int radius = (int)unitdef.getLosRadius();
      if( csai.DebugOn )
      {
         //aicallback.SendTextMsg( "Updating los for " + unitid + " " + unitdef.getHumanName() + " los radius " + radius, 0 );
         //drawingUtils.DrawCircle( pos, radius * 16 );
      }
      //logfile.WriteLine( "Updating los for " + unit.getUnitId() 
      //		+ " " + unitdef.getHumanName()
      //		+ " los radius " + radius
      //		+ " pos " + pos.toString() );
      //

      if( lastpos != null ) {
         // interpolate between last point and now, if necessary
         float distanceFromLastPoint = (float)Math.sqrt(
               pos.GetSquaredDistance( lastpos ) );
         int numberInterpolations = 1 + (int)( distanceFromLastPoint / config.getLosMapInterpolationDistance() );
         // let's think about possibilities:
         // distancefromlastpoint < losmapinterpolationinterval: number = 1
         // distancefromlastpoint == losmapinterpolationinterval: number = 2
         // distancefromlastpoint > losmapinterpolationinterval: number = 2
         // distancefromlastpoint >> losmapinterpolationinterval: number > 2 

         TerrainPos interpolationVector = ( pos.subtract( lastpos ) )
            .divide( numberInterpolations );

         //      logfile.WriteLine( "number interpolations: " + numberInterpolations + " interpolationvector: " + interpolationVector );

         // if numberInterpolations = 1, we want the loop to run once,
         // with interpolation = 1
         // interpolation = 0 was already handled by last refresh
         // so we just start from interpolation = 1
         for( int interpolation = 1; interpolation < numberInterpolations; interpolation++ ) {
            TerrainPos thispos = lastpos.add( 
                  interpolationVector.multiply( interpolation ) );
            MarkInCurrentLos( frame, thispos, radius );
            //         logfile.WriteLine( "    interpolations " + interpolation + " " + thispos );

            // you know, we could just do this for the first and last circle,
            // and then just draw a rectangle between the two?
            // that would be more accurate too...
         }
      } else {
         MarkInCurrentLos( frame, pos, radius );
      }
      LastLosRefreshFrameCountByUnit.put( unit, frame );
      PosAtLastRefreshByUnit.put( unit, pos );
      //      logfile.WriteLine( "...done" );
   }

   // go line by line, determine positive and negative extent of line, mark lostime
   void MarkInCurrentLos( int frame, TerrainPos pos, int radius ) {
      LosMapPos losMapPos = LosMapPos.fromTerrainPos( pos );
      for( int deltay = -radius; deltay <= radius; deltay++ )
      {
         int xextent = (int)Math.sqrt( radius * radius - deltay * deltay );
         for( int deltax = -xextent; deltax <= xextent; deltax++ )
         {
            int thisx = losMapPos.x + deltax;
            int thisy = losMapPos.y + deltay;
            if( thisx >= 0 && thisx < mapwidth / granularity && thisy >= 0 && thisy < mapheight / granularity )
            {
               LastSeenFrameCount[ thisx][ thisy ] = frame;
            }
         }
      }      
   }

   void TotalRefresh()
   {
      logfile.WriteLine( "LosMap start totalrefresh" );
      for( Unit unit : unitcontroller.units ) {
         UpdateLosForUnit( unit );
      }
      logfile.WriteLine( "LosMap end totalrefresh" );
   }

   class UnitControllerHandler extends UnitController.UnitAdapter {
      @Override
      public void UnitAdded( Unit unit )
      {
         if( !LastLosRefreshFrameCountByUnit.containsKey( unit ) )
         {
            //LastLosRefreshFrameCountByUnit.put( unit, aicallback.getGame().getCurrentFrame() );
            //PosAtLastRefreshByUnit.put( unit, unitcontroller.getPos( unit ) );
            UpdateLosForUnit( unit );
         }
      }

      @Override
      public void UnitRemoved( Unit unit )
      {
         //if( LastLosRefreshFrameCountByUnit.containsKey( unit ) )
         //{
         LastLosRefreshFrameCountByUnit.remove( unit );
         PosAtLastRefreshByUnit.remove( unit );
         //}
      }
   }

   class GameListenerHandler extends GameAdapter {
      @Override
      public void Tick( int frame )
      {
         //         if( frame % 10 == 0 ) {  // just occasionally for now, to avoid slowing game down too much...
         DoIncrementalRefreshes();
         //         }

         if( frame - lasttotalrefresh > config.getLosrefreshallintervalframecount() )
         {
            logfile.WriteLine( "losmap: doing total refresh" );
            TotalRefresh();
            lasttotalrefresh = frame;
         }
      }
   }
}
