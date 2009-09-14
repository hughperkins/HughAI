// Copyright Hugh Perkins 2009
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
import hughai.ui.MainUI;
import hughai.unitdata.UnitDefHelp;
import hughai.utils.*;

// this depends directly on EnemyTracker, which it just calls directly, without using
// events
// no other direct dependencies
public class ThreatMap {
   public static class ThreatMapPos extends Int2 {
      public ThreatMapPos() {}
      public ThreatMapPos( int x, int y ) {
         super( x, y );
      }
      public static ThreatMapPos fromTerrainPos( TerrainPos terrainPos ) {
         return new ThreatMapPos( (int)terrainPos.x / 8 / granularity,
               (int)terrainPos.z / 8 / granularity );
      }
      public TerrainPos toTerrainPos() {
         return new TerrainPos( x * 8 * granularity, 
               0,
               y * 8 * granularity );
      }
   }
   
   PlayerObjects playerObjects;
   EnemyTracker enemyTracker;
   Config config;
   
   public static final int granularity = 2; // do it at half size.  why?  I dont know...  If it doesn't work out, I'll change it!
   // maybe because ideally it should match with MovementMaps?
   
   int threatmapwidth;
   int threatmapheight;   
   private float[][]damagePerSecond; // ok, so this is going to store the damage
   // per second that one unit would sustain if
   // was in this location on its own
   // sounds like a good place to start?
   
   public ThreatMap( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
      this.enemyTracker = playerObjects.getEnemyTracker();
      this.config = playerObjects.getConfig();
      
      init();
   }
   
   public float getThreatAt( ThreatMapPos threatMapPos ) {
      return damagePerSecond[threatMapPos.x][threatMapPos.y];
   }
   
   public float getThreatAt( TerrainPos terrainpos ) {
      ThreatMapPos threatMapPos = ThreatMapPos.fromTerrainPos( terrainpos );
      return getThreatAt( threatMapPos );
   }
   
   void init() {
      Map gamemap = playerObjects.getAicallback().getMap();
      int gamemapwidth = gamemap.getWidth();
      int gamemapheight = gamemap.getHeight();
      
      threatmapwidth = gamemapwidth / granularity; 
      threatmapheight = gamemapheight / granularity;
      damagePerSecond = new float[threatmapwidth][threatmapheight];
      
      buildThreatMap();
      
      playerObjects.getCSAI().registerGameListener( new GameListener() );
      playerObjects.getMainUI().registerButton( "Show threat map",
            new ButtonDrawThreatMap() );
   }
   
   class ButtonDrawThreatMap implements MainUI.ButtonHandler {
      @Override
      public void go() {
         boolean[][]maptodisplay = new boolean[threatmapwidth][threatmapheight];
         for( int z = 0; z < threatmapheight; z++ ) {
            for( int x = 0; x < threatmapwidth; x++ ) {
               maptodisplay[x][z] = false;
               if( damagePerSecond[x][z] > 1 ) {
                  maptodisplay[x][z] = true;
               }
            }
         }
         playerObjects.getDrawingUtils().DrawMap( maptodisplay );
      }
   }
   
   void debug( Object message ) {
      playerObjects.getLogFile().WriteLine( "" + message );
   }
   
   void buildThreatMap() {
      long time = System.currentTimeMillis();
      debug("Building threat map ...");
      // right, we're going to assume we're running single-threaded, which we are
      // and we're just going to overwrite the old map...
      for( int z = 0; z < threatmapheight; z++ ) {
         for( int x = 0; x < threatmapwidth; x++ ) {
            damagePerSecond[x][z] = 0f;
         }
      }
      int currentframe = playerObjects.getFrameController().getFrame();
      int maxtimetoconservemobileunitsseconds = config.getMaxTimeToConserveMobileEnemyOnThreatMapGameSeconds();
      int maxtimetoconservemobileunitsframes = 30 * maxtimetoconservemobileunitsseconds;
      for( Unit enemy : enemyTracker.getEnemyUnits() ) {
//         debug("threatmap considering " + enemy.getUnitId() + " ... ");
         TerrainPos pos = enemyTracker.getPos( enemy );
//         debug(" ... pos: " + pos );
         int lastseenframe = enemyTracker.getLastLocatedFrame( enemy );
//         debug(" ... lastseenframe: " + lastseenframe );
         if( pos != null ) {
            int lastseenframesago = currentframe - lastseenframe;
               if ( ( lastseenframesago <= maxtimetoconservemobileunitsframes )
                    || ( enemyTracker.getEnemyPosByStaticUnit().get( enemy ) != null ) ) {
//            debug(" ... calling mapEnemy" );
                  mapEnemy( enemy, pos );
               }
         }
      }
      
      debug(" ... threat map done. milliseconds taken: " + ( System.currentTimeMillis() - time ) );
   }
   
   // this whole function heavily influenced by E323's CThreatMap.cpp class
   void mapEnemy( Unit enemy, TerrainPos pos ) {
      UnitDef enemyUnitDef = enemyTracker.getEnemyUnitDefByUnit().get( enemy );
      debug("... unitdef: " + enemyUnitDef.getHumanName() + " " + pos );
      if( !enemyUnitDef.isAbleToAttack() ){
//         debug("... not able to attack" );
         return;
      }
      int terrainrange = 0;
      try {
         terrainrange = (int)enemyUnitDef.getWeaponMounts().get( 0 ).getWeaponDef().getRange();
         debug("mapping enemy unit " + enemyUnitDef.getHumanName() + " " + enemy.getUnitId() );
      } catch( Exception e ) {
//         debug( enemyUnitDef.getHumanName() + " has no weapon mounts?");
      }

      int threatmapposx = (int)pos.x / 8 / granularity;
      int threatmapposz = (int)pos.z / 8 / granularity;
      debug(" ... threatmappos: " + threatmapposx + " " + threatmapposz );
      
      // use of power heavily influenced by E323's CThreatMap.cpp class
      // I'm not quite sure what 'power' means, but the weapondef itself
      // looks .... formidable...
      float power = enemyUnitDef.getPower();
      int threatmaprange = terrainrange / 8 / granularity;
      debug(" ... power: " + power + " range: " + threatmaprange );
      for( int deltaz = -threatmaprange; deltaz <= threatmaprange; deltaz++ ){
         for( int deltax = -threatmaprange; deltax <= threatmaprange; deltax++ ){
            if( deltax * deltax + deltaz * deltaz < threatmaprange * threatmaprange ) { // don't bother taking squareroot, since squareroots are slow
               int thisx = threatmapposx + deltax;
               int thisz = threatmapposz + deltaz;
               if( thisx >= 0 && thisx < threatmapwidth 
                     && thisz >= 0 && thisz < threatmapheight ) {
                  damagePerSecond[ thisx ][ thisz ] += power;
//                  playerObjects.getDrawingUtils().DrawUnit( "armmex", new Float3( thisx * 8 * 2, 0, thisz * 8 * 2) );
               }
            }
         }         
      }
   }
   
   class GameListener extends GameAdapter {
      @Override
      public void Tick( int frame ) {
         buildThreatMap();
      }
   }

   public float[][] getDamagePerSecond() {
      return damagePerSecond;
   }
}
