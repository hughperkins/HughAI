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

package hughai.utils;

import java.util.*;

import com.springrts.ai.*;
import com.springrts.ai.command.*;
import com.springrts.ai.oo.*;
import com.springrts.ai.oo.Map;

import hughai.CSAI;
import hughai.GameAdapter;
import hughai.PlayerObjects;
import hughai.VoiceCommandHandler;
import hughai.basictypes.*;
import hughai.*;
import hughai.mapping.HeightMap;
import hughai.ui.MainUI;
import hughai.unitdata.*;

// methods to draw stuff on the map
public class DrawingUtils
{
   CSAI csai;
   OOAICallback aicallback;
   BuildTable buildTable;
   LogFile logfile;
   Config config;
   //int squaresize;
   PlayerObjects playerObjects;

   final int maxlines;
   int linesDrawn = 0;

   public DrawingUtils( PlayerObjects playerObjects ){
      this.csai = playerObjects.getCSAI();
      this.aicallback = playerObjects.getAicallback();
      this.buildTable = playerObjects.getBuildTable();
      this.logfile = playerObjects.getLogFile();
      this.config = playerObjects.getConfig();

      this.playerObjects = playerObjects;

      maxlines = config.getMaxLinesOnMap();

      csai.registerGameListener( new GameListener() );
      //      csai.RegisterVoiceCommand( "cleanmap", new VoiceCleanMap() );
      csai.RegisterVoiceCommand( "drawpoint", new VoiceDrawPoint() );

      playerObjects.getMainUI().registerButton( "Clean map lines", new CleanMapButton() );
   }

   class CleanMapButton implements MainUI.ButtonHandler {
      @Override
      public void go() {
         CleanDrawing();
      }
   }


   class VoiceDrawPoint implements VoiceCommandHandler {
      @Override
      public void commandReceived( String command, String[] args, int player ) {
         float posx = Float.parseFloat( args[2] );
         float posz = Float.parseFloat( args[3] );
         drawText(new TerrainPos( posx, 0, posz ), "(" + posx + ", " + posz + ")" );;
      }   
   }

   class VoiceCleanMap implements VoiceCommandHandler {
      @Override
      public void commandReceived( String command, String[] args, int player ) {
         CleanDrawing();
      }
   }

   class GameListener extends GameAdapter {
      //      @Override
      //      public void Tick( int frame ) {
      ////         if( frame % ( 150 * 5 ) == 0 ) {
      ////           // csai.sendTextMessage( "" + frame );
      ////           // CleanDrawing();
      ////         }
      //      }
   }

   public void CleanDrawing() {
      csai.sendTextMessage( "cleaning map..." );
      pushCheating();
      setCheating( false );
      int squaresize = playerObjects.getMaps().getMovementMaps().SQUARE_SIZE;
      int mapwidth = aicallback.getMap().getWidth() * squaresize;
      int mapheight = aicallback.getMap().getHeight() * squaresize;
      logfile.WriteLine( "cleandrawing, " + mapwidth + " x " + mapheight );
      for( int z = 0; z < mapheight; z+= 100 ) {
         for( int x = 0; x < mapwidth; x+= 100 ) {
            float elevation = aicallback.getMap().getElevationAt( x, z );
            csai.handleEngineCommand(
                  new RemovePointDrawAICommand( new AIFloat3( x, elevation, z )));
         }

      }
      popCheating();
      csai.sendTextMessage( " ... done" );
      linesDrawn = 0;
   }

   public void setFigureColor(float r, float g, float b){
      // TODO
      //      csai.handleEngineCommand(
      //            new AddLineDrawAICommand(startpos, endpos));
   }

