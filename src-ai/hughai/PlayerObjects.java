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

package hughai;

import java.util.*;
import java.lang.reflect.*;

import com.springrts.ai.oo.*;

import hughai.building.BuildEconomy;
import hughai.mapping.*;
import hughai.unitdata.*;
import hughai.utils.*;
import hughai.ui.*;
import hughai.building.*;

// These could all be singletons if we weren't running multiple players...
// I suppose another option could be to make them singletons, but passing in
// teamid into the getInstance method.
// Edit: actually, even in multi-player, Singletons would have been fine ;-)
// but I spent ages putting it in this format, so I'm not going to undo this
// right now ;-)
public class PlayerObjects {
   OOAICallback aicallback;

   CSAI CSAI;
   BuildEconomy buildEconomy;
   ResourceManager resourceManager;
   BuildTable buildTable;
   //	FriendlyUnitPositionObserver friendlyUnitPositionObserver;
   GiveOrderWrapper giveOrderWrapper;
   LogFile logfile;
   UnitController unitController;
   UnitDefHelp unitDefHelp;
   DrawingUtils drawingUtils;
   //	BuildMap buildMap;
   //	MovementMaps movementMaps;
   //	TimeHelper timeHelper;
   //	HeightMap heightMap;
   //	SlopeMap slopeMap;
   UnitCommandCache unitCommandCache;
   BuildPlanner buildPlanner;
   Ownership ownership;
   BuildTree buildTree;
   MetalController metalController;
   EnergyController energyController;
   Metal metal;
   ReclaimHelper reclaimHelper;
   EnemyTracker enemyController;
   UnitLists unitLists;
   Maps maps;
   TimeHelper timeHelper;
   EnemyTracker enemyTracker;
   LosHelper losHelper;
   Config config;
   MainUI mainUI;
   FrameController frameController;
   Workflows workflows;

   static Collection<CSAI> csais = new HashSet<CSAI>();

   public PlayerObjects( CSAI CSAI ){
      if( csais.contains( CSAI )) {
         throw new RuntimeException("Error: trying to instantiate duplicated PlayerObjects" );
      }

      this.CSAI = CSAI;
      csais.add( CSAI );
      //this.resourceManager = new ResourceManager( CSAI.aicallback );
   }
   
   public void dispose() throws Exception {
      csais.clear();
      for( Field field : this.getClass().getFields() ) {
         System.out.println( this.getClass().getSimpleName() + " setting field "
               + field.getName() + " to null");
         field.set( this, null );
      }
   }
   
   @Override
   protected void finalize() {
      System.out.println( this.getClass().getSimpleName() + ".finalize()");
   }

   public synchronized CSAI getCSAI(){
      return CSAI; 
   }

   public Config getConfig() {
      if( config != null ) {
         return config;
      }
      config = new Config( this );
      config.init();
      return config;
   }
   
   public synchronized BuildEconomy getBuildEconomy() {
      if( buildEconomy != null ){
         return buildEconomy;
      }
      buildEconomy = new BuildEconomy(this);
      return buildEconomy;
   }

   public synchronized FrameController getFrameController() {
      if( frameController != null ){
         return frameController;
      }
      frameController = new FrameController(this);
      return frameController;
   }

   public synchronized MainUI getMainUI() {
      if( mainUI != null ){
         return mainUI;
      }
      mainUI = new MainUI(this);
      return mainUI;
   }

   public synchronized Maps getMaps() {
      if( maps != null ){
         return maps;
      }
      logfile.WriteLine( "playerobjects instantiating new maps" );
      maps = new Maps(this);
      maps.Init();
      logfile.WriteLine( " ... done" );
      return maps;
   }

   public synchronized LosHelper getLosHelper() {
      if( losHelper != null ){
         return losHelper;
      }
      losHelper = new LosHelper(this);
      return losHelper;
   }

   public synchronized UnitLists getUnitLists() {
      if( unitLists != null ){
         return unitLists;
      }
      unitLists = new UnitLists(this);
      return unitLists;
   }

   public synchronized Workflows getWorkflows() {
      if( workflows != null ){
         return workflows;
      }
      workflows = new Workflows(this);
      return workflows;
   }

   public synchronized EnemyTracker getEnemyTracker() {
      if( enemyTracker != null ){
         return enemyTracker;
      }
      enemyTracker = new EnemyTracker(this);
      return enemyTracker;
   }

   public synchronized DrawingUtils getDrawingUtils() {
      if( drawingUtils != null ){
         return drawingUtils;
      }
      drawingUtils = new DrawingUtils(this);
      return drawingUtils;
   }

