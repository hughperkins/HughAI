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

package hughai.mapping;

import java.util.*;
import java.io.*;


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;
import com.springrts.ai.oo.Map;

import hughai.CSAI;
import hughai.PlayerObjects;
import hughai.basictypes.*;
import hughai.*;
import hughai.loader.utils.TransLoadStorage;
import hughai.mapping.Metal.MetalPos;
import hughai.mapping.Metal.MetalSpot;
import hughai.mapping.SlopeMap.ButtonSlopeDistribution;
import hughai.ui.MainUI;
import hughai.utils.*;

// holds the heightmap of the map
// since this takes ages to load the heightmap, for now, this 
// is cached in the loader module (for speed between ai reloads during dev)
// and in a cache file (which accelerates initial loading at each game
// start, except for the very first, very slightly, but not enough.
// See explanation below for why we can't just use getheightmap
//
// notes on heightmap:
// aicallback.getElevationAt calls ultimately CGround::GetHeight2( float x, float y );
// aicallback.getMap().getHeightMap() returns the heights at the center of each square,
// rather than those at the corners
//
// notes on CGround::GetHeight2 (x,y) algo:
// - first determines the square coordinates, ie sx,sy , which is basically x/8,y/8 for ints 
// - and the coordinates within the square, deltax, deltay, the distance from the corner of the square
//     ( so sounds like ,reasonable guess, this is going to interpolate, and give
//       the exact height at the exact x,y coordinates)
// - hs is ... oh, I reckon it's the position of the square height within the one-dimensional
//   heightmap float array
// - then it is using readmap->GetHeightMap(), which sounds like it gives the 
//   corner heights of the map
// 
// so, this class's, HeightMap's heightmap, gives the corner
// heights, the original readmap->GetHeightMap() ...
//
// If we wanted to avoid making multiple calls to getElevationAt, in order
// to determine the heightmap, I suppose we have a couple of possibilities:
// - somehow reverse the process of getting the center height, to get the corner
//   heights, but that sounds tricky ...
// - just read the original mapfile using java.io ?
//
// thinking about getting corner heights from center heights, I feel maybe
// there is insufficient information.  Imagine a 1-d map, with just three heights:
//
//                                         h[2]
//
//
//                                 c[1]
//
//
//                         h[1]
//            c[0]
//  h[0]
//
// .. then: c[0] = (h[0] + h[1]) / 2
//          c[1] = (h[1] + h[2]) / 2
// going from h -> c, we have three knowns going to two unknowns
// going from c -> h, we have two knowns somehow determining three unknowns,
// which is insufficiently specified
// or, looking at it intuitively, if we fix c[0] and c[1], we can imagine the 
// two lines going through h[0],h[1] and h[1],h[2] being able to fold and unfold
// like a hinge without leaving the fixed points provided by c[0] and c[1]
//
// Looking at just reading the mapfile directly using java.io, the issue
// is that the Java Interface v0.1 does not seem to currently expose the
// classes/methods we would need to easily obtain the map file.
//
// Without having such methods in the AI, we'd basically need to rewrite a 
// whole chunk of Spring code:
// - parse the script file (if we even know which script file it is...)
// - locate the mapfile in the various locations that Spring searches in,
//   which is quite a lot of work in itself, since different os's have
//   different locations, and how am I going to test all of that?
// - use 7z to decompress the actual map image from within the mapfile
// ... ok, that's it, but each of those steps is at least several hours work,
//     maybe more, maybe several man-days per step?
//

public class HeightMap
{
   CSAI csai;
   LogFile logFile;
   Map gameMap;
   PlayerObjects playerObjects;
   //	MovementMaps movementMaps;
   //Maps maps;

   private float[][] heightMap; // corner heights of each map square
   
   public static final int granularity = 1;
   
   public static class HeightMapPos extends Int2 {
      public HeightMapPos() {
         
      }
      public HeightMapPos( Int2 int2 ) {
         x = int2.x;
         y = int2.y;
      }
      public HeightMapPos( int x, int y ) {
         super( x, y );
      }
      public TerrainPos toTerrainPos() {
         return new TerrainPos( x * 8 * granularity, 0, y * 8 * granularity );
      }
      public static HeightMapPos fromTerrainPos( TerrainPos terrainPos ) {
         return new HeightMapPos( (int)terrainPos.x / 8 / granularity,
               (int)terrainPos.z / 8 / granularity );
      }
   }
   
