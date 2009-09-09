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

import hughai.basictypes.*;
import hughai.mapping.*;
import hughai.packcoordinators.*;
import hughai.unitdata.*;
import hughai.utils.*;


// this class works out appropriate enemies to attack, based on profile we define for it
// since different units have a different preference for attacking (air vs ground vs speed etc), we need a different object for each
public class EnemySelector
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

   public EnemySelector( PlayerObjects playerObjects, double maxenemyspeed, UnitDef typicalunitdef )
   {
      csai = playerObjects.getCSAI();
      aicallback = csai.aicallback;
      logfile = playerObjects.getLogFile();
      enemyTracker = playerObjects.getEnemyTracker();
      unitdefhelp = playerObjects.getUnitDefHelp();
      maps = playerObjects.getMaps();

      // csai.EnemyEntersLOSEvent += new CSAI.EnemyEntersLOSHandler( EnemyEntersLOS );

      this.maxenemyspeed = maxenemyspeed;
      //this.WaterOk = WaterOk;
     // this.BadTerrainOk = BadTerrainOk;

      this.typicalunitdef = typicalunitdef;
      //    startarea = maps.getMovementMaps().GetArea(typicalunitdef, startpos);
   }

   public void InitStartPos(TerrainPos startpos)
   {
      startarea = maps.getMovementMaps().GetArea(typicalunitdef, startpos);
   }

   // this is going to have to interact with all sorts of stuff in the future
   // for now keep it simple
   // for now we look for nearby buildings, then nearby enemy units with low speed, then anything
   public TerrainPos ChooseAttackPoint( TerrainPos friendlypos )
   {
      boolean gotbuilding = false;
      boolean gotknownunit = false;
      float BestSquaredDistance = 100000000000f;

      Unit besttarget = null;
      UnitDef defforbestid = null;
      TerrainPos posforbestid = null;
      //   logfile.WriteLine( "EnemySelector: checking mobile... " );
      // mobile units first:
      for( Unit enemy : enemyTracker.getEnemyUnits() ) {
         UnitDef enemyunitdef = enemyTracker.getEnemyUnitDefByUnit().get( enemy );
         TerrainPos enemypos = TerrainPos.fromAIFloat3( enemy.getPos() );
         //Float3 enemypos = EnemyMap.GetInstance().
         // logfile.WriteLine( "Found building " + 
         if (maps.getMovementMaps().GetArea(typicalunitdef, enemypos) == startarea)
         {
            if (enemypos.GetSquaredDistance(new TerrainPos() ) > 1)
            {
               float thissquareddistance = friendlypos.GetSquaredDistance( enemypos);
               //   logfile.WriteLine( "EnemySelector: Potential enemy at " + enemypos.toString() + " squareddistance: " + thissquareddistance );
               if ( enemyunitdef != null)
               {
                  //   logfile.WriteLine( "unitdef not null " + unitdef.getHumanName() + " ismobile: " + unitdefhelp.IsMobile( unitdef ).toString() );
                  //   logfile.WriteLine( "gotbuilding = " + gotbuilding.toString() );
                  if (gotbuilding)
                  {
                     if (!unitdefhelp.IsMobile(enemyunitdef))
                     {
                        if (thissquareddistance < BestSquaredDistance)
                        {
                           //  logfile.WriteLine( "best building so far" );
                           besttarget = enemy;
                           gotbuilding = true;
                           gotknownunit = true;
                           posforbestid = enemypos;
                           defforbestid = enemyunitdef;
                           BestSquaredDistance = thissquareddistance;
                        }
                     }
                  }
                  else
                  {
                     if (enemyunitdef.getSpeed() < maxenemyspeed) // if we already have building we dont care
                     {
                        if (thissquareddistance < BestSquaredDistance)
                        {
                           //    logfile.WriteLine( "best known so far" );
                           besttarget = enemy;
                           gotknownunit = true;
                           posforbestid = enemypos;
                           defforbestid = enemyunitdef;
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
                        besttarget = enemy;
                        posforbestid = enemypos;
                        defforbestid = enemyunitdef;
                        BestSquaredDistance = thissquareddistance;
                     }
                  }
               }
            }
         }
      }

      // static now
      for( Unit enemy : enemyTracker.getEnemyPosByStaticUnit().keySet() ) {
         TerrainPos enemypos = enemyTracker.getEnemyPosByStaticUnit().get( enemy );
         float thissquareddistance = friendlypos.GetSquaredDistance( enemypos );
         //  logfile.WriteLine( "EnemySelector: Potential enemy at " + enemypos.toString() + " squareddistance: " + thissquareddistance );
         if( thissquareddistance < BestSquaredDistance )
         {
            //   logfile.WriteLine( "EnemySelector: best distance so far" );
            besttarget = enemy;
            gotbuilding = true;
            gotknownunit = true;
            posforbestid = enemypos;
            //defforbestid = unitdef;
         }
      }

      //if( enemyTracker.EnemyStaticPosByDeployedId.Contains( bestid ) )
      // {
      //     enemyTracker.EnemyStaticPosByDeployedId.remove( bestid );
      // }

      return posforbestid;
   }
}
