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

package hughai.test;

import java.util.*;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;
import com.springrts.ai.command.*;
import com.springrts.ai.oo.Map;

import hughai.*;
import hughai.unitdata.*;
import hughai.utils.*;
import hughai.basictypes.*;
import hughai.ui.*;

// performance tests and such...
public class Tester {
   Config config;
   PlayerObjects playerObjects;
   OOAICallback aicallback;
   LogFile logfile;
   CSAI csai;

   public Tester( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;

      csai = playerObjects.getCSAI();
      config = playerObjects.getConfig();
      aicallback = playerObjects.getAicallback();
      logfile = playerObjects.getLogFile();

      //      csai.RegisterVoiceCommand( "runtests", new VoiceGo() );
      playerObjects.getMainUI().registerButton( "Run tests",
            new ButtonRunTests() );
      playerObjects.getMainUI().registerButton( "Run drawline tests",
            new ButtonRunDrawlineTests() );
   }

   class ButtonRunTests implements MainUI.ButtonHandler {
      @Override
      public void go() {
         csai.sendTextMessage( "Running tests.   please check console" );
         runTests();
      }
   }

   class ButtonRunDrawlineTests implements MainUI.ButtonHandler {
      @Override
      public void go() {
         csai.sendTextMessage( "Running drawline tests." );
         drawlineTests();
      }
   }

   void drawlineTests() {
      drawlineTests3();
      //    drawlineTests2();
      //      drawmapTests1();
//      drawmapTests2();
   }

   // This will crash spring, at least with Java interface 0.1,
   // running against Spring 0.80.02 :
   void drawlineTests2() {
      CSAI csai = playerObjects.getCSAI();
      csai.handleEngineCommand(
            new CreateLineFigureDrawerAICommand(
                  new AIFloat3( 0,100,0 ),
                  new AIFloat3( 100, 100 , 0 ),
                  1,
                  false,
                  200,
                  0,
                  0 ) );
      //      AIFloat3 pos1, AIFloat3 pos2, float width, 
      //      boolean arrow, int lifeTime, int figureGroupId, 
      //      int ret_newFigureGroupId)
   }

   void drawmapTests1() {
      Map gamemap = aicallback.getMap();
      int mapwidth = gamemap.getWidth();
      int mapheight = gamemap.getHeight();
      boolean[][]map = new boolean[mapwidth][mapheight];
      DrawingUtils drawingUtils = playerObjects.getDrawingUtils();

      for( int x = 100; x < 320; x++ ) {
         for( int z = 100; z < 320; z++ ) {
            map[x][z] = true;
         }
      }
      drawingUtils.DrawMap( map );
   }

   void drawmapTests2() {
      Map gamemap = aicallback.getMap();
      int mapwidth = gamemap.getWidth();
      int mapheight = gamemap.getHeight();
      boolean[][]map = new boolean[mapwidth][mapheight];
      DrawingUtils drawingUtils = playerObjects.getDrawingUtils();

      int thismapwidth = mapwidth;
      int thismapheight = mapheight;

      for( int x = 100; x < 320; x++ ) {
         for( int z = 100; z < 320; z++ ) {
            map[x][z] = true;
         }
      }
      final int multiplier = 8;
      for( int z = 0; z < thismapheight; z += 1 )
      {
         for (int x = 0; x < thismapwidth; x+= 1 )
         {
            //            float elevation = gamemap.getElevationAt( x *multiplier, y * multiplier ) + 10;
            //float elevation = heightMapArray[x][z];
            float elevation = 100;
            if (x < (thismapwidth - 1) &&
                  map[x][z] != map[x + 1][z] )
            {
               drawingUtils.AddLine(new TerrainPos((x + 1) * multiplier, elevation, z * multiplier),
                     new TerrainPos((x + 1) * multiplier, elevation, (z + 1) * multiplier) );
            }
            if (z < (thismapheight - 1) &&
                  map[x][z] != map[x][z + 1] )
            {
               drawingUtils.AddLine(new TerrainPos(x * multiplier, elevation, (z + 1) * multiplier),
                     new TerrainPos((x + 1) * multiplier, elevation, (z + 1) * multiplier) );
            }
         }
      }
   }

