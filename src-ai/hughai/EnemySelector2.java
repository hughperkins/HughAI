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
import java.util.Map;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.*;
import hughai.basictypes.*;
import hughai.mapping.*;
import hughai.packcoordinators.*;
import hughai.unitdata.*;
import hughai.utils.*;

// legacy comment, not sure it's really accurate ;-) :
// this class should ideally work out appropriate enemies to attack,
// based on profile we define for it
// since different units have a different preference for
// attacking (air vs ground vs speed etc), we need a different object for each
public class EnemySelector2
{
   public double maxenemyspeed;
   public boolean WaterOk;
   public boolean BadTerrainOk;

   CSAI csai;
   OOAICallback aicallback;
   LogFile logfile;
   EnemyTracker enemyTracker;
   UnitDefHelp unitdefhelp;
   Maps maps;

   int startarea = 0;
   UnitDef typicalunitdef;

   public EnemySelector2( PlayerObjects playerObjects, double maxenemyspeed, UnitDef typicalunitdef)
   {
      csai = playerObjects.getCSAI();
      aicallback = csai.aicallback;
      logfile = playerObjects.getLogFile();
      enemyTracker = playerObjects.getEnemyTracker();
      unitdefhelp = playerObjects.getUnitDefHelp();
      maps = playerObjects.getMaps();

      // csai.EnemyEntersLOSEvent += new CSAI.EnemyEntersLOSHandler( EnemyEntersLOS );

      //		enemycontroller = EnemyController.GetInstance();
      //		unitdefhelp = new UnitDefHelp(aicallback);

      this.maxenemyspeed = maxenemyspeed;
//      this.WaterOk = WaterOk;
//      this.BadTerrainOk = BadTerrainOk;

      this.typicalunitdef = typicalunitdef;

      csai.RegisterVoiceCommand( "fakeenemy", new VoiceFakeEnemy() );
      csai.RegisterVoiceCommand( "cancelfakeenemy", new VoiceCancelFakeEnemy() );
      //    startarea = MovementMaps.GetInstance().GetArea(typicalunitdef, startpos);
   }

   public void InitStartPos(TerrainPos startpos)
   {
      startarea = maps.getMovementMaps().GetArea(typicalunitdef, startpos);
   }

   class VoiceFakeEnemy implements VoiceCommandHandler {
      @Override
      public void commandReceived( String command, String[] splitargs, int teamnumber ) {
         fakeEnemyPos = new TerrainPos( Integer.parseInt( splitargs[2] ), 0,
               Integer.parseInt( splitargs[3] ) );
         fakeEnemy = true;
      }
   }

   class VoiceCancelFakeEnemy implements VoiceCommandHandler {
      @Override
      public void commandReceived( String command, String[] splitargs, int teamnumber ) {
         fakeEnemy = false;
      }
   }

   boolean fakeEnemy = false;
   TerrainPos fakeEnemyPos;

   // just get nearest known slow enemy
   // otherwise any enemy
   // no priority for buildings
   public TerrainPos ChooseAttackPoint( TerrainPos ourpos)
   {
      if( fakeEnemy ) {
         return fakeEnemyPos;
      }
      Unit bestunit = null;
      boolean gotknownunit = false;
      UnitDef defforbestid = null;
      TerrainPos posforbestid = null;
      double BestSquaredDistance = 100000000000d;
      for( Unit enemyunit : enemyTracker.EnemyUnits ) {
         UnitDef enemyunitdef = enemyTracker.EnemyUnitDefByUnit.get( enemyunit );
         TerrainPos enemypos = enemyTracker.getPos( enemyunit );
         if (enemypos != null)
         {
            if (maps.getMovementMaps().GetArea(typicalunitdef, enemypos) == startarea)
            {
               if (enemypos.GetSquaredDistance(new TerrainPos() ) > 1)
               {
                  double thissquareddistance = enemypos.GetSquaredDistance( ourpos );
                  if (enemyunitdef != null)
                  {
                     if (enemyunitdef.getSpeed() < maxenemyspeed) // if we already have building we dont care
                     {
                        if (thissquareddistance < BestSquaredDistance)
                        {
                           //    logfile.WriteLine( "best known so far" );
                           bestunit = enemyunit;
                           gotknownunit = true;
                           posforbestid = enemypos;
                           defforbestid = enemyunitdef;
                           BestSquaredDistance = thissquareddistance;
                        }
                     }
                  }
                  else
                  {
                     if (!gotknownunit) // if we haven't found a known unit,
                                        // go for an unknown unit
                     {
                        if (thissquareddistance < BestSquaredDistance)
                        {
                           //    logfile.WriteLine( "best unknown so far" );
                           bestunit = enemyunit;
                           posforbestid = enemypos;
                           defforbestid = enemyunitdef;
                           BestSquaredDistance = thissquareddistance;
                        }
                     }
                  }
               }
            }
         }
      }
      for( Unit enemyunit : enemyTracker.EnemyUnits ) {
         //  logfile.WriteLine( "EnemySelector: checking static... " );
         UnitDef unitdef = enemyTracker.EnemyUnitDefByUnit.get( enemyunit );
         TerrainPos enemypos = enemyTracker.getPos( enemyunit );
         if (enemypos != null)
         {
            double thissquareddistance = enemypos.GetSquaredDistance( ourpos );
            //  logfile.WriteLine( "EnemySelector: Potential enemy at " + enemypos.toString() + " squareddistance: " + thissquareddistance );
            if (thissquareddistance < BestSquaredDistance)
            {
               //   logfile.WriteLine( "EnemySelector: best distance so far" );
               bestunit = enemyunit;
               //gotbuilding = true;
               gotknownunit = true;
               posforbestid = enemypos;
               //defforbestid = unitdef;
            }
         }
      }
      if( posforbestid != null ) {
         logfile.WriteLine( "EnemySelector.chooseattackpoint. best enemy: " +
           bestunit.getUnitId() + " " + posforbestid );
      }
      return posforbestid;
   }

