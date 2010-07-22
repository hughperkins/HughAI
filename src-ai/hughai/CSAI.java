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

package hughai;

import java.util.*;
import java.io.*;

import com.springrts.ai.*;
import com.springrts.ai.command.*;
import com.springrts.ai.oo.*;

import hughai.ui.MainUI;
import hughai.utils.*;
import hughai.utils.TimeHelper.TimeSpan;
import hughai.loader.*;
import hughai.loader.utils.*;
import hughai.test.*;


// main AI class, that drives everything else
public class CSAI extends AbstractOOAI implements IHughAI
{
   // make these available for public access to other classes, though we can get them directly through GetInstance too
   public OOAICallback aicallback;
   //public LogFile logfile;

   public PlayerObjects playerObjects;

   TimeHelper timeHelper;
   LogFile logfile;
   DrawingUtils drawingUtils;
   Config config;

   Tester tester;
   Debugging debugging;

   //	TransLoadStorage transLoadStorage;
   //	
   //	@Override
   //	public void setStorage( TransLoadStorage storage ) {
   //	   this.transLoadStorage = storage;
   //	}

   public final String AIVersion = "0.0012";
   //public String AIDirectoryPath = "AI/CSAI";

   public String getAIDirectoryPath(){
      return aicallback.getDataDirs().getWriteableDir();
   }

   public String getCacheDirectoryPath()
   {
      return getAIDirectoryPath() + "cache" + File.separator;
   }

   private static final int DEFAULT_ZONE = 0;

   public int handleEngineCommand(AICommand command) {
      return aicallback.getEngine().handleCommand(
            com.springrts.ai.AICommandWrapper.COMMAND_TO_ID_ENGINE,
            -1, command);
   }
   public void sendTextMessage(String msg) {

      SendTextMessageAICommand msgCmd
      = new SendTextMessageAICommand(msg, DEFAULT_ZONE);
      handleEngineCommand(msgCmd);
   }

   public int Team;

   public boolean DebugOn = false;

   //int reference;

   HughAILoader hughAILoader;

   public HughAILoader getLoader() {
      return hughAILoader;
   }

   @Override
   public void setHughAILoader( HughAILoader hughAILoader) {
      this.hughAILoader = hughAILoader;
   }

   public CSAI()
   {
      System.out.println("CSAI constructor.");
      //  reference = new Random().Next(); // this is just to confirm that GetInstance retrieves a different instance for each AI
   }

   ArrayList<GameAdapter> gameListeners = new ArrayList<GameAdapter>();

   public void registerGameListener( GameAdapter gameListener ) {
      logfile.WriteLine( "registerGameListener " + gameListener.getClass().getEnclosingClass().getSimpleName() );
      gameListeners.add(gameListener);
   }
   
   public void unregisterGameListener( GameAdapter gameListener ) {
      GameAdapter toremove = null;
      for( GameAdapter listener : gameListeners ) {
         if( listener.getClass() == gameListener.getClass() ) {
            toremove = listener;
         }
      }
      gameListeners.remove(toremove );
   }