   // this crashes spring as soon as the area where the lines are being
   // drawn comes into view...
   void drawlineTests3() {
      Map gamemap = aicallback.getMap();
      int mapwidth = gamemap.getWidth();
      int mapheight = gamemap.getHeight();
      DrawingUtils drawingUtils = playerObjects.getDrawingUtils();
      for( int i = 0; i < 8193; i++ ) {
         drawingUtils.AddLine( new TerrainPos( 100, 100, 100 ), 
               new TerrainPos( 200, 100, 100 ) );
      }
   }
   
   void drawlineTests1() {
      Map gamemap = aicallback.getMap();
      int mapwidth = gamemap.getWidth();
      int mapheight = gamemap.getHeight();
      DrawingUtils drawingUtils = playerObjects.getDrawingUtils();
      drawingUtils.AddLine( new TerrainPos( -100, 100, -100 ), 
            new TerrainPos( 5000, 100, 5000 ) );
      for( int z = 0; z < mapheight; z++ ) {
         drawingUtils.AddLine( new TerrainPos( 0, 100, z * 8 ), 
               new TerrainPos( 100, 100, z * 8 ) );         
         drawingUtils.AddLine( new TerrainPos( (mapwidth - 10 ) * 8, 100, z * 8 ), 
               new TerrainPos( mapwidth * 8, 100, z * 8 ) );         
         drawingUtils.AddLine( new TerrainPos( 0, 100, z * 8 ), 
               new TerrainPos( mapwidth * 8, 100, z * 8 ) );         
      }
      for( int x = 0; x < mapheight; x++ ) {
         drawingUtils.AddLine( new TerrainPos( x * 8, 100, 0 ), 
               new TerrainPos( x * 8, 100, 1000 ) );         
         drawingUtils.AddLine( new TerrainPos( x * 8, 100, (mapheight - 10 ) * 8 ), 
               new TerrainPos( x * 8, 100, (mapheight - 0 ) * 8 ) );         
         drawingUtils.AddLine( new TerrainPos( x * 8, 100, 0 ), 
               new TerrainPos( x * 8, 100, (mapheight - 0 ) * 8 ) );         
      }
   }

   class VoiceGo implements VoiceCommandHandler {
      @Override
      public void commandReceived( String command, String[] args, int team ) {
         runTests();
      }
   }

   void runTests() {
      testgetmilliseconds();
      testgetunitdefs();
      testgetfriendlyunits();
      testgetpos();      
      testgetunitdef();     
      testgetsquareddistance();
      testismobile();
      testunithash();
      testgethumanname();
      testgetspeed();
      testCheats();
   }
   
   void debug( Object message ) {
      logfile.WriteLine( "" + message );      
   }
   
   void dumpEnemyUnits() {
      debug("num units: " + aicallback.getEnemyUnits().size() );
      for( Unit unit : aicallback.getEnemyUnits() ) {
         debug( unit.getUnitId() + " " + unit.getDef().getHumanName() + " " + unit.getPos() );
      }      
   }
   
   void testCheats() {
      boolean previousCheatState = aicallback.getCheats().isEnabled();
      
      logfile.WriteLine( "Enemy units with cheating off:" );
      aicallback.getCheats().setEnabled( false );
      dumpEnemyUnits();
      logfile.WriteLine( "Enemy units with cheating ON:" );
      aicallback.getCheats().setEnabled( true );
      dumpEnemyUnits();
      
      aicallback.getCheats().setEnabled( previousCheatState );
   }

   // control test: no test can go faster than this one
   void testgetmilliseconds() {
      //    logfile.WriteLine( "testgetmilliseconds >>>" );
      int count = 0;
      long start = System.currentTimeMillis();
      long finish = start + 1000;
      while( ( (byte)count != 0 ) || ( System.currentTimeMillis() < finish ) ) {
         count++;
      }
      logfile.WriteLine( "testgetmilliseconds count: " + count );
   }

   void testgetunitdefs() {
      //      logfile.WriteLine( "testgetunitdefs >>>" );
      int count = 0;
      long start = System.currentTimeMillis();
      long finish = start + 1000;
      while( ( (byte)count != 0 ) || ( System.currentTimeMillis() < finish ) ) {
         aicallback.getUnitDefs();
         count++;
      }
      logfile.WriteLine( "testgetunitdefs count: " + count );
   }

