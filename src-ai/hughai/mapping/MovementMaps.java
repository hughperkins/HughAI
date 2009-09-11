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

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.CSAI;
import hughai.PlayerObjects;
import hughai.basictypes.*;
import hughai.*;
import hughai.mapping.BuildMap.BuildMapPos;
import hughai.mapping.HeightMap.HeightMapPos;
import hughai.mapping.LosMap.LosMapPos;
import hughai.mapping.SlopeMap.SlopeMapPos;
import hughai.ui.MainUI;
import hughai.unitdata.UnitDefHelp;
import hughai.utils.*;


public class MovementMaps
{
   public static final int granularity = 2;
   
   public static class MovementMapPos extends Int2 {
      public MovementMapPos() {

      }
      public MovementMapPos( Int2 int2 ) {
         x = int2.x;
         y = int2.y;
      }
      public MovementMapPos( int x, int y ) {
         super( x, y );
      }
      public TerrainPos toTerrainPos() {
         return new TerrainPos( x * 8 * granularity, 0, y * 8 * granularity );
      }
      public static MovementMapPos fromHeightMapPos( HeightMapPos heightMapPos ) {
         return new MovementMapPos( heightMapPos.x / granularity, heightMapPos.y / granularity );
      }
      public static MovementMapPos fromTerrainPos( TerrainPos terrainPos ) {
         return new MovementMapPos( (int)terrainPos.x / 8 / granularity,
               (int)terrainPos.z / 8 / granularity );
      }
   }
   
   CSAI csai;
   OOAICallback aicallback;
   UnitDefHelp unitDefHelp;
   LogFile logfile;
   Map gameMap;
   DrawingUtils drawingUtils;
   Config config;
   //	SlopeMap slopeMap;
   //	Maps maps;
   PlayerObjects playerObjects;

//   public float maxvehicleslope = 0.08f;  // arbitrary cutoff, for simplicity
//   public float maxinfantryslope = 0.33f; // arbitrary cutoff, for simplicity

   public final int SQUARE_SIZE = 8;

//   private float[][] slopemap;
   //public double[] heightmap;
   private List<Float> heightmap;

   // these are at half map resolution, so 16 pos per sector
   private boolean[][] infantrymap; // true means troops can move freely in sector
   private boolean[][] vehiclemap; // true means vehicles can move freely in sector
   private boolean[][] boatmap; // true means boats can move freely in sector

   // these are at half map resolution, so 16 pos per sector
   private int[][] infantryareas; // each area has its own number, 0 means infantry cant go there
   private int[][] vehicleareas; // each area has its own number, 0 means vehicles cant go there
   private int[][] boatareas; // each area has its own number, 0 means boats cant go there

   public Integer[] infantryareasizes = null; // indexed by ( area number )
   public Integer[] vehicleareasizes = null; // indexed by ( area number )
   public Integer[] boatareasizes = null; // indexed by ( area number )

   public int mapwidth;
   public int mapheight;

   public MovementMaps(PlayerObjects playerObjects)
   {
      csai = playerObjects.getCSAI();
      aicallback = csai.aicallback;
      logfile = playerObjects.getLogFile();
      unitDefHelp = playerObjects.getUnitDefHelp();
      gameMap = aicallback.getMap();
      drawingUtils = playerObjects.getDrawingUtils();
      config = playerObjects.getConfig();
      
      this.playerObjects = playerObjects;
      //		slopeMap = playerObjects.getSlopeMap();
      //		maps = playerObjects.getMaps();

   }
   
//   class ButtonTestMovementMapsAreas implements MainUI.ButtonHandler {
//      @Override
//      public void go() {
//         for( int x = 0; x < aicallback.getMap().getWidth() * 8; x += 100 ) {
//            for( int z = 0; z < aicallback.getMap().getHeight() * 8; z += 100 ) {
//               int area = GetVehicleArea( currentpos )
//            }
//         }
//      }
//   }   

//   boolean inited = false; // there's probably a better way to do this, but I
//                           // haven't thought of it yet.
   void Init() {
//      if( !inited ) {
         GenerateMaps();
         playerObjects.getMainUI().registerButton( "Show vehicle areas",
               new ShowVehicleAreasButton() );
         playerObjects.getMainUI().registerButton( "Show infantry areas",
               new ShowInfantryAreasButton() );
//         inited = true;
//      }
   }
   
   class ShowVehicleAreasButton implements MainUI.ButtonHandler {
      @Override
      public void go() {
         drawingUtils.DrawMap( vehicleareas );
         csai.sendTextMessage( "vehicle areas drawn" );
//         DrawMetalSpots();
      }
   }

   class ShowInfantryAreasButton implements MainUI.ButtonHandler {
      @Override
      public void go() {
         drawingUtils.DrawMap( infantryareas );
         csai.sendTextMessage( "infantry areas drawn" );
//         DrawMetalSpots();
      }
   }
   
   public boolean infantryCanMoveOk( MovementMapPos movementMapPos ) {
      return infantrymap[movementMapPos.x][movementMapPos.y];
   }