   @Override
   public int init( int team, OOAICallback aicallback )
   {
      this.aicallback = aicallback;

      try{
         sendTextMessage("ai " + team + " initing..");
         this.Team = team;
         //logfile = new LogFile();

         playerObjects = new PlayerObjects( this );

         logfile = playerObjects.getLogFile();
         logfile.Init( getAIDirectoryPath() + "team" + team + ".log" );
         logfile.writeLine( "Opening log" );
         logfile.WriteLine( "Hugh AI started v" + AIVersion + ", team " + team + 
               " map " + aicallback.getMap().getName() + " mod " + aicallback.getMod().getHumanName() );

         //         if( new File( getAIDirectoryPath() + File.separator + "debug.flg" ).exists() ) // if this file exists, activate debug mode; saves manually changing this for releases
         config = playerObjects.getConfig();
         
         playerObjects.getOptionsFromStartScript();
         
         if( config.isDebug() ) {
            logfile.WriteLine( "Toggling debug on" );
            DebugOn = true;
         }

         if( DebugOn )
         {
            //new Testing.RunTests().Go();
         }

         InitCache();

         logfile.WriteLine("Is game paused? : " + aicallback.getGame().isPaused());
         
         playerObjects.getFrameController().Init();

         drawingUtils = playerObjects.getDrawingUtils();
         drawingUtils.CleanDrawing();
         timeHelper = playerObjects.getTimeHelper();
         
         playerObjects.getMainUI();
         playerObjects.getConfigDialog();
//         playerObjects.getWorkflowUI();
         playerObjects.getConsoleJava();
         playerObjects.getConsoleEcma();
                  
         playerObjects.getWelcomeMessages();
         
         playerObjects.getUnitController();
         playerObjects.getEnemyTracker();
         playerObjects.getMaps();
         
         playerObjects.getBuildEconomy();

         if( DebugOn ) {
            tester = new Tester( playerObjects );
            debugging = new Debugging( playerObjects );
         }

         RegisterVoiceCommand( "unpauseai", new VoiceUnpauseAI() );
         playerObjects.getMainUI().registerButton( "Reload AI", new ReloadAIButton() );

         SendTextMsg("Hugh AI initialized v" + AIVersion + ", team " + team);
      }
      catch( Exception e )
      {
     //    e.printStackTrace();
         logfile.WriteLine( "Exception: " + e.toString() + 
               " " + Formatting.exceptionToStackTrace( e ));
         SendTextMsg( "Exception: " + e.toString() );
         if ((playerObjects != null) && (playerObjects.getExceptionList() != null)) {
            playerObjects.getExceptionList().reportException( e );
         }
		 release(0);
         return 1;
      }

      return 0;
   }

   class ReloadAIButton implements MainUI.ButtonHandler {
      @Override
      public void go() {
         try {
            //sendTextMessage( ".hughai reload" );
            hughAILoader.triggerReload();
         } catch( Exception e ) {
            logfile.WriteLine( Formatting.exceptionToStackTrace( e ) );
            throw new RuntimeException( e );
         }
      }
   }

   class VoiceUnpauseAI implements VoiceCommandHandler {
      @Override
      public void commandReceived( String chatmessage, String[]splitchat, int team ){
         sendTextMessage( "unpausing AI" );
         aistopped = false;
      }
   }

   public interface ShutdownHandler {
      public void shutdown();
   }

   HashSet<ShutdownHandler> shutdownHandlers = new HashSet<ShutdownHandler>();
   public void registerShutdown( ShutdownHandler handler ) {
      logfile.WriteLine( "csai.registershutdownhandler " + handler.getClass().getEnclosingClass().getSimpleName() );
      shutdownHandlers.add( handler );
   }

   @Override
   public void Shutdown()
   {
      try {
         for( ShutdownHandler handler : shutdownHandlers ) {
            handler.shutdown();
         }
         playerObjects.dispose();
         logfile.Shutdown();
      } catch( Exception e ) {
         logfile.WriteLine( "Exception: " + e.toString() + 
               " " + Formatting.exceptionToStackTrace( e ));
         throw new RuntimeException( e );
      }
   }

   // int commander = 0;

   public void DebugSay(String message)
   {
      if( DebugOn )
      {
         // sendTextMessage( message );
         logfile.WriteLine(message);
      }
   }

   // creates cache directory, if doesnt exist
   void InitCache()
   {
      if( !new File( getCacheDirectoryPath() ).exists() )
      {
         new File( getCacheDirectoryPath() ).mkdirs();
      }
   }


