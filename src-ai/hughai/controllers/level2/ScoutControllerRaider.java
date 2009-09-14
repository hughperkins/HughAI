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

import hughai.CSAI;
import hughai.EnemyTracker;
import hughai.GameAdapter;
import hughai.GiveOrderWrapper;
import hughai.PlayerObjects;
import hughai.VoiceCommandHandler;
import hughai.EnemyTracker.EnemyAdapter;
import hughai.basictypes.*;
import hughai.mapping.*;
import hughai.packcoordinators.*;
import hughai.unitdata.*;
import hughai.utils.*;


public class ScoutControllerRaider
{
   public int nearbyforenemiesmeans = 250;
   public int enemysightedsearchradius = 1000;

   CSAI csai;
   OOAICallback aicallback;
   LogFile logfile;
   EnemyTracker enemyTracker;
   GiveOrderWrapper giveOrderWrapper;
   UnitController unitController;
   Maps maps;
   Config config;

   //   SpreadSearchPackCoordinatorWithSearchGrid searchcoordinator;
   SpreadSearchPackCoordinatorWithSearchGrid searchcoordinator;

   List< Unit > ScoutUnits = new ArrayList< Unit>();
   HashSet< Unit > movefailed = new HashSet<Unit>(); 

   Random random;

   public ScoutControllerRaider( PlayerObjects playerObjects )
   {
      csai = playerObjects.getCSAI();
      aicallback = csai.aicallback;
      logfile = playerObjects.getLogFile();
      //unitcontroller = playerObjects.getUnitController();
      enemyTracker = playerObjects.getEnemyTracker();
      giveOrderWrapper = playerObjects.getGiveOrderWrapper();
      maps = playerObjects.getMaps();
      unitController = playerObjects.getUnitController();
      config = playerObjects.getConfig();

      random = new Random();

      //      searchcoordinator = new SpreadSearchPackCoordinatorWithSearchGrid( 
      //            playerObjects);
      searchcoordinator = new SpreadSearchPackCoordinatorWithSearchGrid( 
            playerObjects);

      logfile.WriteLine( "*ScoutControllerRaider initialized*" );
   }

   boolean Active = false;

   public void Activate()
   {
      if( !Active )
      {
         logfile.WriteLine( "activating " + this.getClass().getSimpleName() );

         csai.registerGameListener(new GameListenerHandler());
         csai.RegisterVoiceCommand( "countscouts", new VoiceCommandCountScouts() );

         //         enemyTracker.registerListener( new EnemyTrackerHandler() );

         searchcoordinator.Activate();
         Active = true;
      }
   }

   public void Disactivate()
   {
      if( Active )
      {
         csai.unregisterGameListener(new GameListenerHandler());
         csai.UnregisterVoiceCommand( "countscouts" );

         //         enemyTracker.unregisterGameListener( new EnemyTrackerHandler() );

         searchcoordinator.Disactivate();
         Active = false;
      }
   }

   // planned, controller can control any units at its discretion, for now:
   public void AssignUnits( Collection<Unit> units ){
      for( Unit unit : units ) {
         ScoutUnits.add( unit );
         logfile.WriteLine( "New scoutcontrollerraider unit: " +
               unit.getUnitId() + " " + unit.getDef().getHumanName() );
         searchcoordinator.AssignUnits( units );
      }
   }  // give units to this controller
   public void RevokeUnits( Collection<Unit> units ){
      for( Unit unit : units ) {
         ScoutUnits.remove( unit );
         searchcoordinator.RevokeUnits( units );
      }	   
   }  // remove these units from this controller

   // planned, not used yet, controller can use energy and metal at its discretion for now:
   public void AssignEnergy( int energy ){} // give energy to controller; negative to revoke
   public void AssignMetal( int metal ){} // give metal to this controller; negative to revoke
   public void AssignPower( double power ){} // assign continuous power flow to this controller; negative for reverse flow
   public void AssignMetalStream( double metalstream ){} // assign continuous metal flow to this controller; negative for reverse flow        

   class VoiceCommandCountScouts implements VoiceCommandHandler {
      @Override
      public void commandReceived( String cmdline, String[]splitString, int player )
      {
         csai.SendTextMsg( "scouts: " + ScoutUnits.size() );
      }
   }

