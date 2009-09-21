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

package hughai.controllers.level2;

import java.util.*;
import java.util.Map;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.*;
import hughai.EnemyTracker.EnemyAdapter;
import hughai.basictypes.*;
import hughai.mapping.*;
import hughai.packcoordinators.*;
import hughai.unitdata.*;
import hughai.unitdata.UnitController.UnitAdapter;
import hughai.utils.*;


// this will ultimately be migrated to tactics directory as groundmelee or such
// for now it's a quick way of getting a battle going on!
//
// comapred to tankcontroller, tankcontroller2 was made to start using
// the threatmap a bit:
//  -  to decide whethre to attack an enemy
//  - to decide whethre to attack at all
// probalby need to add better heuristics for this...
public class TankController2
{
   public int MinTanksForAttack = 5;
   public int MinTanksForSpreadSearch = 50;

   HashSet<Unit> unitsToControl = new HashSet<Unit>();
   UnitDef typicalunitdef;

   CSAI csai;
   OOAICallback aicallback;
   LogFile logfile;

   PlayerObjects playerObjects;
   UnitController unitcontroller;
   EnemyTracker enemyTracker;
   EnemySelector2 enemyselector;
   BuildTable buildtable;

   AttackPackCoordinator attackpackcoordinator;
   GuardPackCoordinator guardpackcoordinator;
   SpreadSearchPackCoordinatorWithSearchGrid spreadsearchpackcoordinator;
   MoveToPackCoordinator movetopackcoordinator;

   PackCoordinatorSelector packcoordinatorselector;
   Config config;

   //   boolean forceattacking = false; // for debugging
   TerrainPos LastAttackPos = null;

   Random random = new Random();

   public TankController2( PlayerObjects playerObjects, 
         UnitDef typicalunitdef)
   {
      this.playerObjects = playerObjects;

      config = playerObjects.getConfig();
      csai = playerObjects.getCSAI();
      aicallback = csai.aicallback;
      logfile = playerObjects.getLogFile();
      enemyTracker = playerObjects.getEnemyTracker();
      unitcontroller = playerObjects.getUnitController();

      //      this.unitsToControl = unitsToControl;
      this.typicalunitdef = typicalunitdef;

      enemyselector = new EnemySelector2( playerObjects, typicalunitdef.getSpeed() * 2, typicalunitdef );
      // speed here is experimental

      attackpackcoordinator = new AttackPackCoordinator( playerObjects );
      spreadsearchpackcoordinator = new SpreadSearchPackCoordinatorWithSearchGrid( playerObjects );
      movetopackcoordinator = new MoveToPackCoordinator( playerObjects);
      guardpackcoordinator = new GuardPackCoordinator( playerObjects);

      packcoordinatorselector = new PackCoordinatorSelector( playerObjects );
      packcoordinatorselector.LoadCoordinator( attackpackcoordinator );
      packcoordinatorselector.LoadCoordinator( spreadsearchpackcoordinator );
      packcoordinatorselector.LoadCoordinator( movetopackcoordinator );
      packcoordinatorselector.LoadCoordinator( guardpackcoordinator );

      logfile.WriteLine( "*TankController Initialized*" );
   }

   boolean Active = false;

   public void Activate()
   {
      if( !Active )
      {
         enemyTracker.registerListener( new EnemyTrackerHandler() );
         csai.registerGameListener( new GameListenerHandler() );

         if (csai.DebugOn)
         {
            csai.RegisterVoiceCommand("tankscount", new VoiceCommandCountTanks() );
            csai.RegisterVoiceCommand("tanksmoveto", new VoiceCommandMoveTo());
            csai.RegisterVoiceCommand("dumptanks", new VoiceCommandDumpTanksHandler());
         }

         Active = true;
      }
   }

   class VoiceCommandDumpTanksHandler implements VoiceCommandHandler {
      @Override
      public void commandReceived(String cmd, String[] split, int player) {
         logfile.WriteLine("Tankcontroller dump:");
         for( Unit unit : unitsToControl ) {
            logfile.WriteLine( unit.getUnitId() + " " + unit.getDef().getHumanName() );
         }
      }
   }