   public void AddLine(TerrainPos startpos, TerrainPos endpos ) {
      //      csai.handleEngineCommand(
      //            new CreateLineFigureDrawerAICommand(
      //                  startpos,
      //                  endpos,
      //                  2,
      //                  false,
      //                  200,
      //                  -1,
      //                  -1) );
      //                  AIFloat3 pos1, AIFloat3 pos2, float width, 
      //                  boolean arrow, int lifeTime, int figureGroupId, 
      //                  int ret_newFigureGroupId)
         if( linesDrawn >= maxlines ) {
            csai.sendTextMessage( "DrawingUtils.AddLine: too many lines drawn.  drawing more would crash Spring..." );
            return;
            //         throw new RuntimeException("DrawingUtils.AddLine: too many lines drawn.  drawing more would crash Spring..." );
         }
         pushCheating();
         setCheating( false );
         csai.handleEngineCommand(
               new AddLineDrawAICommand(startpos.toAIFloat3(), endpos.toAIFloat3()));
         linesDrawn++;
         popCheating();
   }
   
   Stack<Boolean> cheatingStack = new Stack<Boolean>(); 
   void pushCheating() {
      cheatingStack.push( isCheating() );
   }
   
   boolean isCheating() {
      return playerObjects.getAicallback().getCheats().isEnabled();
   }
   
   void setCheating( boolean cheating ) {
      if( isCheating() != cheating ) {
         playerObjects.getAicallback().getCheats().setEnabled( cheating );
      }
   }
   
   void popCheating() {
      boolean oldCheating = cheatingStack.pop(); 
      setCheating( oldCheating );
   }

   //   public void AddLine(Float3 startpos, Float3 endpos ) {
   //      AddLine(startpos, endpos);
   //   }

   public void DrawUnit( int toDrawUnitDefId, TerrainPos pos, float rotation, int lifeTime, int teamId, boolean transparent, boolean drawBorder, int facing) {
      csai.handleEngineCommand(
            new DrawUnitDrawerAICommand( toDrawUnitDefId, pos.toAIFloat3(), rotation, lifeTime, teamId, transparent, drawBorder, facing));		
   }

   public void DrawUnit( String defname, TerrainPos pos ) {
      DrawUnit( defname, pos, 0, 200, aicallback.getTeamId(), true, false );
   }

   public void DrawUnit( String defname, TerrainPos pos, float rotation, int lifeTime, int teamId, boolean transparent, boolean drawBorder ) {
      if( pos == null ){ throw new RuntimeException( "drawunit: pos was null"); }
      UnitDef toDrawUnitDef = buildTable.getUnitDefByName( defname.toLowerCase() );
      if( toDrawUnitDef == null ){ throw new RuntimeException( "unit " + defname + " doesn't exist."); }

      lifeTime = 200;
      csai.handleEngineCommand(
            new DrawUnitDrawerAICommand(toDrawUnitDef, pos.toAIFloat3(), rotation, lifeTime, teamId, transparent, drawBorder, 0 ) );		
   }

   public void drawText( TerrainPos pos, String text ) {
      pushCheating();
      setCheating( false );
      csai.handleEngineCommand(
            new AddPointDrawAICommand( pos.toAIFloat3(), text ) );
      popCheating();
   }

   public void DrawRectangle(TerrainPos pos, int width, int height )
   {
      //Float3 pos = new Float3(inpos);
      float elevation = aicallback.getMap().getElevationAt(pos.x, pos.z) + 10;
      AddLine(pos.add( new TerrainPos(0f, elevation, 0f)  ),
            pos.add( new TerrainPos(width, elevation, 0) ) );
      AddLine(pos.add( new TerrainPos(width, elevation, 0) ),
            pos.add( new TerrainPos(width, elevation, height) ) );
      AddLine(pos.add( new TerrainPos(width, elevation, height) ),
            pos.add( new TerrainPos(0, elevation, height) ) );
      AddLine(pos.add( new TerrainPos(0, elevation, height) ),
            pos.add( new TerrainPos(0, elevation, 0) ) );
   }