   // this is going to have to interact with all sorts of stuff in the future
   // for now keep it simple
   // for now we look for nearby buildings, then nearby enemy units with low speed, then anything
   public TerrainPos OldChooseAttackPoint(TerrainPos ourpos)
   {
      boolean gotbuilding = false;
      boolean gotknownunit = false;
      double BestSquaredDistance = 100000000000d;

      Unit bestunit = null;
      UnitDef defforbestid = null;
      TerrainPos posforbestid = null;
      //   logfile.WriteLine( "EnemySelector: checking mobile... " );
      for( Unit enemyunit : enemyTracker.EnemyUnits ) {
         UnitDef unitdef = enemyTracker.EnemyUnitDefByUnit.get( enemyunit );
         TerrainPos enemypos = enemyTracker.getPos( enemyunit );
         // logfile.WriteLine( "Found building " + 
         if (maps.getMovementMaps().GetArea(typicalunitdef, enemypos) 
               == startarea)
         {
            if (enemypos.GetSquaredDistance( new TerrainPos() ) > 1)
            {
               double thissquareddistance = ourpos.GetSquaredDistance( enemypos);
               //   logfile.WriteLine( "EnemySelector: Potential enemy at " + enemypos.toString() + " squareddistance: " + thissquareddistance );
               if (unitdef != null)
               {
                  //   logfile.WriteLine( "unitdef not null " + unitdef.getHumanName() + " ismobile: " + unitdefhelp.IsMobile( unitdef ).toString() );
                  //   logfile.WriteLine( "gotbuilding = " + gotbuilding.toString() );
                  if (gotbuilding)
                  {
                     if (!unitdefhelp.IsMobile(unitdef))
                     {
                        if (thissquareddistance < BestSquaredDistance)
                        {
                           //  logfile.WriteLine( "best building so far" );
                           bestunit = enemyunit;
                           gotbuilding = true;
                           gotknownunit = true;
                           posforbestid = enemypos;
                           defforbestid = unitdef;
                           BestSquaredDistance = thissquareddistance;
                        }
                     }
                  }
                  else
                  {
                     if (unitdef.getSpeed() < maxenemyspeed) // if we already have building we dont care
                     {
                        if (thissquareddistance < BestSquaredDistance)
                        {
                           //    logfile.WriteLine( "best known so far" );
                           bestunit = enemyunit;
                           gotknownunit = true;
                           posforbestid = enemypos;
                           defforbestid = unitdef;
                           BestSquaredDistance = thissquareddistance;
                        }
                     }
                  }
               }
               else // if unitdef unknown
               {
                  //  logfile.WriteLine( "gotknownunit = " + gotknownunit.toString() );
                  if (!gotknownunit) // otherwise just ignore unknown units
                  {
                     if (thissquareddistance < BestSquaredDistance)
                     {
                        //    logfile.WriteLine( "best unknown so far" );
                        bestunit = enemyunit;
                        posforbestid = enemypos;
                        defforbestid = unitdef;
                        BestSquaredDistance = thissquareddistance;
                     }
                  }
               }
            }
         }
      }

      for( Unit enemyunit : enemyTracker.EnemyUnits ) {
         UnitDef unitdef = enemyTracker.EnemyUnitDefByUnit.get( enemyunit );
         TerrainPos enemypos = enemyTracker.getPos( enemyunit );
         double thissquareddistance = ourpos.GetSquaredDistance(enemypos);
         //  logfile.WriteLine( "EnemySelector: Potential enemy at " + enemypos.toString() + " squareddistance: " + thissquareddistance );
         if (thissquareddistance < BestSquaredDistance)
         {
            //   logfile.WriteLine( "EnemySelector: best distance so far" );
            bestunit = enemyunit;
            gotbuilding = true;
            gotknownunit = true;
            posforbestid = enemypos;
            //defforbestid = unitdef;
         }
      }

      //if( enemycontroller.EnemyStaticPosByDeployedId.Contains( bestid ) )
      // {
      //     enemycontroller.EnemyStaticPosByDeployedId.remove( bestid );
      // }

      return posforbestid;
   }
}