   public void Disactivate()
   {
      if( Active )
      {
         enemyTracker.unregisterGameListener( new EnemyTrackerHandler() );
         csai.unregisterGameListener(new GameListenerHandler());

         csai.UnregisterVoiceCommand( "tankscount" );
         csai.UnregisterVoiceCommand( "tanksmoveto" );
         csai.UnregisterVoiceCommand( "tanksattackpos" );

         packcoordinatorselector.DisactivateAll();
         Active = false;
      }
   }

   // planned, controller can control any units at its discretion, for now:
   public void AssignUnits( Collection<Unit> units ){
      for( Unit unit : units ) {
         unitsToControl.add( unit );
         //searchcoordinator.
         logfile.WriteLine( "New tankcontroller unit: " +
               unit.getUnitId() + " " + unit.getDef().getHumanName() );
         attackpackcoordinator.AssignUnits( units );
         spreadsearchpackcoordinator.AssignUnits( units );
         movetopackcoordinator.AssignUnits( units );
         guardpackcoordinator.AssignUnits( units );
         //searchcoordinator.ExploreWith( unit );
      }
   }  // give units to this controller
   public void RevokeUnits( Collection<Unit> units ){
      for( Unit unit : units ) {
         unitsToControl.remove( unit );
         attackpackcoordinator.RevokeUnits( units );
         spreadsearchpackcoordinator.RevokeUnits( units );
         movetopackcoordinator.RevokeUnits( units );
         guardpackcoordinator.RevokeUnits( units );
      }    
   }  // remove these units from this controller

   // planned, not used yet, controller can use energy and metal at its discretion for now:
   public void AssignEnergy( int energy ){} // give energy to controller; negative to revoke
   public void AssignMetal( int metal ){} // give metal to this controller; negative to revoke
   public void AssignPower( double power ){} // assign continuous power flow to this controller; negative for reverse flow
   public void AssignMetalStream( double metalstream ){} // assign continuous metal flow to this controller; negative for reverse flow        

   class VoiceCommandCountTanks implements VoiceCommandHandler
   {
      @Override
      public void commandReceived( String voiceString, String[] splitchatString, int player ) {
         csai.SendTextMsg( "Number tanks: " + unitsToControl.size() );
         logfile.WriteLine( "Number tanks: " + unitsToControl.size() );
      }
   }

   class VoiceCommandMoveTo implements VoiceCommandHandler {
      @Override
      public void commandReceived( String voiceString, String[] splitchatString, int player )
      {
         //   int targetteam = Convert.ToInt32( splitchatString[2] );
         //if( targetteam == csai.Team )
         // {
         TerrainPos targetpos = new TerrainPos();
         targetpos.x = Float.parseFloat( splitchatString[2] );
         targetpos.z = Float.parseFloat( splitchatString[3] );
         targetpos.y = playerObjects.getMaps().getHeightMap().getElevationAt( targetpos ); // aicallback.getMap().getElevationAt( targetpos.x, targetpos.z );

         //  GotoPos( targetpos );
         movetopackcoordinator.SetTarget( targetpos );
         packcoordinatorselector.ActivatePackCoordinator( movetopackcoordinator );
         //}
      }
   }

   class GameListenerHandler extends GameAdapter {
      @Override
      public void Tick( int frame )
      {
         DoSomething();
      }
   }

   class UnitControllerHandler extends UnitController.UnitAdapter {
      // very useful listener this ;-)
   }

   class EnemyTrackerHandler extends EnemyTracker.EnemyAdapter {
      @Override
      public void AcquiredEnemy( Unit unit )
      {
         //DoSomething();
      }

      @Override
      public void EnemyDestroyed( Unit unit )
      {
         //DoSomething();
      }
   }

