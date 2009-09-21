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

package hughai;

import java.util.*;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.basictypes.*;
import hughai.unitdata.*;
import hughai.utils.*;


// wraps giveorder, registers command in UnitCommandCache,
// carries out any statistical analysis for debugging, performance analysis
// abstract order interface somewhat
public class GiveOrderWrapper
{
   CSAI csai;
   OOAICallback aicallback;
   LogFile logfile;
   BuildTable buildTable;
   TimeHelper timeHelper;
   UnitCommandCache unitCommandCache;
   DrawingUtils drawingUtils;

   public static class CommandInfo
   {
      public TimeHelper.GameTimePoint datetime;
      public OOPCommands.OOPCommand command;
      public CommandInfo(TimeHelper.GameTimePoint datetime, OOPCommands.OOPCommand command)
      {
         this.datetime = datetime;
         this.command = command;
      }
   }

   int lastcommandssentresetframe = 0;
   ArrayList<CommandInfo> recentcommands = new ArrayList<CommandInfo>(); // for debugging, logging
   ArrayList<CommandInfo> allcommands = new ArrayList<CommandInfo>();

   GiveOrderWrapper( PlayerObjects playerObjects )
   {
      csai = playerObjects.getCSAI();
      aicallback = csai.aicallback;
      logfile = playerObjects.getLogFile();
      buildTable = playerObjects.getBuildTable();
      timeHelper = playerObjects.getTimeHelper();
      unitCommandCache = playerObjects.getUnitCommandCache();
      drawingUtils = playerObjects.getDrawingUtils();

      csai.registerGameListener( new GameListener() );
      //csai.TickEvent += new CSAI.TickHandler(csai_TickEvent);
      if (csai.DebugOn)
      {
         csai.RegisterVoiceCommand("dumporders", new DumpOrdersHandler() );
      }
   }

   class GameListener extends GameAdapter {
      @Override
      public void Tick(int frame)
      {
         // if (frame - lastcommandssentresetframe >= 30)
         // {
         lastcommandssentresetframe = frame;
         if (csai.DebugOn)
         {
            //  DumpCommandsSentStats();
         }
         //}
      }
   }

   public class DumpOrdersHandler implements VoiceCommandHandler {
      public void commandReceived(String cmd, String[] split, int player)
      {
         logfile.WriteLine("Command history:");
         synchronized (allcommands)
         {
            HashMap<Class<?>, Integer> CountByType = new HashMap<Class<?>, Integer>();
            for (CommandInfo commandinfo : allcommands)
            {
               logfile.WriteLine( "" + commandinfo.datetime + ": " + commandinfo.command.toString());
               if (!CountByType.containsKey(commandinfo.command.getClass()))
               {
                  CountByType.put(commandinfo.command.getClass(), 1);
               }
               else
               {
                  int previouscount = CountByType.get( commandinfo.command.getClass() );
                  int newcount = previouscount + 1;
                  CountByType.put(commandinfo.command.getClass(), newcount );
               }
            }

            logfile.WriteLine("Command stats");
            for( Class<?> thisclass : CountByType.keySet() )
               //foreach (KeyValuePair<Type, int> kvp in CountByType)
            {
               logfile.WriteLine( "" + thisclass + ": " + CountByType.get(thisclass) );
            }
         }
      }
   }

   void DumpCommandsSentStats()
   {
      logfile.WriteLine("recent commands:");
      synchronized (recentcommands)
      {
         for (CommandInfo commandinfo : recentcommands)
         {
            logfile.WriteLine(commandinfo.datetime.toString() + ": " + commandinfo.command.toString());
         }
         recentcommands.clear();
      }
   }

   public void BuildUnit(Unit builder, String targetunitname )
   {
      UnitDef unitdeftobuild = buildTable.getUnitDefByName( targetunitname.toLowerCase() );
      GiveOrder( new OOPCommands.BuildCommand( builder, unitdeftobuild, targetunitname ) );
   }

   public void BuildUnit(Unit builder, String targetunitname, TerrainPos pos)
   {
      UnitDef targetunitdef = buildTable.getUnitDefByName( targetunitname.toLowerCase() );
      drawingUtils.DrawUnit( targetunitname, pos, 0, 100, 
            aicallback.getTeamId(), true, true );
      GiveOrder( new OOPCommands.BuildCommand( builder, targetunitdef, pos, targetunitname ) );
   }

   public void MoveTo(Unit unit, TerrainPos pos)
   {
//      logfile.WriteLine( "MoveTo " + unit.getUnitId() + " " + unit.getDef().getHumanName() );
      GiveOrder( new OOPCommands.MoveToCommand( unit, pos ) );
   }

   public void Guard(Unit unit, Unit unittoguard)
   {
      logfile.WriteLine( "giveorder.guard" + unit.getUnitId() + " " + unit.getDef().getHumanName() );
      GiveOrder(new OOPCommands.GuardCommand(unit, unittoguard));
   }

   public void Attack(Unit unit, Unit unittoattack)
   {
      GiveOrder( new OOPCommands.AttackCommand( unit, new OOPCommands.UnitTarget( unittoattack ) ) );
   }

   public void Attack(Unit unit, TerrainPos pos)
   {
      GiveOrder( new OOPCommands.AttackCommand( unit, new OOPCommands.PositionTarget( pos ) ) );
   }

   public void SelfDestruct(Unit unit)
   {
      GiveOrder(new OOPCommands.SelfDestructCommand(unit));
   }

   public void Stop(Unit unit)
   {
      GiveOrder(new OOPCommands.StopCommand(unit));
   }

   public void Reclaim(Unit unit, TerrainPos pos, double radius)
   {
      GiveOrder(new OOPCommands.ReclaimCommand(unit, pos, radius));
   }

   void GiveOrder( OOPCommands.OOPCommand command)
   {
      unitCommandCache.RegisterCommand( command.UnitToReceiveOrder, command);
//      logfile.WriteLine("GiveOrder " + command.toString());
      synchronized (recentcommands)
      {
         TimeHelper.GameTimePoint gametime = timeHelper.GetGameTimePoint();
         recentcommands.add( new CommandInfo( gametime, command) );
         allcommands.add(new CommandInfo(gametime, command));
      }
      if (aicallback.getEngine().handleCommand(
            com.springrts.ai.AICommandWrapper.COMMAND_TO_ID_ENGINE,
            -1, 
            command.ToSpringCommand()) == -1 )
      {
         throw new RuntimeException( "GiveOrder failed");
      }
   }
}