   void testgetfriendlyunits() {
      int count = 0;
      long start = System.currentTimeMillis();
      long finish = start + 1000;
      while( ( (byte)count != 0 ) || ( System.currentTimeMillis() < finish ) ) {
         aicallback.getFriendlyUnits();
         count++;
      }
      logfile.WriteLine( "testgetfriendlyunits count: " + count );      
   }

   void testgetpos() {
      Unit unit = aicallback.getFriendlyUnits().get( 0 );
      int count = 0;
      long start = System.currentTimeMillis();
      long finish = start + 1000;
      while( ( (byte)count != 0 ) || ( System.currentTimeMillis() < finish ) ) {
         AIFloat3 pos = unit.getPos();
         count++;
      }
      logfile.WriteLine( "testgetpos count: " + count );      
   }

   void testgetunitdef() {
      Unit unit = aicallback.getFriendlyUnits().get( 0 );
      int count = 0;
      long start = System.currentTimeMillis();
      long finish = start + 1000;
      while( ( (byte)count != 0 ) || ( System.currentTimeMillis() < finish ) ) {
         UnitDef unitdef = unit.getDef();
         count++;
      }
      logfile.WriteLine( "testgetunitdef count: " + count );            
   }

   void testgethumanname() {
      UnitDef unitdef = aicallback.getFriendlyUnits().get( 0 ).getDef();
      int count = 0;
      long start = System.currentTimeMillis();
      long finish = start + 1000;
      while( ( (byte)count != 0 ) || ( System.currentTimeMillis() < finish ) ) {
         String name = unitdef.getHumanName();
         if( name.equals( "foo" ) ) {
            logfile.WriteLine( "blah" );
         }
         count++;
      }
      logfile.WriteLine( "testgethumanname count: " + count );            
   }

   void testgetspeed() {
      UnitDef unitdef = aicallback.getFriendlyUnits().get( 0 ).getDef();
      int count = 0;
      long start = System.currentTimeMillis();
      long finish = start + 1000;
      while( ( (byte)count != 0 ) || ( System.currentTimeMillis() < finish ) ) {
         float speed = unitdef.getSpeed();
         count++;
      }
      logfile.WriteLine( "testgetspeed count: " + count );            
   }

   void testgetsquareddistance() {
      int count = 0;
      long start = System.currentTimeMillis();
      long finish = start + 1000;
      Float3 a = new Float3( 1000.3353f, 125.25f, 2353.35f );
      Float3 b = new Float3( 3550.3353f, 130.25f, 3235.35f );
      long totaldistance = 0;
      while( ( (byte)count != 0 ) || ( System.currentTimeMillis() < finish ) ) {
         float distance = a.GetSquaredDistance( b );
         totaldistance += distance;
         b.x += 1;
         count++;
      }
      logfile.WriteLine( "testgetsquareddistance count: " + count );            
   }

   void testismobile() {
      int count = 0;
      long start = System.currentTimeMillis();
      long finish = start + 1000;
      Float3 a = new Float3( 1000.3353f, 125.25f, 2353.35f );
      Float3 b = new Float3( 3550.3353f, 130.25f, 3235.35f );
      long totaldistance = 0;
      UnitDefHelp unitDefHelp = playerObjects.getUnitDefHelp();
      List<UnitDef> unitdefs = aicallback.getUnitDefs();
      while( ( (byte)count != 0 ) || ( System.currentTimeMillis() < finish ) ) {
         float distance = a.GetSquaredDistance( b );
         totaldistance += distance;
         b.x += 1;
         UnitDef unitdef = unitdefs.get( count % 100 );
         unitDefHelp.IsMobile( unitdef );
         count++;
      }
      logfile.WriteLine( "testismobile count: " + count );                  
   }

   void testunithash() {
      int count = 0;
      long start = System.currentTimeMillis();
      long finish = start + 1000;
      Unit unit1 = aicallback.getFriendlyUnits().get( 0 );
      Unit unit2 = aicallback.getFriendlyUnits().get( 1 );
      while( ( (byte)count != 0 ) || ( System.currentTimeMillis() < finish ) ) {
         int hashcode = unit1.hashCode();
         count++;
      }
      logfile.WriteLine( "testunithash count: " + count );                  
   }
}
