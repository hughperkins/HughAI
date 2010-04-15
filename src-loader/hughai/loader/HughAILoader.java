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
// Dynamic AI loader, created by Hugh Perkins 2009
// This loader will load a jar file called "UnderlyingAI.jar", from the AI's directory.
// Whenever you say in in-game chat ".hughai reload", the ai will be reloaded.

package hughai.loader;

import hughai.loader.utils.*;

import java.io.*;
import java.lang.management.GarbageCollectorMXBean;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import java.text.*;
import java.util.logging.Formatter;

import com.springrts.ai.*;
import com.springrts.ai.command.*;
import com.springrts.ai.oo.*;


public class HughAILoader extends AbstractOOAI implements OOAI {
   final String underlyingJarFileName = "UnderlyingAI.jar";
   final String underlyingClassNameToLoad = "hughai.CSAI";
   final String reloadCommandString = ".hughai reload";

   private int teamId = -1;
   private Properties info = null;
   private Properties optionValues = null;
   private OOAICallback callback = null;
   private String myLogFile = null;
   private Logger log = null;

   private static final int DEFAULT_ZONE = 0;

   HughAILoader(int teamId, OOAICallback callback) {

      this.teamId = teamId;
      this.callback = callback;
   }

   private int handleEngineCommand(AICommand command) {
      return callback.getEngine().handleCommand(
            com.springrts.ai.AICommandWrapper.COMMAND_TO_ID_ENGINE,
            -1, command);
   }
   private int sendTextMsg(String msg) {

      SendTextMessageAICommand msgCmd
      = new SendTextMessageAICommand(msg, DEFAULT_ZONE);
      return handleEngineCommand(msgCmd);
   }

   @Override
   public int init(int teamId, OOAICallback callback) {
      this.teamId = teamId;
      this.callback = callback;

      try {
         loadUnderlyingAI();
         //((StorageUser)underlyingAI).setStorage( TransLoadStorage.getInstance() );

         if( underlyingAI != null ) {
            underlyingAI.init( teamId, callback );
         }
      } catch( Exception e ) {
         e.printStackTrace();
		 release(0);
		 return 2;
      }

      return 0;
   }
   
   @Override
   public int release( int reason ) {

      try {
         if( underlyingAI != null ) {
            underlyingAI.Shutdown();
		 }
         underlyingAI = null;
         System.runFinalization();
         System.gc();
         System.gc();
      } catch( Exception e ) {
         e.printStackTrace();
		 return 2;
      }

      return 0;
   }

   @Override
   public int update(int frame) {

      try {
//         if( doReloadAI ) {
//            doReloadAI();
//            doReloadAI = false;
//         } else {
            if( underlyingAI != null ) {
               return underlyingAI.update(frame);
            }
//         }
      } catch( Exception e ) {
         e.printStackTrace();
		 return 2;
      }
      return 0;
   }

   IHughAI underlyingAI;

   void loadUnderlyingAI() throws Exception {
      // String path = message.split(" ")[1];
      String path = callback.getDataDirs().getConfigDir() + new String( new byte[]{ (byte)(callback.getDataDirs().getPathSeparator() ) } )
      + underlyingJarFileName;
      //this.getClass().
      sendTextMsg("Reloading ai from " + path + " ...");
      // File pluginFile = new File(callback.getDataDirs() + "/UnderlyingAI.jar");
      File pluginFile = new File(path);
      URL[] locations = new URL[] {pluginFile.toURI().toURL()};
      System.out.println("about to load AI...");
      underlyingAI = hughai.loader.utils.Loader.loadOOAI(locations, underlyingClassNameToLoad );
      underlyingAI.setHughAILoader( this );
   }
   
   String longToMeg( long value ) {
      float valueasmeg = value / 1024f / 1024f;
      return "" + valueasmeg + "MB";
   }
   
   void dumpSystemStats() {
      Runtime runtime = Runtime.getRuntime();
      System.out.println( "total: " + longToMeg(runtime.totalMemory() ) +
            " free: " + longToMeg( runtime.freeMemory() )
            + " used: " + longToMeg( runtime.totalMemory() - runtime.freeMemory() ) );      
   }

   void doReloadAI() throws Exception {
      sendTextMsg( "Reloading ai " + teamId + " ..." );
      underlyingAI.Shutdown();
      underlyingAI = null;
      System.runFinalization();
      System.gc();
      System.gc();
      dumpSystemStats();
      loadUnderlyingAI();
      underlyingAI.init( teamId, callback );      
      dumpSystemStats();
   }

   @Override
   public int message(int player, String message) {
      sendTextMsg( "You said: " + message );
      try {
         if( message.equals(reloadCommandString) ) {
            doReloadAI();
         }
         if( underlyingAI != null ) {
            underlyingAI.message( player, message );
         }
      } catch( Exception e ) {
         e.printStackTrace();
		 return 2;
      }
      if( message.equals(".exception")) {
         throw new RuntimeException("generated exception"); 
      }
      return 0;
   }
   
