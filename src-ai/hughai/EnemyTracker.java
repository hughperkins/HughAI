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

package hughai;

import java.util.*;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.basictypes.*;
import hughai.basictypes.Float3.*;
import hughai.ui.MainUI;
import hughai.unitdata.*;
import hughai.utils.*;


// responsible for tracking enemy units
// Note that the UnitDefs of units outside of LOS are not generally available, so we have to cope with the possibility of null UnitDefs floating around...
// Ditto for poses
// This should be a lowlevel class ,though it's not entirely at the moment ;-)
// we really need a couple of abstraction layers over the top of this:
// - threat evaluation: how quickly a unit will take damage in each area
// - target evaluation: where can we find lots of targets?
// - some sort of evaluator of the pay-off between the two, maybe running over the top of
//   these first two abstractions
// for now we just hack everything into here to get something working
public class EnemyTracker
{
   public static interface EnemyListener {
      public void AcquiredEnemy( Unit enemy );
      public void AcquiredEnemyUnitDef( Unit enemy, UnitDef unitdef );
      public void AcquiredEnemyPos( Unit enemy, TerrainPos pos );
      public void AcquiredStaticEnemy( Unit enemy, UnitDef unitdef, TerrainPos pos );
      public void EnemyDestroyed( Unit enemy );		
   }
   public static class EnemyAdapter implements EnemyListener {
      @Override
      public void AcquiredEnemy( Unit enemy ){}
      @Override
      public void AcquiredEnemyUnitDef( Unit enemy, UnitDef unitdef ){}
      @Override
      public void AcquiredEnemyPos( Unit enemy, TerrainPos pos ){}
      @Override
      public void AcquiredStaticEnemy( Unit enemy, UnitDef unitdef, TerrainPos pos ){}
      @Override
      public void EnemyDestroyed( Unit enemy ){}		
   }

   List<EnemyListener> listeners = new ArrayList<EnemyListener>();

   HashSet<Unit> EnemyUnits = new HashSet<Unit>();
   HashMap<Unit,TerrainPos> EnemyPosByStaticUnit = new HashMap<Unit, TerrainPos>();
   HashMap<Unit,UnitDef> EnemyUnitDefByUnit = new HashMap<Unit, UnitDef>();

   HashMap<Unit,TerrainPos> DynamicEnemyLastSeenPos = new HashMap<Unit, TerrainPos>();
   HashMap<Unit,Integer> DynamicEnemyLastSeenFrame = new HashMap<Unit, Integer>();

   //	public HashMap< Integer, UnitDef> EnemyUnitDefByDeployedId = new HashMap< Integer, UnitDef>();
   //public HashMap< Integer, AIFloat3> EnemyStaticPosByDeployedId = new HashMap< Integer, AIFloat3>();

   PlayerObjects playerObjects;

   CSAI csai;
   OOAICallback aicallback;
   LogFile logfile;
   UnitDefHelp unitdefhelp;
   UnitController unitcontroller;
   DrawingUtils drawingUtils;

   EnemyTracker( PlayerObjects playerObjects )
   {
      this.playerObjects = playerObjects;

      csai = playerObjects.getCSAI();
      aicallback = csai.aicallback;
      logfile = playerObjects.getLogFile();
      drawingUtils = playerObjects.getDrawingUtils();

      autoshowenemies = csai.DebugOn;

      unitcontroller = playerObjects.getUnitController();

      csai.registerGameListener( new GameListenerHandler() );

      csai.RegisterVoiceCommand( "enemiescount", new VoiceCommandCountEnemies() );
      //      csai.RegisterVoiceCommand( "showenemies", new VoiceCommandShowEnemies() );
      csai.RegisterVoiceCommand( "autoshowenemieson", new VoiceCommandAutoShowEnemiesOn() );
      csai.RegisterVoiceCommand( "autoshowenemiesoff", new VoiceCommandAutoShowEnemiesOff() );

      playerObjects.getMainUI().registerButton( "Show enemies", new ButtonShowEnemies() );

      unitdefhelp = playerObjects.getUnitDefHelp();
   }

   public void registerListener( EnemyListener listener ) {
      listeners.add( listener );
   }
   public void unregisterGameListener( EnemyListener targetlistener ) {
      EnemyListener toremove = null;
      for( EnemyListener listener : listeners ) {
         if( listener.getClass() == targetlistener.getClass() ) {
            toremove = listener;
         }
      }
      listeners.remove(toremove );
   }