   TimeSpan unitcreatedtime = new TimeSpan();
   TimeSpan unitfinishedtime = new TimeSpan();
   TimeSpan unitdestroyedtime = new TimeSpan();
   TimeSpan unitidletime = new TimeSpan();
   TimeSpan unitmovefailedtime = new TimeSpan();
   TimeSpan ticktime = new TimeSpan();
   @Override
   public int unitCreated(Unit unit, Unit builder)									//called when a new unit is created on ai team
   {
      logfile.WriteLine("UnitCreated()");
      TimeHelper.RealTimePoint start = timeHelper.GetRealTimePoint();
      try
      {
         //UnitDef unitdef = unit.getDef();
         for( GameListener listener : gameListeners ) {
            listener.UnitCreated(unit, builder);
         }
      }
      catch( Exception e )
      {
         logfile.WriteLine( "Exception: " + e.toString() + 
               " " + Formatting.exceptionToStackTrace( e ));
//       logfile.WriteLine( "Exception: " + e.toString() );
         SendTextMsg("Exception: " + e.toString());
         if ((playerObjects != null) && (playerObjects.getExceptionList() != null)) {
            playerObjects.getExceptionList().reportException( e );
         }
         return 1;
      }
      unitcreatedtime.add( 
            timeHelper.CompareRealTimePoint( 
                  timeHelper.GetRealTimePoint(), start ) );

      return 0;
   }

   @Override
   public int unitFinished(Unit unit)								//called when an unit has finished building
   {
      TimeHelper.RealTimePoint start = timeHelper.GetRealTimePoint();
      try
      {
         logfile.WriteLine( "csai.unitfinished " + unit.getUnitId() + 
               " " + unit.getDef().getHumanName() );
         //UnitDef unitdef = aicallback.getUnitDef(deployedunitid);
         for( GameListener listener : gameListeners ) {
            listener.UnitFinished(unit);
         }
      }
      catch( Exception e )
      {
         logfile.WriteLine( "Exception in csai.unitfinished: " + e.toString() );
         logfile.WriteLine( Formatting.exceptionToStackTrace( e ) );
//       logfile.WriteLine( "Exception: " + e.toString() );
         if ((playerObjects != null) && (playerObjects.getExceptionList() != null)) {
            playerObjects.getExceptionList().reportException( e );
         }
         return 1;
      }
      unitfinishedtime.add( 
            timeHelper.CompareRealTimePoint( 
                  timeHelper.GetRealTimePoint(), start ) );

      return 0;
   }

   @Override
   public int unitDestroyed(Unit unit,Unit attacker)								//called when a unit is destroyed
   {
      TimeHelper.RealTimePoint start = timeHelper.GetRealTimePoint();
      try
      {
         for( GameListener listener : gameListeners ) {
            listener.UnitDestroyed( unit, attacker );
         }
      }
      catch( Exception e )
      {
         logfile.WriteLine( Formatting.exceptionToStackTrace( e ) );
//         logfile.WriteLine( "Exception: " + e.toString() );
         SendTextMsg("Exception: " + e.toString());
         if ((playerObjects != null) && (playerObjects.getExceptionList() != null)) {
            playerObjects.getExceptionList().reportException( e );
         }
         return 1;
      }
      unitdestroyedtime.add( 
            timeHelper.CompareRealTimePoint( 
                  timeHelper.GetRealTimePoint(), start ) );
      return 0;
   }        

   @Override
   public int unitIdle(Unit unit )										//called when a unit go idle and is not assigned to any group
   {
      TimeHelper.RealTimePoint start = timeHelper.GetRealTimePoint();
      try
      {
         for( GameListener listener : gameListeners ) {
            listener.UnitIdle( unit );
         }
      }
      catch( Exception e )
      {
         logfile.WriteLine( Formatting.exceptionToStackTrace( e ) );
//         logfile.WriteLine( "Exception: " + e.toString() );
         SendTextMsg("Exception: " + e.toString());
         if ((playerObjects != null) && (playerObjects.getExceptionList() != null)) {
            playerObjects.getExceptionList().reportException( e );
         }
         return 1;
      }
      unitidletime.add( 
            timeHelper.CompareRealTimePoint( 
                  timeHelper.GetRealTimePoint(), start ) );
      return 0;
   }