   public boolean vehicleCanMoveOk( MovementMapPos movementMapPos ) {
      return vehiclemap[movementMapPos.x][movementMapPos.y];
   }

   public boolean boatCanMoveOk( MovementMapPos movementMapPos ) {
      return boatmap[movementMapPos.x][movementMapPos.y];
   }

   public int GetVehicleArea(TerrainPos currentpos)
   {
      MovementMapPos movementMapPos = MovementMapPos.fromTerrainPos( currentpos );
      return vehicleareas[ movementMapPos.x ][ movementMapPos.y ];
   }

   public int GetBoatArea(TerrainPos currentpos)
   {
      MovementMapPos movementMapPos = MovementMapPos.fromTerrainPos( currentpos );
      return boatareas[ movementMapPos.x ][ movementMapPos.y ];
   }

   public int GetInfantryArea(TerrainPos currentpos)
   {
      MovementMapPos movementMapPos = MovementMapPos.fromTerrainPos( currentpos );
      return infantryareas[ movementMapPos.x ][ movementMapPos.y ];
   }

   public int GetArea(UnitDef unitdef, TerrainPos currentpos)
   {
//      MovementMapPos movementMapPos = MovementMapPos.fromTerrainPos( currentpos );
      //logfile.WriteLine("MovementMaps.GetArea unitDefHelp is null? " + (unitDefHelp == null) + " unitdef is null? " + (unitdef == null) );
      if( unitDefHelp.IsAirCraft( unitdef ) )
      {
         return 1; // always area 1, easy :-DDD
      }
      if( unitDefHelp.IsBoat( unitdef ) )
      {
         return GetBoatArea( currentpos );
      }
      else if( unitdef.getMoveData().getMaxSlope() >= config.getMaxinfantryslope() ) // so infantry, approximately
                                    // basically, the maxinfantryslope means the maximum slope over which all infantry can move
      {
         return GetInfantryArea( currentpos );
      }
      else if( unitdef.getMoveData().getMaxSlope() >= config.getMaxvehicleslope() ) // vehicle, approximately
      {
         return GetVehicleArea( currentpos );
      }
      else 
      {
         csai.sendTextMessage( "Error: maxVehicleslope in configuration is set too high, because unit " + unitdef.getHumanName() + " only has a maxslope of "
               + unitdef.getMoveData().getMaxSlope() + " compared to in confiig file " + config.getMaxvehicleslope() );
         return 0;
      }
   }

   SlopeMap slopeMap;
   public void GenerateMaps()
   {
      logfile.WriteLine( "MovementMaps.GenerateMaps start" );
      slopeMap = playerObjects.getMaps().getSlopeMap();
//      slopemap = playerObjects.getMaps().getSlopeMap().GetSlopeMap();
      heightmap = gameMap.getHeightMap();

      mapwidth = gameMap.getWidth();
      mapheight = gameMap.getHeight();

      GenerateInfantryAccessibleMap();
      GenerateVehicleAccessibleMap();
      GenerateBoatAccessibleMap();

      infantryareas = CreateAreas( infantryareasizes, infantrymap );
      vehicleareas = CreateAreas( vehicleareasizes, vehiclemap );
      boatareas = CreateAreas( boatareasizes, boatmap );

      logfile.WriteLine( "MovementMaps.GenerateMaps done" );
   }

   // start from a valid point on map for unit type, ie accessibilitymap is true, and see where we can get to, this is area 1
   // repeat till all valid points on map have been visited
   int[][] CreateAreas( Integer[]infantryareasizes, boolean[][]accessibilitymap )
   {
      logfile.WriteLine( "CreateAreas" );
      boolean[][]visited = new boolean[ mapwidth / granularity][ mapheight / granularity ];
      int[][] areamap = new int[ mapwidth / granularity][ mapheight / granularity ];

      ArrayList<Integer> areasizesal = new ArrayList<Integer>();
      areasizesal.add( 0 );
      int areanumber = 1;
      for( int y = 0; y < mapheight / granularity; y++ )
      {
         for( int x = 0; x < mapwidth / granularity; x++ )
         {
            if( accessibilitymap[x][ y] && !visited[ x][ y ] )
            {
               int count = Bfs( areamap, areanumber, x, y, accessibilitymap, visited );
//               logfile.WriteLine( "area " + areanumber + " size: " + count );
               areasizesal.add( count );
               areanumber++;
               //return areamap;
            }
         }
      }
      logfile.WriteLine( "number areas: " + ( areanumber - 1 ) );
      infantryareasizes = (Integer[])(areasizesal.toArray( new Integer[0]));
      return areamap;
   }

   class Direction
   {
      public int x; 
      public int y;
      public Direction( int x, int y )
      {
         this.x = x; this.y = y;
      }
   }