   void ExploreWith( Unit unit )
   {
      TerrainPos destination = new TerrainPos();
      destination.x = random.nextFloat() * aicallback.getMap().getWidth() * maps.getMovementMaps().SQUARE_SIZE;
      destination.z = random.nextFloat() * aicallback.getMap().getHeight() * maps.getMovementMaps().SQUARE_SIZE;
      destination.y = maps.getHeightMap().getElevationAt( destination ); // aicallback.getMap().getElevationAt( destination.x, destination.y );
      logfile.WriteLine( "mapwidth: " + aicallback.getMap().getWidth() + " squaresize: " + maps.getMovementMaps().SQUARE_SIZE );
      logfile.WriteLine( "ScoutController sending scout " + unit.getUnitId() + " to " + destination.toString() );
      giveOrderWrapper.MoveTo(unit, destination );
   }

   //   class EnemyTrackerHandler extends EnemyTracker.EnemyAdapter {
   //      @Override
   //      public void AcquiredEnemy( Unit unit )
   //      {
   //      }
   //   }

   void Reappraise()
   {
      //  logfile.WriteLine("reappraise>>>");
      for( Unit scout : ScoutUnits ) {
         TerrainPos scoutpos = unitController.getPos( scout );

         TerrainPos nearestpos = null;
         float bestsquareddistance = 100000000;
         Unit targetenemy = null;
         
         boolean existsPriorityTargets = false;
         for( Unit enemy : enemyTracker.getEnemyUnits() ) {
            UnitDef enemyunitdef = enemyTracker
            .getEnemyUnitDefByUnit()
            .get( enemy );
            if( enemyunitdef != null )
            {               
               if( IsPriorityTarget( enemyunitdef ) )
               {
                  existsPriorityTargets = true;
               }
            }
         }

         // need to add index by position for this, to speed things up
         for( Unit enemy : enemyTracker.getEnemyUnits() ) {
            UnitDef enemyunitdef = enemyTracker
            .getEnemyUnitDefByUnit()
            .get( enemy );
            if( enemyunitdef != null )
            {				
               // if priority targets exist grab those, otherwise target anything
               // if no threat exists
               if( !existsPriorityTargets || IsPriorityTarget( enemyunitdef ) )
               {
                  logfile.WriteLine("considering unit " + enemy.getUnitId() + " " + enemyunitdef.getName());
                  TerrainPos enemypos = TerrainPos.fromAIFloat3( enemy.getPos() );
                  float thissquareddistance = scoutpos.GetSquaredDistance( enemypos);
                  if( thissquareddistance < bestsquareddistance )
                  {
                     if( maps.getThreatMap().getThreatAt( enemypos ) < 1 ) {
                        nearestpos = enemypos;
                        bestsquareddistance = thissquareddistance;
                        targetenemy = enemy;
                     }

                  }
               }
            }
         }
         if( nearestpos != null )
         {
            searchcoordinator.Disactivate();
            giveOrderWrapper.Attack(scout, targetenemy);
            movefailed.remove( scout  );
            if( !attackingscouts.contains( scout ) || movefailed.contains( scout  ) )
            {
               attackingscouts.add( scout );
            }
         }
         else
         {
            if( attackingscouts.contains( scout ) || movefailed.contains( scout ) )
            {
               searchcoordinator.Activate();
               //               ExploreWith( scout );
               attackingscouts.remove( scout );
               movefailed.remove( scout  );
            }
         }
      }
      //     logfile.WriteLine("reappraise<<<");
   }

   //   List<String> prioritytargets = Arrays.asList(new String[] { 
   //    "armmex", "cormex", "armrad", "corrad" });

   boolean IsPriorityTarget(UnitDef unitdef )
   {
      String name = unitdef.getName().toLowerCase();
      return config.getScoutraiderprioritytargets().contains( name );
   }

//   boolean IsLaserTower(UnitDef unitdef )
//   {
//      if( unitdef.getName().toLowerCase() == "armllt" 
//         || unitdef.getName().toLowerCase() == "corllt" 
//            || unitdef.getName().toLowerCase() == "armfrt"
//               || unitdef.getName().toLowerCase() == "corfrt" )
//      {
//         return true;
//      }
//      return false;
//   }

   ArrayList<Unit> attackingscouts = new ArrayList<Unit>(); // scoutid of scouts currently attacking

   class GameListenerHandler extends GameAdapter {
      //      int ticks = 0;
      @Override
      public void Tick( int frame )
      {
         Reappraise();
      }
      @Override
      public void UnitMoveFailed( Unit unit ) {
         movefailed.add( unit );
      }
   }
}