   @Override
   public int unitDamaged(Unit damaged,Unit attacker,float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed )				//called when one of your units are damaged
   {
      try
      {
         for( GameListener listener : gameListeners ) {
            listener.UnitDamaged( damaged, attacker, damage, dir, weaponDef, paralyzed );
         }
      }
      catch( Exception e )
      {
         logfile.WriteLine( Formatting.exceptionToStackTrace( e ) );
         //logfile.WriteLine( "Exception: " + e.toString() );
         SendTextMsg("Exception: " + e.toString());
         if ((playerObjects != null) && (playerObjects.getExceptionList() != null)) {
            playerObjects.getExceptionList().reportException( e );
         }
         return 1;
      }
      return 0;
   }       

   //   HashSet<Unit> unitsmovefailed = new HashSet<Unit>();
   //  int lastframeforunitsmovefailed = 0;
   HashSet<Unit> unitmovesfailed = new HashSet<Unit>(); 
   @Override
   public int unitMoveFailed(Unit unit)
   {
      try
      {
         TimeHelper.RealTimePoint start = timeHelper.GetRealTimePoint();
         //      if( aicallback.getGame().getCurrentFrame() != lastframeforunitsmovefailed ) {
         //       lastframeforunitsmovefailed = aicallback.getGame().getCurrentFrame();
         //     unitsmovefailed = new HashSet<Unit>();
         //}
         //if( !unitsmovefailed.contains( unit )) {
         // unitsmovefailed.add( unit );
         //            logfile.WriteLine( "move failed for " + unit.getUnitId() + " " 
         //                  + unit.getDef().getHumanName() );
         // playerObjects.getGiveOrderWrapper().Stop( unit );
         unitmovesfailed.add( unit );
         for( GameListener listener : gameListeners ) {
            listener.UnitMoveFailed( unit );
         }
         unitmovefailedtime.add( 
               timeHelper.CompareRealTimePoint( 
                     timeHelper.GetRealTimePoint(), start ) );
      }
      catch( Exception e )
      {
         logfile.WriteLine( Formatting.exceptionToStackTrace( e ) );
       //  logfile.WriteLine( "Exception: " + e.toString() );
         SendTextMsg("Exception: " + e.toString());
         if ((playerObjects != null) && (playerObjects.getExceptionList() != null)) {
            playerObjects.getExceptionList().reportException( e );
         }
         return 1;
      }
      return 0;
   }       

   @Override
   public int enemyEnterLOS(Unit enemy)
   {
      try
      {
         for( GameListener listener : gameListeners ) {
            listener.EnemyEnterLOS( enemy );
         }
      }
      catch( Exception e )
      {
         logfile.WriteLine( Formatting.exceptionToStackTrace( e ) );
//       logfile.WriteLine( "Exception: " + e.toString() );
         SendTextMsg("Exception: " + e.toString());
         if ((playerObjects != null) && (playerObjects.getExceptionList() != null)) {
            playerObjects.getExceptionList().reportException( e );
         }
         return 1;
      }
      return 0;
   }

   @Override
   public int enemyLeaveLOS(Unit enemy)
   {
      try
      {
         for( GameListener listener : gameListeners ) {
            listener.EnemyLeaveLOS( enemy );
         }
      }
      catch( Exception e )
      {
         logfile.WriteLine( Formatting.exceptionToStackTrace( e ) );
//       logfile.WriteLine( "Exception: " + e.toString() );
         SendTextMsg("Exception: " + e.toString());
         if ((playerObjects != null) && (playerObjects.getExceptionList() != null)) {
            playerObjects.getExceptionList().reportException( e );
         }
         return 1;
      }
      return 0;
   }

   @Override
   public int enemyEnterRadar(Unit enemy)		
   {
      try
      {
//         sendTextMessage("enemy entered radar: " + enemy );
         logfile.WriteLine("enemy entered radar: " + enemy);
         for( GameListener listener : gameListeners ) {
            listener.EnemyEnterRadar(enemy);
         }
      }
      catch( Exception e )
      {
         logfile.WriteLine( Formatting.exceptionToStackTrace( e ) );
//       logfile.WriteLine( "Exception: " + e.toString() );
         SendTextMsg("Exception: " + e.toString());
         if ((playerObjects != null) && (playerObjects.getExceptionList() != null)) {
            playerObjects.getExceptionList().reportException( e );
         }
         return 1;
      }
      return 0;
   }

