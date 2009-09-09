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

package hughai.controllers.level1;

import java.util.*;
import java.util.Map;


import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.*;
import hughai.basictypes.*;
import hughai.*;
import hughai.mapping.*;
import hughai.packcoordinators.*;
import hughai.unitdata.*;
import hughai.unitdata.UnitController.UnitAdapter;
import hughai.utils.*;
import hughai.controllers.level2.*;

// verbal contract with higher level:
// This class, by being instantiated, is essentially being told
// "You, go and handle reconnaissance.  Do as you like.
// You can use any scout units you need, as I build them
// Note: for now, central workflow class is responsible for deciding
// what to build and when
//
// implementation notes:
// we need a list of scout types, eg jeffy, and those tiny surveillance planes
// maybe those tiny boats if it's water
// our job is to maximize loscoverage at all time, using some heuristic to
// judge this
// maybe we can also be told "over here is a good place to check"
// anywhere with a high threat level / lots of enemy, maybe we just scout
// around the edge, rather than going in
// though perhaps we can use those little bird planes if we have enough of them
// for example if the amount of metal we are producing is really high compared
// to the cost of the small bird planes
public class Reconnaissance {
   final float agingfactor = 1f / 600f; // 1f/ 600f means ages over 10 minutes, ish
   
   PlayerObjects playerObjects;
   LogFile logfile;
   Config config;
   BuildTable buildTable;
   UnitController unitController;
   
   ScoutControllerRaider scoutcontrollerraider;
   Collection<UnitDef> managedUnitTypes = new HashSet<UnitDef>();
   Collection<Unit> managedunits = new HashSet<Unit>();
   
   public Reconnaissance( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
      this.logfile = playerObjects.getLogFile();
      this.config = playerObjects.getConfig();
      this.buildTable = playerObjects.getBuildTable();
      this.unitController = playerObjects.getUnitController();
   }
   
   public void Activate(){
      playerObjects.getCSAI().registerGameListener( new GameListener() );
      unitController.registerListener( new UnitListener() );
      
      List<String> unittypenames = config.getReconnaissanceunitnames();
      managedUnitTypes.clear();
      for( String unittypename : unittypenames ) {
         UnitDef unitdef = buildTable.getUnitDefByName( unittypename );
         managedUnitTypes.add( unitdef );
         logfile.WriteLine( "Allowed unit type for " + this.getClass().getSimpleName()
               + " " + unitdef.getHumanName() );
      }
      
      scoutcontrollerraider = new ScoutControllerRaider( playerObjects );
      scoutcontrollerraider.Activate();
      //scoutcontroller.AssignUnits( units )
   }
   
   void Reappraise( int frame ) {
      if( frame % 150 == 0) {
         getLosCoverageStatistic( frame );
      }
   }
   
   // how should this statistic behave?
   // should prioritize, and reward, searching areas that haven't been checked
   // for ages, or ever, over areas that were searched more recently
   // we could simply make this linear, or we could make it more polynomial
   // or exponential
   // let's make it linear for now perhaps
   // probably good if the number is positive rather than negative,
   // but not essential
   // we could have zero as total loscoverage, up to date
   // or 100 for that
   // we could always normalize later somehow, unless the measure goes
   // down to infinity, where we cannot.
   float getLosCoverageStatistic( int frame ) {
      logfile.WriteLine( "getLosCoverageStatistic running" );
      
      LosMap losMap = playerObjects.getMaps().getLosMap();
//      int[][]lastseenframecountmap = losMap.LastSeenFrameCount;
      
      // how about we give 1 point for each point fully up to date
      // and then reduce that as the point gets older?
      // 
      int numpoints = 0;
      float nonnormalizedscore = 0;
      for( int z = 0; z < losMap.getLosMapHeight(); z++ ) {
         for( int x = 0; x < losMap.getLosMapWidth(); x++ ) {
            LosMap.LosMapPos losMapPos = new LosMap.LosMapPos( x, z );
//            int lastseen = lastseenframecountmap[x][z];
            int lastseen = losMap.getLastSeenFrameCount( losMapPos );
            float thisvalue = 0;
            if( lastseen != 0 ) {
               int age = frame - lastseen;
               thisvalue = Math.max( 0, 1 - age * agingfactor ); // clamp so can't go below zero
            }
            nonnormalizedscore += thisvalue;
            numpoints++;
         }         
      }
      float loscoveragestatistic = nonnormalizedscore * 100 / numpoints;
      playerObjects.getLogFile().WriteLine( "getLosCoverageStatistic:" + loscoveragestatistic );  
      //logfile.WriteLine( "getLosCoverageStatistic running" );
      return loscoveragestatistic;
   }
   
   class GameListener extends GameAdapter {
      @Override
      public void Tick( int frame ) {
//         if( frame % 30 == 0 ) {
            Reappraise( frame );
//         }
      }
   }
   
   class UnitListener extends UnitAdapter {
      @Override
      public void UnitAdded(Unit unit ) {
         UnitDef unitdef = unit.getDef();
         if( managedUnitTypes.contains( unitdef ) ) {
            logfile.WriteLine( "New reconnaissance unit: " +
                  unit.getUnitId() + " " + unit.getDef().getHumanName() );
            managedunits.add( unit );
            scoutcontrollerraider.AssignUnits( 
                  Arrays.asList( new Unit[]{ unit } ) );
         }
      }

      @Override
      public void UnitRemoved( Unit unit ) {
         UnitDef unitdef = unit.getDef();
         if( managedUnitTypes.contains( unitdef ) ) {
            managedunits.remove( unit );
            scoutcontrollerraider.RevokeUnits(  
                  Arrays.asList( new Unit[]{ unit } ) );
         }
      }
   }
}