   class GameListenerHandler extends GameAdapter {
      // To do: handle dynamic units shooting us from beyond los
      // this is basically written and working, just didn't add it back in
      // yet after porting from C#
      @Override
      public void UnitDamaged(Unit damaged, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed )
      {
         /*
			AIFloat3 enemypos = attacker.getPos();
			UnitDef enemydef = attacker.getDef();

			//boolean acquiredunitdef = false;

			//if( enemydef != null ) {
				//EnemyUnitDefByUnit.put( attacker, enemydef );
				//acquiredunitdef = true;
			//}

			if (enemypos != null 
					&& enemypos != new AIFloat3() ) 
					//&& Float3Helper.GetSquaredDistance(enemypos, new AIFloat3())
					//> 10 * 10)
			{
				logfile.WriteLine("unitdamaged, attacker pos " + enemypos);
				//PosByUnit.put(attacker, enemypos);
			}
			else // else we guess...  ok, but this just applies to mobile units, since we 
				// can't detect a static unit until it is in los (otherwise how do we know it is static?)
				// for mobile units, we need to document when it was last seen, by frame or something
				// let's just comment this for now, and do this later
			{
				Float3 ourunitpos = new Float3( damaged.getPos() );
				if (ourunitpos != null)
				{
					Float3 guessvectortotarget = new Float3( dir ).multiply( 300.0 );
					logfile.WriteLine("vectortotarget guess: " + guessvectortotarget.toString());
					Float3 possiblepos = ourunitpos.add( guessvectortotarget );

					PosByUnit.put(attacker, possiblepos);

					for( EnemyListener listener : listeners ) {
						listener.StaticEnemyAdded(attacker, possiblepos.toAIFloat3());
					}
					logfile.WriteLine("unitdamaged, our unit pos " + ourunitpos + " dir " + dir.toString() + " guess: " + possiblepos.toString());
				}
			}
			for( EnemyListener listener : listeners ) {
				listener.StaticEnemyAdded(attacker, enemypos);
			}
          */
      }

      @Override
      public void EnemyEnterRadar( Unit enemy )
      {
         AddEnemy( enemy );
      }

      @Override
      public void EnemyEnterLOS( Unit enemy )
      {
         AddEnemy( enemy );
      }        

      @Override
      public void EnemyLeaveRadar( Unit enemy )
      {
         //RemoveEnemy( enemyid );
      }

      @Override
      public void EnemyDestroyed( Unit enemy, Unit attacker )
      {
         RemoveEnemy( enemy );
      }

      //		int itickcount = 0;
      @Override
      // we need to clean enemies that we thought we knew about, but which
      // no longer exist, assuming their supposed positions are in current los
      public void Tick( int frame )
      {
         //			itickcount++;
         //			//double squaredvisibilitydistance = 40 * 40;
         //			if( itickcount > 30 )
         //			{
         //				itickcount = 0;
         /*
	                foreach (KeyValuePair<int, AIFloat3> kvp in EnemyStaticPosByDeployedId)
	                {
	                    int enemyid = kvp.Key;
	                    AIFloat3 pos = kvp.Value;
	                    if (aicallback.GetCurrentFrame() - LosMap.GetInstance().LastSeenFrameCount[pos.x / 16, pos.z / 16] < 30)
	                    {

	                    }
	                }
          */
         //logfile.WriteLine("EnemyController running static enemy clean" );

         logfile.WriteLine( "enemytracker maphack: " + playerObjects.getConfig().isMapHack() 
                 + " cheating: " + aicallback.getCheats().isEnabled() );
         if( playerObjects.getConfig().isMapHack() ) { // so we cheat ;-)
            if( ! aicallback.getCheats().isEnabled() ) {
               csai.sendTextMessage( "Maphack enabled.  Very alpha ;-)" );
               aicallback.getCheats().setEnabled( true );
            }
            // add all enemy units
            List<Unit> enemyunits = aicallback.getEnemyUnits();
            for( Unit enemy : enemyunits ) {
               AddEnemy( enemy );
            }
            // remove destroyed ones
            List<Unit> unitstopurge = new ArrayList<Unit>();
            for( Unit enemy : EnemyUnits ) {
               if( !enemyunits.contains( enemy ) ) {
                  unitstopurge.add(  enemy );
               }
            }
            for( Unit enemy : unitstopurge ) {
               RemoveEnemy( enemy );
            }
         } else { // not cheating
            if( aicallback.getCheats().isEnabled() ) {
               csai.sendTextMessage( "Maphack disabled." );
               aicallback.getCheats().setEnabled( false );
            }

            for( Unit ourunit : unitcontroller.units ) {
               TerrainPos friendlypos = unitcontroller.getPos( ourunit );
               UnitDef friendlydef = ourunit.getDef();
               ArrayList< Unit > enemiestoclean = new ArrayList< Unit >();
               for( Unit enemy : EnemyPosByStaticUnit.keySet() )
               {
                  TerrainPos enemypos = EnemyPosByStaticUnit.get( enemy );
                  if (enemypos.GetSquaredDistance( friendlypos)
                        < friendlydef.getLosRadius() * friendlydef.getLosRadius()
                        * 16 * 16 ) // because this map is different scale
                  {
                     enemiestoclean.add( enemy );
                  }
               }
               for( Unit enemy : enemiestoclean )
               {
                  RemoveEnemy( enemy );
               }
               
               ArrayList<Unit> unitstopurgepos = new ArrayList<Unit>();
               // clean dynamic ones too.... or at least purge this pos...
               for( Unit enemy : DynamicEnemyLastSeenPos.keySet() ) {
                  TerrainPos enemypos = DynamicEnemyLastSeenPos.get(enemy);
                  if (enemypos.GetSquaredDistance( friendlypos)
                        < friendlydef.getLosRadius() * friendlydef.getLosRadius()
                        * 16 * 16 ) // because this map is different scale
                  {
                     unitstopurgepos.add( enemy );
                  }
               }
               for( Unit enemy : unitstopurgepos )
               {
                  DynamicEnemyLastSeenPos.remove( enemy );
//                  DynamicEnemyLastSeenFrame.remove( enemy ); // should we purge this?  probably not in the long-run, good for now though.
               }
            }

            // at this point, we need to purge old mobile units, at least, their poses...
            // or maybe this should just be in the threatmap, haven't decided really...
//            List<Unit> unitstopurge = new ArrayList<Unit>();
//            for( Unit unit : DynamicEnemyLastSeenFrame.keySet() ) {
//               if( frame - DynamicEnemyLastSeenFrame.get( unit ) 
//                     >  playerObjects.getConfig().getMaxTimeToConserveMobileEnemyOnThreatMapGameSeconds() * 30 ) {
//                  unitstopurge.add(  unit  );
//               }
//            }
//            for( Unit unit : unitstopurge ) {
//               DynamicEnemyLastSeenPos.remove( unit );
////             DynamicEnemyLastSeenFrame.remove( unit );
//            }
            
            // plus, above, we should partially purge mobile units not at that pos too.
            if( csai.DebugOn )
            {
               //ShowEnemies();
            }
         }
      }
      //		}
   }

