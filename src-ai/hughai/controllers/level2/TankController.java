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
public class TankController
{
   public int MinTanksForAttack = 5;
   public int MinTanksForSpreadSearch = 50;

   //	public HashMap< Integer,UnitDef> DefsById = new HashMap< Integer,UnitDef>();
   HashSet<Unit> unitsToControl = new HashSet<Unit>();
   UnitDef typicalunitdef;

   //String[]UnitsWeLike = new String[]{ "armsam", "armstump", "armrock", "armjeth", "armkam", "armanac", "armsfig", "armmh", "armah", 
   //   "armbull", "armmart", "armmav", "armyork" };

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

   public TankController( PlayerObjects playerObjects, 
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
//            csai.RegisterVoiceCommand("tanksattackpos", new VoiceCommandAttackPos());
//            csai.RegisterVoiceCommand( "tankscancelattack", new VoiceCommandCancelAttack() );
            csai.RegisterVoiceCommand("dumptanks", new VoiceCommandDumpTanksHandler());
         }

         //PackCoordinatorSelector.Activate();
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

         //unitcontroller.UnitAddedEvent -= new UnitController.UnitAddedHandler( UnitAdded );
         //unitcontroller.UnitRemovedEvent -= new UnitController.UnitRemovedHandler( UnitRemoved );

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

//   class VoiceCommandAttackPos implements VoiceCommandHandler {
//      @Override
//      public void commandReceived( String voiceString, String[] splitchatString, int player )
//      {
//         //int targetteam = Convert.ToInt32( splitchatString[2] );
//         //if( targetteam == csai.Team )
//         //{
//         Float3 targetpos = new Float3();
//         targetpos.x = Float.parseFloat( splitchatString[2] );
//         targetpos.z = Float.parseFloat( splitchatString[3] );
//         targetpos.y =  aicallback.getMap().getElevationAt( (float)targetpos.x, (float)targetpos.z );
//         LastAttackPos = targetpos;
////         forceattacking = true;
//
//         attackpackcoordinator.SetTarget( targetpos );
//         packcoordinatorselector.ActivatePackCoordinator( attackpackcoordinator );
//         //}
//      }
//   }

//   class VoiceCommandCancelAttack implements VoiceCommandHandler {
//      @Override
//      public void commandReceived( String voiceString, String[] splitchatString, int player )
//      {
////         forceattacking = false;
//      }
//   }

   //boolean WeLikeUnit( String name )
   //{
   //  for( String unitwelike : UnitsWeLike )
   //{
   //  if( unitwelike == name )
   //{
   //  return true;
   //}
   //}
   //return false;
   //}

   class GameListenerHandler extends GameAdapter {
//      int tickcount = 0;

//      int itick = 0;
      @Override
      public void Tick( int frame )
      {
//         tickcount++;
//         itick++;
//         if( itick >= 30 )
//         {
            DoSomething();
//            itick = 0;
//         }
      }

      @Override
      // shifted this to unitidle to give unit time to leave factory
      public void UnitIdle( Unit unit )
      {
         //if( !DefsById.Contains( id ) )
         //{
         //  UnitDef unitdef = aicallback.getUnitDef( id );
         //if( WeLikeUnit( unitdef.getName().toLowerCase() ) )
         //{
         //  logfile.WriteLine( "TankController new tank " + " id " + id + " " + unitdef.getHumanName() + " speed " + unitdef.speed );
         //TankDefsById.add( id, unitdef );
         //   DoSomething();
         //}
         //}
      }
   }

   class UnitControllerHandler extends UnitController.UnitAdapter {
      @Override
      public void UnitAdded( Unit unit )
      {
         /*
            // make this more elegant later, with concept of ownership etc
            logfile.WriteLine("tankcontroller.UnitAdded " + unitdef.getHumanName());
            if( WeLikeUnit( unitdef.getName().toLowerCase() ) )
            {
                logfile.WriteLine("unit in tanktype list");
                if (!TankDefsById.Contains(id))
                {
                    logfile.WriteLine("TankController new tank " + " id " + id + " " + unitdef.getHumanName() + " speed " + unitdef.speed);
                    TankDefsById.add( id, unitdef );
                    if (LastAttackPos == null)
                    {
                        LastAttackPos = aicallback.GetUnitPos(id);
                    }
                    enemyselector.InitStartPos(aicallback.GetUnitPos(id));
                  //  aicallback.GiveOrder( id, new Command( Command.CMD_TRAJECTORY, new double[]{1}) );
                 //   DoSomething();
                }
            }
          */
      }

      @Override
      public void UnitRemoved( Unit unit )
      {
         /*
            if( TankDefsById.Contains( id ) )
            {
                logfile.WriteLine( "TankController tank removed " + id );
                TankDefsById.remove( id );
               // DoSomething();
            }
          */
      }
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

   // note to self: this function is a mess... spaghetti... unreadable...
   void DoSomething()
   {
      String commanderunitname = playerObjects.getBuildTree().listToOurTeamsUnitName( config.getCommanderunitnames() );
      if( unitsToControl.size() >= MinTanksForAttack ) // make sure at least have a few units before attacking
      {
         // ok, so first, we're going to figure out roughly where
         // our units are, or where we were attacking before
         // then we'll ask for some enemies near that
         TerrainPos approximateattackpos = LastAttackPos;
         if( approximateattackpos == null ) {
            for( Unit unit : unitsToControl )
            {
               TerrainPos thispos = unitcontroller.getPos( unit );
               if (thispos != null)
               {
                  approximateattackpos = thispos;
               }
            }
            //approximateattackpos = aicallback.GetUnitPos(DefsById.Keys.GetEnumerator().Current);
         }
         logfile.WriteLine("tankcontroller approximateattackpos: " + approximateattackpos);
         // now we ask for an enemy: 
         TerrainPos nearestenemypos = enemyselector.ChooseAttackPoint( approximateattackpos );
         if( nearestenemypos != null )
         {
            // we got an enemy, so attack...
            logfile.WriteLine("tankcontroller found enemy, attacking: " + nearestenemypos);
            attackpackcoordinator.SetTarget( nearestenemypos );
            packcoordinatorselector.ActivatePackCoordinator( attackpackcoordinator );
            LastAttackPos = nearestenemypos;
         }
         else
         {
            // otherwise search
            if( unitsToControl.size() > MinTanksForSpreadSearch )
            {
               spreadsearchpackcoordinator.SetTarget( nearestenemypos );
               packcoordinatorselector.ActivatePackCoordinator( spreadsearchpackcoordinator );
            }
            else // or if we're too weak, jsut guard the commander
            {
               if (unitcontroller.UnitsByName.containsKey(commanderunitname ))
               {
                  guardpackcoordinator.SetTarget(unitcontroller.UnitsByName.get( commanderunitname ).get( 0 ) );
                  packcoordinatorselector.ActivatePackCoordinator(guardpackcoordinator);
               }
            }
         }
      }
      else // else, not enough tanks to attack, just guard commander, or something
      {
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
