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

package hughai.controllers.level1;

import java.util.*;
import java.util.Map;


import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.*;
import hughai.basictypes.*;
import hughai.*;
import hughai.mapping.*;
import hughai.packcoordinators.*;
import hughai.unitdata.*;
import hughai.unitdata.UnitController.UnitAdapter;
import hughai.utils.*;
import hughai.controllers.level1.Reconnaissance.GameListener;
import hughai.controllers.level1.Reconnaissance.UnitListener;
import hughai.controllers.level2.*;

// level 1 controller for offense
// basically, by instantiating it, we are telling it:
// "go and attack stuff, with the units you feel are attack units.  Go!"
// the rest is up to it: targets and everything.
// that's what level 1 controllers are about ;-)
public class Offense {
   PlayerObjects playerObjects;
   LogFile logfile;
   Config config;
   BuildTable buildTable;
   UnitController unitController;
   
   Collection<UnitDef> managedUnitTypes = new HashSet<UnitDef>();
   Collection<Unit> managedunits = new HashSet<Unit>();

   TankController2 tankcontroller;
   
   public Offense( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
      this.logfile = playerObjects.getLogFile();
      this.config = playerObjects.getConfig();
      this.buildTable = playerObjects.getBuildTable();
      this.unitController = playerObjects.getUnitController();
   }

   public void Activate(){
      playerObjects.getCSAI().registerGameListener( new GameListener() );
      unitController.registerListener( new UnitListener() );
      
      List<String> unittypenames = config.getOffensiveunitnames();
      managedUnitTypes.clear();
      for( String unittypename : unittypenames ) {
         UnitDef unitdef = buildTable.getUnitDefByName( unittypename );
         managedUnitTypes.add( unitdef );
         logfile.WriteLine( "Allowed unit type for " + this.getClass().getSimpleName()
               + " " + unitdef.getHumanName() );
      }
      
      UnitDef typicalunitdef = buildTable.getUnitDefByName( config.getTypicallevel1tankunitdefname() );
      tankcontroller = new TankController2( playerObjects, typicalunitdef );
      tankcontroller.Activate();
      //scoutcontroller.AssignUnits( units )
   }
   
   void Reappraise( int frame ) {
      if( frame % 150 == 0) {
      }
   }
   
   class GameListener extends GameAdapter {
      @Override
      public void Tick( int frame ) {
//         if( frame % 30 == 0 ) {
            Reappraise( frame );
//         }
      }
   }
   
   class UnitListener extends UnitAdapter {
      @Override
      public void UnitAdded(Unit unit ) {
         UnitDef unitdef = unit.getDef();
         if( managedUnitTypes.contains( unitdef ) ) {
            logfile.WriteLine( "New offensive unit: " +
                  unit.getUnitId() + " " + unit.getDef().getHumanName() );
            managedunits.add( unit );
            tankcontroller.AssignUnits( 
                  Arrays.asList( new Unit[]{ unit } ) );
         }
      }

      @Override
      public void UnitRemoved( Unit unit ) {
         UnitDef unitdef = unit.getDef();
         if( managedUnitTypes.contains( unitdef ) ) {
            managedunits.remove( unit );
            tankcontroller.RevokeUnits(  
                  Arrays.asList( new Unit[]{ unit } ) );
         }
      }
   }   
}