   @Override
   public int enemyLeaveRadar(Unit enemy)	
   {
      try
      {
         for( GameListener listener : gameListeners ) {
            listener.EnemyLeaveRadar(enemy);
         }
      }
      catch( Exception e )
      {
         logfile.WriteLine( Formatting.exceptionToStackTrace( e ) );
//       logfile.WriteLine( "Exception: " + e.toString() );
         SendTextMsg("Exception: " + e.toString());
         if ((playerObjects != null) && (playerObjects.getExceptionList() != null)) {
            playerObjects.getExceptionList().reportException( e );
         }
         return 1;
      }
      return 0;
   }

   @Override
   public int enemyDamaged(Unit damaged,Unit attacker,float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed ) //called when an enemy inside los or radar is damaged
   {
      return 0;
   }

   @Override
   public int enemyDestroyed(Unit enemy,Unit attacker)						//will be called if an enemy inside los or radar dies (note that leave los etc will not be called then)
   {
      try
      {
         for( GameListener listener : gameListeners ) {
            listener.EnemyDestroyed( enemy, attacker );
         }
      }
      catch( Exception e )
      {
         logfile.WriteLine( Formatting.exceptionToStackTrace( e ) );
//         logfile.WriteLine( "Exception: " + e.toString() );
         SendTextMsg("Exception: " + e.toString());
  //       aistopped = false;
         if ((playerObjects != null) && (playerObjects.getExceptionList() != null)) {
            playerObjects.getExceptionList().reportException( e );
         }
         return 1;
      }
      return 0;
   }

   @Override
   public int message( int player, String msg )					//called when someone writes a chat msg
   {
      try
      {
         if( msg.toLowerCase().indexOf( ".hughai" ) == 0 )
         {
            boolean commandfound = false;
            if( msg.length() > 9
                  &&  ( msg.toLowerCase().substring( 7, 8 ).equals( "*" ) 
                        || msg.toLowerCase().substring( 7, 8 ).equals( " " ) 
                        || msg.toLowerCase().substring( 7, 8 ).equals( "" + Team ) ) )
            {
               logfile.WriteLine( msg.substring( 7,8 ) );
               String[] splitchatline = msg.split( " " );
               String command = splitchatline[1].toLowerCase();
               for (VoiceCommandInfo info : VoiceCommands)
               {
                  if (info.command.equals( command ) )
                  {
                     info.handler.commandReceived(msg, splitchatline, player);
                     commandfound = true;
                  }
               }
            }
            if( !commandfound )
            {
               String helpString = "HughAI commands available: help, ";
               for( VoiceCommandInfo info : VoiceCommands )
               {
                  helpString += info.command + ", ";
               }
               sendTextMessage( helpString );
               sendTextMessage( "Example: .hughai" + Team + " showmetalspots" );
            }
         }
      }
      catch( Exception e )
      {
         logfile.WriteLine( Formatting.exceptionToStackTrace( e ) );
         logfile.WriteLine( "Exception: " + e.toString() );
         SendTextMsg("Exception: " + e.toString());
         if ((playerObjects != null) && (playerObjects.getExceptionList() != null)) {
            playerObjects.getExceptionList().reportException( e );
         }
//         aistopped = false;
         return 1;
      }
      return 0;
   }

   boolean aistopped = false;
   public boolean getAIStopped() { return aistopped; }

   void DumpTimings()
   {
      logfile.WriteLine( "Timings:" );
      logfile.WriteLine( "unitcreatedtime: " + unitcreatedtime );
      logfile.WriteLine( "unitfinishedtime: " + unitfinishedtime );
      logfile.WriteLine( "unitidletime: " + unitidletime );
      logfile.WriteLine( "unitmovefailedtime: " + unitmovefailedtime );
      logfile.WriteLine( "ticktime: " + ticktime );
      unitcreatedtime = new TimeSpan();
      unitfinishedtime = new TimeSpan();
      unitidletime = new TimeSpan();
      unitmovefailedtime = new TimeSpan();
      ticktime = new TimeSpan();
      if( unitmovesfailed.size() > 0 ) {
         logfile.WriteLine( "moves failed for " + unitmovesfailed.size() + " units:" );
//         logfile.WriteLine( "moves failed for following units:" );
//         for( Unit unit : unitmovesfailed ) {
//            try {
//               logfile.WriteLine( "   move failed for: " + unit.getUnitId() + " " +
//                     playerObjects.getUnitController().getUnitDef( unit ).getHumanName() );
//            } catch( Exception e ) {
//               logfile.WriteLine( "   move failed for: unknown unit" );            
//            }
//         }
      }
   }

