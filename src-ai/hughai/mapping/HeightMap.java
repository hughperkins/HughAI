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
import hughai.mapping.Metal.MetalSpot;
import hughai.mapping.SlopeMap.ButtonSlopeDistribution;
import hughai.ui.MainUI;
import hughai.utils.*;



// holds the heightmap of the map
// since this takes ages to load the heightmap, for now, this 
// is cached in the loadeer module (for speed between ai reloads during dev)
// and in a cache file (which accelerates initial loading at each game
// start, except for the very first, very slightly, but not enough.
// perhaps we can just use getMap().getHeightMap(), but I can't remember
// why that is hard :-D
public class HeightMap
{
   CSAI csai;
   LogFile logFile;
   Map gameMap;
   PlayerObjects playerObjects;
   //	MovementMaps movementMaps;
   //Maps maps;

   float[][] heightMap;

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

   public float[][] GetHeightMap()
   {
         if( heightMap != null ) {
            return heightMap;
         }
         if( LoadCoreStorage() ) {
            return heightMap;
         }
//         if( LoadCache() ) {
//            SaveCoreStorage();
//            return heightMap;
//         }
         calculateHeightMap();
//         SaveCache();
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
         e.printStackTrace();
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
         e.printStackTrace();
         throw new RuntimeException( e );
      }
   }
}

