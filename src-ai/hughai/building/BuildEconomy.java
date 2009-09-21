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

package hughai.building;

import java.util.*;

import com.springrts.ai.oo.*;

import hughai.*;
import hughai.utils.*;
import hughai.basictypes.*;
import hughai.utils.*;

public class BuildEconomy
{
   CSAI csai;
   OOAICallback aicallback;
   Config config;
   Workflows workflows;
   //LogFile logfile;

   public double energymetalratio = 10.0;

   //	MobileFusionController mobile;

   public BuildEconomy(PlayerObjects playerObjects)
   {
      config = playerObjects.getConfig();
      workflows = playerObjects.getWorkflows();

      WorkflowController workflowController = new WorkflowController(playerObjects);
      //mobile = new MobileFusionController();

      //workflow.AddBuildUnitWeighting(0.2, 0.4, "armmex");
      //workflow.AddBuildUnitWeighting(0.2, 0.6, "armsolar");

      String energyextractorunitname = playerObjects.getBuildTree().listToOurTeamsUnitName( config.getBasicenergyextractorunitnames() );
      workflowController.AddEnergyUnit(energyextractorunitname);
      //workflow.AddEnergyUnit("armmfus");

      String metalextractorname = playerObjects.getBuildTree().listToOurTeamsUnitName( config.getBasicmetalextractorunitnames() );
      workflowController.AddMetalUnit(metalextractorname);

//      workflowController.BuildUnit(2.0, "armvp", 1);
//
//      workflowController.BuildUnit(2.1, "armfav", 2);
//      workflowController.BuildUnit(2.0, "armstump", 10);
//      workflowController.BuildUnit(2.0, "armsam", 10);
//      workflowController.BuildUnit(1.95, "armmex", 4);
//      workflowController.BuildUnit(1.95, "armsolar", 4);
//      workflowController.BuildUnit(1.9, "armcv", 3);
//      workflowController.BuildUnit(1.8, "armmstor", 1);
//      workflowController.BuildUnit(1.8, "armavp", 1);
//      workflowController.BuildUnit(2.0, "armbull", 3);
//      workflowController.BuildUnit(2.0, "armmart", 2);
//      workflowController.BuildUnit(1.9, "armseer", 1); // experimental
//      workflowController.BuildUnit(1.7, "armyork", 3);
//      workflowController.BuildUnit(1.7, "armbull", 3);
//      workflowController.BuildUnit(1.7, "armmart", 2);
//      workflowController.BuildUnit(1.0, "armmfus", 1);
//      workflowController.BuildUnit(0.9, "armacv", 2);
//      workflowController.BuildUnit(0.8, "armmmkr", 4);
//      workflowController.BuildUnit(0.8, "armarad", 1);
//      workflowController.BuildUnit(0.8, "armestor", 1);
//      workflowController.BuildUnit(0.8, "armmfus", 8);
//      workflowController.BuildUnit(0.7, "armalab", 1);
//      workflowController.BuildUnit(0.7, "armfark", 2);
//
//      workflowController.BuildUnit(0.6, "armbull", 20);
//      workflowController.BuildUnit(0.6, "armyork", 20);
//      workflowController.BuildUnit(0.6, "armmart", 20);
//      workflowController.BuildUnit(0.5, "armseer", 1);
//      workflowController.BuildUnit(0.5, "armsjam", 1);
//
//      workflowController.BuildUnit(0.4, "armmav", 50); // experimental
//      workflowController.BuildUnit(0.3, "armfark", 4); // experimental
//      //   workflow.BuildUnit(0.3, "armpeep", 3); // experimental
//      //  workflow.BuildUnit(0.3, "armap", 1); // experimental
//      //workflow.BuildUnit(0.2, "armbrawl", 50); // experimental
//      //workflow.BuildUnit(0.2, "armaap", 1); // experimental

      workflowController.Activate();
   }

}

