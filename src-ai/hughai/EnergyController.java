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

import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.basictypes.*;
import hughai.utils.*;


// manages power
public class EnergyController
{
	CSAI csai;
	OOAICallback aicallback;
	LogFile logfile;
	ResourceManager resourceManager;

	EnergyController( PlayerObjects playerObjects )
	{
		csai = playerObjects.getCSAI();
		aicallback = csai.aicallback;
		logfile = playerObjects.getLogFile();
		resourceManager = playerObjects.getResourceManager();
	}

	// looks at current power usage, and requirements for unit, and decides if we can build it without stalling
	// assumes nothing else suddenly starting at same time...
	// do something more elegant later
	public boolean CanBuild( UnitDef constructordef, UnitDef targetdef )
	{
		Resource energy = resourceManager.energyResource;
		float energycost = targetdef.getCost(energy);
		float currentenergy = resourceManager.getCurrentEnergy();
		float energyincome = aicallback.getEconomy().getIncome(energy);
		float energyusage = aicallback.getEconomy().getUsage(energy);

	      float constructorbuildspeed = constructordef.getBuildSpeed();

		float energyupkeep = targetdef.getUpkeep(energy);
		float energymake = targetdef.getMakesResource(energy);

		float actualbuildtime = targetdef.getBuildTime() / constructorbuildspeed;
	        logfile.WriteLine( "constructor build speed: " + constructorbuildspeed +
	              " targetbuildtime: " + targetdef.getBuildTime()
	                + " actual buildtime: " + actualbuildtime );

		logfile.WriteLine("energycontroller.canbuild(" + targetdef.getHumanName() + ") " 
		      + " energy upkeep: " + energyupkeep 
		      + " energymake " + energymake);
		if (energyincome < energyupkeep)
		{
			return false;
		}
		float excessenergyrequired = ( energycost - currentenergy );
		float oursurpluspower = energyincome - energyusage;
		
		float energycreatedwhilstbuilding = actualbuildtime * oursurpluspower;
		
		// logfile.WriteLine( "Out income: " + aicallback.GetEnergyIncome() + " usage: " + aicallback.GetEnergyUsage() + " surplus: " + oursurplus );
//		boolean result =  excesspowerrequired < oursurplus;
		boolean result = ( energycreatedwhilstbuilding * 0.8 ) > excessenergyrequired;
		//logfile.WriteLine( "Current energy: " + aicallback.GetEnergy() + " itemenergycost: " + def.energyCost + " buildtime: " + def.buildTime + " Excesspowerrequired: " + excesspowerrequired + " overall: " + result );
		return result;
	}
}