   boolean autoshowenemies = false;

   public class VoiceCommandAutoShowEnemiesOn implements VoiceCommandHandler {
      @Override
      public void commandReceived( String voiceString, String[] splitchatString, int player )
      {
         autoshowenemies = true;
         ShowEnemies();
      }
   }

   public class VoiceCommandAutoShowEnemiesOff implements VoiceCommandHandler {
      @Override
      public void commandReceived( String voiceString, String[] splitchatString, int player )
      {
         autoshowenemies = false;
      }
   }

   public class VoiceCommandShowEnemies implements VoiceCommandHandler {
      @Override
      public void commandReceived( String voiceString, String[] splitchatString, int player )
      {
         ShowEnemies();
      }
   }

   class ButtonShowEnemies implements MainUI.ButtonHandler {
      @Override
      public void go() {
         ShowEnemies();
      }
   }

   public class VoiceCommandCountEnemies implements VoiceCommandHandler {
      @Override
      public void commandReceived( String voiceString, String[] splitchatString, int player )
      {
         csai.SendTextMsg( "Number enemies: " + EnemyUnits.size() );
         csai.SendTextMsg( "Static enemies: " + EnemyPosByStaticUnit.size() );
         logfile.WriteLine( "Number enemies: " + EnemyUnits.size() );
      }
   }

   void ShowEnemies()
   {
      //      for( Unit enemy : EnemyPosByStaticUnit.keySet() ) {
      int dynamicenemieswithpos = 0;
      int otherenemies = 0;
      int currentframe = playerObjects.getFrameController().frame;
      for( Unit enemy : DynamicEnemyLastSeenPos.keySet() ) {
         TerrainPos staticpos = EnemyPosByStaticUnit.get( enemy );
         if( staticpos != null ) {
            drawingUtils.DrawUnit("ARMAMD", staticpos, 0.0f, 1, aicallback.getTeamId(), true, true);
            drawingUtils.drawText( staticpos, "" + enemy.getUnitId() );
         } else {
            TerrainPos dynamicpos = DynamicEnemyLastSeenPos.get( enemy );
            if( dynamicpos != null ) {
               int lastseenframe = DynamicEnemyLastSeenFrame.get( enemy );
               int frameage = currentframe - lastseenframe;
               int maxframeagetoshow = 30 *
                  playerObjects.getConfig().getMaxTimeToConserveMobileEnemyOnThreatMapGameSeconds();
               if( frameage < maxframeagetoshow ) {
                  drawingUtils.DrawUnit("ARMSAM", dynamicpos, 0.0f, 1, aicallback.getTeamId(), true, true);
                  drawingUtils.drawText( dynamicpos, "" + enemy.getUnitId() );
                  dynamicenemieswithpos++;
               }
            } else {
               otherenemies++;
            }
         }
      }
      logfile.WriteLine( "Number enemies=" + EnemyUnits.size()
            + " Static enemies=" + EnemyPosByStaticUnit.size()
            + " Dynamic enemies with lastpos=" + dynamicenemieswithpos 
            + " Enemies with no known location at all=" + otherenemies );
      //      for( Unit enemy : EnemyUnits ) {
      //         Float3 pos = Float3.fromAIFloat3( enemy.getPos() );
      //         if (pos != null && pos != new Float3() ) {
      //            drawingUtils.DrawUnit("ARMSAM", pos, 0.0f, 50, aicallback.getTeamId(), true, true);
      //         }
      //      }
      //  logfile.WriteLine( "Number enemies: " + EnemyUnitDefByDeployedId.Count );
   }

