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
import java.util.Map;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.basictypes.*;
import hughai.mapping.*;
import hughai.packcoordinators.*;
import hughai.unitdata.*;
import hughai.utils.*;


public class MobileFusionController
{
   CSAI csai;
   OOAICallback aicallback;
   LogFile logfile;
   UnitController unitController;
   GiveOrderWrapper giveOrderWrapper;

   Collection<Unit> mobileunits = new ArrayList<Unit>();

   public MobileFusionController( PlayerObjects playerObjects )
   {
      csai = playerObjects.getCSAI();
      aicallback = csai.aicallback;
      logfile = playerObjects.getLogFile();
      unitController = playerObjects.getUnitController();
      giveOrderWrapper = playerObjects.getGiveOrderWrapper();

      unitController.registerListener( new UnitControllerHandler() );
//      UnitController.GetInstance().UnitAddedEvent += new UnitController.UnitAddedHandler(MobileFusionController_UnitAddedEvent);
//      UnitController.GetInstance().AllUnitsLoaded += new UnitController.AllUnitsLoadedHandler(MobileFusionController_AllUnitsLoaded);
   }

   class UnitControllerHandler extends UnitController.UnitAdapter {
      @Override
      public void AllUnitsLoaded()
      {
         for (Unit mobile : mobileunits)
         {
            // move them to nearest mex?
            TerrainPos targetpos = unitController.getPos( unitController.UnitsByName
                  .get("armmex").get(0) );                //aicallback.GiveOrder(mob, new Command(Command.CMD_PATROL, targetpos.ToDoubleArray()));
            giveOrderWrapper.MoveTo(mobile, targetpos);
         }
      }

      @Override
      public void UnitAdded( Unit unit )
      {
         if (unit.getDef().getName().toLowerCase() == "armmfus")
         {
            mobileunits.add( unit );
         }
      }

   }
}

