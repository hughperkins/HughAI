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
import com.springrts.ai.oo.Map;

import hughai.CSAI;
import hughai.EnemyTracker;
import hughai.PlayerObjects;
import hughai.basictypes.*;
import hughai.*;
import hughai.mapping.BuildMap.BuildMapPos;
import hughai.unitdata.UnitDefHelp;
import hughai.utils.*;


// 2d index of enemies
public class EnemyMap
{
   public static final int granularity = 2;

   public static class EnemyMapPos extends Int2 {
      public EnemyMapPos() {

      }
      public EnemyMapPos( Int2 int2 ) {
         x = int2.x;
         y = int2.y;
      }
      public EnemyMapPos( int x, int y ) {
         super( x, y );
      }
      public TerrainPos toTerrainPos() {
         return new TerrainPos( x * 8 * granularity, 0, y * 8 * granularity );
      }
      public static EnemyMapPos fromTerrainPos( TerrainPos terrainPos ) {
         return new EnemyMapPos( (int)terrainPos.x / 8 / granularity,
               (int)terrainPos.z / 8 / granularity );
      }
   }

   CSAI csai;
   OOAICallback aicallback;
   LogFile logfile;
   UnitDefHelp unitdefhelp;
   //UnitController unitcontroller;
   EnemyTracker enemyTracker;

   public HashMap<Unit, EnemyMapPos> MapPosByStaticEnemy = new HashMap<Unit, EnemyMapPos>();
   private Unit[][] enemyMap; // value is 0 or id of enemy;  this is the position of the centre of the enemy; (its only an index, not a build map)

   public int mapwidth;
   public int mapheight;

   public EnemyMap( PlayerObjects playerObjects )
   {
      csai = playerObjects.getCSAI();
      aicallback = playerObjects.getAicallback();
      logfile = playerObjects.getLogFile();
      unitdefhelp = playerObjects.getUnitDefHelp();
      //unitcontroller = UnitController.GetInstance();
      enemyTracker = playerObjects.getEnemyTracker();

      this.mapwidth = aicallback.getMap().getWidth();
      this.mapheight = aicallback.getMap().getHeight();

      enemyMap = new Unit[ mapwidth / granularity ][ mapheight / granularity ];

      enemyTracker.registerListener( new EnemyControllerHandler() );

      Init();
   }

   public static interface EnemyMapListener {
      public void EnemyMapped( Unit enemy, EnemyMapPos mapPos );
      public void EnemyRemoved( Unit enemy, EnemyMapPos mapPos );
   }
   public static class EnemyMapAdapter implements EnemyMapListener {
      @Override
      public void EnemyMapped( Unit enemy, EnemyMapPos mapPos ){}
      @Override
      public void EnemyRemoved( Unit enemy, EnemyMapPos mapPos ){}		
   }
   ArrayList<EnemyMapListener> listeners = new ArrayList<EnemyMapListener>();
   public void registerListener( EnemyMapListener listener ) {
      listeners.add(listener);
   }

   public void Init()
   {
      Map map = aicallback.getMap();
      mapwidth = map.getWidth();
      mapheight = map.getHeight();

      logfile.WriteLine( "EnemyMap.Init finished()" );
   }

   class EnemyControllerHandler extends EnemyTracker.EnemyAdapter {
      @Override
      public void AcquiredStaticEnemy( Unit enemy, UnitDef unitdef, TerrainPos terrainpos )
      {
         if( !MapPosByStaticEnemy.containsKey( enemy ) )
         {
            EnemyMapPos enemyMapPos = EnemyMapPos.fromTerrainPos( terrainpos );
            enemyMap[ enemyMapPos.x ][ enemyMapPos.y ] = enemy;
//            Int2 mappos = new Int2( mapx, mapy );
            MapPosByStaticEnemy.put( enemy, enemyMapPos );
            for( EnemyMapListener listener : listeners ) {
               listener.EnemyMapped( enemy, enemyMapPos);
            }
         }
      }

      @Override
      public void EnemyDestroyed( Unit unit )
      {
         if( MapPosByStaticEnemy.containsKey( unit ) )
         {
            EnemyMapPos mapPos = MapPosByStaticEnemy.get( unit );
            enemyMap[ mapPos.getX() ][ mapPos.getY() ] = null;
            MapPosByStaticEnemy.remove( unit );
            for( EnemyMapListener listener : listeners ) {
               listener.EnemyRemoved(unit, mapPos);
            }
         }
      }        
   }
}
