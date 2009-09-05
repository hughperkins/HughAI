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

package hughai.packcoordinators;

import java.util.*;


import com.springrts.ai.*;
import com.springrts.ai.oo.*;
import com.springrts.ai.oo.Map;

import hughai.CSAI;
import hughai.GiveOrderWrapper;
import hughai.PlayerObjects;
import hughai.VoiceCommandHandler;
import hughai.basictypes.*;
import hughai.*;
import hughai.mapping.LosHelper;
import hughai.mapping.Maps;
import hughai.utils.*;


// something that controls a group of units in a tactical way
// a group of military units, constructors get other controllers
// it's in a tactical way, rather than strategic, for example, one instance could be tasked
// with rushing as a pack towards a particular enemy laser tower
public abstract class PackCoordinator implements IPackCoordinator {
   Collection< Unit > unitsControlled = new HashSet<Unit>();

   CSAI csai;
   OOAICallback aicallback;
   LogFile logfile;
   DrawingUtils drawingUtils;
   GiveOrderWrapper giveOrderWrapper;
   Maps maps;
   LosHelper losHelper;

   Random random = new Random();
   
   String packcoordinatorname = "";

   boolean debugon = false;
   boolean activated = false; // not started until Activate or SetTarget is called        

   public PackCoordinator( PlayerObjects playerObjects ) {
//      this.unitsControlled = unitsControlled;
      csai = playerObjects.getCSAI();
      aicallback = csai.aicallback;
      logfile = playerObjects.getLogFile();
      drawingUtils = playerObjects.getDrawingUtils();
      giveOrderWrapper = playerObjects.getGiveOrderWrapper();
      maps = playerObjects.getMaps();
      losHelper = playerObjects.getLosHelper();
      
      packcoordinatorname = this.getClass().getSimpleName();

      debugon = csai.DebugOn;
      csai.RegisterVoiceCommand( "dumppacks", new DumpPacks() );
      logfile.WriteLine( this.getClass().getSimpleName() + " initialized." );
   }

   class DumpPacks implements VoiceCommandHandler {
      @Override
      public void commandReceived( String command, String[] args, int player ) {
         if( unitsControlled.size() > 0 ) {
            StringBuilder logline = new StringBuilder();
            logline.append( packcoordinatorname + ": size: " 
                  + unitsControlled.size() + " activated: " + activated
                  + " units: " );
            for( Unit unit : unitsControlled ) {
               logline.append( unit.getUnitId() + ":" + unit.getDef().getHumanName() +  " " );
            }
            logfile.WriteLine( "" + logline );
         }
      }
   }

   abstract void Recoordinate();

   @Override
   public void Activate()
   {
      if( !activated )
      {
         logfile.WriteLine( packcoordinatorname + " activating" );
         activated = true;
         restartedfrompause = true;
         Recoordinate();
      }
   }

   @Override
   public void Disactivate()
   {
      logfile.WriteLine( packcoordinatorname + " deactivating" );
      activated = false;
   }

   @Override
   public void AssignUnits( Collection<Unit> units ) {
      for( Unit unit : units ) {
         unitsControlled.add( unit );
         logfile.WriteLine( "New " + packcoordinatorname + " unit: " +
               unit.getUnitId() + " " + unit.getDef().getHumanName() );

      }
      Recoordinate();
   }

   @Override
   public void RevokeUnits( Collection<Unit> units ) {
      for( Unit unit : units ) {
         unitsControlled.remove( unit );
      }      
   }

   boolean restartedfrompause = true;
}