   // returns the top left corner height of the square at heightMapPos
   public float getElevationAt( HeightMapPos heightMapPos ) {
      GetHeightMap();
      return heightMap[heightMapPos.x][heightMapPos.y];
   }
   
   // technically this isn't quite right, since it gives the top-left corner
   // height of the square that this point is in.
   // we could copy/paste the code from CGround::GetHeight2, in order to return
   // the exact elevation, though I'm not sure there's anything that really 
   // needs such a precise calculation of elevation?
   public float getElevationAt( TerrainPos terrainPos ) {
      GetHeightMap();
      HeightMapPos heightMapPos = HeightMapPos.fromTerrainPos( terrainPos );
      return getElevationAt( heightMapPos );
   }

   public HeightMap( PlayerObjects playerObjects )
   {
      this.csai = playerObjects.getCSAI();
      this.logFile = playerObjects.getLogFile();
      gameMap = playerObjects.getAicallback().getMap();
      this.playerObjects = playerObjects;
      //		movementMaps = playerObjects.getMovementMaps();
      //maps = playerObjects.getMaps();
      
      csai.RegisterVoiceCommand( "showheightmap", new VoiceShowHeightMap() );
      
      playerObjects.getMainUI().registerButton( "Dump heightmap distribution",
            new ButtonHeightDistribution() );
      playerObjects.getMainUI().registerButton( "Calculate heightmap",
            new ButtonCalculateHeightMap() );
      playerObjects.getMainUI().registerButton( "Get heightmap from cache",
            new ButtonGetHeightMapFromCache() );
   }

   class VoiceShowHeightMap implements VoiceCommandHandler {
      @Override
      public void commandReceived( String command, String[] args, int playerid ) {
         int height = Integer.parseInt( args[2] );
         csai.sendTextMessage( "drawing height map for contour height " + height );
         showHeightMap( height );
      }
   }
   
   void showHeightMap( int height ) {
      float[][]heightmap = GetHeightMap();
      int mapwidth = heightmap.length;
      int mapheight = heightmap[0].length;
      boolean contourheight[][] = new boolean[mapwidth][mapheight];
      for( int x = 0; x < mapwidth; x++ ) {
         for( int z = 0; z < mapheight; z++ ) {
            contourheight[x][z] =
               (heightmap[x][z]>=height);
         }         
      }
      playerObjects.getDrawingUtils().DrawMap( contourheight );
   }
   
   @Override
   protected void finalize() {
      System.out.println( this.getClass().getSimpleName() + ".finalize()");
   }

   class ButtonCalculateHeightMap implements MainUI.ButtonHandler { 
      @Override
      public void go() {
         calculateHeightMap();
      }
   }

   class ButtonGetHeightMapFromCache implements MainUI.ButtonHandler { 
      @Override
      public void go() {
         LoadCache();
      }
   }

   class ButtonHeightDistribution implements MainUI.ButtonHandler { 
      @Override
      public void go() {
         float[][]heightmap = GetHeightMap();
         logFile.WriteLine( "heightmap distribution:" );
         new MapHelper( playerObjects ).DumpMapDistribution( heightmap );
      }
   }

   private float[][] GetHeightMap()
   {
         if( heightMap != null ) {
            return heightMap;
         }
         if( LoadCoreStorage() ) {
            return heightMap;
         }
         if( LoadCache() ) {
            SaveCoreStorage();
            return heightMap;
         }
         calculateHeightMap();
         SaveCache();
         SaveCoreStorage();
         return heightMap;
   }
   
   public void calculateHeightMap() {
      logFile.WriteLine("Getting heightmap, this could take a while... ");
      int mapwidth = gameMap.getWidth();
      int mapheight = gameMap.getHeight();
      heightMap = new float[mapwidth + 1][ mapheight + 1];
      for (int x = 0; x < mapwidth + 1; x++)
      {
         for (int y = 0; y < mapheight + 1; y++)
         {
            heightMap[x][ y] = gameMap.getElevationAt(x 
                  * playerObjects.getMaps().getMovementMaps().SQUARE_SIZE, y 
                  * playerObjects.getMaps().getMovementMaps().SQUARE_SIZE);
         }
      }
//      SaveCache();
//      SaveCoreStorage();
   }
   
   // save across ai reloads, to accelerate reload time
   
   boolean LoadCoreStorage() {
      Object storedHeightMap = TransLoadStorage.getInstance().getAttribute( "heightmap" );
      if( storedHeightMap != null ) {
         logFile.WriteLine( "retrieved height map from trans reload cache" );
         heightMap = (float[][])storedHeightMap;
         return true;
      }
      return false;
   }
   
