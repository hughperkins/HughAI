// Copyright Spring project, Hugh Perkins 2006, 2009
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
import java.io.*;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;
import com.springrts.ai.oo.Map;

import hughai.basictypes.*;
import hughai.*;
import hughai.utils.*;
import hughai.ui.*; 

public class SlopeMap
{
   CSAI csai;
   OOAICallback aicallback;
   LogFile logfile;
   //	MovementMaps movementMaps;
   Map gameMap;
   //	HeightMap heightMap;
   //	Maps playerObjects.getMaps();
   PlayerObjects playerObjects;

   float[][] SlopeMap;

   public SlopeMap( PlayerObjects playerObjects )
   {
      csai = playerObjects.getCSAI();
      aicallback = csai.aicallback;
      logfile = playerObjects.getLogFile();
      gameMap = aicallback.getMap();
      this.playerObjects = playerObjects;
      //		movementMaps = playerObjects.getMovementMaps();
      //		heightMap = playerObjects.getHeightMap();
      //		playerObjects.getMaps() = playerObjects.getMaps();
      
      playerObjects.getMainUI().registerButton( "Dump slopemap distribution",
            new ButtonSlopeDistribution() );
   }

   class ButtonSlopeDistribution implements MainUI.ButtonHandler { 
      @Override
      public void go() {
         float[][]slopemap = GetSlopeMap();
         logfile.WriteLine( "Slopemap distribution:" );
         new MapHelper( playerObjects ).DumpMapDistribution( slopemap );
      }
   }

   // ported from Spring's ReadMap.cpp by Hugh Perkins
   public float[][]GetSlopeMap()
   {
      if( SlopeMap != null ) {
         return SlopeMap;
      }
      int mapwidth = gameMap.getWidth();
      int mapheight = gameMap.getHeight();

      int slopemapwidth = mapwidth / 2;
      int slopemapheight = mapheight / 2;

      int squaresize = playerObjects.getMaps().getMovementMaps().SQUARE_SIZE; // jsut to save typing...

      float[][] heightmap = playerObjects.getMaps().getHeightMap().GetHeightMap();
      logfile.WriteLine( "Getting heightmap, this could take a while... " );            

      // ArrayIndexer heightmapindexer = new ArrayIndexer( mapwidth + 1, mapheight + 1 );
      logfile.WriteLine("calculating slopes..." );
      logfile.WriteLine("mapwidth: " + slopemapwidth + " " + slopemapheight);

      SlopeMap = new float[ slopemapwidth][ slopemapheight ];
      for(int y = 2; y < mapheight - 2; y+= 2)
      {
         for(int x = 2; x < mapwidth - 2; x+= 2)
         {
            Float3 e1 = new Float3(-squaresize * 4, heightmap[x - 1][ y - 1] - heightmap[x + 3][ y - 1], 0);
            Float3 e2 = new Float3(0, heightmap[x - 1][ y - 1] - heightmap[x - 1][ y + 3], -squaresize * 4);

            Float3 n=e2.Cross( e1 );

            n.Normalize();

            e1 = new Float3(squaresize * 4, heightmap[x + 3][ y + 3] - heightmap[x - 1][ y + 3], 0);
            e2 = new Float3(0, heightmap[x + 3][ y + 3] - heightmap[x + 3][ y - 1], squaresize * 4);

            Float3 n2 = e2.Cross(e1);
            n2.Normalize();

            SlopeMap[ x / 2][ y / 2 ]= 1 - ( n.y + n2.y ) * 0.5f;
         }
      }
      logfile.WriteLine("... slopes calculated" );
      return SlopeMap;
   }
}