   // run a breath first search for everywhere in the area
   // assume can only move north/south/east/west (not diagonally)
   int Bfs( int[][] areamap, int areanumber, int startx, int starty, boolean[][]accessibilitymap, boolean[][]visited )
   {
      Direction[] directions = new Direction[4];
      directions[0] = new Direction( 0, 1 );
      directions[1] = new Direction( 0, -1 );
      directions[2] = new Direction( 1, 0 );
      directions[3] = new Direction( -1, 0 );

      int numbersquares = 1;
      visited[ startx][ starty ] = true;
      areamap[ startx][ starty ] = areanumber;
      Queue<Integer> queue = new LinkedList<Integer>();
      queue.offer( ( startx << 16 ) + ( starty ) );  // easier than using new int[]{} or making a class or queueing an arraylist.  And it looks cool ;-)

      while( !queue.isEmpty() )
      {
         int value = queue.poll();
         int thisx = value >> 16;
      int thisy = value & 65535;

      // if( numbersquares > 30 )
      // {
      //     return numbersquares;
      //  }

      // add surrounding cells to queue, as long as they are legal for this unit type (accessibilitymap is true)
      for( Direction direction : directions )
      {
         int deltax = direction.x;
         int deltay = direction.y;
         int newx = thisx + deltax;
         int newy = thisy + deltay;
         //logfile.WriteLine( "newx " + newx + "  " + newy + " delta: " + deltax + " " + deltay );
         if( newx >= 0 && newy >= 0 && newx < ( mapwidth / granularity ) && newy < ( mapheight / granularity ) &&
               !(visited[ newx][ newy ]) && (accessibilitymap[ newx][ newy ]) )
         {
            visited[ newx][ newy ] = true;
            numbersquares++;
            areamap[ newx][ newy ] = areanumber;
            //areamap[ newx, newy ] = numbersquares;
            queue.offer( ( newx << 16 ) + newy );
         }
      }
      }
      return numbersquares;
   }

   void GenerateInfantryAccessibleMap()
   {
      logfile.WriteLine( "GenerateInfantryAccessibleMap" );
      infantrymap = new boolean[ mapwidth / granularity][ mapheight / granularity ];
      for( int y = 0; y < mapheight / granularity; y++ )
      {
         for( int x = 0; x < mapwidth / granularity; x++ )
         {
            infantrymap[ x ][ y ] = true;
         }
      }

      ArrayIndexer arrayindexer = new ArrayIndexer( mapwidth, mapheight );
      float maxinfantryslope = config.getMaxinfantryslope();
      for( int y = 0; y < mapheight; y++ )
      {
         for( int x = 0; x < mapwidth; x++ )
         {
            HeightMap.HeightMapPos heightMapPos = new HeightMap.HeightMapPos( x, y );
            SlopeMapPos slopeMapPos = SlopeMapPos.fromHeightMapPos( heightMapPos );
            if( slopeMap.getSlopeAt( slopeMapPos ) > maxinfantryslope )
            {
               infantrymap[ x / granularity][ y / granularity ] = false;
            }
            if( heightmap.get( arrayindexer.GetIndex( x, y ) ) < 0 )
            {
               infantrymap[ x / granularity ][ y / granularity ] = false;
            }
         }
      }
   }

   void GenerateVehicleAccessibleMap()
   {
      logfile.WriteLine( "GenerateVehicleAccessibleMap" );
      vehiclemap = new boolean[ mapwidth / granularity ][ mapheight / granularity ];
      for( int y = 0; y < mapheight / granularity; y++ )
      {
         for( int x = 0; x < mapwidth / granularity; x++ )
         {
            vehiclemap[ x ][ y ] = true;
         }
      }

      ArrayIndexer arrayindexer = new ArrayIndexer( mapwidth, mapheight );
      float vehiclemaximumslope = config.getMaxvehicleslope();
      for( int y = 0; y < mapheight; y++ )
      {
         for( int x = 0; x < mapwidth; x++ )
         {
            HeightMap.HeightMapPos heightMapPos = new HeightMap.HeightMapPos( x, y );
            SlopeMapPos slopeMapPos = SlopeMapPos.fromHeightMapPos( heightMapPos );
            if( slopeMap.getSlopeAt( slopeMapPos ) > vehiclemaximumslope )
//            if( slopemap[ x/ granularity][ y / granularity ] > vehiclemaximumslope ) // too steep
            {
               vehiclemap[ x / granularity][ y / granularity ] = false;
            }
            if( heightmap.get( arrayindexer.GetIndex( x, y ) ) < 0 ) // underwater
            {
               vehiclemap[ x / granularity][ y / granularity ] = false;
            }
         }
      }
   }

   void GenerateBoatAccessibleMap()
   {
      logfile.WriteLine( "GenerateBoatAccessibleMap" );
      boatmap = new boolean[ mapwidth / granularity][ mapheight / granularity ];
      for( int y = 0; y < mapheight / granularity; y++ )
      {
         for( int x = 0; x < mapwidth / granularity; x++ )
         {
            boatmap[ x ][ y ] = true;
         }
      }

      ArrayIndexer arrayindexer = new ArrayIndexer( mapwidth, mapheight );
      for( int y = 0; y < mapheight; y++ )
      {
         for( int x = 0; x < mapwidth; x++ )
         {
            if( heightmap.get( arrayindexer.GetIndex( x, y ) ) > 0 ) // over land
            {
               boatmap[ x /granularity][ y / granularity ] = false;
            }
         }
      }
   }
}