   class ReloadAIThread implements Runnable {
      @Override
      public void run() {
         try {
         Thread.sleep( 100 ); // give triggerReload method time to exit first
         doReloadAI();
         } catch( Exception e ) {
            e.printStackTrace();
            throw new RuntimeException( e );
         }
      }
   }
   
   Thread reloadThread;
   
//   boolean doReloadAI = false;
   public void triggerReload() throws Exception {
//      doReloadAI = true;
//      doReloadAI();
      reloadThread = new Thread( new ReloadAIThread() );
      reloadThread.start();
   }

   @Override
   public int unitCreated(Unit unit, Unit builder) {
      try {
         if( underlyingAI != null ) {
            return underlyingAI.unitCreated( unit, builder );
         }
      } catch( Exception e ) {
         e.printStackTrace();
		 return 2;
      }
      return 0;
   }

   @Override
   public int unitFinished(Unit unit) {
      try {
         if( underlyingAI != null ) {
            return underlyingAI.unitFinished( unit );
         }
      } catch( Exception e ) {
         e.printStackTrace();
		 return 2;
      }
      return 0;
   }

   @Override
   public int unitIdle(Unit unit) {
      try {
         if( underlyingAI != null ) {
            return underlyingAI.unitIdle( unit );
         }
      } catch( Exception e ) {
         e.printStackTrace();
		 return 2;
      }
      return 0;
   }

   @Override
   public int unitMoveFailed(Unit unit) {
      try {
         if( underlyingAI != null ) {
            return underlyingAI.unitMoveFailed( unit );
         }
      } catch( Exception e ) {
         e.printStackTrace();
		 return 2;
      }
      return 0;
   }

   @Override
   public int unitDamaged(Unit unit, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzer ) {
      try {
         if( underlyingAI != null ) {
            return underlyingAI.unitDamaged( unit, attacker, damage, dir, weaponDef, paralyzer );
         }
      } catch( Exception e ) {
         e.printStackTrace();
		 return 2;
      }
      return 0;
   }

   @Override
   public int unitDestroyed(Unit unit, Unit attacker) {
      try {
         if( underlyingAI != null ) {
            return underlyingAI.unitDestroyed( unit, attacker );
         }
      } catch( Exception e ) {
         e.printStackTrace();
		 return 2;
      }
      return 0;
   }

   @Override
   public int unitGiven(Unit unit, int oldTeamId, int newTeamId) {
      try {
         if( underlyingAI != null ) {
            return underlyingAI.unitGiven( unit, oldTeamId, newTeamId );
         }
      } catch( Exception e ) {
         e.printStackTrace();
		 return 2;
      }
      return 0;
   }

   @Override
   public int unitCaptured(Unit unit, int oldTeamId, int newTeamId) {
      try {
         if( underlyingAI != null ) {
            return underlyingAI.unitCaptured( unit, oldTeamId, newTeamId );
         }
      } catch( Exception e ) {
         e.printStackTrace();
		 return 2;
      }
      return 0;
   }

   @Override
   public int enemyEnterLOS(Unit enemy) {
      try {
         if( underlyingAI != null ) {
            return underlyingAI.enemyEnterLOS( enemy );
         }
      } catch( Exception e ) {
         e.printStackTrace();
		 return 2;
      }
      return 0;
   }

   @Override
   public int enemyLeaveLOS(Unit enemy) {
      return 0; // signaling: OK
   }

   @Override
   public int enemyEnterRadar(Unit enemy) {
      return 0; // signaling: OK
   }

   @Override
   public int enemyLeaveRadar(Unit enemy) {
      return 0; // signaling: OK
   }

   @Override
   public int enemyDamaged(Unit enemy, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzer ) {
      return 0; // signaling: OK
   }

   @Override
   public int enemyDestroyed(Unit enemy, Unit attacker) {
      return 0; // signaling: OK
   }

   @Override
   public int weaponFired(Unit unit, WeaponDef weaponDef) {
      return 0; // signaling: OK
   }

   @Override
   public int playerCommand(List<Unit> units, AICommand command, int playerId) {
      try {
         if( underlyingAI != null ) {
            return underlyingAI.playerCommand( units, command, playerId );
         }
      } catch( Exception e ) {
         e.printStackTrace();
		 return 2;
      }
      return 0;
   }

   @Override
   public int commandFinished(Unit unit, int commandId, int commandTopicId) {
      try {
         if( underlyingAI != null ) {
            return underlyingAI.commandFinished( unit, commandId, commandTopicId );
         }
      } catch( Exception e ) {
         e.printStackTrace();
		 return 2;
      }
      return 0;
   }

   @Override
   public int seismicPing(AIFloat3 pos, float strength) {
      return 0; // signaling: OK
   }

}