   // we can just store the height map directly, since it's just an array of 
   // standard system floats, doesn't use any custom ai classes
   void SaveCoreStorage() {
      logFile.WriteLine( "storing heightmap in transcache" );
      TransLoadStorage.getInstance().setAttribute( "heightmap", heightMap );
      System.out.println( "after storage, value in cache is: " + TransLoadStorage.getInstance().getAttribute( "heightmap"  ) );
   }

   String version = "0.2"; // for cache

   String getCacheFilepath() {
      String MapName = gameMap.getName();
      return csai.getCacheDirectoryPath() + File.separator +  MapName + "_heightmap"
      + "_" + playerObjects.getAicallback().getTeamId() + "_" + version + ".dat";      
   }

   // save cache to speed up load times.  storing as xml is sloooowwww
   void SaveCacheOld()
   {
      logFile.WriteLine( "saving height map to cache..." );
      String cachefilepath = getCacheFilepath();
      Document cachedom = XmlHelper.CreateDom();
      Element metadata = XmlHelper.AddChild( cachedom.getDocumentElement(), "metadata" );
      metadata.setAttribute( "type", "HeightMap" );
      metadata.setAttribute( "map", gameMap.getName() );
      metadata.setAttribute( "version", version );
      //      metadata.setAttribute( "datecreated", timeHelper.GetCurrentRealTimeString() );
      metadata.setAttribute( "mapheight", "" + gameMap.getHeight() );
      metadata.setAttribute( "mapwidth", "" + gameMap.getWidth() );

      Element heights = XmlHelper.AddChild( cachedom.getDocumentElement(), "heights" );
      for (int x = 0; x < gameMap.getWidth()  + 1; x++)
      {
         for (int y = 0; y < gameMap.getHeight()  + 1; y++)
         {
            Element height = XmlHelper.AddChild( heights, "height" );
            height.setAttribute( "height", "" + heightMap[x][y] );
         }
      }

      XmlHelper.SaveDom( cachedom, cachefilepath );
      logFile.WriteLine( " ... done" );
   }

   // save cache to speed up load times.  storing as xml is sloooowwww
   void SaveCache()
   {
      try {
         logFile.WriteLine( "saving height map to cache..." );
         String cachefilepath = getCacheFilepath();

         BufferedOutputStream fileOutputStream = new BufferedOutputStream( 
               new FileOutputStream( cachefilepath ) );
         ObjectOutputStream objectOutputStream = new ObjectOutputStream( fileOutputStream );
         for (int x = 0; x < gameMap.getWidth()  + 1; x++)
         {
            for (int y = 0; y < gameMap.getHeight()  + 1; y++)
            {
               objectOutputStream.writeFloat( heightMap[x][y] );
               //fileOutputStream.write( Converter.floatToBytes( heightMap[x][y] ) );
            }
         }     
         objectOutputStream.close();
         fileOutputStream.close();

         logFile.WriteLine( " ... done" );
      } catch( Exception e ) {
         logFile.WriteLine( Formatting.exceptionToStackTrace( e ) );
         throw new RuntimeException( e );
      }
   }

   boolean LoadCache() {
      try {
         if( new File( getCacheFilepath() ).exists() ) {
            logFile.WriteLine( "Loading heightmap from cache..." );

//            byte[] buffer = new byte[ ( gameMap.getWidth()  + 1 ) * ( gameMap.getWidth()  + 1 ) * 4 ];
            BufferedInputStream inputStream = new BufferedInputStream( 
                  new FileInputStream( getCacheFilepath() ) );
            ObjectInputStream objectInputStream = new ObjectInputStream( inputStream );
            
//            inputStream.read( buffer );

            int mapwidth = gameMap.getWidth();
            int mapheight = gameMap.getHeight();
            heightMap = new float[mapwidth + 1][ mapheight + 1];
            int offset = 0;
            for (int x = 0; x < gameMap.getWidth()  + 1; x++)
            {
               for (int y = 0; y < gameMap.getHeight()  + 1; y++)
               {
//                  heightMap[x][y] = Converter.bytesToFloat( buffer, offset );                  
                  heightMap[x][y] = objectInputStream.readFloat();
                  offset += 4;
               }
            }
            
            objectInputStream.close();
            inputStream.close();
            
            logFile.WriteLine( " ... done." );
            return true;
         } else {
            return false;
         }
      } catch( Exception e ) {
         logFile.WriteLine( Formatting.exceptionToStackTrace( e ) );
         throw new RuntimeException( e );
      }
   }
}