   //called every frame
   int lastdebugframe = 0;
   public int currentFrame;
   @Override
   public int update(int frame)
   {
      try
      {
         //sendTextMessage("csai.update");
         //if( frame % 30 == 0 ){ sendTextMessage( "csai.update()" ); }
         TimeHelper.RealTimePoint start = timeHelper.GetRealTimePoint();
         this.currentFrame = frame;
         if (!aicallback.getGame().isPaused() && !aistopped
               && ( ( frame % config.getTickFrameInterval() ) == 0 ) ) {
//            logfile.WriteLine( "Tick" );
            for( GameAdapter listener : gameListeners ) {
               long startmilliseconds = System.currentTimeMillis();
               listener.overriden = true;
               listener.Tick(frame);
               long time = System.currentTimeMillis() - startmilliseconds;
               if( listener.overriden ) {
//                  logfile.WriteLine( "   " + listener.getClass().getEnclosingClass().getSimpleName() + ".Tick() : " + time + " ms" );
                  //                  if( time > 5 ) {
                  //                     logfile.WriteLine( "excessive tick: " + listener.getClass().getCanonicalName() + " " + time );
                  //                  }
               }
               if (DebugOn)
               {
                  //logfile.WriteLine("frame: " + aicallback.GetCurrentFrame());
                  if ( frame - lastdebugframe > 150)
                  {
                     DumpTimings();
                     lastdebugframe = frame;
                  }
               }
            }
            TimeSpan thisticktime = timeHelper.CompareRealTimePoint( 
                  timeHelper.GetRealTimePoint(), start );
            logfile.WriteLine( "Total tick time: " + thisticktime );
            ticktime.add( thisticktime );
         }
      }
      catch( Exception e )
      {
         logfile.WriteLine( Formatting.exceptionToStackTrace( e ) );
         logfile.WriteLine( "Exception: " + e.toString() );
         SendTextMsg("Exception: " + e.toString());
         if ((playerObjects != null) && (playerObjects.getExceptionList() != null)) {
            playerObjects.getExceptionList().reportException( e );
         }
//         if( config.isDebug() ) {
//            aistopped = true;
//            SendTextMsg("AI updates halted.  Say '.hughai unpauseai' to unhalt them." );
//         }
         return 1;
      }
      return 0;
   }    


   static class VoiceCommandInfo
   {
      public String command;
      public VoiceCommandHandler handler;
      public VoiceCommandInfo(String command, VoiceCommandHandler handler)
      {
         this.command = command;
         this.handler = handler;
      }
   }

   List<VoiceCommandInfo> VoiceCommands = new ArrayList<VoiceCommandInfo>();
   public void RegisterVoiceCommand( String commandString, VoiceCommandHandler handler )
   {
      commandString = commandString.toLowerCase();
      //if( !VoiceCommands.Contains( commandString ) && commandString != "help" )
      //{
      logfile.WriteLine("CSAI registering voicecommand " + commandString );
      VoiceCommands.add( new VoiceCommandInfo( commandString, handler ) );
      //}
   }
   public void UnregisterVoiceCommand( String commandString )
   {
      commandString = commandString.toLowerCase();
      //if( VoiceCommands.Contains( commandString ) )
      //{
      //  logfile.WriteLine("CSAI unregistering voicecommand " + commandString );
      // VoiceCommands.Remove( commandString );
      //}
   }

   public void SendTextMsg(String text )
   {
      sendTextMessage(text );
   }
}