   public void LoadExistingUnits()
   {
      List<Unit> enemyunits = aicallback.getEnemyUnitsInRadarAndLos();	
      for( Unit enemy : enemyunits )
      {
         logfile.WriteLine("enemy unit existing: " + enemy.getUnitId() );
         AddEnemy( enemy );
      }
   }
   void AddEnemy( Unit enemy )
   {  
      boolean acquiredenemy = false;
      boolean acquiredenemyunitdef = false;
      boolean acquiredenemypos = false;
      boolean acquiredenemystaticpos = false;

      int frame = playerObjects.getFrameController().getFrame();

      if( !EnemyUnits.contains( enemy ))
      {
         EnemyUnits.add( enemy );			
         logfile.WriteLine("EnemyController acquired new enemy: " + enemy.getUnitId() );
         acquiredenemy = true;
      }

      UnitDef enemyunitdef = EnemyUnitDefByUnit.get( enemy );
      if( enemyunitdef == null ) {
         enemyunitdef = enemy.getDef();
         if( enemyunitdef != null ) {
            logfile.WriteLine("acquired unitdef for " + enemy.getUnitId() + " " + enemyunitdef.getName() );
            EnemyUnitDefByUnit.put( enemy, enemyunitdef );
            acquiredenemyunitdef = true;
         }
      }

      TerrainPos enemypos = TerrainPos.fromAIFloat3( enemy.getPos() );
      if( enemypos.equals( new Float3() ) ) {
         enemypos = null; // make null if equals (0,0,0), because makes life easier
      }
      if( enemypos != null 
            && !enemypos.equals( new Float3() ) ) {
         acquiredenemypos =  true;

         DynamicEnemyLastSeenPos.put( enemy, enemypos );
         DynamicEnemyLastSeenFrame.put( enemy, frame );
         // check for static units and get pos
         if( !EnemyPosByStaticUnit.containsKey(enemy) ) {
            if( enemyunitdef != null ) {
               if( !unitdefhelp.IsMobile( enemyunitdef ) ) {
                  EnemyPosByStaticUnit.put( enemy, enemypos );
                  acquiredenemystaticpos = true;
               }
            }
         }

         // do this last, so all data has been updated first
         if( acquiredenemy ) {
            for( EnemyListener listener : listeners ) {
               listener.AcquiredEnemy( enemy );
            }
         }
         if( acquiredenemyunitdef ) {
            for( EnemyListener listener : listeners ) {
               listener.AcquiredEnemyUnitDef(enemy, enemyunitdef);
            }
         }
         if( acquiredenemypos ) {
            for( EnemyListener listener : listeners ) {
               listener.AcquiredEnemyPos(enemy, enemypos);
            }
         }
         if( acquiredenemystaticpos ) {
            for( EnemyListener listener : listeners ) {
               listener.AcquiredStaticEnemy(enemy, enemyunitdef, enemypos);
            }
         }
      }
   }

   void RemoveEnemy( Unit enemy )
   {
      EnemyPosByStaticUnit.remove( enemy );
      EnemyUnitDefByUnit.remove( enemy );	
      EnemyUnits.remove( enemy );
      DynamicEnemyLastSeenFrame.remove( enemy );
      DynamicEnemyLastSeenPos.remove( enemy );
      logfile.WriteLine("EnemyController Enemy removed: " + enemy );
      for( EnemyListener listener : listeners ) {
         listener.EnemyDestroyed(enemy);
      }
   }

   public HashSet<Unit> getEnemyUnits() {
      return EnemyUnits;
   }

   public HashMap<Unit, TerrainPos> getEnemyPosByStaticUnit() {
      return EnemyPosByStaticUnit;
   }

   public TerrainPos getPos( Unit enemyUnit ) {
      return DynamicEnemyLastSeenPos.get( enemyUnit );
   }

   public int getLastLocatedFrame( Unit enemyUnit ) {
      return DynamicEnemyLastSeenFrame.get(enemyUnit);
   }

   public HashMap<Unit, UnitDef> getEnemyUnitDefByUnit() {
      return EnemyUnitDefByUnit;
   }   
}