   public void DrawCircle( TerrainPos pos, double radius )
   {
      TerrainPos lastpos = null;

      for( int angle = 0; angle <= 360; angle += 20 )
      {
         int x = (int)( (double)radius * Math.cos ( (double)angle * Math.PI / 180 ) );
         int y = (int)( (double)radius * Math.sin ( (double)angle * Math.PI / 180 ) );
         TerrainPos thispos = new TerrainPos( x, 0, y ).add( pos );
         if( lastpos != null )
         {
            AddLine( thispos,lastpos );
         }
         lastpos = thispos;
      }
   }

   // returns figure group
   public void DrawMap(int[][] map )
   {
      Map gamemap = aicallback.getMap();
      int thismapwidth = map.length;
      int thismapheight = map[0].length;
      int gamemapwidth = gamemap.getWidth();
      int gamemapheight = gamemap.getHeight();
      int multiplier = ( gamemapwidth / thismapwidth ) * 8;

      int figuregroup = 0;
      int granularity = config.getMapDrawGranularity();
      int skip = ( granularity * thismapwidth ) / gamemapwidth; // so, if they are the same, skip will be equal to granularity
      // if thismapwidith is half gamemapwidth,  then skip will be granularity / 2
      logfile.WriteLine( "drawingUtils.drawmap granularity: " + granularity 
            + " skip: " + skip + " mapwidth: " + thismapwidth + " / " + gamemapwidth
            + " x " + thismapheight + " / " + gamemapheight );
      if( skip < 1 ) { skip = 1; }
      HeightMap heightMap = playerObjects.getMaps().getHeightMap();
//      float[][] heightMapArray = heightMap.GetHeightMap();
      for( int z = 0; z < thismapheight; z += skip )
      {
         for (int x = 0; x < thismapwidth; x+= skip )
         {
            //            float elevation = gamemap.getElevationAt( x *multiplier, y * multiplier ) + 10;
//            float elevation = heightMapArray[x][z];
            float elevation = heightMap.getElevationAt( 
                  new HeightMap.HeightMapPos( x, z ) );
            if (x < (thismapwidth - skip) &&
                  map[x][z] != map[x + skip][z] )
            {
               AddLine(new TerrainPos((x + skip) * multiplier, elevation, z * multiplier),
                     new TerrainPos((x + skip) * multiplier, elevation, (z + skip) * multiplier) );
            }
            if (z < (thismapheight - skip) &&
                  map[x][z] != map[x][z + skip] )
            {
               AddLine(new TerrainPos(x * multiplier, elevation, (z + skip) * multiplier),
                     new TerrainPos((x + skip) * multiplier, elevation, (z + skip) * multiplier) );
            }
         }
      }
   }
   // returns figure group
   public void DrawMap(boolean[][] map)
   {
      Map gamemap = aicallback.getMap();
      int thismapwidth = map.length;
      int thismapheight = map[0].length;
      int multiplier = ( gamemap.getWidth() / thismapwidth ) * 8;

      int figuregroup = 0;
      for (int y = 1; y < thismapheight - 1; y++)
         //      for (int y = 0; y < thismapheight; y++)
      {
         for (int x = 1; x < thismapwidth - 1; x++)
            //         for (int x = 0; x < thismapwidth; x++)
         {
            if (x < (thismapwidth - 2) &&
                  //            if (x < (thismapwidth - 1) &&
                  map[x][y] != map[x + 1][y])
            {
               float elevation = gamemap.getElevationAt(x * multiplier, y * multiplier) + 10;
               AddLine(new TerrainPos((x + 1) * multiplier, elevation, y * multiplier),
                     new TerrainPos((x + 1) * multiplier, elevation, (y + 1) * multiplier) );
            }
            if (y < (thismapheight - 2) &&
                  //            if (y < (thismapheight - 1) &&
                  map[x][y] != map[x][y + 1])
            {
               float elevation = gamemap.getElevationAt(x * multiplier, y * multiplier) + 10;
               AddLine(new TerrainPos(x * multiplier, elevation, (y + 1) * multiplier),
                     new TerrainPos((x + 1) * multiplier, elevation, (y + 1) * multiplier) );
            }
         }
      }
   }
}