   // just take median along each axis?
   TerrainPos getmedianfriendlypos() {
      int numunits = unitsToControl.size();
      if( numunits == 0) return null;
      float[] xposes = new float[numunits];
      float[] zposes = new float[numunits];
      int index = 0;
      for( Unit unit : unitsToControl ) {
         TerrainPos pos = unitcontroller.getPos( unit );
         xposes[index] = pos.x;
         zposes[index] = pos.z;
         index++;
      }
      float xmedian = xposes[ numunits / 2 ]; // so, if 1 unit, this will be index 0, which is valid
      float zmedian = zposes[ numunits / 2 ];
      TerrainPos terrainPos = new TerrainPos( xmedian, 0, zmedian );
      terrainPos.y = playerObjects.getMaps().getHeightMap().getElevationAt( terrainPos );
      return terrainPos;
   }

   void DoSomething()
   {
      // what we want to write:
      // go through enemy units
      // attack the one close to us that has low threatmap rating
      // ideally: take out high priority targets first, like ... factories? metal ex? enemies?
      //     -> think about the prioritzation by unit def later
      // also, we have to figure out what we can attack, based on how many
      // tansks and. ... their collective power?

      float attackpoweravailable = 0;
      for( Unit unit : unitsToControl ) {
         attackpoweravailable += unitcontroller.getUnitDef( unit ).getPower();
      }
      logfile.WriteLine( "Tankcontroller: attack power available: " + attackpoweravailable );

      TerrainPos medianfriendlypos = getmedianfriendlypos();

      // how to convert this to threatmap stuff? :-O  Maybe if our power is three times
      // threatmap damagepersecond?  five times?
      // run with three times for now, that's the standard for armies I think?

      // this probably should be in enemy selector, but whatever..
      ThreatMap threatMap = playerObjects.getMaps().getThreatMap();
      Unit enemytotarget = null;
      TerrainPos bestenemypos = null;
      float nearestdistancesquared = Float.POSITIVE_INFINITY;
      for( Unit enemy : enemyTracker.getEnemyUnits() ) {
         TerrainPos enemypos = enemyTracker.getPos( enemy );
         if( enemypos != null ) {
            UnitDef enemydef = enemyTracker.getEnemyUnitDefByUnit().get( enemy );
            if( enemydef.getSpeed() < 2 * typicalunitdef.getSpeed() ) {
               if( threatMap.getThreatAt( enemypos ) * 3 < attackpoweravailable ) {
                  // targetable...
                  // what next?
                  // compare with other things somehow?
                  // just look for nearest?

                  // look for nearest..
                  float distancesquared = medianfriendlypos.GetSquaredDistance( enemypos );
                  if( distancesquared < nearestdistancesquared ) {
                     nearestdistancesquared = distancesquared;
                     enemytotarget = enemy;
                     bestenemypos = enemypos;
                  }
               }
            }
         }
      }

      if( enemytotarget != null ) { // found a vaqlid enemy
         logfile.WriteLine("tankcontroller found enemy, attacking: " + bestenemypos);
         attackpackcoordinator.SetTarget( bestenemypos );
         packcoordinatorselector.ActivatePackCoordinator( attackpackcoordinator );
         LastAttackPos = bestenemypos;
      } else { // else, not enough tanks to attack, just guard commander, or something
         // otherwise search
         if( unitsToControl.size() > MinTanksForSpreadSearch ) {
            spreadsearchpackcoordinator.SetTarget( medianfriendlypos );
            packcoordinatorselector.ActivatePackCoordinator( spreadsearchpackcoordinator );
         } else {// or if we're too weak, jsut guard the commander
   
            String commanderunitname = playerObjects.getBuildTree().listToOurTeamsUnitName( config.getCommanderunitnames() );
            if (unitcontroller.UnitsByName.containsKey(commanderunitname))
            {
               List<Unit> commanders = unitcontroller.UnitsByName.get( commanderunitname );
               if( commanders.size() > 0 ) {
                  guardpackcoordinator.SetTarget(commanders.get( 0 ) );
                  packcoordinatorselector.ActivatePackCoordinator(guardpackcoordinator);
               }
            }
         }
      }
   }
}