   public synchronized BuildTree getBuildTree() {
      if( buildTree != null ){
         return buildTree;
      }
      buildTree = new BuildTree(this);
      return buildTree;
   }

   public synchronized ReclaimHelper getReclaimHelper() {
      if( reclaimHelper != null ){
         return reclaimHelper;
      }
      reclaimHelper = new ReclaimHelper(this);
      return reclaimHelper;
   }

   public synchronized MetalController getMetalController() {
      if( metalController != null ){
         return metalController;
      }
      metalController = new MetalController(this);
      return metalController;
   }

   public synchronized EnergyController getEnergyController() {
      if( energyController != null ){
         return energyController;
      }
      energyController = new EnergyController(this);
      return energyController;
   }

   public synchronized Metal getMetal() {
      if( metal != null ){
         return metal;
      }
      metal = new Metal(this);
      return metal;
   }

   public synchronized BuildPlanner getBuildPlanner() {
      if( buildPlanner != null ){
         return buildPlanner;
      }
      buildPlanner = new BuildPlanner(this);
      return buildPlanner;
   }

   public synchronized Ownership getOwnership() {
      if( ownership != null ){
         return ownership;
      }
      ownership = new Ownership(this);
      return ownership;
   }
   //
   //	public synchronized MovementMaps getMovementMaps() {
   //		if( movementMaps != null ){
   //			return movementMaps;
   //		}
   //		movementMaps = new MovementMaps(this);
   //		return movementMaps;
   //	}
   //
   //	public synchronized SlopeMap getSlopeMap() {
   //		if( slopeMap != null ){
   //			return slopeMap;
   //		}
   //		slopeMap = new SlopeMap(this);
   //		return slopeMap;
   //	}
   //
   //	public synchronized HeightMap getHeightMap() {
   //		if( heightMap != null ){
   //			return heightMap;
   //		}
   //		heightMap = new HeightMap(this);
   //		return heightMap;
   //	}

   public synchronized ResourceManager getResourceManager() {
      if( resourceManager != null ){
         return resourceManager;
      }
      resourceManager = new ResourceManager(aicallback);
      return resourceManager;
   }

   public synchronized TimeHelper getTimeHelper() {
      //System.out.println("PlayerObjects.getTimeHelper");
      if( timeHelper != null ){
         //System.out.println("... returning existing timehelper");
         return timeHelper;
      }
      System.out.println("Creating new timehelper");
      timeHelper = new TimeHelper( this );
      return timeHelper;
   }

   public synchronized UnitCommandCache getUnitCommandCache() {
      if( unitCommandCache != null ){
         return unitCommandCache;
      }
      unitCommandCache = new UnitCommandCache(this);
      return unitCommandCache;
   }

   public synchronized BuildTable getBuildTable() {
      if( buildTable != null ){
         return buildTable;
      }
      buildTable = new BuildTable(this);
      return buildTable;
   }

   public synchronized UnitController getUnitController() {
      if( unitController != null ){
         return unitController;
      }
      unitController = new UnitController(this);
      return unitController;
   }

   //	public synchronized BuildMap getBuildMap() {
   //		if( buildMap != null ){
   //			return buildMap;
   //		}
   //		buildMap = new BuildMap(this);
   //		return buildMap;
   //	}

   public synchronized GiveOrderWrapper getGiveOrderWrapper() {
      if( giveOrderWrapper != null ){
         return giveOrderWrapper;
      }
      giveOrderWrapper = new GiveOrderWrapper(this);
      return giveOrderWrapper;
   }
   /*
	public synchronized FriendlyUnitPositionObserver getFriendlyUnitPositionObserver() {
		if( friendlyUnitPositionObserver != null ){
			return friendlyUnitPositionObserver;
		}
		friendlyUnitPositionObserver = new FriendlyUnitPositionObserver(this);
		return friendlyUnitPositionObserver;
	}
    */
   public synchronized UnitDefHelp getUnitDefHelp() {
      if( unitDefHelp != null ){
         return unitDefHelp;
      }
      unitDefHelp = new UnitDefHelp(this);
      return unitDefHelp;
   }

   public synchronized LogFile getLogFile() {
      if( logfile != null ){
         return logfile;
      }
      logfile = new LogFile(this);
      return logfile;
   }

   public synchronized OOAICallback getAicallback() {
      if( aicallback != null ) {
         return aicallback;
      }
      this.aicallback = CSAI.aicallback;
      return aicallback;
   };
}
